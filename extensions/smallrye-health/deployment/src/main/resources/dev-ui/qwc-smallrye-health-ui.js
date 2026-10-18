import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import '@vaadin/progress-bar';
import '@qomponent/qui-badge';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-card';
import '@vaadin/icon';
import '@vaadin/popover';
import { popoverRenderer } from '@vaadin/popover/lit.js';
import '@vaadin/item';
import '@vaadin/list-box';
import { StorageController } from 'storage-controller';
import { msg, updateWhenLocaleChanges } from 'localization';

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
            padding-left: 20px;
            padding-right: 20px;
        }
        .cards {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 15px;
            padding: 10px;
            align-items: start;
        }
        .cards qui-card {
            min-width: 0;
            overflow: hidden;
        }
        .cardcontents {
            display: flex;
            flex-direction: column;
            padding: 10px 12px;
        }
        .key {
            font-weight: bold;
            white-space: nowrap;
            flex-shrink: 0;
        }
        .entry {
            display: flex;
            padding: 5px 8px;
            gap: 10px;
            border-radius: 4px;
            font-size: var(--lumo-font-size-s);
            min-width: 0;
        }
        .entry .value {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            line-height: 1.4;
            min-width: 0;
            cursor: pointer;
        }
        .entry .value.expanded {
            white-space: normal;
            overflow-wrap: anywhere;
        }
        .entry:hover {
            background-color: var(--lumo-contrast-5pct);
        }
        div[slot="header"] {
            overflow: hidden;
            width: 100%;
            padding: 4px 0;
        }
        .headingIcon {
            display: flex;
            justify-content: space-between;
            gap: 10px;
            align-items: center;
            width: 100%;
            min-width: 0;
        }
        .headingIcon vaadin-icon {
            flex-shrink: 0;
        }
        .headingIcon .checkname {
            flex: 1 1 0%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            line-height: 1.4;
            min-width: 0;
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
            cursor: pointer;
        }

    `;

    static properties = {
        _health: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
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
            <vaadin-icon id="configureIcon" icon="font-awesome-solid:gear" title=${msg('Configure health status updates', { id: 'quarkus-smallrye-health-configure' })}></vaadin-icon>
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
                    <div class="headingIcon ${headingClass}"><span class="checkname" title="${check.name.string}">${check.name.string}</span><vaadin-icon icon="${icon}"></vaadin-icon></div>
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
                                        <span class="key">${key}: </span><span class="value" @click=${this._toggleValue}>${value.string}</span>
                                    </div>
                                `)}
                            </div>
                        </div>`;
        }
    }

    _toggleValue(e){
        e.target.classList.toggle('expanded');
    }

    _cancelObserver(){
        if(this._observer){
            this._observer.cancel();
        }
    }
}
customElements.define('qwc-smallrye-health-ui', QwcSmallryeHealthUi);
