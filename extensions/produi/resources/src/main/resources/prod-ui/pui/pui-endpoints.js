import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

export class PuiEndpoints extends LitElement {

    jsonRpc = new JsonRpc('quarkus-produi');

    static styles = css`
        :host { display: block; height: 100%; }
        .grid { height: 100%; }
        .path { font-family: monospace; font-size: 13px; }
        .methods { font-size: 12px; color: var(--lumo-secondary-text-color); }
    `;

    static properties = {
        _routes: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getAllRoutes().then(response => {
            this._routes = response.result;
        });
    }

    render() {
        if (!this._routes) {
            return html`<span>Loading endpoints...</span>`;
        }
        return html`
            <vaadin-grid .items="${this._routes}" class="grid" theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width header="Path" path="path"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Methods" path="methods"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pui-endpoints', PuiEndpoints);
