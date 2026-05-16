import {LitElement, html, css} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/progress-bar';
import 'qui-alert';

export class QwcKeycloakRealmExport extends LitElement {

    jsonRpc = new JsonRpc("quarkus-devservices-keycloak");

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            padding: 15px;
            gap: 10px;
        }
    `;

    static properties = {
        _loading: {state: true},
        _error: {state: true}
    };

    constructor() {
        super();
        this._loading = false;
        this._error = null;
    }

    render() {
        return html`
            <vaadin-button theme="primary" @click=${this._exportRealm} ?disabled=${this._loading}>
                <vaadin-icon icon="font-awesome-solid:file-export" slot="prefix"></vaadin-icon>
                Export Realm
            </vaadin-button>
            ${this._loading ? html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>` : html``}
            ${this._error ? html`<qui-alert level="error" showIcon><span>${this._error}</span></qui-alert>` : html``}`;
    }

    _exportRealm() {
        this._loading = true;
        this._error = null;
        this.jsonRpc.exportRealm().then(response => {
            this._loading = false;
            const data = response.result;
            if (data.success) {
                const blob = new Blob([data.realmJson], {type: 'application/json'});
                const a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'quarkus-realm.json';
                a.click();
                URL.revokeObjectURL(a.href);
            } else {
                this._error = data.error;
            }
        }).catch(error => {
            this._loading = false;
            this._error = error.message || String(error);
        });
    }
}

customElements.define('qwc-keycloak-realm-export', QwcKeycloakRealmExport);
