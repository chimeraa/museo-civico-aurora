// views/admin-bookings.js — prenotazioni di un evento (vista amministrativa).

import { el } from '../dom.js';
import { api, ApiError } from '../api.js';
import {
  announce, loadingBlock, errorBlock, emptyState, pager, statusBadge,
  fmtDateTime, isStarted, confirmDialog,
} from '../ui.js';

export function adminBookingsView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const id = ctx.params.id;
  const state = { page: clampPage(ctx.query.get('page')), size: pickSize(ctx.query.get('size')) };
  const listRegion = el('div', { attrs: { 'aria-live': 'polite' } });
  let eventItem = null;

  const header = el('header', { class: 'view-head' },
    el('p', { class: 'kicker', text: 'Amministrazione' }),
    el('h1', { text: 'Prenotazioni evento' }),
    el('p', { class: 'lede', text: 'Elenco delle prenotazioni per questo evento, con il nome visualizzato dei visitatori.' }),
  );

  root.append(
    el('a', { class: 'back-link', href: '#/admin/eventi' }, '← Gestione eventi'),
    header,
    listRegion,
  );

  loadEventTitle();
  load();

  async function loadEventTitle() {
    try {
      eventItem = await api.get(`/admin/events/${encodeURIComponent(id)}`);
      if (!ctx.alive() || !eventItem) return;
      header.replaceChildren(
        el('p', { class: 'kicker', text: 'Amministrazione' }),
        el('h1', { text: `Prenotazioni · ${eventItem.title}` }),
        el('p', { class: 'lede', text: `Inizio ${fmtDateTime(eventItem.startsAt)} · ${eventItem.location}` }),
      );
    } catch { /* l'elenco resta comunque utilizzabile */ }
  }

  async function load() {
    if (!ctx.alive()) return;
    listRegion.replaceChildren(loadingBlock('Caricamento delle prenotazioni'));
    try {
      const page = await api.get(`/admin/events/${encodeURIComponent(id)}/bookings`, {
        query: { page: state.page, size: state.size },
      });
      if (!ctx.alive()) return;
      render(page);
    } catch (error) {
      if (!ctx.alive()) return;
      listRegion.replaceChildren(errorBlock(error, { onRetry: load }));
    }
  }

  function render(page) {
    const content = Array.isArray(page && page.content) ? page.content : [];
    const totalPages = Number(page && page.totalPages) || 0;
    const totalElements = Number(page && page.totalElements) || 0;
    const currentPage = Number(page && page.page) || 0;

    if (!content.length) {
      listRegion.replaceChildren(emptyState('Nessuna prenotazione', 'Per questo evento non risultano ancora prenotazioni.'));
      return;
    }

    const table = el('table', { class: 'data' },
      el('caption', { text: 'Le email dei visitatori non sono mostrate.' }),
      el('thead', {},
        el('tr', {},
          el('th', { attrs: { scope: 'col' }, text: 'Visitatore' }),
          el('th', { attrs: { scope: 'col' }, text: 'Prenotata il' }),
          el('th', { attrs: { scope: 'col' }, text: 'Stato' }),
          el('th', { attrs: { scope: 'col' }, text: 'Azioni' }),
        ),
      ),
      el('tbody', {}, content.map(row)),
    );

    listRegion.replaceChildren(
      el('div', { class: 'table-scroll', attrs: { tabindex: '0', role: 'region', 'aria-label': 'Tabella delle prenotazioni evento' } }, table),
      pager({
        page: currentPage, totalPages, totalElements, size: state.size,
        onPage: (next) => { state.page = next; load(); },
        onSize: (size) => { state.size = size; state.page = 0; load(); },
      }),
    );
  }

  function row(booking) {
    const started = eventItem ? isStarted(eventItem.startsAt) : false;
    const actions = el('div', { class: 'row-actions' });

    if (booking.status === 'CONFIRMED' && !started) {
      actions.append(el('button', {
        class: 'btn btn-danger btn-sm', type: 'button', text: 'Annulla',
        on: { click: (e) => cancel(booking, e.currentTarget) },
      }));
    } else if (booking.status === 'CONFIRMED' && started) {
      actions.append(el('span', { class: 'side-note', text: 'Evento iniziato' }));
    } else {
      actions.append(el('span', { class: 'side-note', text: '—' }));
    }

    return el('tr', {},
      el('td', { text: booking.visitorDisplayName }),
      el('td', { class: 'num', text: fmtDateTime(booking.createdAt) }),
      el('td', {}, statusBadge(booking.status)),
      el('td', {}, actions),
    );
  }

  async function cancel(booking, button) {
    const ok = await confirmDialog({
      title: 'Annullare la prenotazione?',
      message: `La prenotazione di ${booking.visitorDisplayName} sarà annullata e il posto tornerà disponibile.`,
      confirmLabel: 'Annulla prenotazione',
      cancelLabel: 'Torna indietro',
      danger: true,
    });
    if (!ok || !ctx.alive()) return;
    button.disabled = true;
    try {
      await api.post(`/admin/bookings/${encodeURIComponent(booking.id)}/cancel`);
      if (!ctx.alive()) return;
      announce('Prenotazione annullata.');
      load();
    } catch (error) {
      if (!ctx.alive()) return;
      button.disabled = false;
      const message = error instanceof ApiError && error.code === 'BOOKING_ALREADY_STARTED'
        ? 'L’evento è già iniziato: la prenotazione non può più essere annullata.'
        : error.message;
      announce(message, { assertive: true });
      const tr = button.closest('tr');
      clearCancelErrorRows(tr);
      tr.parentNode.insertBefore(
        el('tr', { class: 'cancel-error-row' }, el('td', { attrs: { colspan: '4' } },
          el('div', { class: 'notice notice--error', attrs: { role: 'alert' } }, el('p', { text: message })),
        )),
        tr.nextSibling,
      );
    }
  }

  return root;
}

function clampPage(value) {
  const n = Number(value);
  return Number.isInteger(n) && n >= 0 ? n : 0;
}
function pickSize(value) {
  return [20, 50].includes(Number(value)) ? Number(value) : 20;
}
// Rimuove eventuali righe d'errore precedenti nella stessa tabella prima di
// inserirne una nuova, evitando che si accumulino.
function clearCancelErrorRows(scope) {
  const table = scope && scope.closest ? scope.closest('table') : null;
  if (!table) return;
  table.querySelectorAll('tr.cancel-error-row').forEach((row) => row.remove());
}
