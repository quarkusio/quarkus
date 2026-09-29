import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the configured OIDC tenants. For each tenant it shows
 * only non-sensitive configuration (tenant id, application type, auth server URL,
 * discovery flag, client id, roles source) and the discovered provider metadata
 * endpoints (issuer, JWKS, authorization, token, user-info, introspection,
 * end-session). It never shows client secrets or credentials, and offers no
 * login / token-acquisition actions.
 */
export class PwcOidcTenants extends LitElement {

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
        .tenant {
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
            padding: 15px;
        }
        .tenant-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;
        }
        .tenant-header h3 {
            margin: 0;
        }
        .disabled {
            color: var(--lumo-error-text-color);
        }
        .enabled {
            color: var(--lumo-success-text-color);
        }
        .section-title {
            font-size: var(--lumo-font-size-s);
            font-weight: 600;
            color: var(--lumo-secondary-text-color);
            margin: 12px 0 4px 0;
        }
        dl {
            display: grid;
            grid-template-columns: max-content 1fr;
            gap: 4px 20px;
            margin: 0;
        }
        dt {
            color: var(--lumo-secondary-text-color);
        }
        dd {
            margin: 0;
            word-break: break-all;
        }
        code {
            font-size: 85%;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _tenants: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getTenants().then(jsonRpcResponse => {
            this._tenants = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._tenants) {
            return html`<span class="empty">Loading OIDC tenants...</span>`;
        }
        if (this._tenants.length === 0) {
            return html`<span class="empty">No OIDC tenants configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._tenants.map(tenant => this._renderTenant(tenant))}`;
    }

    _renderTenant(tenant) {
        return html`
            <div class="tenant">
                <div class="tenant-header">
                    ${tenant.enabled
                        ? html`<vaadin-icon class="enabled" icon="font-awesome-solid:circle-check"></vaadin-icon>`
                        : html`<vaadin-icon class="disabled" icon="font-awesome-solid:circle-xmark"></vaadin-icon>`}
                    <h3><code>${tenant.tenantId}</code></h3>
                </div>
                <dl>
                    ${this._entry('Enabled', tenant.enabled ? 'yes' : 'no')}
                    ${this._entry('Application type', tenant.applicationType)}
                    ${this._entry('Auth server URL', tenant.authServerUrl)}
                    ${this._entry('Discovery enabled', tenant.discoveryEnabled === null ? null : (tenant.discoveryEnabled ? 'yes' : 'no'))}
                    ${this._entry('Client id', tenant.clientId)}
                    ${this._entry('Roles source', tenant.rolesSource)}
                </dl>
                ${this._renderMetadata(tenant.metadata)}
            </div>`;
    }

    _renderMetadata(metadata) {
        if (!metadata) {
            return html`<div class="section-title">Provider metadata not resolved</div>`;
        }
        return html`
            <div class="section-title">Provider metadata</div>
            <dl>
                ${this._entry('Issuer', metadata.issuer)}
                ${this._entry('JWKS', metadata.jwksUri)}
                ${this._entry('Authorization', metadata.authorizationUri)}
                ${this._entry('Token', metadata.tokenUri)}
                ${this._entry('User info', metadata.userInfoUri)}
                ${this._entry('Introspection', metadata.introspectionUri)}
                ${this._entry('End session', metadata.endSessionUri)}
            </dl>`;
    }

    _entry(label, value) {
        if (value === null || value === undefined || value === '') {
            return html``;
        }
        return html`<dt>${label}</dt><dd><code>${value}</code></dd>`;
    }
}
customElements.define('pwc-oidc-tenants', PwcOidcTenants);
