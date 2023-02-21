import { LitElement, html, css} from 'lit';
import { allConfiguration } from 'devui-data';

/**
 * This component allows users to change the configuration
 */
export class QwcConfiguration extends LitElement {
  static styles = css`
        .todo {
            padding-left: 10px;
            height: 100%;
        }`;

  static properties = {
    _configurations: {state: true}
  };
  
  constructor() {
    super();
    this._configurations = allConfiguration;
  }

  render() {
    if(this._configurations){
      return html`<div class="todo">${this._configurations}</div>`;
    }
  }
  
}
customElements.define('qwc-configuration', QwcConfiguration);