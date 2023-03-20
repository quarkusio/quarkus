import { LitElement, html, css} from 'lit';
import { continuousTesting } from 'devui-data';

/**
 * This component shows the Continuous Testing Page
 */
export class QwcContinuousTesting extends LitElement {

  static styles = css`
        .todo {
            padding-left: 10px;
            height: 100%;
        }`;

  static properties = {
    _tests: {state: true}
  };
  
  constructor() {
    super();
    this._tests = continuousTesting;
  }

  render() {
    if(this._tests){
      return html`<div class="todo">${this._tests}</div>`;
    }
  }

}
customElements.define('qwc-continuous-testing', QwcContinuousTesting);