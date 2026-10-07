# Contratto API — Aurora Cultura

> **Stato:** contratto v1 approvato da Oracle per la prima implementazione. Questa specifica è l'unica fonte del contratto per frontend e backend; le modifiche successive devono aggiornare prima questo documento e annotare la motivazione.

## 1. Scopo e perimetro MVP

Prototipo per il museo simulato **Museo Civico Aurora** (nome provvisorio), sviluppato con Java 21, Spring Boot 4.1.1 / Spring Security 7.1.1, PostgreSQL 17 e frontend HTML/CSS/JavaScript servito dalla stessa applicazione.

### Incluso

- Consultazione pubblica del catalogo di eventi culturali pubblicati.
- Registrazione e sessione per visitatori.
- Prenotazione singola di un posto, annullamento e successiva nuova prenotazione.
- Gestione amministrativa degli eventi e delle prenotazioni.
- Feedback leggero: una valutazione (1–5) e un commento per utente/evento.
- Endpoint amministrativo con indicatori aggregati.

### Escluso dall'MVP

- Dashboard grafica, prenotazioni multi-posto, pagamenti, email/notifiche, upload immagini, recupero password, analytics cloud e autenticazione federata.
- Il feedback non richiede nel prototipo la prova di partecipazione all'evento; questa limitazione sarà dichiarata.
- La scelta di feedback e statistiche non costituisce una rivendicazione di originalità tecnologica: il contributo è l'integrazione del flusso nel contesto culturale.

## 2. Convenzioni HTTP

- Base path: `/api`; payload e risposte JSON salvo il login, che usa `application/x-www-form-urlencoded` per integrarsi con `formLogin`.
- Same-origin: il frontend chiama API relative; **nessuna configurazione CORS**.
- Date/ore in ISO 8601 con offset o `Z`; persistenza PostgreSQL `timestamptz`; il server è fonte autorevole per ID, date di creazione, stato e ruolo.
- Le regole temporali usano UTC e un `Clock` iniettabile (`Clock.systemUTC()` nell'applicazione; clock fisso nei test che validano eventi futuri/passati e statistiche) per rendere deterministici i boundary tests.
- Il client non invia `userId`, ruolo, stato della prenotazione o contatori di capienza. Il visitatore è ricavato dalla sessione.
- Paginazione: indici 0-based; default `page=0`, `size=20`; `size` massimo 50. `page<0`, `size<1`, `size>50` o `from>to` → `400`; `page` oltre l'ultima pagina → `200` con `content=[]`. `from`/`to` sono inclusivi e filtrano `startsAt`, con offset esplicito; ordine predefinito `startsAt ASC`. Nessun sort arbitrario nell'MVP.

### DTO di pagina

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Schemi dei DTO

Tutti i campi delle request sono obbligatori salvo quelli marcati facoltativi/default; gli ID sono `integer` positivi; le date sono stringhe ISO 8601 con offset. I campi non previsti nei DTO non possono modificare ID, ruolo o proprietario.

| DTO | Campi e vincoli |
|---|---|
| `UserDto` | `id: integer`; `email: string` (normalizzata minuscola); `displayName: string`; `role: VISITOR\|ADMIN`. Mai password/hash. |
| `EventSummaryDto` | `id`; `title: string`; `location: string`; `startsAt`, `endsAt`; `seatsAvailable: integer >= 0`. |
| `AdminEventSummaryDto` | Tutti i campi summary + `status: DRAFT\|PUBLISHED\|CANCELLED`. |
| `EventDetailDto` | Tutti i campi summary + `description: string\|null`; `capacity: integer >= 1`; `status: DRAFT\|PUBLISHED\|CANCELLED` (pubblico sempre PUBLISHED; admin può vedere tutti). |
| `EventCreateRequest` | `title: string` (1–160); `description: string\|null` (max 5.000); `location: string` (1–200); `startsAt`, `endsAt`; `capacity: integer >= 1`; `status: DRAFT\|PUBLISHED` (default DRAFT). |
| `EventUpdateRequest` | Tutti i campi modificabili del create obbligatori, inclusi `status`; PUT sostituisce tutti i campi modificabili, non parziale. ID/date audit non ammessi. |
| `BookingCreateRequest` | `eventId: integer > 0`. Nessun user ID, status, timestamp o numero posti dal client. |
| `BookingDto` | `id`; `eventId`; `eventTitle`; `startsAt`; `status: CONFIRMED\|CANCELLED`; `createdAt`; `cancelledAt: string\|null`. |
| `AdminBookingDto` | Tutti i campi `BookingDto` + `visitorDisplayName: string`; non include email o altri dati personali. |
| `FeedbackCreateRequest` | `rating: integer` (1–5); `comment: string\|null` (max 2.000 caratteri). |
| `FeedbackDto` | `id`; `eventId`; `displayName`; `rating`; `comment: string\|null`; `createdAt`. Non restituisce email. |
| `AdminStatsDto` | `publishedEventCount: integer`; `upcomingEventCount: integer`; `confirmedBookingCount: integer`; `capacityUtilizationPercent: number`; `averageRating: number\|null`; `feedbackCount: integer`. |
| `CsrfTokenDto` | `token: string`; `headerName: string` (atteso `X-XSRF-TOKEN`); `parameterName: string`; `cookieName: string` (atteso `XSRF-TOKEN`). |

Registrazione: `email` valida (max 254), `password` di almeno 8 caratteri e non oltre 72 byte UTF-8 per BCrypt, `displayName` 1–100 caratteri. Il login usa il parametro `username` come email registrata.

### Error envelope

```json
{
  "code": "EVENT_FULL",
  "message": "Non ci sono posti disponibili per questo evento.",
  "fieldErrors": []
}
```

`fieldErrors` è omesso o vuoto se non pertinente; quando presente, ogni voce contiene `field` (nome JSON del campo, per esempio `startsAt`) e `message`. Il medesimo envelope è prodotto anche dai componenti Security filter-level (`AuthenticationEntryPoint`, `AccessDeniedHandler`); `CSRF_INVALID` è distinto da `FORBIDDEN`. `@ControllerAdvice` da solo non intercetta le eccezioni dei filtri. Non esporre stack trace, query SQL, hash di password o dettagli interni.

| HTTP | Codici applicativi esemplificativi | Uso |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_PAGE`, `INVALID_PAGE_SIZE`, `INVALID_TIME_RANGE`, `EVENT_ALREADY_STARTED` | Payload/query non validi o evento già iniziato |
| 401 | `UNAUTHENTICATED`, `INVALID_CREDENTIALS` | Sessione assente o login non riuscito; mai redirect HTML per `/api/**` |
| 403 | `FORBIDDEN`, `CSRF_INVALID` | Ruolo insufficiente o token CSRF assente/non valido |
| 404 | `NOT_FOUND` | Risorsa non esistente o evento non pubblicato |
| 409 | `EVENT_FULL`, `ALREADY_BOOKED`, `FEEDBACK_EXISTS`, `EMAIL_EXISTS`, `INVALID_EVENT_STATE`, `CAPACITY_BELOW_BOOKINGS`, `BOOKING_ALREADY_STARTED`, `EVENT_CANNOT_BE_CANCELLED` | Conflitto con stato o vincolo dati |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Body non JSON per gli endpoint JSON |

## 3. Modello dati e invarianti

Le migrazioni Flyway sono la fonte dello schema; Hibernate usa `ddl-auto=validate` e non crea/aggiorna schema automaticamente.

### `app_user`

- `id`, `email`, `display_name`, `password_hash`, `role`, `created_at`.
- Email normalizzata in minuscolo in applicazione e indice univoco funzionale PostgreSQL `UNIQUE (lower(email))` per imporre l'unicità case-insensitive anche a livello DB. Violazioni concorrenti durante la registrazione → `409 EMAIL_EXISTS`, mai `500`.
- Ruoli ammessi: `VISITOR`, `ADMIN`. Le richieste pubbliche di registrazione creano sempre `VISITOR`, anche se il JSON contiene un campo `role`.
- Il DTO pubblico non contiene mai `password_hash`.
- Vincoli DB: `NOT NULL` sui campi richiesti; `CHECK (role IN ('VISITOR','ADMIN'))`.

### `cultural_event`

- `id`, `title`, `description`, `location`, `starts_at`, `ends_at`, `capacity`, `status`, `created_at`, `updated_at`.
- `status`: `DRAFT`, `PUBLISHED`, `CANCELLED`.
- Vincoli DB: `NOT NULL` su titolo, sede, date, capienza e stato; `CHECK (ends_at >= starts_at)`, `CHECK (capacity > 0)`, `CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED'))`.
- Transizioni consentite: create solo `DRAFT` (default) o `PUBLISHED`; PUT può mantenere `DRAFT`, pubblicare `DRAFT → PUBLISHED` o mantenere `PUBLISHED`; `PUBLISHED → DRAFT` non è consentito. La cancellazione passa solo dall'endpoint cancel; `CANCELLED` è terminale. Qualsiasi PUT su un evento `CANCELLED` → `409 INVALID_EVENT_STATE`.
- `PUT` è sostitutivo dei campi modificabili (tutti obbligatori); `GET /api/admin/events/{id}` fornisce i dati per l'editing.
- Ridurre `capacity` sotto il numero di prenotazioni `CONFIRMED` → `409 CAPACITY_BELOW_BOOKINGS`.
- Gli eventi con prenotazioni non sono cancellati fisicamente: l'amministratore li porta a `CANCELLED` solo prima di `starts_at`; dopo l'inizio → `409 EVENT_CANNOT_BE_CANCELLED`. Nella stessa transazione, dopo lock della riga evento, tutte le prenotazioni `CONFIRMED` diventano `CANCELLED` e valorizzano `cancelled_at`. Questo serializza la cancellazione rispetto a nuove prenotazioni. L'MVP non invia notifiche.
- Gli endpoint pubblici restituiscono solo eventi `PUBLISHED` e non includono informazioni interne non necessarie.

### `booking`

- `id`, `event_id`, `user_id`, `status`, `created_at`, `cancelled_at`.
- `status`: `CONFIRMED` o `CANCELLED`; una prenotazione equivale a un posto; `CHECK (status IN ('CONFIRMED','CANCELLED'))`, chiavi esterne e `NOT NULL` sui campi necessari.
- Indice univoco parziale PostgreSQL su `(event_id, user_id)` **solo quando `status='CONFIRMED'`**: impedisce prenotazioni attive duplicate ma consente di prenotare nuovamente dopo annullamento.
- Indice su `(event_id, status)` per conteggio dei posti occupati.
- La cancellazione modifica lo stato; libera il posto. Visitatore e admin possono cancellare prima dell'inizio evento; dopo `starts_at` → `409 BOOKING_ALREADY_STARTED`. La stessa operazione ripetuta è idempotente (`204`), anche dopo l'inizio. L'utente che annulla può creare successivamente una nuova prenotazione.
- La capienza disponibile è derivata: `capacity - COUNT(booking CONFIRMED)`. Non mantenere un contatore denormalizzato nell'MVP.

### `feedback`

- `id`, `event_id`, `user_id`, `rating`, `comment`, `created_at`.
- Vincoli: `NOT NULL` su utente/evento/rating/data; `CHECK (rating BETWEEN 1 AND 5)`; una sola valutazione per coppia evento/utente; commento facoltativo con limite applicativo 2.000 caratteri.
- Solo utenti autenticati; nessun dato personale reale nei dati demo. Non verificare la presenza all'evento nell'MVP.

### Concorrenza e capienza

La creazione della prenotazione avviene in un'unica transazione:

1. Caricare la riga evento con lock pessimista (`SELECT ... FOR UPDATE` / `PESSIMISTIC_WRITE`).
2. Verificare che l'evento sia `PUBLISHED`, non iniziato e non pieno; evento inesistente/non pubblicato/annullato → `404 NOT_FOUND`, evento già iniziato → `400 EVENT_ALREADY_STARTED`.
3. Contare le prenotazioni `CONFIRMED`; se il conteggio è uguale alla capienza, restituire `409 EVENT_FULL`.
4. Inserire la prenotazione; il vincolo univoco parziale intercetta comunque richieste duplicate concorrenti. Il conflitto di vincolo viene mappato a `409 ALREADY_BOOKED`, mai a `500`.

Il test di concorrenza usa PostgreSQL embedded 17, transazioni/thread distinti e non usa H2 come prova sostitutiva.

## 4. Endpoint

### Eventi pubblici

| Metodo e percorso | Richiesta | Esito |
|---|---|---|
| `GET /api/events?page=&size=&from=&to=` | Filtra `startsAt` inclusivo | `200 PageDto<EventSummaryDto>`; solo pubblicati; default sort `startsAt ASC` |
| `GET /api/events/{id}` | — | `200 EventDetailDto`; `404` se inesistente o non pubblicato |

`EventSummaryDto` include ID, titolo, sede, inizio/fine e disponibilità sintetica. `EventDetailDto` aggiunge descrizione, capienza e posti disponibili. La disponibilità è informativa: la prenotazione ripete sempre i controlli transazionali.

### Sessione e autenticazione

| Metodo e percorso | Richiesta | Esito |
|---|---|---|
| `GET /api/auth/csrf` | — | `200 CsrfTokenDto` con token/header/parameter/cookie name e cookie CSRF materializzato; accessibile prima del login |
| `POST /api/auth/register` | JSON `{email,password,displayName}` | `201 UserDto`; `400` validazione; `409 EMAIL_EXISTS` |
| `POST /api/auth/login` | Form URL-encoded `username`, `password` | `200 UserDto`; `401 INVALID_CREDENTIALS` in JSON, senza redirect |
| `GET /api/auth/me` | — | `200 UserDto`; `401` se non autenticato |
| `POST /api/auth/logout` | CSRF header | `204`; termina la sessione, senza redirect |

Il token CSRF è richiesto anche per registrazione e login. Il client lo recupera prima di ogni operazione POST e lo recupera di nuovo dopo login/logout, perché la token repository lo invalida/ruota in quelle transizioni.

### Prenotazioni visitatore

| Metodo e percorso | Richiesta | Esito |
|---|---|---|
| `POST /api/bookings` | JSON `{eventId}` | `201 BookingDto`; `401`; `404` evento inesistente/non pubblicato/annullato; `400 EVENT_ALREADY_STARTED`; `409 EVENT_FULL` o `ALREADY_BOOKED` |
| `GET /api/bookings/me?page=&size=` | — | `200 PageDto<BookingDto>` delle prenotazioni dell'utente corrente; `401` |
| `DELETE /api/bookings/{id}` | — | `204` se annullata o già annullata; `401`; `404` se inesistente o non posseduta (non rivelare l'esistenza di ID altrui); `409 BOOKING_ALREADY_STARTED` se attiva dopo `starts_at` |

L'identità proprietaria deriva sempre dal principal autenticato, mai dal body o dai parametri URL forniti dal client.

### Amministrazione (`ROLE_ADMIN`)

| Metodo e percorso | Scopo | Esito |
|---|---|---|
| `GET /api/admin/events?page=&size=` | Elenco incl. bozze | `200 PageDto<AdminEventSummaryDto>` |
| `GET /api/admin/events/{id}` | Dettaglio per editing | `200 EventDetailDto`; `404` |
| `POST /api/admin/events` | Crea con status `DRAFT` o `PUBLISHED` (default `DRAFT`) | `201 EventDetailDto`; `400` campi non validi |
| `PUT /api/admin/events/{id}` | Sostituisce tutti i campi modificabili | `200 EventDetailDto`; `404`; `409 INVALID_EVENT_STATE` o `CAPACITY_BELOW_BOOKINGS` |
| `POST /api/admin/events/{id}/cancel` | Annulla logicamente e cancella le prenotazioni attive | `204` anche se già annullato; `404`; `409 EVENT_CANNOT_BE_CANCELLED` |
| `GET /api/admin/events/{id}/bookings?page=&size=` | Elenco prenotazioni incl. nome visitatore | `200 PageDto<AdminBookingDto>`; nessuna email |
| `POST /api/admin/bookings/{id}/cancel` | Annulla per conto amministrativo | `204` anche se già annullata; `404`; `409 BOOKING_ALREADY_STARTED` se attiva dopo `starts_at` |
| `GET /api/admin/stats` | Indicatori aggregati | `200 AdminStatsDto` |
| `GET /api/admin/events/{id}/feedback?page=&size=` | Feedback evento | `200 PageDto<FeedbackDto>` |

Ogni percorso `/api/admin/**` richiede `ROLE_ADMIN`: anonimo → `401`, visitatore autenticato → `403`.

### Feedback

| Metodo e percorso | Richiesta | Esito |
|---|---|---|
| `POST /api/events/{id}/feedback` | JSON `{rating,comment}` | `201 FeedbackDto`; `401`; `404` evento inesistente/bozza/annullato; `400` rating/commento non valido; `409 FEEDBACK_EXISTS` |

Il feedback è ammesso su eventi pubblicati, inclusi quelli già conclusi; non è ammesso su bozze o annullati. Non è richiesto che l'utente abbia prenotato o partecipato (limite esplicito dell'MVP).

### Indicatori amministrativi

`AdminStatsDto` è definito così (conteggi e formula devono essere riprodotti anche nel test T10):

```json
{
  "publishedEventCount": 4,
  "upcomingEventCount": 3,
  "confirmedBookingCount": 18,
  "capacityUtilizationPercent": 60.0,
  "averageRating": 4.5,
  "feedbackCount": 2
}
```

- `publishedEventCount`: tutti gli eventi con status `PUBLISHED`.
- `upcomingEventCount`: eventi `PUBLISHED` con `starts_at >= now`.
- `confirmedBookingCount`: prenotazioni `CONFIRMED` associate a eventi `PUBLISHED` futuri.
- `capacityUtilizationPercent`: `100 * confirmedBookingCount / somma(capacity)` sugli stessi eventi futuri pubblicati; se la somma è 0, restituire `0.0`.
- `averageRating` e `feedbackCount`: media e conteggio di tutti i feedback persistiti; media arrotondata a 2 decimali; senza feedback la media è `null` e il conteggio `0`.

Il prototipo non presenta una dashboard grafica.

## 5. Sicurezza HTTP

- Spring Boot 4.1.1 gestisce Spring Security 7.1.1. Usare lambda DSL e API non deprecate.
- Catena API selezionata con `securityMatcher("/api/**")`, ordinata prima della catena frontend; catena web consente static asset same-origin, `FORWARD`/`ERROR` dispatch e fallback `/error`. Nessun CORS.
- Nella catena API: permit-all per `GET /api/events/**`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/csrf`; `/api/admin/**` richiede `ROLE_ADMIN`; gli altri endpoint richiedono autenticazione. Il filtro logout gestisce `POST /api/auth/logout` con CSRF.
- CSRF esplicito con `.csrf(csrf -> csrf.spa())`; cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`; `GET /api/auth/csrf` materializza il token. Le richieste unsafe, incluso login/logout/registrazione, inviano header CSRF. Il frontend ripete il GET dopo login/logout perché il token viene invalidato/ruotato.
- Il cookie CSRF è leggibile dal frontend per inviare l'header e non è un bearer token/session credential; la sua materializzazione e il refresh restano obbligatori per il flusso.
- `formLogin` gestisce `/api/auth/login`; success/failure handler restituiscono JSON (`200`/`401`) e non redirect HTML.
- Il campo `username` del form login contiene l'email registrata; `UserDetailsService` normalizza la stessa email del flusso registration.
- L'API `SecurityFilterChain` usa entry point JSON `401` e access denied handler JSON `403`; entrambi producono l'error envelope definito sopra. L'access denied handler riconosce `CsrfException` e restituisce `403 CSRF_INVALID`; gli altri dinieghi sono `403 FORBIDDEN`. `formLogin` contribuisce normalmente un entry point redirect: sovrascriverlo per `/api/**`. `@ControllerAdvice` non intercetta le eccezioni lanciate dai filtri.
- API anonime restituiscono `401` JSON; l'utente autenticato privo del ruolo richiesto riceve `403` JSON. Testare esplicitamente anonymous admin `401`, visitatore admin `403` e assenza di `302`.
- Password codificate con `BCryptPasswordEncoder`; accettare lunghezza in byte non superiore al limite BCrypt (72 byte). Nessuna password, secret o dato utente reale è hardcoded.
- Account amministratore inizializzato solo da variabili d'ambiente `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` in profilo locale/dev; se mancanti, non creare credenziali predefinite. Non loggare password o hash.
- Cookie sessione HttpOnly e SameSite=Lax (SameSite va configurato esplicitamente); `Secure=false` solo per HTTP locale e abilitato in deployment HTTPS. Protezione anti-session-fixation di default mantenuta.

## 6. Mappa dei test di accettazione

### Wiring dei test PostgreSQL

Il test smoke Zonky conferma il motore PG17, ma non fornisce da solo un `DataSource` al contesto Spring. I test JPA/API usano una fixture condivisa `EmbeddedPostgres` avviata prima del contesto e chiusa dopo il test, con `@DynamicPropertySource` che registra `spring.datasource.url`, `spring.datasource.username` e `spring.datasource.password` (`postgres`) usando `getJdbcUrl("postgres", "postgres")`. Il profilo test esegue Flyway e `ddl-auto=validate`; non dipende dal PostgreSQL locale né da Docker.

Una fixture `@TestConfiguration` crea visitatore/amministratore nel database con `PasswordEncoder`; non legge le env admin di sviluppo e non usa credenziali reali. Il test di concorrenza usa thread/transazioni distinti, senza transazione esterna del test che annulli tutto alla fine.

| ID | Scenario | Evidenza attesa |
|---|---|---|
| T1 | M prenotazioni concorrenti per capienza N, M>N | Esattamente N confermate; M−N ricevono `409`; nessun overbooking |
| T2 | Doppia prenotazione attiva, anche in race | `409 ALREADY_BOOKED`; unique violation DB mappata a 409, mai 500; una sola booking attiva |
| T3 | Annullamento e nuova prenotazione | Cancellazione libera posto; stessa cancellazione restituisce 204; utente può riprenotare |
| T4 | API anonima / redirect | Dopo aver ottenuto token CSRF valido, `POST /api/bookings` anonima e `GET /api/admin/**` restituiscono `401` JSON, mai `302`; POST anonima senza CSRF è `403 CSRF_INVALID` |
| T5 | Autorizzazione admin | Anonimo su `/api/admin/**` → `401`; visitatore → `403`; admin fixture → `200` |
| T6 | Flusso CSRF | POST senza token → `403 CSRF_INVALID`; token valido accetta richiesta; token recuperato dopo login/logout |
| T7 | Registrazione/email/password | Email duplicata o case-variant → `409 EMAIL_EXISTS`; ruolo forzato `VISITOR`; hash non raw e validabile; password >72 byte → `400` |
| T8 | Validazione/lifecycle evento | Capienza ≤0/date incoerenti → `400`; transizione vietata o capacity sotto bookings → `409`; bozze invisibili (`404`) |
| T9 | Feedback | Rating fuori range → `400`; doppio feedback utente/evento → `409` |
| T10 | Statistiche | Ogni metrica e denominatore corrispondono ai valori fixture noti, incluso zero capacity |
| T11 | Migrazione/cancel evento | Flyway e `ddl-auto=validate` passano su PG17; annullare evento annulla in transazione le booking confermate |
| T12 | Pagination/content type | Page/size/from/to invalidi → `400`; JSON endpoint con media type errato → `415` |

I test d'integrazione database e T1 usano PostgreSQL embedded già verificato nel progetto; i dati sono fittizi. `mvnw.cmd -B verify` è il comando di validazione da README.

## 7. Matrice backend/frontend

- Backend owner: `/api/**`, DTO, validation, error envelope, security, `db/migration/**`, repository e test.
- Frontend owner: `src/main/resources/static/**`; usa solo questo contratto, non conosce schema DB e non implementa logica di autorizzazione client-side come misura di sicurezza.
- Owner del contratto: orchestrator. Ogni modifica di endpoint/status/DTO richiede aggiornamento qui prima di adattare implementazioni.

## 8. Non-obiettivi e limiti da documentare

- Prenotazioni sempre da un posto; nessun pagamento, conferma email o gestione waitlist.
- Feedback non verificato rispetto all'effettiva partecipazione; moderazione recensioni non implementata.
- Statistiche aggregate semplici, senza analisi predittiva, dashboard grafica o analytics esterni.
- API versioning, ordinamenti client-selectable e sorting multiplo non previsti nell'MVP; paginazione limitata a pagina/dimensione e filtro temporale definito sopra.
- Prototipo didattico con dati simulati; non presentarlo come sistema pronto per produzione senza ulteriori hardening, privacy review, backup, monitoraggio e test di carico.
