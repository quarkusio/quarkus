import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/text-area';
import '@vaadin/dialog';
import '@vaadin/progress-bar';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { dialogRenderer } from '@vaadin/dialog/lit.js';

/**
 * This component shows the scheduled methods.
 */
export class QwcSchedulerCronBuilder extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100%;
            padding-left: 5px;
            padding-right: 5px;
        }
    
        .input {
            display: flex;
            gap: 5px;
            align-items: baseline;
            justify-content: space-between;
        }
        vaadin-text-area {
            width: 100%;
        }
        
    `;
    
    static properties = {
        _description: {state: true},
        _createCronLoading: {state: true},
        _cron: {state: true},
        _example: {state: true}
    };
    
    constructor() {
        super();
        this._createCronLoading = false;
        this._cron = null;
        this._example = null;
        this._description = '';
        this.md = new MarkdownIt();
    }
    
    connectedCallback() {
        super.connectedCallback();
    }
    
    disconnectedCallback() {
        super.disconnectedCallback();
    }

    render() {
        return html`${this._renderLoadingDialog()}
                    ${this._renderInput()}
                    ${this._renderOutput()}
                    `;
    }
    
    _renderInput(){
        return html`<div class="input">
                        <vaadin-text-area 
                            min-rows="4" 
                            max-rows="8" 
                            label="Describe the cron you want to create" 
                            .value=${this._description ?? ''}
                            @value-changed=${(e) => { this._description = e.detail.value; }}>
                        </vaadin-text-area>
                        ${this._renderButton()}
                    </div>`;
    }
    
    _renderOutput(){
        if(this._cron){
            const htmlContent = this.md.render(this._example);
            return html`<div class="output">
                        <h3>Cron:</h3>
                        <code>${this._cron}</code>
                        <h3>Example</h3>
                        <div class="example">
                            ${unsafeHTML(htmlContent)}
                        </div>
                    </div>`;
        }
    }
    
    _renderButton(){
        if(!this._createCronLoading){
            return html`<vaadin-button @click="${this._createCron}">Create cron</vaadin-button>`;
        }
    }
    
    _renderLoadingDialog(){
        return html`
            <vaadin-dialog
              .opened="${this._createCronLoading}"
              @closed="${() => {
                this._createCronLoading = false;
              }}"
              ${dialogRenderer(() => html`
                            <label class="text-secondary" id="pblabel">Talking to the Dev Assistant ... please wait</label>
                            <vaadin-progress-bar indeterminate aria-labelledby="pblabel"></vaadin-progress-bar>`, [])}
            ></vaadin-dialog>`;
    }
    
    _createCron(){
        this._createCronLoading = true;
        document.body.style.cursor = 'wait';
        this.jsonRpc.createCron({"description": this._description}).then(jsonResponse => {
            this._createCronLoading = false;
            document.body.style.cursor = 'default';
            this._cron = jsonResponse.result.cron;
            this._example = jsonResponse.result.markdown;
        });
    }
}
customElements.define('qwc-scheduler-cron-builder', QwcSchedulerCronBuilder);