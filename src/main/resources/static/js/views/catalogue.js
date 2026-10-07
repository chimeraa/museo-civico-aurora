// views/catalogue.js — programma pubblico degli eventi pubblicati.

import { el } from '../dom.js';
import { api } from '../api.js';
import {
  emptyState, loadingBlock, errorBlock, pager, fmtDay, fmtMonth, fmtTime,
  fmtRange, isStarted,
} from '../ui.js';

export function catalogueView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const query = ctx.query;
  const state = {
    page: clampInt(query.get('page'), 0, 0),
    size: [20, 50].includes(Number(query.get('size'))) ? Number(query.get('size')) : 20,
    from: query.get('from') || '',
    to: query.get('to') || '',
  };

  const listRegion = el('div', { attrs: { 'aria-live': 'polite' } });
  const summary = el('p', { class: 'filter-summary' });
  const filterForm = buildFilterForm();
  const results = el('section', { attrs: { 'aria-label': 'Elenco degli eventi' } }, summary, listRegion);

  const hero = el('header', { class: 'hero' },
    el('div', { class: 'hero-inner' },
      el('p', { class: 'kicker', text: 'Programma culturale' }),
      el('h1', {}, 'Eventi in ', el('em', { text: 'programma' })),
      el('p', { class: 'lede', text: 'Visite, incontri e laboratori del Museo Civico Aurora. La disponibilità è aggiornata in tempo reale: scegli un evento e prenota il tuo posto.' }),
    ),
  );

  root.append(hero, filterForm.form, results);
  load();

  function buildFilterForm() {
    const fromInput = el('input', {
      type: 'date', id: 'filter-from', value: state.from,
      attrs: { name: 'from' },
    });
    const toInput = el('input', {
      type: 'date', id: 'filter-to', value: state.to,
      attrs: { name: 'to' },
    });

    const form = el('form', {
      class: 'filter-bar', attrs: { 'aria-label': 'Filtra gli eventi per data' },
      on: {
        submit: (event) => {
          event.preventDefault();
          state.from = fromInput.value;
          state.to = toInput.value;
          state.page = 0;
          syncUrl();
          load();
        },
        reset: () => {
          window.setTimeout(() => {
            state.from = ''; state.to = ''; state.page = 0;
            syncUrl();
            load();
          }, 0);
        },
      },
    },
      el('div', { class: 'field' },
        el('label', { for: 'filter-from', text: 'Dal' }),
        fromInput,
      ),
      el('div', { class: 'field' },
        el('label', { for: 'filter-to', text: 'Al' }),
        toInput,
      ),
      el('div', { class: 'filter-actions' },
        el('button', { class: 'btn btn-primary', type: 'submit', text: 'Filtra' }),
        el('button', { class: 'btn btn-ghost', type: 'reset', text: 'Azzera' }),
      ),
    );
    return { form, fromInput, toInput };
  }

  async function load() {
    if (!ctx.alive()) return;
    listRegion.replaceChildren(loadingBlock('Caricamento del programma'));
    summary.textContent = '';

    const query = { page: state.page, size: state.size };
    if (state.from) query.from = toBoundaryIso(state.from, 'start');
    if (state.to) query.to = toBoundaryIso(state.to, 'end');

    try {
      const page = await api.get('/events', { query });
      if (!ctx.alive()) return;
      renderPage(page);
    } catch (error) {
      if (!ctx.alive()) return;
      summary.textContent = '';
      listRegion.replaceChildren(errorBlock(error, { onRetry: load }));
    }
  }

  function renderPage(page) {
    const content = Array.isArray(page && page.content) ? page.content : [];
    const totalPages = Number(page && page.totalPages) || 0;
    const totalElements = Number(page && page.totalElements) || 0;
    const currentPage = Number(page && page.page) || 0;

    if (!content.length) {
      summary.textContent = '';
      listRegion.replaceChildren(emptyState(
        'Nessun evento in programma',
        state.from || state.to
          ? 'Non ci sono eventi pubblicati nell’intervallo di date selezionato. Prova ad ampliare il periodo.'
          : 'Al momento non ci sono eventi pubblicati. Torna a trovarci prossimamente.',
      ));
      return;
    }

    const from = state.from ? fmtLongSafe(state.from) : null;
    const to = state.to ? fmtLongSafe(state.to) : null;
    summary.textContent = from && to
      ? `Eventi dal ${from} al ${to}.`
      : from
        ? `Eventi a partire dal ${from}.`
        : to
          ? `Eventi fino al ${to}.`
          : `${totalElements} ${totalElements === 1 ? 'evento pubblicato' : 'eventi pubblicati'}.`;

    const list = el('ol', { class: 'program' });
    content.forEach((eventItem, index) => {
      list.appendChild(programRow(eventItem, currentPage * state.size + index + 1));
    });

    listRegion.replaceChildren(
      list,
      pager({
        page: currentPage,
        totalPages,
        totalElements,
        size: state.size,
        onPage: (next) => { state.page = next; syncUrl(); load(); },
        onSize: (size) => { state.size = size; state.page = 0; syncUrl(); load(); },
      }),
    );
  }

  function syncUrl() {
    const params = new URLSearchParams();
    if (state.page) params.set('page', String(state.page));
    if (state.size !== 20) params.set('size', String(state.size));
    if (state.from) params.set('from', state.from);
    if (state.to) params.set('to', state.to);
    const qs = params.toString();
    window.history.replaceState(null, '', `#/eventi${qs ? '?' + qs : ''}`);
  }

  return root;
}

function programRow(item, index) {
  const full = Number(item.seatsAvailable) <= 0;
  const ended = isStarted(item.startsAt);
  return el('li', { class: 'program-item' },
    el('a', { class: 'program-link', href: `#/eventi/${encodeURIComponent(item.id)}` },
      el('span', { class: 'program-index', text: String(index).padStart(2, '0') }),
      el('span', { class: 'program-date', attrs: { 'aria-hidden': 'true' } },
        el('span', { class: 'program-day', text: fmtDay(item.startsAt) }),
        el('span', { class: 'program-mon', text: fmtMonth(item.startsAt) }),
      ),
      el('span', { class: 'program-body' },
        el('span', { class: 'program-title', text: item.title }),
        el('span', { class: 'program-loc', text: item.location + ' · ' + fmtRange(item.startsAt, item.endsAt) }),
      ),
      el('span', { class: 'program-meta' },
        el('span', { class: 'program-time', text: fmtTime(item.startsAt) }),
        el('span', {
          class: 'program-seats' + (full && !ended ? ' is-full' : ''),
          text: ended ? 'Concluso' : (full ? 'Esaurito' : `${item.seatsAvailable} posti`),
        }),
      ),
    ),
  );
}

function clampInt(value, min, fallback) {
  const n = Number(value);
  return Number.isInteger(n) && n >= min ? n : fallback;
}

function toBoundaryIso(dateString, edge) {
  const [y, m, d] = dateString.split('-').map(Number);
  if (!y || !m || !d) return null;
  const date = edge === 'start'
    ? new Date(y, m - 1, d, 0, 0, 0, 0)
    : new Date(y, m - 1, d, 23, 59, 59, 999);
  return date.toISOString();
}

function fmtLongSafe(dateString) {
  const [y, m, d] = dateString.split('-').map(Number);
  if (!y || !m || !d) return dateString;
  return new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', year: 'numeric' })
    .format(new Date(y, m - 1, d));
}
