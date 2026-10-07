// views/auth.js — accesso e registrazione dei visitatori.

import { el } from '../dom.js';
import { ApiError } from '../api.js';
import { login, register, isAuthenticated } from '../session.js';
import { navigate } from '../router.js';
import {
  announce, clearFieldErrors, applyFieldErrors, showFormError, setFormBusy,
} from '../ui.js';

export function authView(ctx) {
  const root = el('div', { class: 'wrap view' });
  const next = safeNext(ctx.query.get('next')) || '/eventi';

  if (isAuthenticated()) {
    root.append(el('div', { class: 'empty' },
      el('h1', { text: 'Hai già effettuato l’accesso' }),
      el('p', { text: 'Puoi continuare a consultare il programma o gestire le tue prenotazioni.' }),
      el('p', { class: 'form-actions', attrs: { style: 'justify-content:center' } },
        el('a', { class: 'btn btn-primary', href: `#${next}`, text: 'Continua' }),
      ),
    ));
    return root;
  }

  const panel = el('div', { class: 'panel form-narrow' });
  const tablist = el('div', { class: 'tabs', attrs: { role: 'tablist', 'aria-label': 'Accesso o registrazione' } });
  const loginTab = el('button', { class: 'tab', type: 'button', id: 'tab-login', attrs: { role: 'tab', 'aria-selected': 'true', 'aria-controls': 'panel-login' }, text: 'Accedi' });
  const registerTab = el('button', { class: 'tab', type: 'button', id: 'tab-register', attrs: { role: 'tab', 'aria-selected': 'false', 'aria-controls': 'panel-register' }, text: 'Registrati' });
  tablist.append(loginTab, registerTab);

  const loginPanel = buildLoginForm(next);
  const registerPanel = buildRegisterForm();
  registerPanel.hidden = true;

  tablist.addEventListener('keydown', (event) => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    const toRegister = event.key === 'ArrowRight';
    activate(toRegister ? 'register' : 'login', true);
  });
  loginTab.addEventListener('click', () => activate('login'));
  registerTab.addEventListener('click', () => activate('register'));

  function activate(which, focusTab = false) {
    const showLogin = which === 'login';
    loginPanel.hidden = !showLogin;
    registerPanel.hidden = showLogin;
    loginTab.setAttribute('aria-selected', String(showLogin));
    registerTab.setAttribute('aria-selected', String(!showLogin));
    loginTab.tabIndex = showLogin ? 0 : -1;
    registerTab.tabIndex = showLogin ? -1 : 0;
    if (focusTab) (showLogin ? loginTab : registerTab).focus();
    else (showLogin ? loginPanel : registerPanel).querySelector('input')?.focus();
  }
  loginTab.tabIndex = 0;
  registerTab.tabIndex = -1;

  panel.append(tablist, loginPanel, registerPanel);
  root.append(
    el('header', { class: 'view-head' },
      el('p', { class: 'kicker', text: 'Area visitatori' }),
      el('h1', { text: 'Accedi o registrati' }),
      el('p', { class: 'lede', text: 'Per prenotare un posto e lasciare un giudizio serve un account. Le credenziali restano solo in memoria durante la richiesta e non vengono conservate nel browser.' }),
    ),
    panel,
  );

  return root;

  function buildLoginForm(nextPath) {
    const email = el('input', { type: 'email', id: 'login-email', name: 'email', autocomplete: 'username', required: '', attrs: { maxlength: '254' } });
    const password = el('input', { type: 'password', id: 'login-password', name: 'password', autocomplete: 'current-password', required: '' });
    const form = el('form', { attrs: { id: 'panel-login', role: 'tabpanel', 'aria-labelledby': 'tab-login', novalidate: '' } },
      el('div', { class: 'field' }, el('label', { for: 'login-email', text: 'Email' }), email),
      el('div', { class: 'field' }, el('label', { for: 'login-password', text: 'Password' }), password),
      el('div', { class: 'form-actions' }, el('button', { class: 'btn btn-primary', type: 'submit', text: 'Accedi' })),
    );

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      const emailValue = email.value.trim();
      if (!emailValue || !password.value) {
        const errors = [];
        if (!emailValue) errors.push({ field: 'email', message: 'Inserisci la tua email.' });
        if (!password.value) errors.push({ field: 'password', message: 'Inserisci la tua password.' });
        applyFieldErrors(form, errors);
        return;
      }
      setFormBusy(form, true);
      try {
        await login(emailValue, password.value);
        if (!ctx.alive()) return;
        announce('Accesso effettuato.');
        navigate(nextPath, { replace: true });
      } catch (error) {
        if (!ctx.alive()) return;
        setFormBusy(form, false);
        if (error instanceof ApiError && error.status === 401) {
          showFormError(form, 'Email o password non corretti.');
        } else if (error instanceof ApiError && error.fieldErrors.length) {
          applyFieldErrors(form, error.fieldErrors);
        } else {
          showFormError(form, error.message);
        }
      } finally {
        password.value = '';
      }
    });

    return form;
  }

  function buildRegisterForm() {
    const email = el('input', { type: 'email', id: 'reg-email', name: 'email', autocomplete: 'email', required: '', attrs: { maxlength: '254' } });
    const displayName = el('input', { type: 'text', id: 'reg-name', name: 'displayName', autocomplete: 'name', required: '', attrs: { maxlength: '100' } });
    const password = el('input', { type: 'password', id: 'reg-password', name: 'password', autocomplete: 'new-password', required: '', attrs: { 'aria-describedby': 'reg-password-hint' } });
    const form = el('form', { attrs: { id: 'panel-register', role: 'tabpanel', 'aria-labelledby': 'tab-register', novalidate: '' } },
      el('div', { class: 'field' },
        el('label', { for: 'reg-name', text: 'Nome visualizzato' }),
        displayName,
        el('p', { class: 'hint', text: 'Da 1 a 100 caratteri. Sarà mostrato accanto ai tuoi contributi.' }),
      ),
      el('div', { class: 'field' }, el('label', { for: 'reg-email', text: 'Email' }), email),
      el('div', { class: 'field' },
        el('label', { for: 'reg-password', text: 'Password' }),
        password,
        el('p', { class: 'hint', id: 'reg-password-hint', text: 'Almeno 8 caratteri e non oltre 72 byte.' }),
      ),
      el('div', { class: 'form-actions' }, el('button', { class: 'btn btn-primary', type: 'submit', text: 'Crea account' })),
    );

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFieldErrors(form);
      const errors = [];
      const emailValue = email.value.trim();
      const nameValue = displayName.value.trim();
      const pwd = password.value;

      if (!emailValue) errors.push({ field: 'email', message: 'Inserisci un’email.' });
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue)) errors.push({ field: 'email', message: 'Inserisci un indirizzo email valido.' });
      if (!nameValue) errors.push({ field: 'displayName', message: 'Inserisci un nome visualizzato.' });
      if (!pwd) errors.push({ field: 'password', message: 'Inserisci una password.' });
      else if (pwd.length < 8) errors.push({ field: 'password', message: 'La password deve contenere almeno 8 caratteri.' });
      else if (new TextEncoder().encode(pwd).length > 72) errors.push({ field: 'password', message: 'La password supera il limite di 72 byte consentito.' });

      if (errors.length) { applyFieldErrors(form, errors); return; }

      setFormBusy(form, true);
      try {
        await register(emailValue, pwd, nameValue);
        if (!ctx.alive()) return;
        setFormBusy(form, false);
        announce('Registrazione completata. Ora puoi accedere.');
        form.replaceChildren(el('div', { class: 'notice notice--ok', attrs: { role: 'status' } },
          el('p', { text: 'Account creato. Accedi con le credenziali appena registrate.' }),
        ));
        // Sposta sulla scheda di accesso con l'email precompilata.
        activate('login');
        loginPanel.elements.email.value = emailValue;
        loginPanel.elements.password.focus();
      } catch (error) {
        if (!ctx.alive()) return;
        setFormBusy(form, false);
        if (error instanceof ApiError && error.status === 409 && error.code === 'EMAIL_EXISTS') {
          applyFieldErrors(form, [{ field: 'email', message: 'Esiste già un account con questa email.' }]);
        } else if (error instanceof ApiError && error.fieldErrors.length) {
          applyFieldErrors(form, error.fieldErrors);
        } else {
          showFormError(form, error.message);
        }
      } finally {
        password.value = '';
      }
    });

    return form;
  }
}

function safeNext(value) {
  if (!value) return null;
  // Consenti solo percorsi interni.
  if (value.startsWith('/') && !value.startsWith('//') && !value.includes('\\')) return value;
  return null;
}
