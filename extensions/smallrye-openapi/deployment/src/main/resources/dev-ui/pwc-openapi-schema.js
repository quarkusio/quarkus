import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the application's OpenAPI schema. It shows the
 * generated OpenAPI document (JSON) and derives an operation list (path, method,
 * summary and tags) from it client-side. It deliberately omits the Dev UI's
 * embedded Swagger UI "try it out" execution client and the assistant-only
 * client generator: there is no way to invoke an operation from this view.
 */
export class PwcOpenapiSchema extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
            gap: 20px;
            padding: 10px;
        }
        .toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .info {
            color: var(--lumo-secondary-text-color);
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .method {
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-xs);
            text-transform: uppercase;
        }
        .tag {
            display: inline-block;
            margin-right: 4px;
            padding: 1px 6px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-primary-color-10pct);
            font-size: var(--lumo-font-size-xs);
        }
        h4 {
            margin: 0 0 6px 0;
        }
        .schema {
            margin: 0;
            padding: 12px;
            border-radius: var(--lumo-border-radius-m);
            background-color: var(--lumo-contrast-5pct);
            font-family: var(--lumo-font-family-monospace, monospace);
            font-size: var(--lumo-font-size-s);
            white-space: pre;
            overflow: auto;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _schema: { state: true },
        _operations: { state: true },
        _title: { state: true },
        _showRaw: { state: true }
    };

    constructor() {
        super();
        this._showRaw = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getOpenAPISchema().then(jsonRpcResponse => {
            this._schema = jsonRpcResponse.result;
            this._parse(jsonRpcResponse.result);
        });
    }

    _parse(schemaText) {
        this._operations = [];
        this._title = '';
        try {
            const doc = JSON.parse(schemaText);
            if (doc.info) {
                this._title = [doc.info.title, doc.info.version].filter(Boolean).join(' - ');
            }
            const paths = doc.paths || {};
            const methods = ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace'];
            for (const path of Object.keys(paths)) {
                const item = paths[path] || {};
                for (const method of methods) {
                    const operation = item[method];
                    if (operation) {
                        this._operations.push({
                            path: path,
                            method: method,
                            summary: operation.summary || operation.operationId || '',
                            tags: operation.tags || []
                        });
                    }
                }
            }
            this._operations.sort((a, b) => a.path.localeCompare(b.path) || a.method.localeCompare(b.method));
        } catch (e) {
            // Leave the operation list empty; the raw schema is still shown.
        }
    }

    render() {
        if (this._schema === undefined) {
            return html`<span class="empty">Loading OpenAPI schema...</span>`;
        }
        return html`
            <div class="toolbar">
                <span class="info">${this._title}</span>
                <span>
                    <vaadin-button theme="small" @click=${this._toggleRaw} class="button">
                        <vaadin-icon icon="font-awesome-solid:file-code"></vaadin-icon>
                        ${this._showRaw ? 'Hide' : 'Show'} raw schema
                    </vaadin-button>
                    <vaadin-button theme="small" @click=${this._load} class="button">
                        <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                    </vaadin-button>
                </span>
            </div>
            ${this._renderOperations()}
            ${this._showRaw ? this._renderSchema() : html``}`;
    }

    _renderOperations() {
        if (!this._operations || this._operations.length === 0) {
            return html`<span class="empty">No OpenAPI operations.</span>`;
        }
        return html`
            <div>
                <h4>Operations</h4>
                <vaadin-grid .items="${this._operations}" theme="no-border row-stripes compact" all-rows-visible>
                    <vaadin-grid-column auto-width flex-grow="0" header="Method"
                        ${columnBodyRenderer(this._methodRenderer, [])}></vaadin-grid-column>
                    <vaadin-grid-sort-column auto-width header="Path" path="path" frozen></vaadin-grid-sort-column>
                    <vaadin-grid-sort-column auto-width header="Summary" path="summary"></vaadin-grid-sort-column>
                    <vaadin-grid-column auto-width header="Tags"
                        ${columnBodyRenderer(this._tagsRenderer, [])}></vaadin-grid-column>
                </vaadin-grid>
            </div>`;
    }

    _renderSchema() {
        return html`
            <div>
                <h4>Schema (JSON)</h4>
                <pre class="schema">${this._schema}</pre>
            </div>`;
    }

    _toggleRaw() {
        this._showRaw = !this._showRaw;
    }

    _methodRenderer(operation) {
        return html`<span class="method">${operation.method}</span>`;
    }

    _tagsRenderer(operation) {
        return html`${operation.tags.map(tag => html`<span class="tag">${tag}</span>`)}`;
    }
}
customElements.define('pwc-openapi-schema', PwcOpenapiSchema);
