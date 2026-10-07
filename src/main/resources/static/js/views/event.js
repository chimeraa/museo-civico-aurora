// views/event.js — dettaglio di un evento pubblicato, prenotazione e feedback.

import { el } from '../dom.js';
import { api, ApiError } from '../api.js';
import { getUser } from '../session.js';
import {
  announce, loadingBlock, errorBlock, factsList, availabilityMeter, statusBadge,
  fmtDateTime, isStarted, clearFieldErrors, applyFieldErrors, showFormError,
  setFormBusy,
} from '../ui.js';

export function eventView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const id = ctx.params.id;
  root.appendChild(loadingBlock('Caricamento del dettaglio evento'));

  load();

  async function load() {
    try {
      const event = await api.get(`/events/${encodeURIComponent(id)}`);
      if (!ctx.alive()) return;
      render(event);
    } catch (error) {
      if (!ctx.alive()) return;
      root.replaceChildren(
        el('a', { class: 'back-link', href: '#/eventi' }, '← Tutti gli eventi'),
        errorBlock(error, { onRetry: load }),
      );
    }
  }

  function render(eventItem) {
    document.title = `${eventItem.title} — Museo Civico Aurora`;
    const ended = isStarted(eventItem.startsAt);
    const full = Number(eventItem.seatsAvailable) <= 0;
    const user = getUser();

    const facts = factsList([
      { term: 'Sede', value: eventItem.location },
      { term: 'Inizio', value: fmtDateTime(eventItem.startsAt) },
      { term: 'Fine', value: fmtDateTime(eventItem.endsAt) },
      { term: 'Capienza', value: String(eventItem.capacity) },
    ]);

    const description = el('div', { class: 'prose' },
      el('h2', { text: 'Descrizione' }),
      eventItem.description
        ? el('p', { text: eventItem.description })
        : el('p', { class: 'side-note', text: 'Nessuna descrizione disponibile per questo evento.' }),
    );

    const article = el('article', {},
      el('a', { class: 'back-link', href: '#/eventi' }, '← Tutti gli eventi'),
      el('p', { class: 'kicker', text: 'Evento' }),
      el('h1', { class: 'detail-title', text: eventItem.title }),
      el('p', {}, statusBadge(ended ? 'ENDED' : 'PUBLISHED', ended ? 'Concluso' : 'Pubblicato')),
      el('div', { style: 'margin-top:1.6rem' }, facts),
      description,
    );

    const side = el('aside', { class: 'side-panel', attrs: { 'aria-label': 'Prenotazione' } },
      el('div', { class: 'panel' },
        el('h2', { style: 'font-size:1.15rem;margin-bottom:.9rem', text: 'Disponibilità' }),
        availabilityMeter(eventItem),
        buildActionArea(),
      ),
    );

    root.replaceChildren(el('div', { class: 'detail-grid' }, article, side));

    if (user) root.appendChild(buildFeedbackSection(eventItem));

    function buildActionArea() {
      const actions = el('div', { class: 'side-actions', style: 'margin-top:1.4rem' });
      const note = el('p', { class: 'side-note' });

      if (!user) {
        note.textContent = 'Accedi per prenotare un posto.';
        actions.append(
          el('a', { class: 'btn btn-primary', href: `#/accedi?next=${encodeURIComponent('/eventi/' + eventItem.id)}`, text: 'Accedi per prenotare' }),
          note,
        );
        return actions;
      }

      if (ended) {
        note.textContent = 'L’evento è già iniziato: non è più possibile prenotare.';
        return el('div', { style: 'margin-top:1.4rem' }, el('div', { class: 'notice' }, el('p', { text: note.textContent })));
      }
      if (full) {
        note.textContent = 'Non ci sono posti disponibili per questo evento.';
        return el('div', { style: 'margin-top:1.4rem' }, el('div', { class: 'notice' }, el('p', { text: note.textContent })));
      }

      const button = el('button', {
        class: 'btn btn-primary', type: 'button', text: 'Prenota il tuo posto',
        on: { click: () => reserve(button) },
      });
      actions.append(button, note);
      return actions;
    }

    async function reserve(button) {
      button.disabled = true;
      const original = button.textContent;
      button.textContent = 'Prenotazione in corso…';
      try {
        await api.post('/bookings', { body: { eventId: Number(eventItem.id) } });
        announce('Prenotazione confermata. La trovi ne “Le mie prenotazioni”.');
        await load();
        window.location.hash = '#/prenotazioni';
      } catch (error) {
        button.disabled = false;
        button.textContent = original;
        let message = error.message;
        if (error instanceof ApiError) {
          if (error.status === 401) {
            message = 'La sessione è scaduta. Accedi di nuovo per prenotare.';
          } else if (error.code === 'EVENT_FULL') {
            message = 'Non ci sono posti disponibili per questo evento.';
          } else if (error.code === 'ALREADY_BOOKED') {
            message = 'Hai già una prenotazione attiva per questo evento.';
          }
        }
        announce(message, { assertive: true });
        const box = el('div', { class: 'notice notice--error', attrs: { role: 'alert' } }, el('p', { text: message }));
        actions.prepend(box);
      }
    }
  }

  function buildFeedbackSection(eventItem) {
    const form = el('form', { class: 'form-narrow', attrs: { 'aria-label': 'Lascia un giudizio', novalidate: '' } });
    const ratingLegend = el('legend', { text: 'Valutazione da 1 a 5' });
    const ratingOptions = el('div', { class: 'rating-options' });
    for (let value = 1; value <= 5; value++) {
      const inputId = `rating-${value}`;
      ratingOptions.append(
        el('input', { type: 'radio', name: 'rating', id: inputId, value: String(value) }),
        el('label', { for: inputId, text: String(value) }),
      );
    }
    const ratingField = el('fieldset', { class: 'field rating', style: 'border:0;padding:0;margin:0 0 1.1rem' }, ratingLegend, ratingOptions);

    const comment = el('textarea', { id: 'comment', name: 'comment', maxlength: '2000', attrs: { 'aria-describedby': 'comment-hint' } });
    const commentField = el('div', { class: 'field' },
      el('label', { for: 'comment', text: 'Commento (facoltativo)' }),
      comment,
      el('p', { class: 'hint', id: 'comment-hint', text: 'Massimo 2.000 caratteri.' }),
    );

    const submit = el('button', { class: 'btn btn-outline', type: 'submit', text: 'Invia giudizio' });
    form.append(
      ratingField,
      commentField,
      el('div', { class: 'form-actions' }, submit),
      el('p', { class: 'side-note', text: 'Puoi lasciare un solo giudizio per evento. Il giudizio è ammesso anche dopo la conclusione dell’evento e non richiede la prenotazione.' }),
    );

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      const rating = form.querySelector('input[name="rating"]:checked');
      if (!rating) {
        applyFieldErrors(form, [{ field: 'rating', message: 'Seleziona una valutazione da 1 a 5.' }]);
        return;
      }
      setFormBusy(form, true);
      try {
        await api.post(`/events/${encodeURIComponent(eventItem.id)}/feedback`, {
          body: { rating: Number(rating.value), comment: comment.value.trim() || null },
        });
        if (!ctx.alive()) return;
        window.setTimeout(() => setFormBusy(form, false), 0);
        form.replaceChildren(el('div', { class: 'notice notice--ok', attrs: { role: 'status' } },
          el('p', { text: 'Grazie, il tuo giudizio è stato registrato.' }),
        ));
        announce('Giudizio registrato.');
      } catch (error) {
        if (!ctx.alive()) return;
        setFormBusy(form, false);
        if (error instanceof ApiError && error.status === 400 && error.fieldErrors.length) {
          applyFieldErrors(form, error.fieldErrors);
        } else if (error instanceof ApiError && error.code === 'FEEDBACK_EXISTS') {
          showFormError(form, 'Hai già lasciato un giudizio per questo evento.');
        } else {
          showFormError(form, error.message);
        }
      }
    });

    return el('section', { class: 'feedback-block', attrs: { 'aria-labelledby': 'feedback-title' } },
      el('h2', { id: 'feedback-title', text: 'Lascia un giudizio' }),
      form,
    );
  }

  return root;
}
