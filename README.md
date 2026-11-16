# Museo Civico Aurora - Aurora Cultura

Aurora Cultura è un progetto realizzato per il Project Work del corso di laurea L-31 in Informatica per le Aziende Digitali, seguendo la Traccia n. 22.

Si tratta di una piattaforma web pensata per gestire le attività di un museo, in particolare eventi, prenotazioni e utenti.

Il museo è inventato e tutti i dati utilizzati nel progetto sono fittizi. L'applicazione è stata sviluppata a scopo didattico.

## Tecnologie utilizzate

| Ambito | Tecnologia |
|---|---|
| Linguaggio | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Sicurezza | Spring Security 7.1.1 |
| Build | Maven Wrapper 3.9.16 |
| Database | PostgreSQL 17 |
| Migrazioni | Flyway |
| Persistenza | JPA / Hibernate |
| Serializzazione | Jackson 3 |
| Frontend | HTML, CSS e JavaScript |

## Prerequisiti

Per eseguire il progetto servono:

- JDK 21 installato.
- PowerShell oppure un terminale.
- PostgreSQL 17.

Non è necessario installare Maven, perché nel progetto è già presente il Maven Wrapper.

Per eseguire i test non serve installare PostgreSQL né Docker. I test utilizzano una versione embedded di PostgreSQL che viene avviata automaticamente.

## Build e test

I comandi vanno eseguiti dalla cartella principale del progetto (`museo-civico-aurora`).

Windows (PowerShell):

```powershell
.\mvnw.cmd -B verify
```

Linux / macOS:

```bash
./mvnw -B verify
```

Il comando esegue la compilazione e tutti i test.

Per i test di integrazione viene utilizzato PostgreSQL 17.2 embedded tramite la libreria Zonky (`io.zonky.test:embedded-postgres`). Il database viene avviato automaticamente durante i test e chiuso al termine.

Non servono quindi un database locale, Docker o credenziali esterne per eseguire i test.

## Configurazione del database

L'applicazione utilizza PostgreSQL 17.

La configurazione predefinita è già presente nel progetto:

| Parametro | Valore |
|---|---|
| Database | `museo_civico_aurora` |
| Utente | `postgres` |
| Password | `postgres` |
| Indirizzo | `localhost:5432` |

È sufficiente avere PostgreSQL installato e in esecuzione. Non è necessario modificare la configurazione se si utilizzano questi valori.

Se si preferisce utilizzare credenziali diverse, è possibile modificarle tramite variabili d'ambiente:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Ad esempio, su Windows (PowerShell):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/museo_civico_aurora"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
```

## Avvio dell'applicazione

Dopo aver avviato PostgreSQL, è possibile avviare il progetto.

Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
./mvnw spring-boot:run
```

Una volta avviata l'applicazione, è possibile accedere a:

- Frontend: http://localhost:8080/
- API: http://localhost:8080/api/

## Account amministratore

Non sono configurate credenziali amministrative predefinite. Per creare un amministratore locale, imposta `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` e avvia l'applicazione con il profilo `dev`. La password deve avere almeno 8 caratteri e non superare 72 byte UTF-8.

Su Windows (PowerShell), sostituisci il valore segnaposto con una password locale adeguata:

```powershell
$env:APP_ADMIN_EMAIL = "admin@admin.it"
$env:APP_ADMIN_PASSWORD = "<scegli-una-password-di-almeno-8-caratteri>"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Se le variabili non sono impostate, l'account amministratore non viene creato. Non usare credenziali di esempio in ambienti condivisi o di produzione.

## Configurazione HTTPS

È disponibile anche un profilo `https`, definito nel file `application-https.properties`.

Questo profilo abilita l'attributo `Secure` sui cookie di sessione e CSRF, da utilizzare quando l'applicazione viene eseguita tramite HTTPS.

Per attivarlo:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=https
```

I cookie utilizzano inoltre `SameSite=Lax` e quello di sessione mantiene l'attributo `HttpOnly`.

In ambiente locale, utilizzando HTTP, l'attributo `Secure` rimane disattivato.

## Login e comunicazione con le API

Il frontend comunica con il backend attraverso le API REST.

Il login e la gestione della protezione CSRF funzionano in questo modo:

1. Il frontend richiede il token CSRF tramite `GET /api/auth/csrf`.
2. L'utente effettua il login tramite `POST /api/auth/login`, inviando email e password.
3. Il backend restituisce una risposta JSON con esito positivo (`200`) oppure negativo (`401`).
4. Le richieste che modificano i dati devono includere il token CSRF nell'header `X-XSRF-TOKEN`.

Dopo il login e il logout il token viene aggiornato e deve essere richiesto nuovamente.

Per conoscere tutti gli endpoint disponibili, i parametri, i DTO e i codici di risposta è possibile consultare il file [docs/api-contract.md](docs/api-contract.md).

## Struttura e diagrammi

Nella cartella `docs/diagrams/` si trovano i diagrammi realizzati per il progetto:

- Modello entità-relazione.
- Architettura dell'applicazione.
- Sequenza di prenotazione e controllo della capienza.
- Login.
- Protezione CSRF.

I diagrammi sono disponibili sia in formato Mermaid (`diagrammi.md`) sia come immagini PNG. Le immagini sono le stesse inserite nel documento del Project Work.

## Database e migrazioni

La struttura del database viene gestita tramite Flyway. Le migrazioni si trovano in:

`src/main/resources/db/migration`

Attualmente è presente la migrazione iniziale `V1__init.sql`.

Hibernate utilizza la modalità `ddl-auto=validate`, quindi non crea o modifica le tabelle, ma controlla che la struttura del database corrisponda alle entità definite nel progetto.

## Limiti del progetto

L'applicazione è un prototipo realizzato a scopo didattico. In questa versione non sono previste alcune funzionalità:

- Non sono presenti sistemi di pagamento.
- Non è previsto l'invio di email o notifiche.
- Non è possibile caricare immagini.
- Non è disponibile il recupero della password.
- I feedback non verificano che l'utente abbia effettivamente partecipato all'evento.
- La dimensione massima delle pagine restituite dalle API è 50 elementi.
- Non sono presenti test automatici per il browser e per i flussi completi dell'interfaccia.
- Non è stata implementata una configurazione CORS né il versionamento delle API.

## Project Work

Questo repository contiene il codice sorgente dell'applicazione realizzata per il Project Work.

Il documento finale di consegna e la traccia assegnata sono separati dal codice e non sono inclusi nel repository.