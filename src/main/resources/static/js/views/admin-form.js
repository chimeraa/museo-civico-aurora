// views/admin-form.js — creazione e modifica di un evento (PUT sostitutivo).

import { el } from '../dom.js';
import { api, ApiError } from '../api.js';
import { navigate } from '../router.js';
import {
  announce, loadingBlock, errorBlock, clearFieldErrors, applyFieldErrors,
  showFormError, setFormBusy, isoToLocalInput, localInputToIso,
} from '../ui.js';

export function adminEventFormView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const id = ctx.params.id || null;
  const isEdit = id != null;

  showLoading();
  if (isEdit) loadEvent();
  else renderForm(null);

  return root;

  function showLoading() {
    root.replaceChildren(loadingBlock(isEdit ? 'Caricamento dell’evento' : 'Preparazione del modulo'));
  }

  // Ricarica dentro il root già montato: il retry aggiorna la vista corrente
  // invece di creare e scartare un albero non collegato al DOM.
  function loadEvent() {
    if (!ctx.alive()) return;
    showLoading();
    api.get(`/admin/events/${encodeURIComponent(id)}`)
      .then((eventItem) => {
        if (!ctx.alive()) return;
        if (eventItem.status === 'CANCELLED') {
          root.replaceChildren(
            el('a', { class: 'back-link', href: '#/admin/eventi' }, '← Gestione eventi'),
            el('div', { class: 'notice', attrs: { role: 'status' } },
              el('h1', { style: 'font-size:1.5rem', text: 'Evento annullato' }),
              el('p', { text: 'Un evento annullato è in stato terminale e non può essere modificato.' }),
            ),
          );
          return;
        }
        renderForm(eventItem);
      })
      .catch((error) => {
        if (!ctx.alive()) return;
        root.replaceChildren(
          el('a', { class: 'back-link', href: '#/admin/eventi' }, '← Gestione eventi'),
          errorBlock(error, { onRetry: loadEvent }),
        );
      });
  }

  function renderForm(eventItem) {
    const editing = !!eventItem;
    const confirmed = editing ? Math.max((Number(eventItem.capacity) || 0) - (Number(eventItem.seatsAvailable) || 0), 0) : 0;

    const title = textInput('title', 'Titolo', editing ? eventItem.title : '', { maxlength: 160, required: true });
    const location = textInput('location', 'Sede', editing ? eventItem.location : '', { maxlength: 200, required: true });
    const startsAt = dateInput('startsAt', 'Inizio', editing ? isoToLocalInput(eventItem.startsAt) : '');
    const endsAt = dateInput('endsAt', 'Fine', editing ? isoToLocalInput(eventItem.endsAt) : '');
    const capacity = el('input', {
      type: 'number', id: 'capacity', name: 'capacity', min: '1', step: '1', required: '',
      value: editing ? String(eventItem.capacity) : '',
      attrs: { 'aria-describedby': 'capacity-hint' },
    });
    const description = el('textarea', { id: 'description', name: 'description', maxlength: '5000', attrs: { 'aria-describedby': 'description-hint' } });
    if (editing && eventItem.description) description.value = eventItem.description;

    const statusSelect = el('select', { id: 'status', name: 'status', required: '' });
    if (!editing || eventItem.status === 'DRAFT') {
      statusSelect.append(
        el('option', { value: 'DRAFT', text: 'Bozza (non visibile al pubblico)' }),
        el('option', { value: 'PUBLISHED', text: 'Pubblicato' }),
      );
    } else {
      // PUBLISHED non può retrocedere a DRAFT.
      statusSelect.append(el('option', { value: 'PUBLISHED', text: 'Pubblicato' }));
    }
    statusSelect.value = editing ? eventItem.status : 'DRAFT';

    const form = el('form', { attrs: { novalidate: '' } },
      field('title', title),
      field('location', location),
      el('div', { class: 'field-row' },
        field('startsAt', startsAt),
        field('endsAt', endsAt),
      ),
      el('div', { class: 'field-row' },
        el('div', { class: 'field' },
          el('label', { for: 'capacity', text: 'Capienza' }),
          capacity,
          el('p', { class: 'hint', id: 'capacity-hint', text: editing
            ? `Numero massimo di posti. Non inferiore alle prenotazioni confermate (${confirmed}).`
            : 'Numero massimo di posti, almeno 1.' }),
        ),
        el('div', { class: 'field' },
          el('label', { for: 'status', text: 'Stato' }),
          statusSelect,
        ),
      ),
      el('div', { class: 'field' },
        el('label', { for: 'description', text: 'Descrizione (facoltativa)' }),
        description,
        el('p', { class: 'hint', id: 'description-hint', text: 'Massimo 5.000 caratteri.' }),
      ),
      el('div', { class: 'form-actions' },
        el('button', { class: 'btn btn-primary', type: 'submit', text: editing ? 'Salva modifiche' : 'Crea evento' }),
        el('a', { class: 'btn btn-ghost', href: '#/admin/eventi', text: 'Annulla' }),
      ),
    );

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);

      const values = {
        title: title.value.trim(),
        location: location.value.trim(),
        startsAt: localInputToIso(startsAt.value),
        endsAt: localInputToIso(endsAt.value),
        capacity: Number(capacity.value),
        status: statusSelect.value,
        description: description.value.trim() || null,
      };

      const localErrors = [];
      if (!values.title) localErrors.push({ field: 'title', message: 'Inserisci un titolo.' });
      else if (values.title.length > 160) localErrors.push({ field: 'title', message: 'Il titolo supera i 160 caratteri.' });
      if (!values.location) localErrors.push({ field: 'location', message: 'Inserisci una sede.' });
      if (!values.startsAt) localErrors.push({ field: 'startsAt', message: 'Indica la data e l’ora di inizio.' });
      if (!values.endsAt) localErrors.push({ field: 'endsAt', message: 'Indica la data e l’ora di fine.' });
      if (values.startsAt && values.endsAt && new Date(values.endsAt) < new Date(values.startsAt)) {
        localErrors.push({ field: 'endsAt', message: 'La fine non può precedere l’inizio.' });
      }
      if (!Number.isInteger(values.capacity) || values.capacity < 1) {
        localErrors.push({ field: 'capacity', message: 'La capienza deve essere un numero intero maggiore di zero.' });
      } else if (editing && values.capacity < confirmed) {
        localErrors.push({ field: 'capacity', message: `La capienza non può essere inferiore alle ${confirmed} prenotazioni confermate.` });
      }
      if (values.description && values.description.length > 5000) {
        localErrors.push({ field: 'description', message: 'La descrizione supera i 5.000 caratteri.' });
      }
      if (localErrors.length) { applyFieldErrors(form, localErrors); return; }

      setFormBusy(form, true);
      try {
        if (editing) {
          await api.put(`/admin/events/${encodeURIComponent(id)}`, { body: values });
          announce('Evento aggiornato.');
        } else {
          await api.post('/admin/events', { body: values });
          announce('Evento creato.');
        }
        if (!ctx.alive()) return;
        navigate('/admin/eventi', { replace: true });
      } catch (error) {
        if (!ctx.alive()) return;
        setFormBusy(form, false);
        if (error instanceof ApiError && error.fieldErrors.length) {
          applyFieldErrors(form, error.fieldErrors);
        } else if (error instanceof ApiError && error.code === 'CAPACITY_BELOW_BOOKINGS') {
          applyFieldErrors(form, [{ field: 'capacity', message: 'La capienza non può essere inferiore alle prenotazioni confermate.' }]);
        } else if (error instanceof ApiError && error.code === 'INVALID_EVENT_STATE') {
          showFormError(form, 'L’evento non è in uno stato modificabile.');
        } else {
          showFormError(form, error.message);
        }
      }
    });

    root.replaceChildren(
      el('a', { class: 'back-link', href: '#/admin/eventi' }, '← Gestione eventi'),
      el('header', { class: 'view-head' },
        el('p', { class: 'kicker', text: editing ? 'Modifica evento' : 'Nuovo evento' }),
        el('h1', { text: editing ? 'Modifica “' + eventItem.title + '”' : 'Crea un evento' }),
        el('p', { class: 'lede', text: editing
          ? 'Il salvataggio sostituisce tutti i campi modificabili. Un evento pubblicato non può tornare in bozza.'
          : 'Compila i campi per creare un nuovo evento. Puoi salvarlo come bozza e pubblicarlo in un secondo momento.' }),
      ),
      el('div', { class: 'panel' }, form),
    );
    title.focus();
  }

  function field(name, control) {
    return el('div', { class: 'field' },
      el('label', { for: name, text: labelFor(name) }),
      control,
    );
  }
}

const LABELS = {
  title: 'Titolo',
  location: 'Sede',
  startsAt: 'Inizio',
  endsAt: 'Fine',
};
function labelFor(name) { return LABELS[name] || name; }

function textInput(name, label, value, { maxlength, required } = {}) {
  const attrs = {};
  if (maxlength) attrs.maxlength = String(maxlength);
  if (required) attrs.required = '';
  return el('input', { type: 'text', id: name, name, value, attrs });
}

function dateInput(name, label, value) {
  return el('input', { type: 'datetime-local', id: name, name, value, required: '', attrs: { required: '' } });
}
