import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import '@vaadin/progress-bar';
import 'qui-badge';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-card';
import '@vaadin/icon';

/**
 * This component shows the health UI
 */
export class QwcSmallryeHealthUi extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            padding-left: 20px;
            padding-right: 20px;
        }
        .cards {
            display: flex;
            flex-wrap: wrap;
            padding: 10px;
            gap:10px;
        }
        .cardcontents {
            display: flex;
            flex-direction: column;
            padding-top: 10px;
            padding-bottom: 10px;
            padding-left: 2px;
            padding-right: 2px;
        }
        .key {
            font-weight: bold;
        }
        .entry {
            display: flex;
            padding: 3px;
            gap: 10px;
        }
        .empty {
            height: 4em;
            visibility: visible;
        }
        .headingIcon {
            display:flex;
            justify-content: space-between;
            gap: 10px;
            align-items: center;
        }
        .headingUp {
            color: var(--lumo-success-text-color);
        }
        .headingDown {
            color: var(--lumo-error-text-color);
        }
        
    `;

    static properties = {
        _health: {state: true}
    };

    constructor() {
        super();
        this._health = null;
    }

    connectedCallback() {
        super.connectedCallback();

        if(!this._health){
            this.hotReload();
        }
    }

    disconnectedCallback() {
        this._cancelObserver();
        super.disconnectedCallback();
    }

    hotReload(){
        this._cancelObserver();
        this._observer = this.jsonRpc.streamHealth().onNext(jsonRpcResponse => {
            this._health = jsonRpcResponse.result;
        });
        this.jsonRpc.getHealth().then(jsonRpcResponse => { 
           this._health = jsonRpcResponse.result; 
        });
    }

    render() {
        if(this._health && this._health.payload){
            return html`<div class="cards">${this._health.payload.checks.map((check) =>
                html`${this._renderCard(check)}`
            )}</div>`;
        }else {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
    }
    
    _renderCard(check){
        let icon = "font-awesome-solid:thumbs-down";
        let headingClass = "headingDown";
        if(check.status.string=="UP"){
            icon = "font-awesome-solid:thumbs-up";
            headingClass = "headingUp";
        }

        return html`<qui-card>
                <div slot="header">
                    <div class="headingIcon ${headingClass}">${check.name.string}<vaadin-icon icon="${icon}"></vaadin-icon></div>
                </div>
                ${this._renderCardContent(check)}
            </qui-card>`;
    }
    
    _renderCardContent(check){
        if(check.data){
            return html`<div slot="content">
                            <div class="cardcontents">
                                ${Object.entries(check.data).map(([key, value]) => html`
                                    <div class="entry">
                                        <span class="key">${key}: </span><span>${value.string}</span>
                                    </div>
                                `)}
                            </div>
                        </div>`;
        }else{
            return html`<div slot="content">
                            <div class="empty"></div>
                        </div>`;
        }
    }
    
    _cancelObserver(){
        if(this._observer){
            this._observer.cancel();
        }
    }
}
customElements.define('qwc-smallrye-health-ui', QwcSmallryeHealthUi);