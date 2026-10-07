// dom.js — costruttori di nodi sicuri.
// Nessun innerHTML: ogni stringa proveniente dall'API passa da textContent.

export function el(tag, opts = {}, ...children) {
  const node = document.createElement(tag);
  if (opts) {
    for (const key of Object.keys(opts)) {
      const value = opts[key];
      if (value == null || value === false) continue;
      if (key === 'class') node.className = value;
      else if (key === 'text') node.textContent = value;
      else if (key === 'dataset') Object.assign(node.dataset, value);
      else if (key === 'on') {
        for (const evt of Object.keys(value)) node.addEventListener(evt, value[evt]);
      } else if (key === 'attrs') {
        for (const a of Object.keys(value)) {
          const av = value[a];
          if (av == null || av === false) continue;
          node.setAttribute(a, av === true ? '' : String(av));
        }
      } else if (key === 'props') {
        Object.assign(node, value);
      } else {
        node.setAttribute(key, String(value));
      }
    }
  }
  append(node, children);
  return node;
}

export function append(parent, children) {
  for (const child of children) {
    if (child == null || child === false) continue;
    if (Array.isArray(child)) append(parent, child);
    // nodeType evita problemi tra realm diversi (iframe) oltre a Node.
    else if (typeof child === 'object' && typeof child.nodeType === 'number') parent.appendChild(child);
    else parent.appendChild(document.createTextNode(String(child)));
  }
}

export function frag(...children) {
  const f = document.createDocumentFragment();
  append(f, children);
  return f;
}

export function qs(selector, root = document) {
  return root.querySelector(selector);
}

export function setChildren(parent, ...children) {
  parent.replaceChildren();
  append(parent, children);
  return parent;
}

export function svgIcon(pathData, { size = 18, stroke = 2 } = {}) {
  const NS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(NS, 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('width', size);
  svg.setAttribute('height', size);
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', stroke);
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.setAttribute('aria-hidden', 'true');
  svg.setAttribute('focusable', 'false');
  const path = document.createElementNS(NS, 'path');
  path.setAttribute('d', pathData);
  svg.appendChild(path);
  return svg;
}
