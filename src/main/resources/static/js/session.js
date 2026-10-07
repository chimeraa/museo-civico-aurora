// session.js — stato di sessione del visitatore/amministratore.
// Le credenziali non vengono mai conservate: restano nella singola richiesta.

import { api, refreshCsrf } from './api.js';

let user = null;
const listeners = new Set();

export function getUser() {
  return user;
}

export function isAuthenticated() {
  return user != null;
}

export function isAdmin() {
  return !!user && user.role === 'ADMIN';
}

export function onSessionChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

function emit() {
  for (const fn of listeners) {
    try { fn(user); } catch (err) { console.error(err); }
  }
}

// GET /api/auth/me — 401 significa "non autenticato", non un errore fatale.
export async function loadSession() {
  try {
    user = await api.get('/auth/me');
  } catch (error) {
    if (error.status === 401) user = null;
    else throw error;
  }
  emit();
  return user;
}

// POST /api/auth/login (form-urlencoded, username=email). Il token CSRF ruota.
export async function login(email, password) {
  const logged = await api.post('/auth/login', {
    form: { username: email, password },
  });
  await refreshCsrf();
  user = logged;
  emit();
  return user;
}

// POST /api/auth/register (JSON) -> 201 UserDto. Non autentica la sessione.
export async function register(email, password, displayName) {
  return api.post('/auth/register', {
    body: { email, password, displayName },
  });
}

// POST /api/auth/logout -> 204. Poi il token CSRF viene ruotato.
export async function logout() {
  await api.post('/auth/logout');
  await refreshCsrf();
  user = null;
  emit();
}
