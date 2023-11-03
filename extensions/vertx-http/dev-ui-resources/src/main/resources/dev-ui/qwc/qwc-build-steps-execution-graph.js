import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import 'echarts-bar-stack';
import '@vaadin/button';
import '@vaadin/checkbox';
import '@vaadin/checkbox-group';
import '@vaadin/progress-bar';

/**
 * This component shows the Build Step Execution Graph
 */
export class QwcBuildStepsExecutionGraph extends LitElement {

    static styles = css`
        .top-bar {
            display: flex;
            align-items: baseline;
            gap: 20px;
            padding-left: 20px;
            padding-right: 20px;
        }
    
        .top-bar h4 {
            color: var(--lumo-contrast-60pct);
        }
    `;

    static properties = {
        extensionName: {type: String}, // TODO: Add 'pane' concept in router to register internal extension pages.
        _threadSlotRecords: {state: true},
        _slots: {state: true}
    };

    constructor() {
        super();
        this._threadSlotRecords = null;
        this._slots = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc = new JsonRpc(this.extensionName);
        this._fetchBuildStepsExecutionData();    
    }

    _fetchBuildStepsExecutionData(){
        this.jsonRpc.getThreadSlotRecords().then(jsonRpcResponse => {
            this._slots = jsonRpcResponse.result.slots;
            this._threadSlotRecords = jsonRpcResponse.result.threadSlotRecords;
        });
    }

    render() {
        
        if(this._threadSlotRecords){
            let xdata = this._slots.toString();
            let xname = this._slots.length + " time slots (" + this._slots[0] +" ms)";
            let yname = "Number of build threads used in a time slot";
            return html`${this._renderTopBar()}
                <echarts-bar-stack width="400px" height="400px"
                        xdata="${xdata}"
                        xdataName="${xname}"
                        ydataName="${yname}"
                        series="${JSON.stringify(this._threadSlotRecords)}">
                    </echarts-bar-stack>
            `;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Loading Build Steps Execution Graph...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
        
        
    }
    
    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${this._backAction}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>Build Steps Concurrent Execution Chart</h4>
                    </div>`;
    }
    
    _backAction(){
        const back = new CustomEvent("build-steps-graph-back", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false,
        });
        this.dispatchEvent(back);
    }
}
customElements.define('qwc-build-steps-execution-graph', QwcBuildStepsExecutionGraph);