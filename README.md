# Conservation Requests

API REST per la gestione di richieste di conservazione documentale. Il servizio traccia solo i **metadati** di richieste (`Request`) e documenti (`Document`), senza salvare il contenuto dei file. Ogni richiesta proviene da un produttore (`producerId` + `externalId`, univoci in coppia), può avere uno o più documenti allegati, e transita tra gli stati `RECEIVED` → `VALIDATED`/`REJECTED` → `COMPLETED`.

## Stack

Java 25 LTS, Spring Boot, Spring Web, Spring Data JPA, Spring Security, Bean Validation, MapStruct, Liquibase, PostgreSQL, springdoc-openapi.

## Avvio rapido

```bash
cp .env.example .env      # valorizza le variabili richieste
docker-compose -f compose.yaml up -d
./mvnw clean verify        # build + test
./mvnw spring-boot:run
```

Documentazione OpenAPI disponibile a runtime su `/swagger-ui.html` e `/v3/api-docs`.

## Architettura

Struttura a package per layer (`entity` / `repository` / `service` / `controller` / `model` / `mapper`). Lo schema del database è gestito con Liquibase (`db/changelog/db.changelog-master.yaml`); `spring.jpa.hibernate.ddl-auto` è `validate`, quindi ogni modifica alle entity richiede un changeset corrispondente.

## API

Base path: `/api/v1/requests`

| Metodo | Path | Descrizione |
|---|---|---|
| `POST` | `/` | Crea una richiesta di conservazione (con i documenti allegati) |
| `GET` | `/` | Lista paginata, filtrabile per `producerId` e `status` |
| `GET` | `/{id}` | Dettaglio di una richiesta |
| `PATCH` | `/{id}/validate` | Transizione a `VALIDATED` |
| `PATCH` | `/{id}/reject` | Transizione a `REJECTED` |
| `PATCH` | `/{id}/complete` | Transizione a `COMPLETED` |

Le transizioni di stato non ammesse (vedi `Status.canTransitionTo`) rispondono `409 Conflict`; una richiesta duplicata (stesso `producerId` + `externalId`) risponde `409 Conflict`; un id inesistente risponde `404 Not Found`; un payload che viola le validazioni minime risponde `400 Bad Request`.

## Analisi tecnica

### Validazione asincrona da parte di un sistema esterno

Oggi la validazione è sincrona: chi chiama `PATCH /validate` decide lì per lì l'esito. Se la decisione venisse invece presa da un sistema esterno in modo asincrono (es. invio della richiesta a un motore di validazione, risposta recapitata più tardi via callback/coda), cambiano tre cose: gli stati, il modello dati, e il modo in cui evitiamo di applicare due volte lo stesso esito.

**Nuovi stati.** `RECEIVED` non basta più a distinguere "nessuno l'ha ancora presa in carico" da "è in attesa di risposta dal sistema esterno". Introdurrei uno stato intermedio `VALIDATING` (o `PENDING_VALIDATION`), impostato nel momento in cui la richiesta viene inoltrata all'esterno. Se il sistema esterno può non rispondere entro un tempo ragionevole, serve anche un modo per rappresentare quel caso, ad esempio un timeout gestito come transizione a `REJECTED` con motivazione dedicata, oppure uno stato `VALIDATION_TIMEOUT` se va distinto da un rifiuto di merito. La macchina a stati diventa quindi `RECEIVED → VALIDATING → (VALIDATED | REJECTED)`, con `COMPLETED` invariato a valle di `VALIDATED`.

**Modifiche al modello dati.**
- Sulla `Request`: un identificativo di correlazione (`validationRequestId`, tipicamente un UUID generato al momento dell'invio) che permette di far corrispondere la risposta asincrona alla richiesta giusta; timestamp `validationRequestedAt` / `validationCompletedAt` per poter misurare le latenze e individuare richieste bloccate in `VALIDATING` oltre un SLA atteso.
- Uno storico degli stati (`RequestStatusHistory`, già citato come estensione opzionale) diventa molto più utile in questo scenario: con due attori che modificano lo stato (il nostro sistema e quello esterno), avere traccia di ogni transizione con timestamp è ciò che permette di ricostruire cosa è successo in caso di anomalie o di callback fuori ordine.
- Se l'invio all'esterno avviene tramite un broker di messaggi, conviene adottare il pattern **Transactional Outbox**: scrivere l'evento "richiesta di validazione inviata" nella stessa transazione che salva la `Request`, e pubblicarlo verso il broker in un passo successivo affidabile. Evita il problema classico del doppio scrittore (commit sul DB riuscito ma pubblicazione del messaggio fallita, o viceversa).

**Evitare elaborazioni duplicate.** I sistemi a messaggi tipicamente garantiscono consegna *at-least-once*: bisogna quindi assumere che lo stesso esito di validazione possa arrivare più di una volta.
- Il `validationRequestId` funge anche da chiave di idempotenza: quando arriva il callback, l'update dello stato va condizionato sia sull'id sia sullo stato atteso (`UPDATE request SET status=... WHERE id=? AND status='VALIDATING' AND validation_request_id=?`); se l'update tocca zero righe, il callback è un duplicato già processato e va ignorato silenziosamente, non trattato come errore.
- Un campo di *optimistic locking* (`@Version`) sulla `Request` protegge da scritture concorrenti sulla stessa riga (es. due callback duplicati elaborati in parallelo da istanze diverse del servizio).
- Se la ricezione avviene da coda, conviene affiancare all'Outbox un **Inbox** lato consumo: una tabella dei messaggi già processati (con vincolo di unicità sull'id del messaggio) per scartare messaggi ridondanti prima ancora di toccare lo stato della richiesta.
- La transizione di stato applicata dal callback deve essere resa idempotente a livello applicativo: se la richiesta è già in `VALIDATED` e arriva un secondo callback che chiede la stessa transizione, il servizio deve rispondere con successo (no-op) invece di sollevare `InvalidStateTransitionException`, perché un retry è un comportamento atteso, non un errore di dominio.

### Performance su scala di un milione di richieste

**Indici.** Il vincolo di unicità `(producer_id, external_id)` già crea un indice composito che serve sia al controllo duplicati sia, in parte, ai filtri; oltre a quello:
- un indice su `producer_id` (o composito `(producer_id, status)`, visto che i due filtri della lista sono spesso usati insieme) per evitare una scansione completa della tabella `request` quando si filtra;
- un indice su `created_at` se si introduce paginazione per cursore ordinata per data (vedi sotto) o se si vuole ordinare la lista per recency;
- un indice esplicito su `document.request_id`: Postgres non indicizza automaticamente le colonne di foreign key (indicizza solo il lato referenziato), quindi senza un indice dedicato il join `document ↔ request` o il recupero dei documenti di una richiesta degradano linearmente con la dimensione della tabella `document`.

**Paginazione.** L'endpoint di lista usa oggi paginazione basata su `Pageable` (quindi `LIMIT`/`OFFSET`). Con `OFFSET` il database deve comunque scandire e scartare le righe precedenti alla pagina richiesta: a un milione di righe, le pagine profonde (`OFFSET` alto) diventano via via più lente anche con gli indici giusti. Per l'endpoint di lista consiglierei una **paginazione per cursore (keyset pagination)**: ordinare per `(created_at, id)`, e invece di un numero di pagina accettare un cursore (ultimo `id`/`created_at` visto), con query del tipo `WHERE (created_at, id) > (:lastCreatedAt, :lastId) ORDER BY created_at, id LIMIT :size`. Il costo resta pressoché costante indipendentemente da quanto si sia avanzati nella lista. Se serve mantenere la paginazione per numero di pagina (es. per un "vai a pagina N" in UI), conviene almeno evitare un `COUNT(*)` esatto ad ogni richiesta (costoso su tabelle grandi con filtri), sostituendolo con una stima (es. `pg_class.reltuples`) o calcolandolo una tantum lato client.

**Come individuare una query lenta.**
- A livello di database: abilitare `log_min_duration_statement` (es. tutto ciò che supera 200ms) e l'estensione `pg_stat_statements`, che aggrega le statistiche di esecuzione per query normalizzata ed è il punto di partenza standard per capire "quale query" sta rallentando il sistema, non solo "che il sistema è lento".
- Una volta identificata la query sospetta, `EXPLAIN (ANALYZE, BUFFERS)` su di essa per verificare se il piano di esecuzione fa una scansione sequenziale dove ci si aspetterebbe una scansione indicizzata.
- A livello applicativo: correlare le richieste lente ai log strutturati già presenti (SLF4J) tramite un id di correlazione, e strumentare i tempi di risposta per endpoint (es. Micrometer + un dashboard Grafana) per vedere percentili (p95/p99) nel tempo, non solo il singolo caso segnalato.

**Come investigare un degrado prestazionale.** Un degrado va distinto in base alla sua forma nel tempo, perché le cause tipiche sono diverse:
- *Graduale*, che cresce insieme al volume dei dati: quasi sempre un indice mancante il cui effetto diventa visibile solo oltre una certa cardinalità, oppure statistiche del planner non più aggiornate (`ANALYZE`) che portano Postgres a scegliere un piano peggiore man mano che la tabella cresce — si verifica confrontando `EXPLAIN` prima/dopo un `ANALYZE` manuale.
- *A gradino*, coincidente con un deploy, una migrazione o un cambio di configurazione: si individua correlando l'istante del peggioramento (dai grafici di latenza) con il changelog/i deploy, più che con analisi sui dati.
- *Legato al carico/pool di connessioni*: a un milione di richieste, un pool Hikari saturo si manifesta come una latenza crescente che sembra "il database è più lento", mentre in realtà le richieste stanno in coda in attesa di una connessione libera; le metriche Hikari (connessioni attive/idle/in attesa) lo distinguono chiaramente da una query realmente più lenta.
- *Specifico di questo dominio*: la tabella `request` ha un pattern di scrittura ad alta frequenza di `UPDATE` (ogni transizione di stato aggiorna la riga), che genera tuple morte a un ritmo elevato; se l'autovacuum non tiene il passo, la tabella e i suoi indici si gonfiano nel tempo (bloat) e le query rallentano progressivamente senza che sia cambiato nulla nel codice. `pg_stat_user_tables` (`n_dead_tup`, data dell'ultimo autovacuum) è il punto da controllare per questo tipo di degrado.
