# Diagrammi di supporto — Aurora Cultura (Project Work L-31, Traccia 22)

> Documento di lavoro interno. Contiene diagrammi testuali in sintassi Mermaid, redatti a partire dai fatti di progetto (`docs/api-contract.md`, `V1__init.sql`, codice in `src/main/java/**`). I diagrammi sono materiali di supporto per il report e vanno eventualmente trasferiti nel template DOCX come immagini, senza alterare le immagini già presenti. Non sono presenti nomi, credenziali o dati personali reali.

## 1. Architettura a livelli

```mermaid
flowchart TB
    subgraph CLIENT["Client — browser (stessa origine)"]
        UI["Frontend statico<br/>HTML / CSS / JavaScript vanilla"]
        APIMOD["Modulo API<br/>fetch same-origin + token CSRF"]
        UI --> APIMOD
    end

    subgraph APP["Applicazione Spring Boot 4.1.1 (Java 21)"]
        subgraph SEC["Sicurezza — Spring Security 7.1.1"]
            CHAIN_API["Catena /api/**"]
            CHAIN_FE["Catena asset statici"]
        end
        WEB["Controller REST<br/>/api/** (JSON)"]
        SVC["Servizi applicativi"]
        REPO["Repository Spring Data JPA<br/>Hibernate 7"]
        FLYWAY["Flyway<br/>unica fonte DDL"]
        CHAIN_API --> WEB
        WEB --> SVC
        SVC --> REPO
    end

    DB[("PostgreSQL 17<br/>app_user, cultural_event, booking, feedback")]

    APIMOD -->|"HTTP JSON (nessun CORS)"| CHAIN_API
    UI -->|"asset same-origin"| CHAIN_FE
    REPO -->|"JDBC / HikariCP"| DB
    FLYWAY -->|"migrazioni"| DB
```

Note alla lettura del diagramma:

- Il frontend statico è servito dalla stessa applicazione del backend: non è prevista alcuna configurazione CORS.

- La catena di sicurezza dedicata a `/api/**` è ordinata prima della catena che serve gli asset statici.

- Flyway gestisce lo schema; Hibernate è configurato per validarlo, non per generarlo.

## 2. Modello entità-relazione del database

Lo schema deriva dalla migrazione `V1__init.sql`. I tipi sono quelli PostgreSQL; le date sono `timestamptz`.

```mermaid
erDiagram
    APP_USER ||--o{ BOOKING : "effettua"
    CULTURAL_EVENT ||--o{ BOOKING : "riceve"
    APP_USER ||--o{ FEEDBACK : "scrive"
    CULTURAL_EVENT ||--o{ FEEDBACK : "riceve"

    APP_USER {
        bigint id PK
        varchar email "UNIQUE lower(email)"
        varchar display_name
        varchar password_hash
        varchar role "CHECK VISITOR|ADMIN"
        timestamptz created_at
    }

    CULTURAL_EVENT {
        bigint id PK
        varchar title
        varchar description
        varchar location
        timestamptz starts_at
        timestamptz ends_at "CHECK ends_at >= starts_at"
        integer capacity "CHECK capacity > 0"
        varchar status "CHECK DRAFT|PUBLISHED|CANCELLED"
        timestamptz created_at
        timestamptz updated_at
    }

    BOOKING {
        bigint id PK
        bigint event_id FK
        bigint user_id FK
        varchar status "CHECK CONFIRMED|CANCELLED"
        timestamptz created_at
        timestamptz cancelled_at "nullable"
    }

    FEEDBACK {
        bigint id PK
        bigint event_id FK
        bigint user_id FK
        integer rating "CHECK rating BETWEEN 1 AND 5"
        varchar comment "nullable, max 2000"
        timestamptz created_at
    }
```

Vincoli e indici rilevanti:

- `ux_app_user_email_lower`: indice univoco funzionale su `lower(email)`, unicità case-insensitive.
- `ux_booking_active`: indice univoco parziale su `booking (event_id, user_id) WHERE status = 'CONFIRMED'`, impedisce prenotazioni attive duplicate ma consente di riprenotare dopo un annullamento.
- `ix_booking_event_status`: indice su `(event_id, status)` usato per il conteggio dei posti occupati.
- `ix_event_status_starts`: indice su `(status, starts_at)` per la consultazione del catalogo.
- `ux_feedback_event_user`: vincolo di unicità su `feedback (event_id, user_id)`, un solo feedback per utente ed evento.

## 3. Sequenza — prenotazione di un posto

```mermaid
sequenceDiagram
    actor V as Visitatore
    participant FE as Frontend (same-origin)
    participant API as API /api/**
    participant BS as BookingService
    participant DB as PostgreSQL 17

    V->>FE: Azione "Prenota"
    FE->>API: GET /api/auth/csrf
    API-->>FE: 200 CsrfTokenDto + cookie XSRF-TOKEN
    FE->>API: POST /api/bookings {eventId} + header X-XSRF-TOKEN
    API->>BS: create(principal, request)

    Note over BS,DB: unica transazione
    BS->>DB: SELECT ... FOR UPDATE sulla riga cultural_event
    BS->>DB: COUNT booking CONFIRMED per l'evento

    alt posto disponibile
        BS->>DB: INSERT booking (status = CONFIRMED)
        DB-->>BS: riga inserita
        BS-->>API: BookingDto
        API-->>FE: 201 BookingDto
    else evento pieno
        BS-->>API: 409 EVENT_FULL
        API-->>FE: 409 envelope di errore
    else prenotazione attiva gia' presente
        BS-->>API: 409 ALREADY_BOOKED
        API-->>FE: 409 envelope di errore
    else evento inesistente / non pubblicato
        BS-->>API: 404 NOT_FOUND
        API-->>FE: 404 envelope di errore
    end
```

Note alla lettura:

- Il lock pessimistico sulla riga evento serializza le richieste concorrenti sullo stesso evento.
- Il conteggio delle prenotazioni confermate avviene nella stessa transazione del controllo e dell'inserimento.
- Il vincolo univoco parziale intercetta comunque la doppia prenotazione attiva concorrente; la violazione è tradotta in `409 ALREADY_BOOKED`, mai in errore interno.
- L'identità del visitatore deriva dalla sessione autenticata, non dal corpo della richiesta.

## 4. Flusso di autenticazione e protezione CSRF

```mermaid
sequenceDiagram
    participant FE as Frontend (same-origin)
    participant API as Spring Security 7.1.1 — /api/**

    FE->>API: GET /api/auth/csrf
    API-->>FE: 200 token + Set-Cookie XSRF-TOKEN (SameSite=Lax)

    FE->>API: POST /api/auth/login (form-urlencoded, username = email) + X-XSRF-TOKEN
    alt credenziali valide
        API-->>FE: 200 UserDto + sessione JSESSIONID
        Note over FE,API: il token CSRF viene invalidato/ruotato
    else credenziali non valide
        API-->>FE: 401 INVALID_CREDENTIALS (JSON, nessun redirect)
    end

    FE->>API: GET /api/auth/csrf
    API-->>FE: nuovo token + cookie aggiornato

    FE->>API: POST /api/bookings + X-XSRF-TOKEN
    alt token valido e utente autorizzato
        API-->>FE: 201
    else token assente o non valido
        API-->>FE: 403 CSRF_INVALID
    end

    FE->>API: GET /api/admin/stats (senza ruolo ADMIN)
    API-->>FE: 403 FORBIDDEN

    FE->>API: POST /api/auth/logout + X-XSRF-TOKEN
    API-->>FE: 204 (nessun redirect), sessione terminata
```

Note alla lettura:

- La protezione CSRF è configurata in modalità per applicazioni a pagina singola, con cookie `XSRF-TOKEN` leggibile dal frontend e header `X-XSRF-TOKEN`.
- Il token è richiesto anche per registrazione, login e logout e viene recuperato di nuovo dopo login/logout perché viene ruotato.
- Le richieste anonime agli endpoint API ricevono `401` JSON, mai un reindirizzamento HTML; il conflitto CSRF (`403 CSRF_INVALID`) è distinto dal diniego per ruolo (`403 FORBIDDEN`).
- L'account amministratore di sviluppo è creato solo con profilo `dev` e sole variabili d'ambiente valorizzate; nessuna credenziale predefinita è presente nel repository.
