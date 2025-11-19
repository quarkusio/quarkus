import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import 'echarts-bar-stack';
import '@vaadin/button';
import '@vaadin/checkbox';
import '@vaadin/checkbox-group';
import '@vaadin/progress-bar';
import { msg, updateWhenLocaleChanges } from 'localization';

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
            justify-content: space-between;
        }
    
        .top-bar h4 {
            color: var(--lumo-contrast-60pct);
        }
    `;

    static properties = {
        extensionName: {type: String}, // TODO: Add 'pane' concept in router to register internal extension pages.
        _threadSlotRecords: {state: true},
        _slots: {state: true},
        _showLegend: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._threadSlotRecords = null;
        this._slots = null;
        this._showLegend = false;
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
            let xname = this._slots.length + " " + msg('time slots', { id: 'buildmetrics-time-slots' }) + " (" + this._slots[0] +" ms)";
            let yname = msg('Number of build threads used in a time slot', { id: 'buildmetrics-thread-count' });
            return html`${this._renderTopBar()}
                <echarts-bar-stack width="400px" height="400px"
                        xdata="${xdata}"
                        xdataName="${xname}"
                        ydataName="${yname}"
                        series="${JSON.stringify(this._threadSlotRecords)}"
                        .showLegend=${this._showLegend}>
                    </echarts-bar-stack>
            `;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>${msg('Loading Build Steps Execution Graph...', { id: 'buildmetrics-loading-graph' })}</div>
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
                            ${msg('Back', { id: 'buildmetrics-back' })}
                        </vaadin-button>
                        <h4>${msg('Build Steps Concurrent Execution Chart', { id: 'buildmetrics-concurrent-chart' })}</h4>
                        <vaadin-checkbox label="${msg('Legend', { id: 'buildmetrics-legend' })}"
                            @change="${(event) => {
                                this._checkedChanged(event, event.target.checked);
                            }}"
                        ></vaadin-checkbox>
                    </div>`;
    }
    
    _backAction(){
        const back = new CustomEvent("build-steps-graph-back", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false
        });
        this.dispatchEvent(back);
    }
    
    _checkedChanged(event, value) {
        event.preventDefault();
        this._showLegend = value;
    }
}
customElements.define('qwc-build-steps-execution-graph', QwcBuildStepsExecutionGraph);