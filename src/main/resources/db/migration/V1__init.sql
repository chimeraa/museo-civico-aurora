-- Museo Civico Aurora — schema iniziale (fonte unica DDL: Flyway).
-- Hibernate usa ddl-auto=validate e non modifica lo schema.

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_app_user_role CHECK (role IN ('VISITOR', 'ADMIN'))
);

-- Unicita' case-insensitive dell'email imposta anche a livello DB.
CREATE UNIQUE INDEX ux_app_user_email_lower ON app_user (lower(email));

CREATE TABLE cultural_event (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(160) NOT NULL,
    description VARCHAR(5000),
    location    VARCHAR(200) NOT NULL,
    starts_at   TIMESTAMPTZ  NOT NULL,
    ends_at     TIMESTAMPTZ  NOT NULL,
    capacity    INTEGER      NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_event_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CONSTRAINT chk_event_capacity CHECK (capacity > 0),
    CONSTRAINT chk_event_dates CHECK (ends_at >= starts_at)
);

CREATE INDEX ix_event_status_starts ON cultural_event (status, starts_at);

CREATE TABLE booking (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT      NOT NULL REFERENCES cultural_event (id),
    user_id      BIGINT      NOT NULL REFERENCES app_user (id),
    status       VARCHAR(16) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT chk_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

-- Impedisce prenotazioni attive duplicate ma consente di riprenotare dopo annullamento.
CREATE UNIQUE INDEX ux_booking_active ON booking (event_id, user_id) WHERE status = 'CONFIRMED';

CREATE INDEX ix_booking_event_status ON booking (event_id, status);
CREATE INDEX ix_booking_user_created ON booking (user_id, created_at);

CREATE TABLE feedback (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT       NOT NULL REFERENCES cultural_event (id),
    user_id    BIGINT       NOT NULL REFERENCES app_user (id),
    rating     INTEGER      NOT NULL,
    comment    VARCHAR(2000),
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ux_feedback_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX ix_feedback_event_created ON feedback (event_id, created_at);
