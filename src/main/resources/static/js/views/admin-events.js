// views/admin-events.js — elenco amministrativo degli eventi (incluse le bozze).

import { el } from '../dom.js';
import { api, ApiError } from '../api.js';
import {
  announce, loadingBlock, errorBlock, emptyState, pager, statusBadge,
  fmtDateTime, confirmDialog,
} from '../ui.js';

export function adminEventsView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const state = { page: clampPage(ctx.query.get('page')), size: pickSize(ctx.query.get('size')) };
  const listRegion = el('div', { attrs: { 'aria-live': 'polite' } });

  root.append(adminHeader(), listRegion);
  load();

  function adminHeader() {
    return el('header', {},
      el('div', { class: 'admin-head' },
        el('div', {},
          el('p', { class: 'kicker', text: 'Amministrazione' }),
          el('h1', { text: 'Gestione eventi' }),
          el('p', { class: 'lede', text: 'Crea, modifica, pubblica o annulla gli eventi del museo. Le bozze non sono visibili al pubblico.' }),
        ),
        el('a', { class: 'btn btn-primary', href: '#/admin/eventi/nuovo', text: 'Nuovo evento' }),
      ),
      el('nav', { class: 'admin-subnav', attrs: { 'aria-label': 'Sezioni amministrative' } },
        el('a', { class: 'btn btn-ghost btn-sm', href: '#/admin/eventi', attrs: { 'aria-current': 'page' }, text: 'Eventi' }),
        el('a', { class: 'btn btn-ghost btn-sm', href: '#/admin/eventi/nuovo', text: 'Crea evento' }),
      ),
    );
  }

  async function load() {
    if (!ctx.alive()) return;
    listRegion.replaceChildren(loadingBlock('Caricamento degli eventi'));
    try {
      const page = await api.get('/admin/events', { query: { page: state.page, size: state.size } });
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
        'Nessun evento',
        'Non è stato ancora creato alcun evento.',
        el('a', { class: 'btn btn-primary', href: '#/admin/eventi/nuovo', text: 'Crea il primo evento' }),
      ));
      return;
    }

    const table = el('table', { class: 'data' },
      el('caption', { text: 'Elenco completo degli eventi, comprese bozze e annullati.' }),
      el('thead', {},
        el('tr', {},
          el('th', { attrs: { scope: 'col' }, text: 'Evento' }),
          el('th', { attrs: { scope: 'col' }, text: 'Sede' }),
          el('th', { attrs: { scope: 'col' }, text: 'Inizio' }),
          el('th', { attrs: { scope: 'col' }, text: 'Disponibilità' }),
          el('th', { attrs: { scope: 'col' }, text: 'Stato' }),
          el('th', { attrs: { scope: 'col' }, text: 'Azioni' }),
        ),
      ),
      el('tbody', {}, content.map(row)),
    );

    listRegion.replaceChildren(
      el('div', { class: 'table-scroll', attrs: { tabindex: '0', role: 'region', 'aria-label': 'Tabella degli eventi amministrativi' } }, table),
      pager({
        page: currentPage, totalPages, totalElements, size: state.size,
        onPage: (next) => { state.page = next; load(); },
        onSize: (size) => { state.size = size; state.page = 0; load(); },
      }),
    );
  }

  function row(eventItem) {
    const cancelled = eventItem.status === 'CANCELLED';
    const actions = el('div', { class: 'row-actions' });

    if (cancelled) {
      actions.append(el('a', { class: 'btn btn-ghost btn-sm', href: `#/admin/eventi/${encodeURIComponent(eventItem.id)}/prenotazioni`, text: 'Prenotazioni' }));
      actions.append(el('a', { class: 'btn btn-ghost btn-sm', href: `#/admin/eventi/${encodeURIComponent(eventItem.id)}/feedback`, text: 'Feedback' }));
    } else {
      actions.append(
        el('a', { class: 'btn btn-outline btn-sm', href: `#/admin/eventi/${encodeURIComponent(eventItem.id)}/modifica`, text: 'Modifica' }),
        el('a', { class: 'btn btn-ghost btn-sm', href: `#/admin/eventi/${encodeURIComponent(eventItem.id)}/prenotazioni`, text: 'Prenotazioni' }),
        el('a', { class: 'btn btn-ghost btn-sm', href: `#/admin/eventi/${encodeURIComponent(eventItem.id)}/feedback`, text: 'Feedback' }),
        el('button', { class: 'btn btn-danger btn-sm', type: 'button', text: 'Annulla', on: { click: (e) => cancelEvent(eventItem, e.currentTarget) } }),
      );
    }

    return el('tr', {},
      el('td', {}, el('strong', { text: eventItem.title })),
      el('td', { text: eventItem.location }),
      el('td', { class: 'num', text: fmtDateTime(eventItem.startsAt) }),
      el('td', { class: 'num', text: `${eventItem.seatsAvailable} posti` }),
      el('td', {}, statusBadge(eventItem.status)),
      el('td', {}, actions),
    );
  }

  async function cancelEvent(eventItem, button) {
    const ok = await confirmDialog({
      title: 'Annullare l’evento?',
      message: `“${eventItem.title}” sarà annullato e tutte le prenotazioni attive saranno cancellate. L’operazione non è reversibile.`,
      confirmLabel: 'Annulla evento',
      cancelLabel: 'Torna indietro',
      danger: true,
    });
    if (!ok || !ctx.alive()) return;
    button.disabled = true;
    try {
      await api.post(`/admin/events/${encodeURIComponent(eventItem.id)}/cancel`);
      if (!ctx.alive()) return;
      announce('Evento annullato.');
      load();
    } catch (error) {
      if (!ctx.alive()) return;
      button.disabled = false;
      const message = error instanceof ApiError && error.code === 'EVENT_CANNOT_BE_CANCELLED'
        ? 'L’evento è già iniziato e non può essere annullato.'
        : error.message;
      announce(message, { assertive: true });
      const tr = button.closest('tr');
      clearCancelErrorRows(tr);
      tr.parentNode.insertBefore(el('tr', { class: 'cancel-error-row' }, el('td', { attrs: { colspan: '6' } },
        el('div', { class: 'notice notice--error', attrs: { role: 'alert' } }, el('p', { text: message })),
      )), tr.nextSibling);
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
