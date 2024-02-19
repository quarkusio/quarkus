import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer, gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/dialog';
import { dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/button';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@quarkus-webcomponents/codeblock';
import '@vaadin/split-layout';
import './qwc-kafka-add-message.js';

/**
 * This component shows the Kafka Messages for a certain topic
 */
export class QwcKafkaMessages extends observeState(QwcHotReloadElement) {
    
    static styles = css`
        .kafka {
            height: 100%;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        .top-bar {
            display: flex;
            align-items: baseline;
            gap: 20px;
        }
        .backButton{
            margin-left: 10px;
        }
        .detail-block {
            width: 50%;
            padding: 10px;
            display: flex;
            flex-direction: column;
        }
        .header-grid, .code-block {
            padding: 10px;
        }
        .bottom {
            display: flex;
            flex-direction: row-reverse;
        }
    
        .create-button {
            margin-right: 30px;
            margin-bottom: 10px;
            font-size: xx-large;
            cursor: pointer;
        } 
    `;

    static properties = {
        partitionsCount: {type: Number},
        topicName: {type: String},
        extensionName: {type: String}, // TODO: Add 'pane' concept in router to register internal extension pages.
        _messages: {state: false, type: Array},
        _createMessageDialogOpened: {state: true},
        _messagesDetailOpenedItem: {state: false, type: Array}
    };

    constructor() { 
        super();
        this._messages = null;
        this._createMessageDialogOpened = false;
        this._messagesDetailOpenedItem = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc = new JsonRpc(this.extensionName);
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.topicMessages({topicName: this.topicName}).then(jsonRpcResponse => {
            this._messages = jsonRpcResponse.result.messages;
        });
    }

    render() {
        if(this._messages){
            return html`<div class="kafka">
                    ${this._renderCreateMessageDialog()}
                    ${this._renderTopBar()}                
                    ${this._renderMessages()}
                    ${this._renderAddMessagesPlusButton()}
                </div>`;
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }

    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${this._backAction}" class="backButton">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>${this.topicName}</h4>
                    </div>`;
    }

    _backAction(){
        const back = new CustomEvent("kafka-messages-back", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false,
        });
        this.dispatchEvent(back);
    }
    
    _renderMessages(){
        
        return html`<vaadin-grid .items="${this._messages}" 
                        class="table" theme="no-border"
                        .detailsOpenedItems="${this._messagesDetailOpenedItem}"
                        @active-item-changed="${(event) => {
                            const prop = event.detail.value;
                            this._messagesDetailOpenedItem = prop ? [prop] : [];
                        }}"
                        ${gridRowDetailsRenderer(this._messagesDetailRenderer, [])}>
                    <vaadin-grid-sort-column auto-width
                        path="offset"
                        header="Offset"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="partition"
                        header="Partitions"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="timestamp"
                        header="Timestamp"
                        ${columnBodyRenderer(this._timestampRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="key"
                        header="Key"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="value"
                        header="Value"
                        resizable>
                    </vaadin-grid-sort-column>

                </vaadin-grid>`;
    }

    _messagesDetailRenderer(message) {
        let headers = [];
        for (const [key, value] of Object.entries(message.headers)) {
            headers.push({key:key, value: value});
        }

        return html`<vaadin-split-layout>
                        <master-content class="detail-block">
                            <span>Message value:</span>
                            <div class="code-block">    
                                <qui-code-block
                                    mode='json'
                                    content='${message.value}'
                                    theme='${themeState.theme.name}'>
                                </qui-code-block>
                            </div>
                        </master-content>
                        <detail-content class="detail-block">
                            <span>Message headers:</span>
                            <vaadin-grid class="header-grid" .items="${headers}" 
                                    theme="no-border" all-rows-visible>
                                <vaadin-grid-sort-column auto-width
                                    path="key"
                                    header="Key"
                                    resizable>
                                </vaadin-grid-sort-column>

                                <vaadin-grid-sort-column auto-width
                                    path="value"
                                    header="Value"
                                    resizable>
                                </vaadin-grid-sort-column>
                            </vaadin-grid>
                        </detail-content>
                    </vaadin-split-layout>`;
    }

    _timestampRenderer(message){
        return html`${this._timestampToFormattedString(message.timestamp)}`;
    }

    _timestampToFormattedString(UNIX_timestamp) {
        const a = new Date(UNIX_timestamp);
        const year = a.getFullYear();
        const month = this._addTrailingZero(a.getMonth());
        const date = this._addTrailingZero(a.getDate());
        const hour = this._addTrailingZero(a.getHours());
        const min = this._addTrailingZero(a.getMinutes());
        const sec = this._addTrailingZero(a.getSeconds());
        return date + '/' + month + '/' + year + ' ' + hour + ':' + min + ':' + sec;
    }

    _addTrailingZero(data) {
        if (data < 10) {
            return "0" + data;
        }
        return data;
    }

    _renderAddMessagesPlusButton(){    
        if(this._messages){
            return html`<div class="bottom">
                <vaadin-icon class="create-button" icon="font-awesome-solid:circle-plus" @click="${() => {this._createMessageDialogOpened = true}}" title="Create message"></vaadin-icon>
            </div>`;
        }
    }

    _renderCreateMessageDialog(){
        if(this._createMessageDialogOpened){
            return html`<vaadin-dialog class="createDialog"
                        header-title="Add new message to ${this.topicName}"
                        resizable
                        .opened="${this._createMessageDialogOpened}"
                        @opened-changed="${(e) => (this._createMessageDialogOpened = e.detail.value)}"
                        ${dialogRenderer(() => this._renderCreateMessageDialogForm(), "Add new message to ${this.topicName}")}
                    ></vaadin-dialog>`;
        }
    }

    _renderCreateMessageDialogForm(){
        return html`<qwc-kafka-add-message 
                        partitionsCount="${this.partitionsCount}" 
                        topicName="${this.topicName}"
                        extensionName="${this.extensionName}"
                        @kafka-message-added-success=${this._messageAdded}
                        @kafka-message-added-canceled=${this._messageAddedCanceled}>
                    </qwc-kafka-add-message>`;
    }

    _messageAddedCanceled(){
        this._createMessageDialogOpened = false;
    }

    _messageAdded(e){
        this._messages = e.detail.result.messages;
        this._createMessageDialogOpened = false;
    }
}

customElements.define('qwc-kafka-messages', QwcKafkaMessages);