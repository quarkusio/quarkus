import { LitElement, html, css} from 'lit'; 
import { JsonRpc } from 'jsonrpc';
import '@vaadin/form-layout';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/text-area';
import '@vaadin/button';

/**
 * This component shows the Add Message screen
 */
export class QwcKafkaAddMessage extends LitElement { 
    
    static styles = css`

        :root {
            display: flex; 
            flex-direction: column; 
        }

    `;

    static properties = {
        partitionsCount: {type: Number},
        topicName: {type: String},
        extensionName: {type: String}, // TODO: Add 'pane' concept in router to register internal extension pages.
        _targetPartitions: {state: false},
        _types: {state: false},
        _newMessage: {state: true},
        _newMessageHeaders: {state: true, type: Array},
    };

    constructor() { 
        super();
        this.partitionsCount = 0;
        this.topicName = null;
        this._targetPartitions = [];
        this._types = [];
        this._reset();
        this.responsiveSteps = [
            { minWidth: 0, columns: 1 },
            { minWidth: '320px', columns: 2 },
        ];
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc = new JsonRpc(this.extensionName);

        this._targetPartitions.push({name: "Any", value: "any"});
        for (var i = 0; i < this.partitionsCount; i++) {
            this._targetPartitions.push({name: i.toString(), value: i});
        }

        this._types.push({name: "Text", value: "text"});
        this._types.push({name: "None (Tombstone)", value: "none"});
        this._types.push({name: "JSON", value: "json"});
        this._types.push({name: "Binary", value: "binary"});
    }

    _reset(){
        this._newMessage = new Object();
        this._newMessage.partition = 'any';
        this._newMessage.type = 'text';
        this._newMessage.key = null;
        this._newMessage.value = null;
        this._newMessageHeaders = null;
    }

    _cancel(){
        this._reset();
        const canceled = new CustomEvent("kafka-message-added-canceled", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false,
        });
        this.dispatchEvent(canceled);
    }

    render() {

        return html`<vaadin-form-layout .responsiveSteps="${this.responsiveSteps}">
                        <vaadin-combo-box
                            label="Target partition"
                            item-label-path="name"
                            item-value-path="value"
                            value="${this._newMessage.partition ?? 'any'}" 
                            .items="${this._targetPartitions}"
                            @value-changed="${(e) => this._createMessagePartitionChanged(e)}">
                        </vaadin-combo-box>
                    
                        <vaadin-combo-box
                            label="Type"
                            item-label-path="name"
                            item-value-path="value"
                            value="${this._newMessage.type ?? 'text'}" 
                            .items="${this._types}"
                            @value-changed="${(e) => this._createMessageTypeChanged(e)}">
                        </vaadin-combo-box>

                        <vaadin-text-field
                            label="Key"
                            placeholder="my-awesome-key"
                            value="${this._newMessage.key ?? ''}"
                            @value-changed="${(e) => this._createMessageKeyChanged(e)}"
                            required
                            clear-button-visible>
                        </vaadin-text-field>
                        
                        <vaadin-text-area style="min-height: 160px;"
                                    colspan="2"
                                    label="Value"
                                    value="${this._newMessage.value ?? ''}"
                                    @value-changed="${(e) => this._createMessageValueChanged(e)}"
                                    required>
                        </vaadin-text-area>
                        
                        <div colspan="2">
                            <span>Headers</span>
                            <vaadin-text-field id="key"
                                placeholder="key" value=''>
                            </vaadin-text-field>
                            <vaadin-text-field id="value"
                                placeholder="value"
                                value=''>
                            </vaadin-text-field>
                            <vaadin-button slot="suffix" theme="icon" aria-label="Add header" @click=${(e) => this._newMessageAddHeader(e)}>
                                <vaadin-icon icon="font-awesome-solid:plus"></vaadin-icon>
                            </vaadin-button>
                        </div>
                        
                    </vaadin-form-layout>

                    ${this._renderAddedMessageHeaders()}    
                        
                    ${this._renderCreateMessageButtons()}
                    `;
    }

    _renderAddedMessageHeaders(){
        if(this._newMessageHeaders && this._newMessageHeaders.length > 0){
            return html`<vaadin-grid class="header-grid" .items="${this._newMessageHeaders}" 
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
                        </vaadin-grid>`;
        }
    }

    _renderCreateMessageButtons(){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary" @click=${this._submitCreateMessageForm}>Create</vaadin-button>
                        <vaadin-button theme="secondary error" @click=${this._cancel}>Cancel</vaadin-button>
                    </div>`;
    }

    _submitCreateMessageForm(){
        if (this._newMessage.partition === 'any') this._newMessage.partition = -1;

        let headers = new Object();
        if(this._newMessageHeaders && this._newMessageHeaders.length > 0){
            this._newMessageHeaders.forEach(function (h) {
                headers[h.key] = h.value;
            });
        }

        this.jsonRpc.createMessage({topicName:this.topicName, 
                        partition: Number(this._newMessage.partition), 
                        key:this._newMessage.key,
                        value:this._newMessage.value,
                        headers: headers
                    }).then(jsonRpcResponse => {
                        this._reset();
                        const success = new CustomEvent("kafka-message-added-success", {
                            detail: {result: jsonRpcResponse.result},
                            bubbles: true,
                            cancelable: true,
                            composed: false,
                        });

                        this.dispatchEvent(success);
                        
                    });

        
    }

    _createMessagePartitionChanged(e){
        this._newMessage.partition = e.detail.value;
    }

    _createMessageTypeChanged(e){
        this._newMessage.type = e.detail.value.trim();
    }

    _createMessageKeyChanged(e){
        this._newMessage.key = e.detail.value.trim();
    }

    _createMessageValueChanged(e){
        this._newMessage.value = e.detail.value.trim();
    }

    _newMessageAddHeader(e){
        let target = e.target;
        let parent = null;
        if(target.nodeName.toLowerCase() === "vaadin-icon"){
            parent = target.parentElement.parentElement;
        }else{
            parent = target.parentElement;
        }

        let h = new Object();

        var children = parent.children;
        for (var i = 0; i < children.length; i++) {
            var child = children[i];
            if(child.nodeName.toLowerCase() === "vaadin-text-field"){
                h[child.id] = child.value;
                child.value = '';
            }
        }
        this._addToHeaders(h);
    }

    _addToHeaders(h){
        if(!this._newMessageHeaders){
            this._newMessageHeaders = [h];
        } else {
            this._newMessageHeaders = [
                ...this._newMessageHeaders,
                h
            ];
        }
    }

}

customElements.define('qwc-kafka-add-message', QwcKafkaAddMessage);