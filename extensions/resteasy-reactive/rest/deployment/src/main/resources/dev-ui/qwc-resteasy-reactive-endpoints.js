import { LitElement, html, css } from 'lit';
import 'qwc/qwc-endpoints.js';


/**
 * This component shows the Rest Easy Reactive Endpoints
 */
export class QwcResteasyReactiveEndpoints extends LitElement {

    static styles = css`
        :host { 
            display: flex;
            flex-direction: column;
            padding-left: 20px;
        }
    `;

    static properties = {};

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<qwc-endpoints filter="Resource Endpoints"></qwc-endpoints>`;
    }

}
customElements.define('qwc-resteasy-reactive-endpoints', QwcResteasyReactiveEndpoints);