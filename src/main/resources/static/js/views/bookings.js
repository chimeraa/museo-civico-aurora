// views/bookings.js — prenotazioni del visitatore autenticato.

import { el } from '../dom.js';
import { api, ApiError } from '../api.js';
import {
  announce, loadingBlock, errorBlock, emptyState, pager, statusBadge,
  fmtDateTime, isStarted, confirmDialog,
} from '../ui.js';

export function bookingsView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const state = { page: clampPage(ctx.query.get('page')), size: pickSize(ctx.query.get('size')) };

  const listRegion = el('div', { attrs: { 'aria-live': 'polite' } });

  root.append(
    el('header', { class: 'view-head' },
      el('p', { class: 'kicker', text: 'Area visitatori' }),
      el('h1', { text: 'Le mie prenotazioni' }),
      el('p', { class: 'lede', text: 'Qui trovi le prenotazioni attive e quelle annullate. Puoi annullare una prenotazione fino all’inizio dell’evento; il posto torna disponibile.' }),
    ),
    listRegion,
  );

  load();

  async function load() {
    if (!ctx.alive()) return;
    listRegion.replaceChildren(loadingBlock('Caricamento delle prenotazioni'));
    try {
      const page = await api.get('/bookings/me', { query: { page: state.page, size: state.size } });
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
      listRegion.replaceChildren(emptyState(
        'Nessuna prenotazione',
        'Non hai ancora prenotato nessun evento. Consulta il programma per trovare un’attività adatta a te.',
        el('a', { class: 'btn btn-primary', href: '#/eventi', text: 'Vai al programma eventi' }),
      ));
      return;
    }

    const tickets = el('div', { class: 'tickets' });
    for (const booking of content) tickets.appendChild(ticket(booking));

    listRegion.replaceChildren(
      tickets,
      pager({
        page: currentPage, totalPages, totalElements, size: state.size,
        onPage: (next) => { state.page = next; load(); },
        onSize: (size) => { state.size = size; state.page = 0; load(); },
      }),
    );
  }

  function ticket(booking) {
    const cancelled = booking.status === 'CANCELLED';
    const started = isStarted(booking.startsAt);
    const actions = el('div', { class: 'row-actions', style: 'justify-content:flex-end' });

    if (!cancelled) {
      if (started) {
        actions.append(el('p', { class: 'side-note', text: 'Evento iniziato: non annullabile.' }));
      } else {
        actions.append(el('button', {
          class: 'btn btn-danger btn-sm', type: 'button', text: 'Annulla prenotazione',
          on: { click: (event) => cancel(booking, event.currentTarget) },
        }));
      }
    }

    return el('article', { class: 'ticket' + (cancelled ? ' is-cancelled' : '') },
      el('div', { class: 'ticket-body' },
        el('h2', { class: 'ticket-title', text: booking.eventTitle }),
        el('p', { class: 'ticket-meta', text: 'Inizio: ' + fmtDateTime(booking.startsAt) }),
        el('p', { class: 'ticket-meta', text: 'Prenotata il ' + fmtDateTime(booking.createdAt) + (booking.cancelledAt ? ' · annullata il ' + fmtDateTime(booking.cancelledAt) : '') }),
      ),
      el('div', { class: 'ticket-side' },
        statusBadge(booking.status),
        el('a', { class: 'btn btn-ghost btn-sm', href: `#/eventi/${encodeURIComponent(booking.eventId)}`, text: 'Vedi evento' }),
        actions,
      ),
    );
  }

  async function cancel(booking, button) {
    const ok = await confirmDialog({
      title: 'Annullare la prenotazione?',
      message: `La prenotazione per “${booking.eventTitle}” sarà annullata e il posto tornerà disponibile. Potrai prenotare di nuovo in seguito.`,
      confirmLabel: 'Annulla prenotazione',
      cancelLabel: 'Torna indietro',
      danger: true,
    });
    if (!ok || !ctx.alive()) return;

    button.disabled = true;
    button.textContent = 'Annullamento…';
    try {
      await api.del(`/bookings/${encodeURIComponent(booking.id)}`);
      if (!ctx.alive()) return;
      announce('Prenotazione annullata.');
      load();
    } catch (error) {
      if (!ctx.alive()) return;
      button.disabled = false;
      button.textContent = 'Annulla prenotazione';
      const message = error instanceof ApiError && error.code === 'BOOKING_ALREADY_STARTED'
        ? 'L’evento è già iniziato: la prenotazione non può più essere annullata.'
        : error.message;
      announce(message, { assertive: true });
      button.closest('.ticket').appendChild(el('div', { class: 'notice notice--error', style: 'margin-top:1rem', attrs: { role: 'alert' } }, el('p', { text: message })));
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
