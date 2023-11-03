import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/form-layout';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import '@vaadin/grid';
import 'qui-alert';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

import {datasources} from 'build-time-data';


export class QwcDatasourcesReset extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .button {
            cursor: pointer;
        }
        .clearIcon {
            color: orange;
        }
        .message {
          padding: 15px;
          text-align: center;
          margin-left: 20%;
          margin-right: 20%;
          border: 2px solid orange;
          border-radius: 10px;
          font-size: large;
        }
        `;

    static properties = {
        "_ds": {state: true},
        "_message": {state: true}
    }

    connectedCallback() {
        super.connectedCallback();
        this._ds = datasources;
    }

    render() {
        if (this._ds) {
            return this._renderDataSourceTable();
        } else {
            return html`<span>Loading datasources...</span>`;
        }
    }

    _renderDataSourceTable() {
        return html`
                ${this._message}
                <vaadin-grid .items="${this._ds}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header="Name"
                                        ${columnBodyRenderer(this._nameRenderer, [])}>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Action"
                                        ${columnBodyRenderer(this._actionRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

    _actionRenderer(ds) {
        return html`
            <vaadin-button theme="small" @click=${() => this._reset(ds)} class="button">
                <vaadin-icon class="clearIcon" icon="font-awesome-solid:broom"></vaadin-icon> Reset
            </vaadin-button>`;
    }

    _nameRenderer(ds) {
        return html`${ds}`;
    }

    _reset(ds) {
        this._message = '';
        this.jsonRpc.reset({ds: ds}).then(jsonRpcResponse => {
            if (ds === "<default>") {
                ds = "default"
            }
            this._message = html`<qui-alert level="success" showIcon>
                                    <span>The datasource <code>${ds}</code> has been cleared.</span>
                                 </qui-alert>`
        });
    }


}
customElements.define('qwc-datasources-reset', QwcDatasourcesReset);