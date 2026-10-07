// ui.js — componenti condivisi, formattazione, gestione degli errori di modulo.

import { el, setChildren } from './dom.js';

/* --- Annunci per la tecnologia assistiva --------------------------------- */
const statusRegion = () => document.getElementById('status');
let announceTimer = null;
export function announce(message, { assertive = false } = {}) {
  const region = statusRegion();
  if (!region) return;
  region.setAttribute('aria-live', assertive ? 'assertive' : 'polite');
  region.textContent = '';
  window.clearTimeout(announceTimer);
  announceTimer = window.setTimeout(() => { region.textContent = message; }, 40);
}

/* --- Menu di navigazione mobile ----------------------------------------- */
// Unico punto di verità per menu aperto/chiuso: tiene allineati la classe
// visiva, aria-expanded e l'etichetta accessibile visibile del pulsante.
export function setMobileNav(open) {
  const nav = document.getElementById('primary-nav');
  const toggle = document.querySelector('.nav-toggle');
  if (nav) nav.classList.toggle('is-open', open);
  if (toggle) {
    toggle.setAttribute('aria-expanded', String(open));
    const label = toggle.querySelector('.nav-toggle-label');
    if (label) label.textContent = open ? 'Chiudi' : 'Menu';
  }
  return open;
}

/* --- Formattazione date/ora (it-IT) -------------------------------------- */
export function parseDate(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

const dayFmt = new Intl.DateTimeFormat('it-IT', { day: '2-digit' });
const timeFmt = new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' });
const longFmt = new Intl.DateTimeFormat('it-IT', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
const dateTimeFmt = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });

export function fmtDay(value) { const d = parseDate(value); return d ? dayFmt.format(d) : '—'; }
export function fmtMonth(value) {
  const d = parseDate(value);
  if (!d) return '—';
  return new Intl.DateTimeFormat('it-IT', { month: 'short' }).format(d).replace('.', '').toUpperCase();
}
export function fmtTime(value) { const d = parseDate(value); return d ? timeFmt.format(d) : '—'; }
export function fmtLong(value) { const d = parseDate(value); return d ? longFmt.format(d) : '—'; }
export function fmtDateTime(value) { const d = parseDate(value); return d ? dateTimeFmt.format(d) : '—'; }

export function fmtRange(startISO, endISO) {
  const start = parseDate(startISO);
  const end = parseDate(endISO);
  if (!start) return '—';
  if (!end || start.toDateString() === end.toDateString()) {
    return `${fmtLong(start)} · ${timeFmt.format(start)}–${end ? timeFmt.format(end) : '—'}`;
  }
  return `Dal ${fmtDateTime(start)} al ${fmtDateTime(end)}`;
}

export function isStarted(startsAtISO, now = new Date()) {
  const start = parseDate(startsAtISO);
  return !!start && start.getTime() <= now.getTime();
}

/* --- Badge di stato ------------------------------------------------------ */
const BADGE = {
  PUBLISHED: ['badge--published', 'Pubblicato'],
  DRAFT: ['badge--draft', 'Bozza'],
  CANCELLED: ['badge--cancelled', 'Annullato'],
  CONFIRMED: ['badge--confirmed', 'Confermata'],
  ENDED: ['badge--ended', 'Concluso'],
};

export function statusBadge(status, labelOverride) {
  const [cls, label] = BADGE[status] || ['badge--draft', status || '—'];
  return el('span', { class: `badge ${cls}`, text: labelOverride || label });
}

/* --- Stati di interfaccia ------------------------------------------------ */
export function emptyState(title, message, action) {
  return el('div', { class: 'empty' },
    el('h2', { text: title }),
    message ? el('p', { text: message }) : null,
    action ? el('p', { class: 'form-actions', attrs: { style: 'justify-content:center' } }, action) : null,
  );
}

export function loadingBlock(label = 'Caricamento in corso…') {
  return el('div', { class: 'skeleton', attrs: { role: 'status', 'aria-label': label } },
    el('div', { class: 'skeleton-row' }),
    el('div', { class: 'skeleton-row' }),
    el('div', { class: 'skeleton-row' }),
  );
}

export function errorBlock(error, { onRetry } = {}) {
  const title = error.status === 404
    ? 'Elemento non trovato'
    : error.status === 403
      ? 'Accesso non consentito'
      : error.status === 401
        ? 'Accesso richiesto'
        : 'Si è verificato un problema';
  return el('div', { class: 'error-block', attrs: { role: 'alert' } },
    el('h2', { text: title }),
    el('p', { text: error.message || 'Errore imprevisto.' }),
    onRetry ? el('div', { class: 'error-actions' },
      el('button', { class: 'btn btn-outline', type: 'button', text: 'Riprova', on: { click: onRetry } }),
    ) : null,
  );
}

/* --- Modulo: errori e summary ------------------------------------------- */
function errorIdFor(field) {
  return `error-${field}`;
}

export function clearFieldErrors(form) {
  const summary = form.querySelector('.form-error-summary');
  if (summary) summary.remove();
  form.querySelectorAll('[aria-invalid="true"]').forEach((input) => input.removeAttribute('aria-invalid'));
  form.querySelectorAll('.field-error').forEach((node) => node.remove());
  form.querySelectorAll('[data-error-linked]').forEach((input) => {
    const described = (input.getAttribute('aria-describedby') || '')
      .split(/\s+/).filter((id) => id && !id.startsWith('error-'));
    if (described.length) input.setAttribute('aria-describedby', described.join(' '));
    else input.removeAttribute('aria-describedby');
    delete input.dataset.errorLinked;
  });
}

// Applica il formato fieldErrors dell'error envelope e mostra il summary.
export function applyFieldErrors(form, fieldErrors, summaryMessage) {
  clearFieldErrors(form);
  const errors = Array.isArray(fieldErrors) ? fieldErrors : [];
  if (!errors.length) return null;

  const list = el('ul');
  const seen = new Set();
  for (const entry of errors) {
    if (!entry || !entry.field) continue;
    const field = String(entry.field);
    const raw = form.elements[field];
    if (!raw) continue;
    // form.elements restituisce una RadioNodeList per i gruppi di radio.
    const control = raw.nodeType === 1 ? raw : (raw.length ? raw[0] : null);

    if (!seen.has(field)) {
      seen.add(field);
      const errorId = errorIdFor(field);
      const link = el('a', {
        href: `#${(control && control.id) || field}`,
        text: entry.message || 'Campo non valido.',
        on: {
          click: (event) => {
            event.preventDefault();
            if (control && control.focus) control.focus();
          },
        },
      });
      list.appendChild(el('li', {}, link));
    }

    if (control && control.setAttribute) {
      control.setAttribute('aria-invalid', 'true');
      const existing = (control.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
      if (!existing.includes(errorIdFor(field))) existing.push(errorIdFor(field));
      control.setAttribute('aria-describedby', existing.join(' '));
      control.dataset.errorLinked = 'true';
    }

    const fieldWrapper = control && control.closest ? control.closest('.field') : null;
    if (fieldWrapper && !fieldWrapper.querySelector(`#${CSS.escape(errorIdFor(field))}`)) {
      fieldWrapper.appendChild(el('p', {
        class: 'field-error', id: errorIdFor(field), text: entry.message || 'Campo non valido.',
      }));
    }
  }

  const summary = el('div', {
    class: 'form-error-summary', attrs: { role: 'alert', tabindex: '-1' },
  },
    el('h2', { text: summaryMessage || 'Controlla i campi evidenziati' }),
    list.childNodes.length ? list : el('p', { text: 'Alcuni valori non sono accettati dal server.' }),
  );
  form.prepend(summary);
  summary.focus();
  announce('Il modulo contiene errori. Controlla i campi evidenziati.', { assertive: true });
  return summary;
}

// Mostra un errore generale (non legato a un campo) in cima al modulo.
export function showFormError(form, message) {
  clearFieldErrors(form);
  const box = el('div', { class: 'form-error-summary', attrs: { role: 'alert', tabindex: '-1' } },
    el('h2', { text: 'Operazione non riuscita' }),
    el('p', { text: message }),
  );
  form.prepend(box);
  box.focus();
  announce(message, { assertive: true });
}

export function setFormBusy(form, busy) {
  form.querySelectorAll('button, input, select, textarea').forEach((ctrl) => {
    if (busy) ctrl.setAttribute('aria-disabled', 'true');
    else ctrl.removeAttribute('aria-disabled');
    ctrl.disabled = busy;
  });
}

/* --- Dialogo di conferma (elemento nativo <dialog>) ---------------------- */
export function confirmDialog({
  title, message, confirmLabel = 'Conferma', cancelLabel = 'Annulla', danger = false,
}) {
  return new Promise((resolve) => {
    const suffix = Math.random().toString(36).slice(2, 8);
    const titleId = 'dlg-title-' + suffix;
    const messageId = 'dlg-message-' + suffix;
    const dialog = el('dialog', {
      class: 'dialog',
      attrs: {
        tabindex: '-1',
        'aria-labelledby': titleId,
        ...(message ? { 'aria-describedby': messageId } : {}),
      },
    });
    const cancel = el('button', { class: 'btn btn-ghost', type: 'button', text: cancelLabel });
    const confirm = el('button', { class: `btn ${danger ? 'btn-danger' : 'btn-primary'}`, type: 'button', text: confirmLabel });

    dialog.appendChild(el('h2', { id: titleId, text: title }));
    if (message) dialog.appendChild(el('p', { id: messageId, text: message }));
    dialog.appendChild(el('div', { class: 'dialog-actions' }, cancel, confirm));

    let result = false;
    cancel.addEventListener('click', () => dialog.close('cancel'));
    confirm.addEventListener('click', () => { result = true; dialog.close('ok'); });
    dialog.addEventListener('close', () => {
      dialog.remove();
      resolve(result);
    });

    document.body.appendChild(dialog);
    if (typeof dialog.showModal === 'function') {
      dialog.showModal();
      // Il focus sul dialogo fa annunciare nome e descrizione (quindi la
      // conseguenza); il <dialog> nativo ripristina poi il focus sull'elemento
      // che lo aveva aperto, garantendo un ritorno sicuro.
      dialog.focus();
    } else {
      result = window.confirm(message ? `${title}\n\n${message}` : title);
      dialog.remove();
      resolve(result);
    }
  });
}

/* --- Paginazione --------------------------------------------------------- */
export function pager({ page, totalPages, totalElements, size, onPage, onSize }) {
  const hasPrev = page > 0;
  const hasNext = page + 1 < totalPages;
  const status = totalElements === 0
    ? 'Nessun risultato'
    : `Pagina ${page + 1} di ${Math.max(totalPages, 1)} · ${totalElements} risultati`;

  const prev = el('button', {
    class: 'btn btn-ghost btn-sm', type: 'button', text: 'Precedente',
    attrs: hasPrev ? {} : { disabled: true },
    on: { click: () => hasPrev && onPage(page - 1) },
  });
  const next = el('button', {
    class: 'btn btn-ghost btn-sm', type: 'button', text: 'Successiva',
    attrs: hasNext ? {} : { disabled: true },
    on: { click: () => hasNext && onPage(page + 1) },
  });

  let sizeControl = null;
  if (onSize) {
    const select = el('select', {
      attrs: { 'aria-label': 'Risultati per pagina' },
      on: { change: (e) => onSize(Number(e.target.value)) },
    },
      el('option', { value: '20', text: '20 per pagina' }),
      el('option', { value: '50', text: '50 per pagina' }),
    );
    select.value = String(size);
    sizeControl = el('label', { class: 'pager-size' }, select);
  }

  return el('nav', { class: 'pager', attrs: { 'aria-label': 'Paginazione dei risultati' } },
    el('p', { class: 'pager-status', text: status }),
    el('div', { class: 'pager-controls' },
      sizeControl,
      prev,
      next,
    ),
  );
}

/* --- Blocchi accessori --------------------------------------------------- */
export function factsList(items) {
  const dl = el('dl', { class: 'facts' });
  for (const item of items) {
    if (!item) continue;
    dl.appendChild(el('div', {},
      el('dt', { text: item.term }),
      el('dd', {}, item.value),
    ));
  }
  return dl;
}

export function availabilityMeter({ capacity, seatsAvailable }) {
  const total = Number(capacity) || 0;
  const free = Number(seatsAvailable) || 0;
  const taken = Math.max(total - free, 0);
  const percent = total > 0 ? Math.round((taken / total) * 100) : 0;
  const text = free > 0
    ? el('p', { class: 'availability-text' },
        el('strong', { text: String(free) }),
        ` ${free === 1 ? 'posto disponibile' : 'posti disponibili'} su ${total}`,
      )
    : el('p', { class: 'availability-text' }, el('strong', { text: 'Esaurito' }));

  return el('div', { class: 'availability' },
    text,
    el('div', { class: 'meter', attrs: { 'aria-hidden': 'true' } },
      el('div', { class: 'meter-fill', attrs: { style: `width:${percent}%` } }),
    ),
  );
}

// Converte datetime-local <-> ISO 8601 mantenendo l'ora locale.
export function isoToLocalInput(iso) {
  const d = parseDate(iso);
  if (!d) return '';
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

export function localInputToIso(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

export function clearNode(node) {
  setChildren(node);
  return node;
}
