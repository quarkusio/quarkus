import { QwcAbstractLogElement, html, css} from 'qwc-abstract-log-element';
import { repeat } from 'lit/directives/repeat.js';
import { LogController } from 'log-controller';
import { JsonRpc } from 'jsonrpc';

/**
 * This component represent Log file in the footer.
 */
export class QwcFooterLog extends QwcAbstractLogElement {
    logControl = new LogController(this);
    jsonRpc = new JsonRpc(this,false);
    
    static styles = css`
        .log {
            width: 100%;
            height: 100%;
            max-height: 100%;
            display: flex;
            flex-direction:column;
        }
    
        .text-warn {
            color: var(--lumo-warning-color-50pct);
        }
        .text-error{
            color: var(--lumo-error-text-color);
        }
    
        .message {
        }
    `;
    
    static properties = {
        jsonRPCMethodName:{type: String},
        _messages: {state:true},
        _zoom: {state:true},
        _increment: {state: false},
        _followLog: {state: false},
        _observer: {state:false}
    };
    
    constructor() {
        super();
         
        this.logControl
                .addToggle("On/off switch", true, (e) => {
                    this._toggleOnOffClicked(e);
                }).addItem("Zoom out", "font-awesome-solid:magnifying-glass-minus", "var(--lumo-tertiary-text-color)", (e) => {
                    this._zoomOut();
                }).addItem("Zoom in", "font-awesome-solid:magnifying-glass-plus", "var(--lumo-tertiary-text-color)", (e) => {
                    this._zoomIn();
                }).addItem("Clear", "font-awesome-solid:trash-can", "var(--lumo-error-color)", (e) => {
                    this._clearLog();
                }).addFollow("Follow log", true , (e) => {
                    this._toggleFollowLog(e);
                }).done();
                
        this._messages = [];
        this._zoom = parseFloat(1.0);
        this._increment = parseFloat(0.05);
        this._followLog = true;
        this.jsonRPCMethodName = null;
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._toggleOnOff(true);
    }
    
    disconnectedCallback() {
        this._toggleOnOff(false);
        
        super.disconnectedCallback();
    }
    
    render() {
        if(this.jsonRPCMethodName){
            return html`<code class="log" style="font-size: ${this._zoom}em;">
                ${repeat(
                    this._messages,
                    (message) => message.sequenceNumber,
                    (message, index) => html`
                    <div class="logEntry">
                        ${this._renderLogEntry(message)}
                    </div>
                  `
                  )}
                </code>`;
        }
    }
    
    _renderLogEntry(message){
        
        if(message.length>0){
            let c="message";
            if (message.includes(' ERROR ')) {
                c = "text-error";
            } else if (message.includes(' WARN ')) {
                c = "text-warn";
            }

            return html`<span class='${c}'>${message}</span>`;
        }else{
            return html`<br/>`;
        }
    }
    
    _handleKeyPress(event) {
        if (event.key === 'Enter') {
            this._addLogEntry("");
        }
    }
    
    _handleZoomIn(event){
        this._zoomIn();
    }
    
    _handleZoomOut(event){
        this._zoomOut();
    }
    
    _toggleOnOffClicked(e){
        this._toggleOnOff(e);
        // Add line on stop
        if(!e){
            this._addLogEntry("----------------------------------------------------------------------");
        }
    }
    
    hotReload(){
        this._clearLog();
        if(this._observer != null){
            this._toggleOnOff(false);
            this._toggleOnOff(true);
        }
    }
    
    _toggleOnOff(e){
        if(e){
            this._observer = this.jsonRpc[this.jsonRPCMethodName]().onNext(jsonRpcResponse => {
                this._addLogEntry(jsonRpcResponse.result);
            });
        }else{
            this._observer.cancel();
            this._observer = null;
        }
    }
    
    _addLogEntry(entry){
        this._messages = [
            ...this._messages,
            entry
        ];

        this._scrollToBottom();
    }
    
    async _scrollToBottom(){
        if(this._followLog){
            await this.updateComplete;
            
            const last = Array.from(
                this.shadowRoot.querySelectorAll('.logEntry')
            ).pop();
            
            if(last){
                last.scrollIntoView({
                    behavior: "smooth",
                    block: "end"
                });
            }
        }
    }
    
    _zoomOut(){
        this._zoom = parseFloat(parseFloat(this._zoom) - parseFloat(this._increment)).toFixed(2);
    }
    
    _zoomIn(){
        this._zoom = parseFloat(parseFloat(this._zoom) + parseFloat(this._increment)).toFixed(2);
    }
    
    _clearLog(){
        this._messages = [];
    }
    
    _toggleFollowLog(e){
        this._followLog = e;
        this._scrollToBottom();   
    }
    
}
customElements.define('qwc-footer-log', QwcFooterLog);