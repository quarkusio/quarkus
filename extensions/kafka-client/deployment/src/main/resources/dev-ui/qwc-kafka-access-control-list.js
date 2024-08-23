import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/progress-bar';

/**
 * This component shows the Kafka Access Control List
 */
export class QwcKafkaAccessControlList extends QwcHotReloadElement { 

    jsonRpc = new JsonRpc(this);

    static styles = css``;

    static properties = {
        _aclInfo: {state: true},
    };

    constructor() { 
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getAclInfo().then(jsonRpcResponse => { 
            this._aclInfo = jsonRpcResponse.result;
        });
    }

    render() {
        if(this._aclInfo){
            return html`<vaadin-grid .items="${this._aclInfo.entries}" 
                                class="table" theme="no-border">
                        <vaadin-grid-sort-column auto-width
                            path="operation"
                            header="Operation"
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="principal"
                            header="Principal"
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="perm"
                            header="Permission"
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="pattern"
                            header="Resource Pattern"
                            resizable>
                        </vaadin-grid-sort-column>

                    </vaadin-grid>`;
        }else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }  
    }
}

customElements.define('qwc-kafka-access-control-list', QwcKafkaAccessControlList);