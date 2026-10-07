// views/admin-feedback.js — giudizi dei visitatori per un evento (vista amministrativa).

import { el } from '../dom.js';
import { api } from '../api.js';
import {
  loadingBlock, errorBlock, emptyState, pager, fmtDateTime,
} from '../ui.js';

export function adminFeedbackView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const id = ctx.params.id;
  const state = { page: clampPage(ctx.query.get('page')), size: pickSize(ctx.query.get('size')) };
  const listRegion = el('div', { attrs: { 'aria-live': 'polite' } });

  const header = el('header', { class: 'view-head' },
    el('p', { class: 'kicker', text: 'Amministrazione' }),
    el('h1', { text: 'Giudizi evento' }),
    el('p', { class: 'lede', text: 'Valutazioni e commenti lasciati dai visitatori per questo evento. Il giudizio non è verificato rispetto alla partecipazione.' }),
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
      const eventItem = await api.get(`/admin/events/${encodeURIComponent(id)}`);
      if (!ctx.alive() || !eventItem) return;
      header.replaceChildren(
        el('p', { class: 'kicker', text: 'Amministrazione' }),
        el('h1', { text: `Giudizi · ${eventItem.title}` }),
        el('p', { class: 'lede', text: 'Valutazioni e commenti lasciati dai visitatori per questo evento.' }),
      );
    } catch { /* l'elenco resta comunque utilizzabile */ }
  }

  async function load() {
    if (!ctx.alive()) return;
    listRegion.replaceChildren(loadingBlock('Caricamento dei giudizi'));
    try {
      const page = await api.get(`/admin/events/${encodeURIComponent(id)}/feedback`, {
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
      listRegion.replaceChildren(emptyState('Nessun giudizio', 'Per questo evento non è ancora stato lasciato alcun giudizio.'));
      return;
    }

    const table = el('table', { class: 'data' },
      el('caption', { text: 'Le email dei visitatori non sono mostrate.' }),
      el('thead', {},
        el('tr', {},
          el('th', { attrs: { scope: 'col' }, text: 'Visitatore' }),
          el('th', { attrs: { scope: 'col' }, text: 'Valutazione' }),
          el('th', { attrs: { scope: 'col' }, text: 'Commento' }),
          el('th', { attrs: { scope: 'col' }, text: 'Data' }),
        ),
      ),
      el('tbody', {}, content.map(row)),
    );

    listRegion.replaceChildren(
      el('div', { class: 'table-scroll', attrs: { tabindex: '0', role: 'region', 'aria-label': 'Tabella dei giudizi' } }, table),
      pager({
        page: currentPage, totalPages, totalElements, size: state.size,
        onPage: (next) => { state.page = next; load(); },
        onSize: (size) => { state.size = size; state.page = 0; load(); },
      }),
    );
  }

  function row(feedback) {
    const comment = feedback.comment
      ? el('span', { text: feedback.comment })
      : el('span', { class: 'side-note', text: 'Nessun commento' });
    return el('tr', {},
      el('td', { text: feedback.displayName }),
      el('td', {}, ratingMeter(feedback.rating)),
      el('td', {}, comment),
      el('td', { class: 'num', text: fmtDateTime(feedback.createdAt) }),
    );
  }

  return root;
}

function ratingMeter(rating) {
  const value = Math.max(0, Math.min(5, Number(rating) || 0));
  const stars = el('span', { class: 'mono', text: '★'.repeat(value) + '☆'.repeat(5 - value) });
  return el('span', { attrs: { role: 'img', 'aria-label': `${value} su 5` } },
    stars,
    el('span', { class: 'side-note', style: 'margin-left:.5rem', text: `${value}/5` }),
  );
}

function clampPage(value) {
  const n = Number(value);
  return Number.isInteger(n) && n >= 0 ? n : 0;
}
function pickSize(value) {
  return [20, 50].includes(Number(value)) ? Number(value) : 20;
}
