import { LitElement, html, css} from 'lit';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { JsonRpc } from 'jsonrpc';
import { swaggerUiPath } from 'devui-data';

/**
 * This component show all available endpoints
 */
export class QwcEndpoints extends LitElement {
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
        .infogrid {
            width: 99%;
        }
        a {
            cursor: pointer;
            color: var(--lumo-body-text-color);
        }
        a:link { 
            text-decoration: none;
            color: var(--lumo-body-text-color); 
        }
        a:visited { 
            text-decoration: none;
            color: var(--lumo-body-text-color); 
        }
        a:active { 
            text-decoration: none; 
            color: var(--lumo-body-text-color);
        }
        a:hover {
            color: var(--quarkus-blue);
        }
    `;

    static properties = {
        filter: {type: String},
        _info: {state: true}
    }

    constructor() {
        super();
        this._info = null;
        this.filter = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getJsonContent().then(jsonRpcResponse => {
            this._info = jsonRpcResponse.result;
        });
    }
    
    render() {
        if (this._info) {
            const typeTemplates = [];
            for (const [type, list] of Object.entries(this._info)) {
                if(!this.filter || this.filter === type){
                    typeTemplates.push(html`${this._renderType(type,list)}`);
                }
            }
            return html`${typeTemplates}`;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching information...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }

    _renderType(type, items){
        return html`<h3>${type}</h3>
                    <vaadin-grid .items="${items}" class="infogrid" all-rows-visible>
                        <vaadin-grid-sort-column header='URL'
                                                path="uri" 
                                            ${columnBodyRenderer((endpoint) => this._uriRenderer(endpoint, type), [])}>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column 
                                            header="Description" 
                                            path="description"
                                            ${columnBodyRenderer(this._descriptionRenderer, [])}>
                        </vaadin-grid-sort-column>
                    </vaadin-grid>`;
    }

    _uriRenderer(endpoint, type) {
        if (endpoint.uri && (endpoint.description && endpoint.description.startsWith("GET")) || type !== "Resource Endpoints") {
            return html`<a href="${endpoint.uri}" target="_blank">${endpoint.uri}</a>`;
        }else if(swaggerUiPath!==""){
            return html`<a href="${swaggerUiPath}" title="Test this Swagger UI" target="_blank">${endpoint.uri}</a>`;
        }else{
            return html`<span>${endpoint.uri}</span>`;
        }
    }

    _descriptionRenderer(endpoint) {
        if (endpoint.description) {
            return html`<span>${endpoint.description}</span>`;
        }
    }

}
customElements.define('qwc-endpoints', QwcEndpoints);
