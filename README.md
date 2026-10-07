# Museo Civico Aurora — "Aurora Cultura"

Prototipo **"Aurora Cultura"** per il **Museo Civico Aurora**: una piattaforma informatica per la gestione di **eventi, prenotazioni e utenti** di un'impresa culturale. Il progetto è sviluppato come **Project Work del CdS L-31** (Informatica per le Aziende Digitali), in risposta alla **Traccia n. 22**.

> **Nota sul contesto:** si tratta di un **prototipo didattico** ambientato in un **museo simulato**; tutti i **dati sono fittizi** e non riferibili a persone o organizzazioni reali.

---

## Stack tecnologico

| Ambito | Tecnologia |
|---|---|
| Linguaggio | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Sicurezza | Spring Security 7.1.1 |
| Build | Maven Wrapper (distribuzione Maven 3.9.16, `mvnw` / `mvnw.cmd` committati) |
| Database | PostgreSQL 17 |
| Migrazioni | Flyway (fonte unica dello schema) |
| Persistenza | JPA / Hibernate (`ddl-auto=validate`) |
| Serializzazione | Jackson 3 |
| Frontend | HTML / CSS / JavaScript statico servito dalla stessa applicazione |

---

## Prerequisiti

- **JDK 21** installato e raggiungibile (`java -version`).
- **Nessuna installazione globale di Maven**: si usa il **Maven Wrapper** committato nel repository.
- **PowerShell** o un terminale per i comandi di build/avvio.
- **PostgreSQL 17** — richiesto **solo per l'esecuzione runtime** dell'applicazione.

> I **test NON richiedono** PostgreSQL locale, Docker o credenziali esterne: avviano un'istanza PostgreSQL embedded.

---

## Build e test

Eseguire dal root del progetto `museo-civico-aurora/`.

**Windows (PowerShell):**
```powershell
.\mvnw.cmd -B verify
```

**Unix / macOS:**
```bash
./mvnw -B verify
```

I test di integrazione avviano un **PostgreSQL 17.2 embedded** tramite la libreria **Zonky** (`io.zonky.test:embedded-postgres`). L'istanza viene avviata prima del contesto Spring e chiusa a fine test; non serve **Docker**, **Testcontainers** né una configurazione del PostgreSQL locale. Il profilo di test abilita Flyway e usa `ddl-auto=validate`.

---

## Configurazione runtime

Le credenziali del database provengono **solo da variabili d'ambiente**; i valori di fallback presenti in `application.properties` **non sono segreti**:

| Variabile | Default nel repository |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/museo_civico_aurora` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | *(vuoto)* |

Prima dell'avvio creare **un database e un ruolo dedicati**, ad esempio:

```sql
CREATE ROLE museo_app LOGIN PASSWORD '<<password-locale>>';
CREATE DATABASE museo_civico_aurora OWNER museo_app;
```

Esempio di valorizzazione in PowerShell (nessun segreto va committato):

```powershell
$env:DB_URL      = "jdbc:postgresql://localhost:5432/museo_civico_aurora"
$env:DB_USERNAME = "museo_app"
$env:DB_PASSWORD = "<<password-locale>>"
```

> **Nessuna password, secret o credenziale reale è presente nel repository.** Non committare file `.env` o configurazioni locali.

---

## Avvio dell'applicazione

**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Unix / macOS:**
```bash
./mvnw spring-boot:run
```

Una volta avviata l'applicazione:

- **Frontend:** <http://localhost:8080/>
- **API:** <http://localhost:8080/api/...>

---

## Account amministratore

L'account amministratore viene creato **solo** quando è attivo il profilo **`dev`** e sono valorizzate **entrambe** le variabili `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD`. Se mancano, **non viene creata alcuna credenziale predefinita**.

La password deve rispettare i requisiti applicativi: almeno **8 caratteri** e non oltre **72 byte UTF-8**.

**Windows (PowerShell):**
```powershell
$env:APP_ADMIN_EMAIL    = "admin@example.org"
$env:APP_ADMIN_PASSWORD = "<<password-almeno-8-caratteri>>"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**Unix / macOS:**
```bash
APP_ADMIN_EMAIL=admin@example.org \
APP_ADMIN_PASSWORD='<<password-almeno-8-caratteri>>' \
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Profilo HTTPS

Il profilo **`https`** (`application-https.properties`) imposta l'attributo **`Secure`** sul cookie di sessione e sul cookie CSRF. Va attivato, ad esempio, con:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=https
```

I cookie mantengono **`SameSite=Lax`** e la sessione resta **HttpOnly**. In locale su HTTP l'attributo `Secure` è disattivato; va abilitato solo in deployment HTTPS.

---

## Flusso frontend / API (in breve)

1. Il client recupera il token CSRF con **`GET /api/auth/csrf`**; la risposta contiene token, nome header (`X-XSRF-TOKEN`) e materializza il cookie `XSRF-TOKEN`.
2. Il login usa **`POST /api/auth/login`** con body **`application/x-www-form-urlencoded`** (campi `username` = email registrata e `password`). La risposta è **JSON** (`200` / `401`), senza redirect HTML.
3. Le richieste di modifica inviano l'header **`X-XSRF-TOKEN`**; dopo login/logout il token viene invalidato/ruotato e va recuperato di nuovo.

Gli **endpoint principali** (eventi pubblici, prenotazioni, area amministrativa, feedback, statistiche) e i relativi DTO, codici di errore e regole sono definiti nel contratto **[`docs/api-contract.md`](docs/api-contract.md)**.

---

## Migrazioni database

Lo schema è governato **esclusivamente da Flyway**, con le migrazioni in `src/main/resources/db/migration` (attualmente `V1__init.sql`). Hibernate è configurato con **`ddl-auto=validate`**: non crea né modifica tabelle, si limita a **validare** lo schema migrato.

---

## Limiti e note

- Prototipo **didattico**: non è un sistema pronto per la produzione.
- **Nessun pagamento**, invio di **email/notifiche**, **upload di immagini** o **recupero password**.
- Il **feedback** non è legato alla prova di effettiva partecipazione all'evento.
- Paginazione limitata a pagina/dimensione, con **`size` massimo 50**.
- I **test browser/E2E non sono automatizzati**.
- Nessuna configurazione CORS e nessun API versioning nell'MVP.

---

## Riferimenti al Project Work

Il repository è l'**artefatto complementare** del Project Work. Il **documento di consegna** è il **template universitario compilato** (`1777560537.docx`) con il **brief dell'assegnazione** (`1777277810.docx`) e la guida di lavoro `PROJECT_WORK_GUIDE.md`: questi materiali sono conservati **fuori dal repository di codice** e **non** sono inclusi in questa cartella.
