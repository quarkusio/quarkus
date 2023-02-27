import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { arcState } from './arc-state.js';
import { observeState } from 'lit-element-state';

import './qwc-arc-beans-grid.js';
import './qwc-arc-beans-dependency.js';

/**
 * This component shows the Arc Beans
 */
export class QwcArcBeans extends observeState(LitElement) {

  constructor() {
    super();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    arcState.clear();
  }

  render() {
    return html`${unsafeHTML('<' + arcState.component + '></' + arcState.component + '>')}`;
  }

}
customElements.define('qwc-arc-beans', QwcArcBeans);