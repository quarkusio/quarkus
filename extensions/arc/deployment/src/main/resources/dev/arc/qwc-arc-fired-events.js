import { LitElement, html, css} from 'lit';
import { until } from 'lit/directives/until.js';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/details';
import '@vaadin/vertical-layout';
import '@vaadin/button';
import '@vaadin/checkbox';

/**
 * This component shows the Arc Fired Events
 */
export class QwcArcFiredEvents extends LitElement {
    jsonRpc = new JsonRpc("ArC");

    static styles = css`
        .menubar {
            display: flex;
            justify-content: flex-start;
            align-items: center;
            padding-left: 5px;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .arctable {
            height: 100%;
            padding-bottom: 10px;
        }
        .payload {
            color: grey;
            font-size: small;
        }`;

    static properties = {
        _firedEvents: {state: true},
        _observer: {state:false},
    };
  
    connectedCallback() {
        super.connectedCallback();
        this._refresh();
        this._observer = this.jsonRpc.streamEvents().onNext(jsonRpcResponse => {
            this._addToEvents(jsonRpcResponse.result);
        });
    }

    disconnectedCallback() {
        this._observer.cancel();
        super.disconnectedCallback();
    }
        
    render() {
        return html`${until(this._renderFiredEvents(), html`<span>Loading ArC fired event...</span>`)}`;
    }

    _renderFiredEvents(){
        if(this._firedEvents){
            return html`<div class="menubar">
                    <vaadin-button theme="small" @click=${()=>this._refresh} class="button">
                        <vaadin-icon icon="font-awesome-solid:rotate"></vaadin-icon> Refresh
                    </vaadin-button> 
                    <vaadin-button theme="small" @click=${()=>this._clear} class="button">
                        <vaadin-icon icon="font-awesome-solid:trash-can"></vaadin-icon> Clear
                    </vaadin-button> 
                    <vaadin-checkbox theme="small" label="Skip context lifecycle events" @click=${()=>this._toggleContext}></vaadin-checkbox>
                </div>
                <vaadin-grid .items="${this._firedEvents}" class="arctable" theme="no-border">
                    <vaadin-grid-column auto-width
                        header="Timestamp"
                        path="timestamp"
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Event Type"
                        ${columnBodyRenderer(this._payloadRenderer, [])}
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Qualifiers"
                        path="qualifiers"
                        resizable>
                    </vaadin-grid-column>

                </vaadin-grid>`;
        }
    }
    
    _payloadRenderer(event) {
        return html`
            <vaadin-details>
                <div slot="summary">${event.type}</div>

                <vaadin-vertical-layout>
                    <span><code class="payload">${event.payload}</code></span>
                </vaadin-vertical-layout>
            </vaadin-details>
        `;
    }
    
    _refresh(){
        console.log("refresh");
        this.jsonRpc.getLastEvents().then(events => {
            this._firedEvents = events.result;
        });
    }
    
    _clear(){
        console.log("clear");
        this.jsonRpc.clearLastEvents().then(events => {
            this._firedEvents = events.result;
        });
    }
    
    _toggleContext(){
        console.log("context");
    }

    _addToEvents(event){
        this._firedEvents = [
            ...this._firedEvents,
            event,
        ];
    }
}
customElements.define('qwc-arc-fired-events', QwcArcFiredEvents);