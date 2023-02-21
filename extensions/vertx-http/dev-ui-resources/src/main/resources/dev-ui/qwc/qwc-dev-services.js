import { LitElement, html, css} from 'lit';
import { devServices } from 'devui-data';

/**
 * This component shows the Dev Services Page
 */
export class QwcDevServices extends LitElement {
  static styles = css`
        .todo {
            padding-left: 10px;
            height: 100%;
        }`;

  static properties = {
    _services: {state: true}
  };
  
  constructor() {
    super();
    this._services = devServices;
  }

  render() {
    if(this._services){
      return html`<div class="todo">${this._services}</div>`;
    }
  }
}
customElements.define('qwc-dev-services', QwcDevServices);