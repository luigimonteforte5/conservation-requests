# Guida al setup dell'ambiente

Come configurare e avviare l'applicazione in locale. Per il modello di dominio e le
API vedi il [README](README.md).

## 1. Variabili d'ambiente

L'applicazione legge tutti i valori sensibili da variabili d'ambiente, senza default in
`application.yaml`: **non parte senza un `.env` valido**.

Copia il template e valorizzalo:

```bash
cp .env.example .env
```

> Il file `.env` è escluso da git: non committare mai credenziali reali.

### Variabili richieste

| Variabile                          | Descrizione                                              |
|------------------------------------|---------------------------------------------------------|
| `DB_HOST` / `DB_PORT` / `DB_NAME`  | Coordinate del database PostgreSQL                      |
| `DB_USER` / `DB_PASSWORD`          | Credenziali del database                                |
| `SECURITY_USER`                    | Username dell'utente amministratore                     |
| `SECURITY_PASSWORD`                | Hash **bcrypt** della password admin (vedi sotto)       |
| `JWT_SECRET`                       | Chiave di firma dei JWT (min. 256 bit consigliati)      |
| `JWT_EXPIRATION`                   | Durata del token in ms (default `86400000`, cioè 24h)   |

### Generare l'hash della password admin

`SECURITY_PASSWORD` deve contenere l'**hash bcrypt**, non la password in chiaro.
Generalo con `BCryptPasswordEncoder` (strength 12) di Spring Security:

```java
new BCryptPasswordEncoder(12).encode("la-tua-password");
```

Incolla l'hash risultante (`$2a$...`) nel `.env`.

## 2. Avvio del database

```bash
docker-compose -f compose.yaml up -d
docker ps   # verifica che il container sia up
```

## 3. Avvio dell'applicazione

```bash
./mvnw spring-boot:run
```

L'app si connette al database usando le variabili caricate dal `.env`.

## File di configurazione

| File               | Scopo                        | Git            |
|--------------------|------------------------------|----------------|
| `application.yaml` | Configurazione principale    | ✅ versionato  |
| `.env.example`     | Template delle variabili     | ✅ versionato  |
| `.env`             | Valori locali                | ❌ gitignored  |

## Note per la produzione

1. **Genera chiavi robuste** e non riusare quelle di sviluppo:
   ```bash
   openssl rand -base64 32   # JWT_SECRET
   ```
2. **Non tenere i segreti in un file**: usa un secret manager (AWS Secrets Manager,
   Spring Cloud Config, Consul, ...).
3. **Rigenera l'hash della password admin** con una password forte prima del deploy.
