// api.js — client HTTP same-origin con gestione CSRF e error envelope.
// Contratto: museo-civico-aurora/docs/api-contract.md

const BASE = '/api';
const UNSAFE = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export class ApiError extends Error {
  constructor(status, envelope, fallback) {
    super((envelope && envelope.message) || fallback || `Errore di rete (${status}).`);
    this.name = 'ApiError';
    this.status = status;
    this.code = (envelope && envelope.code) || null;
    this.fieldErrors = (envelope && Array.isArray(envelope.fieldErrors)) ? envelope.fieldErrors : [];
    this.envelope = envelope || null;
  }
}

// Messaggi di riserva quando il server non fornisce `message`.
const FALLBACK_BY_CODE = {
  UNAUTHENTICATED: 'Devi accedere per continuare.',
  INVALID_CREDENTIALS: 'Email o password non corretti.',
  FORBIDDEN: 'Non hai i permessi per questa operazione.',
  CSRF_INVALID: 'Sessione di sicurezza scaduta. Riprova.',
  NOT_FOUND: 'Risorsa non trovata.',
  EVENT_FULL: 'Non ci sono posti disponibili per questo evento.',
  ALREADY_BOOKED: 'Hai già una prenotazione attiva per questo evento.',
  FEEDBACK_EXISTS: 'Hai già lasciato un giudizio per questo evento.',
  EMAIL_EXISTS: 'Esiste già un account con questa email.',
  INVALID_EVENT_STATE: "Operazione non consentita nello stato attuale dell'evento.",
  CAPACITY_BELOW_BOOKINGS: 'La capienza non può essere inferiore alle prenotazioni confermate.',
  EVENT_ALREADY_STARTED: "L'evento è già iniziato.",
  BOOKING_ALREADY_STARTED: "L'evento è già iniziato: la prenotazione non può più essere annullata.",
  EVENT_CANNOT_BE_CANCELLED: "L'evento è già iniziato e non può essere annullato.",
  VALIDATION_ERROR: 'Alcuni campi non sono validi.',
  INVALID_PAGE: 'Parametro di pagina non valido.',
  INVALID_PAGE_SIZE: 'Dimensione di pagina non valida.',
  INVALID_TIME_RANGE: 'Intervallo di date non valido.',
};

let csrf = { token: null, headerName: 'X-XSRF-TOKEN', cookieName: 'XSRF-TOKEN' };
let csrfReady = null; // Promise in-flight

function readCookie(name) {
  if (!name) return null;
  const pattern = new RegExp('(?:^|; )' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '=([^;]*)');
  const match = document.cookie.match(pattern);
  return match ? decodeURIComponent(match[1]) : null;
}

function currentToken() {
  // Il cookie è la fonte autorevole per il pattern .spa() di Spring Security.
  return readCookie(csrf.cookieName) || csrf.token || null;
}

export function getCsrfInfo() {
  return { ...csrf };
}

// GET /api/auth/csrf — materializza/ruota il token.
// Va richiamato all'avvio e DOPO login e logout.
export async function refreshCsrf() {
  const res = await fetch(`${BASE}/auth/csrf`, {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });
  if (!res.ok) {
    throw new ApiError(res.status, null, 'Impossibile inizializzare la sessione di sicurezza.');
  }
  const dto = await res.json();
  csrf = {
    token: dto && dto.token ? String(dto.token) : null,
    headerName: (dto && dto.headerName) || 'X-XSRF-TOKEN',
    cookieName: (dto && dto.cookieName) || 'XSRF-TOKEN',
  };
  return { ...csrf };
}

export async function ensureCsrf() {
  if (currentToken()) return;
  if (!csrfReady) {
    csrfReady = refreshCsrf().finally(() => { csrfReady = null; });
  }
  await csrfReady;
}

function buildUrl(path, query) {
  const url = new URL(BASE + path, window.location.origin);
  if (query) {
    for (const key of Object.keys(query)) {
      const value = query[key];
      if (value == null || value === '') continue;
      url.searchParams.set(key, value);
    }
  }
  return url;
}

// Richiesta generica. opts: { query, body (JSON), form (urlencoded), headers, _retried }
export async function request(method, path, opts = {}) {
  const unsafe = UNSAFE.has(method);
  if (unsafe) await ensureCsrf();

  const url = buildUrl(path, opts.query);
  const headers = Object.assign({ Accept: 'application/json' }, opts.headers || {});
  let body;

  if (opts.form != null) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
    body = new URLSearchParams(opts.form).toString();
  } else if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(opts.body);
  }
  // Richieste unsafe senza corpo (es. logout, cancel): nessun content-type.

  if (unsafe) {
    const token = currentToken();
    if (token) headers[csrf.headerName] = token;
  }

  let res;
  try {
    res = await fetch(url, { method, credentials: 'same-origin', headers, body });
  } catch (cause) {
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Connessione al server non riuscita. Verifica la rete e riprova.' }, 'Connessione al server non riuscita.');
  }

  if (res.status === 204) return null;

  let data = null;
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try { data = await res.json(); } catch { data = null; }
  }

  if (!res.ok) {
    const code = data && data.code;
    const fallback = FALLBACK_BY_CODE[code];
    const error = new ApiError(res.status, data, fallback);

    // Token CSRF scaduto/ruotato: un solo nuovo tentativo.
    if (res.status === 403 && error.code === 'CSRF_INVALID' && !opts._retried) {
      await refreshCsrf();
      return request(method, path, Object.assign({}, opts, { _retried: true }));
    }
    throw error;
  }

  return data;
}

export const api = {
  get: (path, opts) => request('GET', path, opts),
  post: (path, opts) => request('POST', path, opts),
  put: (path, opts) => request('PUT', path, opts),
  del: (path, opts) => request('DELETE', path, opts),
};
