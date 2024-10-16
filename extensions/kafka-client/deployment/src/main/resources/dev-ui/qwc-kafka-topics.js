import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/dialog';
import { dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/text-field';
import '@vaadin/integer-field';
import '@vaadin/button';
import './qwc-kafka-messages.js';
import './qwc-kafka-add-topic.js';


/**
 * This component shows the Kafka Topics
 */
export class QwcKafkaTopics extends QwcHotReloadElement { 
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
        .kafka {
            height: 100%;
            display: flex;
            flex-direction: column;
            overflow: hidden;
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

        .delete-button {
            font-size: small;
            color: var(--lumo-error-text-color);
            cursor: pointer;
        }

        .clickableCell {
            display: block;
            width: 100%;
            cursor: pointer;
        }
    `;

    static properties = {
        _topics: {status: true, type: Array},
        _selectedTopic: {state: true},
        _createTopicDialogOpened: {state: true},
        
        _deleteTopicDialogOpened: {state: true},
        _deleteTopicName: {state: false}
    };

    constructor() { 
        super();
        this._topics = null;
        this._selectedTopic = null;
        
        this._createTopicDialogOpened = false;
        this._deleteTopicName = '';
        this._deleteTopicDialogOpened = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getTopics().then(jsonRpcResponse => { 
            this._topics = jsonRpcResponse.result;
        });
    }

    render() {
        if(this._topics && this._selectedTopic === null){
            return this._renderTopicList();
        } else if(this._topics && this._selectedTopic!=null){
            return html`<qwc-kafka-messages 
                extensionName="${this.jsonRpc.getExtensionName()}"
                topicName="${this._selectedTopic.name}" 
                partitionsCount=${this._selectedTopic.partitionsCount}
                @kafka-messages-back=${this._showTopicList}></qwc-kafka-messages>`
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }

    _showTopicList(){
        this._selectedTopic = null;
    }

    _renderTopicList(){
        return html`<div class="kafka">
                ${this._renderCreateTopicDialog()}
                ${this._renderConfirmDeleteDialog()}
                ${this._renderTopicGrid()}
                
                <div class="bottom">
                    <vaadin-icon class="create-button" icon="font-awesome-solid:circle-plus" @click="${() => {this._createTopicDialogOpened = true}}" title="Create topic"></vaadin-icon>
                </div>    
            </div>`;
    }

    _renderTopicGrid(){
        return html`<vaadin-grid .items="${this._topics}" 
                                    class="table" theme="no-border">
                        <vaadin-grid-sort-column auto-width
                            path="name"
                            header="Topic Name"
                            ${columnBodyRenderer(this._nameRenderer, [])}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="topicId"
                            header="ID"
                            ${columnBodyRenderer(this._idRenderer, [])}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="partitionsCount"
                            header="Partitions count"
                            ${columnBodyRenderer(this._partitionsCountRenderer, [])}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="nmsg"
                            header="Number of msg"
                            ${columnBodyRenderer(this._nmsgRenderer, [])}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-column
                            text-align="end"
                            ${columnBodyRenderer(this._deleteActionRenderer, [])}>
                        ></vaadin-grid-column>

                    </vaadin-grid>`;
    }

    _renderCreateTopicDialog(){
        if(this._createTopicDialogOpened){
            return html`<vaadin-dialog class="createDialog"
                    header-title="Create topic"
                    resizable
                    .opened="${this._createTopicDialogOpened}"
                    @opened-changed="${(e) => (this._createTopicDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderCreateTopicDialogForm(), "Create topic")}
                ></vaadin-dialog>`;
        }
    }

    _renderConfirmDeleteDialog(){

        return html`<vaadin-dialog class="deleteDialog"
                    header-title="Confirm delete"
                    .opened="${this._deleteTopicDialogOpened}"
                    @opened-changed="${(e) => (this._deleteTopicDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderDeleteTopicDialogForm(), "Confirm delete")}
                ></vaadin-dialog>`;
    }

    _renderDeleteTopicDialogForm(){
        return html`
            Are you sure you want to delete topic <b>${this._deleteTopicName}</b><br/>?
            ${this._renderDeleteTopicButtons()}
        `;
    }

    _openConfirmDeleteDialog(e){
        this._deleteTopicName = e.target.dataset.topicName;
        this._deleteTopicDialogOpened = true;
    }

    _nameRenderer(topic){
        return this._clickableCell(topic, topic.name);
    }

    _idRenderer(topic){
        return this._clickableCell(topic, topic.topicId);
    }

    _partitionsCountRenderer(topic){
        return this._clickableCell(topic, topic.partitionsCount);
    }

    _nmsgRenderer(topic){
        return this._clickableCell(topic, topic.nmsg);
    }

    _clickableCell(topic, val){
        return html`<span class="clickableCell" @click=${() => this._selectTopic(topic)}>${val}</span>`;
    }

    _selectTopic(topic){
        this._selectedTopic = topic;
    }

    _deleteActionRenderer(topic){
        return html`<vaadin-icon data-topic-name="${topic.name}" class="delete-button" icon="font-awesome-solid:trash-can" @click="${this._openConfirmDeleteDialog}" title="Delete topic"></vaadin-icon>`;
    }

    _renderCreateTopicDialogForm(){
        return html`<qwc-kafka-add-topic 
                        @kafka-topic-added-success=${this._topicAdded}
                        @kafka-topic-added-canceled=${this._topicAddedCanceled}
                        extensionName="${this.jsonRpc.getExtensionName()}">
                    </qwc-kafka-add-topic>`;
    }
    
    _topicAdded(e){
        this._createTopicDialogOpened = false;
        this._topics = e.detail.result;
    }

    _topicAddedCanceled(e){
        this._createTopicDialogOpened = false;
    }

    _renderDeleteTopicButtons(){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary error" @click=${this._submitDeleteTopicForm}>Delete</vaadin-button>
                        <vaadin-button theme="secondary" @click=${this._resetDeleteTopicForm}>Cancel</vaadin-button>
                    </div>`;
    }

    _resetDeleteTopicForm(){
        this._deleteTopicName = '';
        this._deleteTopicDialogOpened = false;
    }
    
    _submitDeleteTopicForm(){
        this.jsonRpc.deleteTopic({
            topicName: this._deleteTopicName
        }).then(jsonRpcResponse => { 
            this._topics = jsonRpcResponse.result;
        });
        this._resetDeleteTopicForm();
    }
}

customElements.define('qwc-kafka-topics', QwcKafkaTopics);