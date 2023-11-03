import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/vertical-layout';
import '@vaadin/button';
import '@vaadin/checkbox';

/**
 * This component shows the Arc Fired Events
 */
export class QwcArcFiredEvents extends LitElement {
    jsonRpc = new JsonRpc(this);

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
        }`;

    static properties = {
        _firedEvents: {state: true},
        _skipLifecycleEvents: {state: true}
    };
  
    connectedCallback() {
        super.connectedCallback();
        this._refresh();
        this._eventsStream = this.jsonRpc.streamEvents().onNext(jsonRpcResponse => {
            this._addToEvents(jsonRpcResponse.result);
        });
        // Context lifecycle events are skipped by default; updates are handled by the stream
        this._skipLifecycleEvents = true;
        this._skipLifecycleEventsStream = this.jsonRpc.streamSkipContextEvents().onNext(jsonRpcResponse => {
            this._skipLifecycleEvents = jsonRpcResponse.result;
        });
    }

    disconnectedCallback() {
        this._eventsStream.cancel();
        this._skipLifecycleEventsStream.cancel();
        super.disconnectedCallback();
    }
        
    render() {
        if(this._firedEvents){
            return this._renderFiredEvents();
        } else {
            return html`<span>Loading ArC fired event...</span>`;
        }
    }

    _renderFiredEvents(){
        return html`<div class="menubar">
                    <vaadin-button theme="small" @click=${() => this._refresh()} class="button">
                        <vaadin-icon icon="font-awesome-solid:rotate"></vaadin-icon> Refresh
                    </vaadin-button> 
                    <vaadin-button theme="small" @click=${() => this._clear()} class="button">
                        <vaadin-icon icon="font-awesome-solid:trash-can"></vaadin-icon> Clear
                    </vaadin-button> 
                    <vaadin-checkbox theme="small" .checked="${this._skipLifecycleEvents}" label="Skip monitoring of context lifecycle events" @change="${() => this._toggleContext()}"></vaadin-checkbox>
                </div>
                <vaadin-grid .items="${this._firedEvents}" class="arctable" theme="no-border">
                    <vaadin-grid-column auto-width
                        header="Timestamp"
                        path="timestamp"
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Event Type"
                        ${columnBodyRenderer(this._eventTypeRenderer, [])}
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Qualifiers"
                        path="qualifiers"
                        resizable>
                    </vaadin-grid-column>

                </vaadin-grid>`;
    }
    
    _eventTypeRenderer(event) {
        return html`
            <code>${event.type}</code>
        `;
    }
    
    _refresh(){
        this.jsonRpc.getLastEvents().then(events => {
            this._firedEvents = events.result;
        });
    }
    
    _clear(){
        this.jsonRpc.clearLastEvents().then(events => {
            this._firedEvents = events.result;
        });
    }
    
    _toggleContext(){
        this.jsonRpc.toggleSkipContextEvents().then(events => {
            this._firedEvents = events.result;
        });
    }

    _addToEvents(event){
        this._firedEvents = [
            event,
            ...this._firedEvents,
        ];
    }
}
customElements.define('qwc-arc-fired-events', QwcArcFiredEvents);