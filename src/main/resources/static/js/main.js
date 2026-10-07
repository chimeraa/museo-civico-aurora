// main.js — avvio dell'applicazione: sessione, navigazione, rotte.

import { el, setChildren } from './dom.js';
import { refreshCsrf } from './api.js';
import { isAdmin, isAuthenticated, getUser, loadSession, logout, onSessionChange } from './session.js';
import { route, startRouter, navigate } from './router.js';
import { announce, loadingBlock, setMobileNav } from './ui.js';
import { catalogueView } from './views/catalogue.js';
import { eventView } from './views/event.js';
import { authView } from './views/auth.js';
import { bookingsView } from './views/bookings.js';
import { adminEventsView } from './views/admin-events.js';
import { adminEventFormView } from './views/admin-form.js';
import { adminBookingsView } from './views/admin-bookings.js';
import { adminFeedbackView } from './views/admin-feedback.js';

/* --- Guardie di rotta ---------------------------------------------------- */
function guard(handler, { auth = false, admin = false, title = '' } = {}) {
  return (ctx) => {
    ctx.routeTitle = title;
    if (admin) {
      if (!isAuthenticated()) return redirectToLogin(ctx);
      if (!isAdmin()) return accessDenied();
    } else if (auth && !isAuthenticated()) {
      return redirectToLogin(ctx);
    }
    return handler(ctx);
  };
}

function redirectToLogin(ctx) {
  navigate(`/accedi?next=${encodeURIComponent(ctx.path)}`, { replace: true });
  const wrap = el('div', { class: 'wrap view' });
  wrap.appendChild(loadingBlock('Reindirizzamento all’accesso'));
  return wrap;
}

function accessDenied() {
  return el('div', { class: 'wrap view' },
    el('div', { class: 'error-block', attrs: { role: 'alert' } },
      el('h1', { text: 'Accesso non consentito' }),
      el('p', { text: 'Questa sezione è riservata agli amministratori. Il tuo account non dispone del ruolo necessario.' }),
      el('div', { class: 'error-actions' },
        el('a', { class: 'btn btn-outline', href: '#/eventi', text: 'Torna al programma eventi' }),
      ),
    ),
  );
}

/* --- Sessione nell'intestazione ----------------------------------------- */
let sessionHost = null;
function renderSession() {
  if (!sessionHost) return;
  const user = getUser();
  const booksLi = document.querySelector('[data-nav="bookings"]');
  const adminLi = document.querySelector('[data-nav="admin"]');
  if (booksLi) booksLi.hidden = !user;
  if (adminLi) adminLi.hidden = !isAdmin();

  if (!user) {
    setChildren(sessionHost,
      el('a', { class: 'btn btn-outline', href: '#/accedi', text: 'Accedi' }),
    );
    return;
  }

  const logoutBtn = el('button', {
    class: 'btn btn-ghost btn-sm', type: 'button', text: 'Esci',
    on: {
      click: async () => {
        logoutBtn.disabled = true;
        try {
          await logout();
          announce('Hai effettuato l’uscita.');
          navigate('/eventi', { replace: false });
        } catch (error) {
          logoutBtn.disabled = false;
          announce(error.message, { assertive: true });
        }
      },
    },
  });

  setChildren(sessionHost,
    el('span', { class: 'session-user' },
      el('span', { class: 'session-name', text: user.displayName }),
      el('span', { class: 'session-role', text: isAdmin() ? 'Amministratore' : 'Visitatore' }),
    ),
    logoutBtn,
  );
}

/* --- Menu mobile --------------------------------------------------------- */
function setupNavToggle() {
  const toggle = document.querySelector('.nav-toggle');
  const nav = document.getElementById('primary-nav');
  if (!toggle || !nav) return;

  const isOpen = () => toggle.getAttribute('aria-expanded') === 'true';

  toggle.addEventListener('click', () => setMobileNav(!isOpen()));

  // Esc chiude il menu e riporta il focus al pulsante.
  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape' || !isOpen()) return;
    setMobileNav(false);
    toggle.focus({ preventScroll: true });
  });

  // Un clic fuori dal menu (o dal pulsante) lo chiude.
  document.addEventListener('click', (event) => {
    if (!isOpen()) return;
    if (nav.contains(event.target) || toggle.contains(event.target)) return;
    setMobileNav(false);
  });
}

/* --- Registrazione rotte ------------------------------------------------- */
function setupRoutes() {
  route('/', guard(catalogueView));
  route('/eventi', guard(catalogueView));
  route('/eventi/:id', guard(eventView));
  route('/accedi', guard(authView));
  route('/prenotazioni', guard(bookingsView, { auth: true }));
  route('/admin/eventi', guard(adminEventsView, { admin: true }));
  route('/admin/eventi/nuovo', guard(adminEventFormView, { admin: true }));
  route('/admin/eventi/:id/modifica', guard(adminEventFormView, { admin: true }));
  route('/admin/eventi/:id/prenotazioni', guard(adminBookingsView, { admin: true }));
  route('/admin/eventi/:id/feedback', guard(adminFeedbackView, { admin: true }));
}

/* --- Avvio --------------------------------------------------------------- */
async function boot() {
  sessionHost = document.getElementById('session-area');
  setupNavToggle();
  onSessionChange(renderSession);
  renderSession();
  setupRoutes();

  try {
    await refreshCsrf();
    await loadSession();
  } catch (error) {
    console.error(error);
    const main = document.getElementById('main');
    main.replaceChildren(el('div', { class: 'wrap view' },
      el('div', { class: 'notice notice--error', attrs: { role: 'alert' } },
        el('p', { text: 'Non è stato possibile contattare il server del museo. Verifica che l’applicazione sia avviata e ricarica la pagina.' }),
      ),
    ));
    return;
  }

  startRouter('/eventi');
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
