import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import '@vaadin/progress-bar';
import 'qui-badge';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-card';
import '@vaadin/icon';
import '@vaadin/popover';
import { popoverRenderer } from '@vaadin/popover/lit.js';
import '@vaadin/item';
import '@vaadin/list-box';
import { StorageController } from 'storage-controller';

/**
 * This component shows the health UI
 */
export class QwcSmallryeHealthUi extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);
    storageControl = new StorageController(this);
    
    static styles = css`
        :host {
            display: flex;
            justify-content: space-between;
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
        #configureIcon {
            margin-top: 10px;
            margin-right: 10px;
            cursor:pointer;
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
        this.hotReload();
    }

    disconnectedCallback() {
        this._cancelObserver();
        super.disconnectedCallback();
    }

    hotReload(){
        let interval = this.storageControl.get("interval");
        this.jsonRpc.getHealth().then(jsonRpcResponse => { 
           this._health = jsonRpcResponse.result;
           this._startStreaming(interval);
        });
    }

    _getIntervalIndex(){
        const interval = this.storageControl.get("interval");
        if(interval && interval ==="3s") return 0;
        if(interval && interval ==="30s") return 2;
        if(interval && interval ==="60s") return 3;
        if(interval && interval ==="Off") return 4;
        return 1; // default (10s)
    }

    _startStreaming(interval){
        this._cancelObserver();
        if(!interval)interval = "";
        this._observer = this.jsonRpc.streamHealth({interval:interval}).onNext(jsonRpcResponse => {
            this._health = jsonRpcResponse.result;
        });
    }

    render() {
        if(this._health && this._health.payload){
            return html`<div class="cards">${this._health.payload.checks.map((check) =>
                html`${this._renderCard(check)}`
            )}</div>
            <vaadin-icon id="configureIcon" icon="font-awesome-solid:gear" title="Configure health status updates"></vaadin-icon>
            <vaadin-popover
                for="configureIcon"
                .position="start-bottom"
                ${popoverRenderer(this._configurePopoverRenderer)}
            ></vaadin-popover>

            `;
        }else {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
    }
    
    _configurePopoverRenderer(){
        let i = this._getIntervalIndex();
        return html`<vaadin-list-box selected="${i}" @selected-changed=${this._onSelectedChanged}>
                        <vaadin-item>3s</vaadin-item>
                        <vaadin-item>10s</vaadin-item>
                        <vaadin-item>30s</vaadin-item>
                        <vaadin-item>60s</vaadin-item>
                        <vaadin-item>Off</vaadin-item>
                    </vaadin-list-box>`;
    }
    
    _onSelectedChanged(event) {
        const listBox = event.target;
        const selectedIndex = listBox.selected;
        const selectedItem = listBox.children[selectedIndex];
        this.storageControl.set('interval', selectedItem?.textContent?.trim());
        
        this.hotReload();
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