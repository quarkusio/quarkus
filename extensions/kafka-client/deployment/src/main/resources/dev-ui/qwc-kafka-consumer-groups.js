import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer, gridRowDetailsRenderer } from '@vaadin/grid/lit.js';

/**
 * This component shows the Kafka Consumer Groups
 */
export class QwcKafkaConsumerGroups extends QwcHotReloadElement { 
    jsonRpc = new JsonRpc(this);

    static styles = css`
        .kafka {
            height: 100%;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .table {
            height: 100%;
        }
        .top-bar {
            display: flex;
            align-items: baseline;
            gap: 20px;
        }
    `;

    static properties = {
        _consumerGroups: {state: true},
        _selectedConsumerGroups: {state: true, type: Array},
        _memberDetailOpenedItem: {state: true, type: Array},
    };

    constructor() { 
        super();
        this._consumerGroups = null;
        this._selectedConsumerGroups = [];
        this._memberDetailOpenedItem = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getInfo().then(jsonRpcResponse => { 
            this._consumerGroups = jsonRpcResponse.result.consumerGroups;
        });
    }

    render() {
        if(this._consumerGroups){
            return html`<div class="kafka">
                    ${this._renderConsumerGroups()}
                    ${this._renderSelectedConsumerGroup()}

            </div>`;
        }else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        } 
    }

    _renderConsumerGroups(){
        if(this._selectedConsumerGroups.length === 0){
            return html`
                <vaadin-grid .items="${this._consumerGroups}" 
                            .selectedItems="${this._selectedConsumerGroups}"
                            class="table" theme="no-border"
                            @active-item-changed="${(e) => {
                                const item = e.detail.value;
                                this._selectedConsumerGroups = item ? [item] : [];
                            }}">
                    <vaadin-grid-sort-column auto-width
                        path="state"
                        header="State"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="name"
                        header="Name"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="coordinatorId"
                        header="Coordinator"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="protocol"
                        header="Protocol"
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="members"
                        header="Members"
                        ${columnBodyRenderer(this._membersRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        path="lag"
                        header="Lag(Sum)"
                        resizable>
                    </vaadin-grid-sort-column>

                </vaadin-grid>`;
        }
    }
    
    _renderSelectedConsumerGroup(){
        if(this._selectedConsumerGroups.length > 0){
            let name = this._selectedConsumerGroups[0].name;
            let members = this._selectedConsumerGroups[0].members;
            return html`<div class="top-bar">
                            <vaadin-button @click="${() => {this._selectedConsumerGroups = []}}">
                                <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                                Back
                            </vaadin-button>
                            <h4>${name}</h4>
                        </div>
                        <vaadin-grid .items="${members}" 
                                    class="table" theme="no-border"
                                    .detailsOpenedItems="${this._memberDetailOpenedItem}"
                                    @active-item-changed="${(event) => {
                                        const prop = event.detail.value;
                                        this._memberDetailOpenedItem = prop ? [prop] : [];
                                    }}"
                                    ${gridRowDetailsRenderer(this._memberDetailRenderer, [])}>
                            <vaadin-grid-sort-column auto-width
                                path="memberId"
                                header="Member ID"
                                resizable>
                            </vaadin-grid-sort-column>

                            <vaadin-grid-sort-column auto-width
                                path="host"
                                header="Host"
                                resizable>
                            </vaadin-grid-sort-column>

                            <vaadin-grid-sort-column auto-width
                                path="partitions"
                                header="Partitions"
                                ${columnBodyRenderer(this._memberPartitionsRenderer, [])}
                                resizable>
                            </vaadin-grid-sort-column>


                        </vaadin-grid>`;
        }
    }

    
    

    _membersRenderer(consumerGroup) {
        return html`${consumerGroup.members.length}`;
    }

    _memberPartitionsRenderer(member){
        return html`${member.partitions.length}`;
    }
    
    _memberDetailRenderer(member) {
        if(member.partitions && member.partitions.length > 0){
            return html`<vaadin-grid .items="${member.partitions}" theme="no-row-borders" all-rows-visible>
                <vaadin-grid-column path="topic"></vaadin-grid-column>
                <vaadin-grid-column path="partition"></vaadin-grid-column>
                <vaadin-grid-column path="lag"></vaadin-grid-column>
              </vaadin-grid>`;
        }
    }

    _topicHeaderRenderer(){
        return html`<b>Topic</b>`;
    }
    _partitionHeaderRenderer(){
        return html`<b>Partition</b>`;
    }
    _topicHeade_lagHeaderRendererrRenderer(){
        return html`<b>Lag</b>`;
    }
}

customElements.define('qwc-kafka-consumer-groups', QwcKafkaConsumerGroups);