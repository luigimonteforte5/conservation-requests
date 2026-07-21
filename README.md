# Conservation Requests

API REST (Spring Boot, Java 25) per la gestione dei **metadati** di richieste di conservazione documentale: il servizio traccia richieste e documenti, **non** il contenuto dei file. Ogni richiesta arriva da un produttore (`producerId` + `externalId`, univoci in coppia), porta uno o più documenti e attraversa un ciclo di stati:

```
RECEIVED ──▶ VALIDATED ──▶ COMPLETED
   └───────▶ REJECTED
```

## Stack

Java 25 · Spring Boot 4.1 · Spring Web · Spring Data JPA · Spring Security (JWT) · Bean Validation · MapStruct · Liquibase · PostgreSQL · springdoc-openapi.

## Avvio rapido

```bash
cp .env.example .env             # valorizza le variabili richieste (vedi SETUP.md)
docker-compose -f compose.yaml up -d
./mvnw clean verify              # build + test
./mvnw spring-boot:run
```

Documentazione OpenAPI a runtime: `/swagger-ui.html` · `/v3/api-docs`.

## API

Base path `/api/v1/requests`. Autenticazione JWT: login su `POST /api/v1/auth/login`, poi `Authorization: Bearer <token>`.

| Metodo | Path | Descrizione |
|---|---|---|
| `POST` | `/` | Crea una richiesta con i suoi documenti (header `Location`) |
| `GET` | `/` | Lista paginata, filtrabile per `producerId` e `status` |
| `GET` | `/{id}` | Dettaglio |
| `GET` | `/{id}/history` | Storico delle transizioni di stato |
| `PATCH` | `/{id}/validate` · `/reject` · `/complete` | Transizioni di stato |

Errori coerenti come `ProblemDetail` (RFC 9457): `400` validazione · `401` non autenticato · `404` inesistente · `409` duplicato o transizione non ammessa.

## Scelte di progettazione

- **A livelli** Controller · Service · Repository, con DTO (record) separati dalle entity e mapping MapStruct.
- **Macchina a stati** centralizzata in `Status.canTransitionTo`: una transizione illegale è un `409`, non uno stato incoerente sul DB.
- **Concorrenza**: il cambio di stato legge con lock pessimistico (`findWithLockById`) dentro la transazione, così due transizioni concorrenti non si sovrascrivono; la creazione intercetta anche la violazione del vincolo unico a livello DB per rispondere `409` in caso di race.
- **Storico stati** come audit trail append-only, scritto nella *stessa* transazione del cambio (propagazione `MANDATORY`): una riga di storico orfana non può esistere.
- **Schema con Liquibase** (`ddl-auto: validate`, un file per changeset): l'entità JPA non è mai la fonte di verità dello schema.
- **Config senza default** per i valori sensibili (DB, JWT, cifratura): l'app non parte senza un `.env` valido.

## Analisi tecnica

### Se la validazione fosse asincrona (sistema esterno)

Oggi `PATCH /validate` decide in modo sincrono. Se l'esito arrivasse da un motore esterno più tardi (callback o coda):

- **Stati** — un `VALIDATING` intermedio (richiesta inoltrata, in attesa) tra `RECEIVED` e l'esito; e la gestione del timeout (transizione a `REJECTED` motivata, oppure un `VALIDATION_TIMEOUT` se va distinto).
- **Modello dati** — un `validationRequestId` (UUID di correlazione) per agganciare la risposta alla richiesta giusta, più timestamp `validationRequestedAt/CompletedAt` per misurare le latenze e trovare richieste bloccate. Lo storico stati diventa ancora più utile con due attori che scrivono.
- **Niente doppie elaborazioni** (consegna *at-least-once*):
  - il `validationRequestId` è chiave di idempotenza: `UPDATE ... WHERE id=? AND status='VALIDATING' AND validation_request_id=?`; zero righe aggiornate ⇒ callback duplicato, ignorato in silenzio.
  - `@Version` (optimistic lock) contro callback concorrenti sulla stessa riga.
  - la transizione va resa idempotente: un secondo callback con lo stesso esito è un no-op di successo, non un errore di dominio.
  - **Transactional Outbox** per l'invio verso il broker: l'evento è scritto nella stessa transazione della richiesta, evitando il doppio-scrittore DB/broker.

### Performance su un milione di richieste

- **Indici** — il vincolo unico `(producer_id, external_id)` già copre il duplicate-check; aggiungerei `(producer_id, status)` per i filtri della lista e un indice esplicito su `document.request_id` (Postgres non indicizza le FK da solo). Lo storico stati indicizza già `request_id`.
- **Paginazione** — `OFFSET` degrada sulle pagine profonde (scandisce e scarta le precedenti). Per la lista, **keyset pagination** ordinata per `(created_at, id)`: costo pressoché costante. Evitare il `COUNT(*)` esatto a ogni pagina (stima via `pg_class.reltuples`).
- **Trovare una query lenta** — `log_min_duration_statement` + `pg_stat_statements` per capire *quale* query; poi `EXPLAIN (ANALYZE, BUFFERS)` per vedere un seq-scan dove serve un index-scan. Lato app, metriche per endpoint (Micrometer) con percentili p95/p99.
- **Investigare un degrado** — distinguerne la forma:
  - *graduale* col volume ⇒ indice mancante o statistiche del planner non aggiornate (`ANALYZE`);
  - *a gradino* su un deploy ⇒ correlare con il changelog, non con i dati;
  - *da pool* ⇒ Hikari saturo: latenza che sembra "DB lento" ma sono richieste in coda per una connessione libera;
  - *da bloat* ⇒ `request` ha molti `UPDATE` (ogni transizione); se l'autovacuum non tiene, tabella e indici si gonfiano — controllare `pg_stat_user_tables` (`n_dead_tup`).

## Test

`./mvnw test` — 104 test: unit (Mockito) su service e mapper, slice `@DataJpaTest` sui repository, `@WebMvcTest` sui controller, e integrazione `@SpringBootTest` per i confini transazionali (lock pessimistico e atomicità dell'audit trail).
