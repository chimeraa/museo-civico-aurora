// router.js — router minimale basato su hash.
// L'hash evita di dover configurare il fallback lato Spring Boot per le rotte SPA.

import { setMobileNav } from './ui.js';

const routes = [];
const changeListeners = new Set();
let activeToken = 0;
let startPath = '/eventi';

function compile(pattern) {
  const keys = [];
  const regex = pattern
    .replace(/\/:([A-Za-z0-9_]+)/g, (_, key) => { keys.push(key); return '/([^/?]+)'; })
    .replace(/\//g, '\\/');
  return { regex: new RegExp(`^${regex}$`), keys };
}

export function route(pattern, handler, meta = {}) {
  const { regex, keys } = compile(pattern);
  routes.push({ pattern, regex, keys, handler, meta });
}

export function onRouteChange(fn) {
  changeListeners.add(fn);
  return () => changeListeners.delete(fn);
}

export function parseHash(hash = window.location.hash) {
  let raw = (hash || '').replace(/^#/, '');
  if (!raw) raw = startPath;
  if (!raw.startsWith('/')) raw = '/' + raw;
  const [path, queryString = ''] = raw.split('?');
  return { path, query: new URLSearchParams(queryString) };
}

export function currentPath() {
  return parseHash().path;
}

export function pathFor(path, params = {}) {
  return path.replace(/:([A-Za-z0-9_]+)/g, (_, key) => encodeURIComponent(params[key] ?? ''));
}

export function navigate(path, { replace = false } = {}) {
  const target = '#' + (path.startsWith('/') ? path : '/' + path);
  if (window.location.hash === target) {
    resolve();
    return;
  }
  if (replace) window.location.replace(target);
  else window.location.hash = target;
}

function setActiveNav(path, meta) {
  document.querySelectorAll('[data-nav]').forEach((li) => {
    const key = li.dataset.nav;
    const link = li.querySelector('a');
    const active =
      (key === 'catalogue' && (path === '/' || path.startsWith('/eventi'))) ||
      (key === 'bookings' && path.startsWith('/prenotazioni')) ||
      (key === 'admin' && path.startsWith('/admin'));
    if (link) {
      if (active) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    }
    void meta;
  });
  setMobileNav(false);
}

async function resolve() {
  const { path, query } = parseHash();
  const match = routes.find((r) => r.regex.test(path));

  const token = ++activeToken;
  const ctx = {
    path,
    query,
    params: {},
    alive: () => token === activeToken,
  };

  const main = document.getElementById('main');
  setActiveNav(path, match && match.meta);
  for (const fn of changeListeners) {
    try { fn(path, match && match.meta, ctx); } catch (err) { console.error(err); }
  }

  if (!match) {
    main.replaceChildren(renderFallback('Pagina non trovata', 'La pagina richiesta non esiste. Torna al programma eventi.', '/eventi', 'Vai al programma'));
    applyTitleAndFocus(main);
    return;
  }

  match.keys.forEach((key, i) => { ctx.params[key] = decodeURIComponent(match.regex.exec(path)[i + 1]); });
  ctx.meta = match.meta;

  let node;
  try {
    node = await match.handler(ctx);
  } catch (error) {
    if (!ctx.alive()) return;
    console.error(error);
    node = renderErrorNode(error);
  }

  if (!ctx.alive()) return;

  main.replaceChildren(node);

  applyTitleAndFocus(main);
}

// Aggiorna titolo del documento e focus iniziale, applicato a ogni vista
// risolta e anche al fallback per le rotte sconosciute.
function applyTitleAndFocus(main) {
  const heading = main.querySelector('h1');
  if (heading && heading.textContent.trim()) {
    document.title = `${heading.textContent.trim()} — Museo Civico Aurora`;
  } else {
    document.title = 'Museo Civico Aurora — Aurora Cultura';
  }

  // Focus al titolo della vista o al contenitore principale.
  const focusTarget = heading || main;
  if (focusTarget.id !== 'main') focusTarget.setAttribute('tabindex', '-1');
  focusTarget.focus({ preventScroll: true });
  window.scrollTo({ top: 0, behavior: 'auto' });
}

function renderFallback(title, message, href, label) {
  const wrap = document.createElement('div');
  wrap.className = 'wrap view';
  const box = document.createElement('div');
  box.className = 'empty';
  const h1 = document.createElement('h1');
  h1.textContent = title;
  const p = document.createElement('p');
  p.textContent = message;
  const a = document.createElement('a');
  a.className = 'btn btn-outline';
  a.href = '#' + href;
  a.textContent = label;
  box.append(h1, p, a);
  wrap.appendChild(box);
  return wrap;
}

function renderErrorNode(error) {
  const wrap = document.createElement('div');
  wrap.className = 'wrap view';
  const box = document.createElement('div');
  box.className = 'error-block';
  box.setAttribute('role', 'alert');
  const h1 = document.createElement('h1');
  h1.textContent = 'Si è verificato un problema';
  const p = document.createElement('p');
  p.textContent = (error && error.message) || 'Errore imprevisto.';
  box.append(h1, p);
  wrap.appendChild(box);
  return wrap;
}

export function startRouter(defaultPath = '/eventi') {
  startPath = defaultPath;
  window.addEventListener('hashchange', () => { resolve(); });
  resolve();
}
