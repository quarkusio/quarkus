import { LitElement, html, css} from 'lit';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@qomponent/qui-badge';
import { JsonRpc } from 'jsonrpc';
import { swaggerUiPath } from 'devui-data';
import { msg, updateWhenLocaleChanges, dynamicMsg } from 'localization';

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
        h3 {
            padding-left: 5px;
        }
        .method-detail {
            color: var(--lumo-contrast-50pct);
            font-size: var(--lumo-font-size-xs);
            margin-left: 6px;
        }
    `;

    static _methodColors = {
        GET:     { background: 'hsla(142, 70%, 45%, 0.12)', color: 'hsl(142, 70%, 35%)' },
        POST:    { background: 'hsla(214, 90%, 50%, 0.12)', color: 'hsl(214, 90%, 40%)' },
        PUT:     { background: 'hsla(35, 90%, 50%, 0.12)',  color: 'hsl(35, 90%, 35%)' },
        DELETE:  { background: 'hsla(0, 75%, 50%, 0.12)',   color: 'hsl(0, 75%, 40%)' },
        PATCH:   { background: 'hsla(280, 65%, 50%, 0.12)', color: 'hsl(280, 65%, 40%)' },
        OPTIONS: { background: 'hsla(190, 60%, 45%, 0.12)', color: 'hsl(190, 60%, 35%)' },
        HEAD:    { background: 'hsla(50, 60%, 45%, 0.12)',  color: 'hsl(50, 60%, 35%)' },
    };

    static properties = {
        filter: {type: String},
        _info: {state: true}
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._info = null;
        this.filter = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getAllEndpoints().then(jsonRpcResponse => {
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
                <div>${msg('Fetching information...', { id: 'endpoints-fetching-info' })}</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }

    _renderType(type, items){
        return html`<h3>${dynamicMsg('endpoints', type)}</h3>
                    <vaadin-grid .items="${items}" class="infogrid" all-rows-visible theme="no-border">
                        <vaadin-grid-sort-column header='${msg('URL', { id: 'endpoints-url' })}'
                                                path="uri"
                                            ${columnBodyRenderer((endpoint) => this._uriRenderer(endpoint, type), [])}>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column
                                            header="${msg('Description', { id: 'endpoints-description' })}"
                                            path="description"
                                            ${columnBodyRenderer(this._descriptionRenderer, [])}>
                        </vaadin-grid-sort-column>
                    </vaadin-grid>`;
    }

    _uriRenderer(endpoint, type) {
        if (endpoint.uri && (endpoint.description && endpoint.description.startsWith("GET")) || type !== "Resource Endpoints") {
            return html`<a href="${endpoint.uri}" target="_blank">${endpoint.uri}</a>`;
        }else if(swaggerUiPath!==""){
            return html`<a href="${swaggerUiPath}" title="${msg('Test this Swagger UI', { id: 'endpoints-test-swagger-ui' })}" target="_blank">${endpoint.uri}</a>`;
        }else{
            return html`<span>${endpoint.uri}</span>`;
        }
    }

    _descriptionRenderer(endpoint) {
        if (endpoint.description) {
            const desc = endpoint.description;
            for (const [method, colors] of Object.entries(QwcEndpoints._methodColors)) {
                if (desc === method || desc.startsWith(method + ' ')) {
                    const detail = desc.substring(method.length).trim();
                    return html`<qui-badge small background="${colors.background}" color="${colors.color}"><span>${method}</span></qui-badge>${detail ? html`<span class="method-detail">${detail}</span>` : ''}`;
                }
            }
            return html`<span>${desc}</span>`;
        }
    }

}
customElements.define('qwc-endpoints', QwcEndpoints);
