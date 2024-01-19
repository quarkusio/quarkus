import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import 'qui-code-block';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/progress-bar';

/**
 * This component allows users to change the configuration in an online editor
 */
export class QwcConfigurationEditor extends LitElement {
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
            return html`<span>Error: ${this._error}</span>`;
        }

        return html`
        ${this._renderToolbar()}
        <qui-code-block id="code"
            mode='${this._type}'
            content='${this._value}'
            value='${this._value}'
            editable>
        </qui-code-block>`;
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
                Save
            </vaadin-button>`;
        }
    }

    _save(){
        this._inProgress = true;
        let newValue = this.shadowRoot.getElementById('code').getAttribute('value');
        this.jsonRpc.updateProperties({content: newValue, type: this._type}).then(jsonRpcResponse => {
            this._inProgress = false;
            if(jsonRpcResponse.result === false){
                notifier.showErrorMessage("Configuration failed to update. See log file for details");
            }else{
                notifier.showSuccessMessage("Configuration successfully updated");
            }
        });
    }
}

customElements.define('qwc-configuration-editor', QwcConfigurationEditor);

