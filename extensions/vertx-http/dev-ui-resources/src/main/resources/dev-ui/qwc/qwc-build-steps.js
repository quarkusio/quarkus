import { LitElement, html, css} from 'lit';
import { buildSteps } from 'devui-data';

/**
 * This component shows the Build Steps Page
 */
export class QwcBuildSteps extends LitElement {
  static styles = css`
        .todo {
            padding-left: 10px;
            height: 100%;
        }`;

  static properties = {
    _steps: {state: true}
  };
  
  constructor() {
    super();
    this._steps = buildSteps;
  }

  render() {
    if(this._steps){
      return html`<div class="todo">${this._steps}</div>`;
    }
  }
}
customElements.define('qwc-build-steps', QwcBuildSteps);