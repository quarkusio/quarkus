import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { observeState } from 'lit-element-state';
import { devuiState } from 'devui-state';
import 'qui-themed-code-block';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/progress-bar';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component allows users to change the configuration in an online editor
 */
export class QwcConfigurationEditor extends observeState(LitElement) {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction:column;
            gap: 20px;
        }
        .toolbar {
            display: flex;
            gap: 20px;
            align-items: center;
        }
    `;

    static properties = {      
        _type: {state: true},
        _value: {state: true},
        _error: {state: true},
        _inProgress: {state: true, type: Boolean}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._error = null;
        this._value = null;
        this._type = null;
        this._inProgress = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getProjectProperties().then(e => {
            if(e.result.error){
                this._error = e.result.error;
            }else{
                this._type = e.result.type;
                this._value = e.result.value;
            }
        });

        this.addEventListener('keydown', this._handleCtrlS);
    }

    disconnectedCallback() {
        this.removeEventListener('keydown', this._handleCtrlS);
        super.disconnectedCallback();
    }

    _handleCtrlS(e){
        if (e.ctrlKey && e.key === 's') {
            e.preventDefault();
            this._save();
        }
    }

    render() {
        if(this._error){
            return html`<span>${msg('Error', { id: 'configuration-error' })}: ${this._error}</span>`;
        }

        return html`
        ${this._renderToolbar()}
        <qui-themed-code-block id="code"
            mode='${this._type}'
            content='${this._value}'
            value='${this._value}'
            editable>
        </qui-themed-code-block>`;
    }

    _renderToolbar(){
        return html`<div class="toolbar">
            <code>application.${this._type}</code>
            ${this._renderProgressOrButton()}
        </div>`;
    }

    _renderProgressOrButton(){
        if(this._inProgress){
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }else{
            return html`<vaadin-button @click="${() => this._save()}">
                <vaadin-icon icon="font-awesome-solid:floppy-disk" slot="prefix"></vaadin-icon>
                ${msg('Save', { id: 'configuration-save' })}
            </vaadin-button>`;
        }
    }

    _save(){
        this._inProgress = true;
        let newValue = this.shadowRoot.getElementById('code').getAttribute('value');
        this.jsonRpc.updateProperties({
            type: this._type,
            content: newValue,
            target: 'application.properties'
        }).then(jsonRpcResponse => {
            this._inProgress = false;
            if (jsonRpcResponse.result === false) {
                notifier.showErrorMessage(msg('Configuration failed to update. See log file for details', { id: 'configuration-update-failed' }));
            } else {
                fetch(devuiState.applicationInfo.contextRoot);
                notifier.showSuccessMessage(msg('Configuration successfully updated', { id: 'configuration-update-success' }));
            }
        });
    }
}

customElements.define('qwc-configuration-editor', QwcConfigurationEditor);

