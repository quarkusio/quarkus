import { LitElement, html, css} from 'lit';
import { decorators } from 'build-time-data';

/**
 * This component shows the Arc Decorators
 */
export class QwcArcDecorators extends LitElement {

    static styles = css`
        .todo {
            font-size: small;
            color: #4695EB;
            padding-left: 10px;
            background: white;
            height: 100%;
        }`;

    static properties = {
        _decorators: {attribute: false}
    };
  
    constructor() {
        super();
        this._decorators = decorators;
    }
  
    render() {
        if (this._decorators) {
            html`${this._decorators}`;
        }
    }

}
customElements.define('qwc-arc-decorators', QwcArcDecorators);