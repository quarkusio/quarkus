import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * This component shows the Rest Easy Reactive Parameter Converter Providers
 */
export class QwcResteasyReactiveParameterConverterProviders extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        .datatable {
            height: 100%;
            padding-bottom: 10px;
            border: none;
        }`;

    static properties = {
        _paramConverterProviders: {state: true},
    };

    constructor() {
        super();
        this._paramConverterProviders = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getParamConverterProviders().then(paramConverterProviders => {
            this._paramConverterProviders = paramConverterProviders.result;
        });
    }

    render() {
        if(this._paramConverterProviders){
            
            return html`<vaadin-grid .items="${this._paramConverterProviders}" class="datatable" theme="row-stripes">
                <vaadin-grid-sort-column header="Priority" path="priority" resizable auto-width></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Class Name" path="className" resizable auto-width></vaadin-grid-sort-column>
            </vaadin-grid>`;
        }
    }
}
customElements.define('qwc-resteasy-reactive-parameter-converter-providers', QwcResteasyReactiveParameterConverterProviders);