import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the application's GraphQL schema. It lists the
 * operations (queries, mutations and subscriptions) with their arguments and
 * return types, and shows the generated schema document (SDL). It deliberately
 * omits the Dev UI's GraphiQL execution client: there is no way to run a query,
 * mutation or subscription from this view.
 */
export class PwcGraphqlSchema extends LitElement {

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
            justify-content: flex-end;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .kind {
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
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
        _operations: { state: true },
        _schema: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getOperations().then(jsonRpcResponse => {
            this._operations = jsonRpcResponse.result;
        });
        this.jsonRpc.getSchema().then(jsonRpcResponse => {
            this._schema = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._operations) {
            return html`<span class="empty">Loading GraphQL schema...</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._renderOperations()}
            ${this._renderSchema()}`;
    }

    _renderOperations() {
        if (this._operations.length === 0) {
            return html`<span class="empty">No GraphQL operations.</span>`;
        }
        return html`
            <div>
                <h4>Operations</h4>
                <vaadin-grid .items="${this._operations}" theme="no-border row-stripes compact" all-rows-visible>
                    <vaadin-grid-column auto-width flex-grow="0" header="Kind"
                        ${columnBodyRenderer(this._kindRenderer, [])}></vaadin-grid-column>
                    <vaadin-grid-sort-column auto-width header="Name" path="name" frozen></vaadin-grid-sort-column>
                    <vaadin-grid-column auto-width header="Arguments"
                        ${columnBodyRenderer(this._argumentsRenderer, [])}></vaadin-grid-column>
                    <vaadin-grid-sort-column auto-width header="Returns" path="returnType"></vaadin-grid-sort-column>
                </vaadin-grid>
            </div>`;
    }

    _renderSchema() {
        if (!this._schema) {
            return html``;
        }
        return html`
            <div>
                <h4>Schema (SDL)</h4>
                <pre class="schema">${this._schema}</pre>
            </div>`;
    }

    _kindRenderer(operation) {
        return html`<span class="kind">${operation.kind}</span>`;
    }

    _argumentsRenderer(operation) {
        return html`<code>${operation.arguments}</code>`;
    }
}
customElements.define('pwc-graphql-schema', PwcGraphqlSchema);
