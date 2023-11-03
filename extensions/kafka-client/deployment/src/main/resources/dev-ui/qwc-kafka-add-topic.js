import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';

import '@vaadin/text-field';
import '@vaadin/integer-field';
import '@vaadin/button';

/**
 * This component shows the add Topics Screen
 */
export class QwcKafkaAddTopic extends LitElement { 
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
        :host {
            display: flex; 
            flex-direction: column;
        }
    `;

    static properties = {
        extensionName: {type: String}, // TODO: Add 'pane' concept in router to register internal extension pages.
        _newTopic: {state: true},
    };

    constructor() { 
        super();
        this._reset();
    }

    connectedCallback() {
        super.connectedCallback();
        this._reset();
        this.jsonRpc = new JsonRpc(this.extensionName);
    }

    render(){
        return html`<vaadin-text-field
                        label="Topic Name"
                        placeholder="my-awesome-topic"
                        value="${this._newTopic.name ?? ''}"
                        @value-changed="${(e) => this._nameChanged(e)}"
                        required
                        clear-button-visible>
                    </vaadin-text-field>
                    <vaadin-integer-field 
                        label="Partitions"
                        value="${this._newTopic.partitions ?? '1'}" 
                        step-buttons-visible 
                        @value-changed="${(e) => this._partitionsChanged(e)}"
                        min="0" 
                        max="99">
                    </vaadin-integer-field>
                    <vaadin-integer-field 
                        label="Replications"
                        value="${this._newTopic.replications ?? '1'}" 
                        step-buttons-visible 
                        @value-changed="${(e) => this._replicationsChanged(e)}"    
                        min="0" 
                        max="99">
                    </vaadin-integer-field>
                    ${this._renderButtons()}`;
    }
    
    _renderButtons(){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary" @click=${this._submit}>Create</vaadin-button>
                        <vaadin-button theme="secondary error" @click=${this._cancel}>Cancel</vaadin-button>
                    </div>`;
    }
    
    _reset(){
        this._newTopic = new Object();
        this._newTopic.name = '';
        this._newTopic.partitions = 1;
        this._newTopic.replications = 1;        
    }

    _cancel(){
        this._reset();
        const canceled = new CustomEvent("kafka-topic-added-canceled", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false,
        });
        this.dispatchEvent(canceled);
    }

    _submit(){
        if(this._newTopic.name.trim() !== ''){
            
            this.jsonRpc.createTopic({
                topicName: this._newTopic.name,
                partitions: parseInt(this._newTopic.partitions),
                replications: parseInt(this._newTopic.replications)
            }).then(jsonRpcResponse => { 
                this._reset();
                const success = new CustomEvent("kafka-topic-added-success", {
                    detail: {result: jsonRpcResponse.result},
                    bubbles: true,
                    cancelable: true,
                    composed: false,
                });

                this.dispatchEvent(success);
            });
        }
    }
    
    _nameChanged(e){
        this._newTopic.name = e.detail.value.trim();
    }
    
    _partitionsChanged(e){
        this._newTopic.partitions = e.detail.value;
    }
    
    _replicationsChanged(e){
        this._newTopic.replications = e.detail.value;
    }
}

customElements.define('qwc-kafka-add-topic', QwcKafkaAddTopic);