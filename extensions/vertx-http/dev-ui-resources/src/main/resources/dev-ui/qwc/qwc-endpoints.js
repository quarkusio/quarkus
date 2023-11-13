import { LitElement, html, css} from 'lit';
import { basepath } from 'devui-data';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * This component show all available endpoints
 */
export class QwcEndpoints extends LitElement {
    
    static styles = css`
        .infogrid {
            width: 99%;
            height: 99%;
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
            color: var(--quarkus-red);
        }
    `;

    static properties = {
        _info: {state: true},
    }

    constructor() {
        super();
        this._info = null;
    }

    async connectedCallback() {
        super.connectedCallback();
        await this.load();
    }
        
    async load() {
        const response = await fetch(basepath + "/endpoints.json");
        const data = await response.json();
        this._info = data;
    }

    render() {
        if (this._info) {
            const items = [];
            for (const [key, value] of Object.entries(this._info)) {
                items.push({"uri" : key, "description": value});
            }
            
            return html`<vaadin-grid .items="${items}" class="infogrid">
                        <vaadin-grid-sort-column header='URL'
                                                path="uri" 
                                            ${columnBodyRenderer(this._uriRenderer, [])}>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column 
                                            header="Description" 
                                            path="description"
                                            ${columnBodyRenderer(this._descriptionRenderer, [])}>
                        </vaadin-grid-sort-column>
                    </vaadin-grid>`;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching information...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }

    _uriRenderer(endpoint) {
        if (endpoint.uri) {
            return html`<a href="${endpoint.uri}" target="_blank">${endpoint.uri}</a>`;
        }
    }

    _descriptionRenderer(endpoint) {
        if (endpoint.description) {
            return html`<span>${endpoint.description}</span>`;
        }
    }

}
customElements.define('qwc-endpoints', QwcEndpoints);
