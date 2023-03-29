import { LitElement, html, css} from 'lit';
import { buildItems } from 'devui-data';

/**
 * This component shows the Build Items
 */
export class QwcBuildItems extends LitElement {
  static styles = css`
        .todo {
            padding-left: 10px;
            height: 100%;
        }`;

  static properties = {
    _items: {state: true}
  };
  
  constructor() {
    super();
    this._items = buildItems;
  }

  render() {
    if(this._items){
      return html`<div class="todo">${this._items}</div>`;
    }
  }
}
customElements.define('qwc-build-items', QwcBuildItems);