import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/combo-box';
import '@vaadin/progress-bar';
import { notifier } from 'notifier';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@quarkus-webcomponents/codeblock';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class HibernateOrmPersistenceUnitsComponent extends observeState(QwcHotReloadElement) {

    static styles = css`
        :host {
            display: flex;
            padding-left: 10px;
            padding-right: 10px;
        }
        .full-height {
            height: 100%;
            width: 100%;
        }
        a.script-heading {
            display: block;
            float:left;
            width: 90%;
            text-decoration: none;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _persistenceUnits: { state: true, type: Array },
        _selectedPersistenceUnit: { state: true }
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._persistenceUnits = null;
        this._selectedPersistenceUnit = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this.jsonRpc.getInfo().then(response => {
            this._persistenceUnits = response.result.persistenceUnits;
            this._selectedPersistenceUnit = this._persistenceUnits[0] ?? null;
        }).catch(error => {
            console.error("Failed to fetch persistence units:", error);
            this._persistenceUnits = [];
            notifier.showErrorMessage(
                msg(
                    str`Failed to fetch persistence units: ${0}`,
                    {
                        id: 'quarkus-hibernate-orm-failed-to-fetch',
                        args: [String(error)]
                    }
                ),
                'bottom-start',
                30
            );
        });
    }

    render() {
        if (this._persistenceUnits === null) {
            return html`
                <div style="color: var(--lumo-secondary-text-color);width: 95%;">
                    <div>${msg('Fetching persistence units...', { id: 'quarkus-hibernate-orm-fetching-persistence-units' })}</div>
                    <vaadin-progress-bar indeterminate></vaadin-progress-bar>
                </div>`;
        }
        return this._renderAllPUs();
    }

    _renderAllPUs() {
        if (this._persistenceUnits.length === 0) {
            return html`
                <p>
                    ${msg('No persistence units were found.', { id: 'quarkus-hibernate-orm-no-persistence-units' })}
                    <vaadin-button @click="${this.hotReload}" theme="small">
                        ${msg('Check again', { id: 'quarkus-hibernate-orm-check-again' })}
                    </vaadin-button>
                </p>`;
        }
        return html`
            <div class="full-height">
                ${this._persistenceUnits.length > 1 ? html`
                    <vaadin-combo-box
                        label="${msg('Persistence Unit', { id: 'quarkus-hibernate-orm-persistence-unit' })}"
                        item-label-path="name"
                        item-value-path="name"
                        .items="${this._persistenceUnits}"
                        .value="${this._selectedPersistenceUnit?.name || ''}"
                        @value-changed="${this._onPersistenceUnitChanged}"
                        .allowCustomValue="${false}">
                    </vaadin-combo-box>` : ''}
                ${this._selectedPersistenceUnit
                    ? this._renderPersistenceUnit(this._selectedPersistenceUnit)
                    : html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`}
            </div>`;
    }

    _onPersistenceUnitChanged(event) {
        const selected = this._persistenceUnits.find(pu => pu.name === event.detail.value);
        if (selected) {
            this._selectedPersistenceUnit = selected;
        }
    }

    _renderPersistenceUnit(pu) {
        return html`
            <vaadin-details>
                <vaadin-details-summary slot="summary" theme="filled">
                    <vaadin-horizontal-layout
                        theme="spacing"
                        style="align-items: center;">
                        <span>${msg('Create Script', { id: 'quarkus-hibernate-orm-create-script' })}</span>
                        <vaadin-button
                            @click="${(e) => this._copyToClipboard(e, msg('Create Script', { id: 'quarkus-hibernate-orm-create-script' }))}"
                            theme="small">
                            <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                            ${msg('Copy', { id: 'quarkus-hibernate-orm-copy' })}
                        </vaadin-button>
                    </vaadin-horizontal-layout>
                </vaadin-details-summary>
                <qui-code-block
                    mode="sql"
                    content="${pu.createDDL}"
                    theme="${themeState.theme.name}">
                </qui-code-block>
            </vaadin-details>

            <vaadin-details>
                <vaadin-details-summary slot="summary" theme="filled">
                    <vaadin-horizontal-layout
                        theme="spacing"
                        style="align-items: center;">
                        <span>${msg('Update Script', { id: 'quarkus-hibernate-orm-update-script' })}</span>
                        <vaadin-button
                            @click="${(e) => this._copyToClipboard(e, msg('Update Script', { id: 'quarkus-hibernate-orm-update-script' }))}"
                            theme="small">
                            <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                            ${msg('Copy', { id: 'quarkus-hibernate-orm-copy' })}
                        </vaadin-button>
                    </vaadin-horizontal-layout>
                </vaadin-details-summary>
                <qui-code-block
                    mode="sql"
                    content="${pu.updateDDL}"
                    theme="${themeState.theme.name}">
                </qui-code-block>
            </vaadin-details>

            <vaadin-details>
                <vaadin-details-summary slot="summary" theme="filled">
                    <vaadin-horizontal-layout
                        theme="spacing"
                        style="align-items: center;">
                        <span>${msg('Drop Script', { id: 'quarkus-hibernate-orm-drop-script' })}</span>
                        <vaadin-button
                            @click="${(e) => this._copyToClipboard(e, msg('Drop Script', { id: 'quarkus-hibernate-orm-drop-script' }))}"
                            theme="small">
                            <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                            ${msg('Copy', { id: 'quarkus-hibernate-orm-copy' })}
                        </vaadin-button>
                    </vaadin-horizontal-layout>
                </vaadin-details-summary>
                <qui-code-block
                    mode="sql"
                    content="${pu.dropDDL}"
                    theme="${themeState.theme.name}">
                </qui-code-block>
            </vaadin-details>`;
    }

    _copyToClipboard(event, what) {
        event.stopPropagation();
        const text = event.target
            .closest('vaadin-details')
            .querySelector('qui-code-block')
            .value;

        const listener = (ev) => {
            ev.clipboardData.setData('text/plain', text);
            ev.preventDefault();
        };

        document.addEventListener('copy', listener);
        document.execCommand('copy');
        document.removeEventListener('copy', listener);

        notifier.showInfoMessage(
            msg(
                str`Copied "${0}" to clipboard.`,
                {
                    id: 'quarkus-hibernate-orm-copied-to-clipboard',
                    args: [what]
                }
            )
        );
    }

}
customElements.define('hibernate-orm-persistence-units', HibernateOrmPersistenceUnitsComponent);
