import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import { LogController } from 'log-controller';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the log of scheduled executions.
 */
export class QwcSchedulerLog extends LitElement {
    
    jsonRpc = new JsonRpc(this, false);
    logControl = new LogController(this);

    static styles = css`
        .text-error {
            color: var(--lumo-error-text-color);
        }
        .text-normal {
            color: var(--lumo-success-color-50pct);
        }
        .text-info {
            color: var(--lumo-primary-text-color);
        }
    `;
    
    static properties = {
        _logEntries: {state:true},
        _observer: {state:false},
        _zoom: {state:true},
        _increment: {state: false},
        _followLog: {state: false}
    };
    
    constructor() {
        super();
        updateWhenLocaleChanges(this);

        this.logControl
                .addToggle(
                    msg('On/off switch', { id: 'quarkus-scheduler-log-on-off-switch' }),
                    true,
                    (e) => {
                        this._toggleOnOffClicked(e);
                    })
                .addItem(
                    msg('Zoom out', { id: 'quarkus-scheduler-log-zoom-out' }),
                    'font-awesome-solid:magnifying-glass-minus',
                    'grey',
                    (e) => {
                        this._zoomOut();
                    })
                .addItem(
                    msg('Zoom in', { id: 'quarkus-scheduler-log-zoom-in' }),
                    'font-awesome-solid:magnifying-glass-plus',
                    'grey',
                    (e) => {
                        this._zoomIn();
                    })
                .addItem(
                    msg('Clear', { id: 'quarkus-scheduler-log-clear' }),
                    'font-awesome-solid:trash-can',
                    '#FF004A',
                    (e) => {
                        this._clearLog();
                    })
                .addFollow(
                    msg('Follow log', { id: 'quarkus-scheduler-log-follow-log' }),
                    true,
                    (e) => {
                        this._toggleFollowLog(e);
                    })
                .done();
        this._logEntries = [];
        this._zoom = parseFloat(1.0);
        this._increment = parseFloat(0.05);
        this._followLog = true;
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._connect();
    }
    
    disconnectedCallback() {
        this._disconnect();
        super.disconnectedCallback();
    }
    
    _connect(){
        if(!this._observer){
            this._observer = this.jsonRpc.streamLog().onNext(jsonRpcResponse => {
                this._addLogEntry(jsonRpcResponse.result);
            });
        }
    }
    
    _disconnect() {
        if(this._observer){
            this._observer.cancel();
        }
    }
    
    render() {
        return html`${this._logEntries.map((le) =>
            html`<code class="log" style="font-size: ${this._zoom}em;">
                    <div class="logEntry">
                        ${this._renderLogEntry(le)}
                    </div>
            </code>`
        )}`;    
    }
    
    _renderLogEntry(logEntry){
         return html`
                ${this._renderTime(logEntry)}
                ${this._renderIcon(logEntry)}
                ${this._renderMessage(logEntry)}
                `;
    }
    
    _renderTime(logEntry){
        return html`<span title=${msg('Time', { id: 'quarkus-scheduler-log-time' })}>${logEntry.timestamp}</span>`;
    }
    
    _renderMessage(logEntry){
        return logEntry.success
            ? html`${msg('Job executed', { id: 'quarkus-scheduler-log-job-executed' })} ${this._renderInfo(logEntry)}`
            : html`${msg('Job failed', { id: 'quarkus-scheduler-log-job-failed' })} ${this._renderInfo(logEntry)}: <span class="text-error">${logEntry.message}<span>` ;
    }
    
    _renderInfo(logEntry){
        return html`<span
            title=${msg('Info', { id: 'quarkus-scheduler-log-info' })}
            class='text-info'>
            [${this._renderIdentity(logEntry)}${this._renderMethodDescription(logEntry)}]
        </span>`;
    }
    
    _renderIdentity(logEntry){
        return logEntry.triggerIdentity
            ? html`${msg('identity:', { id: 'quarkus-scheduler-log-identity-label' })} ${logEntry.triggerIdentity}${logEntry.triggerMethodDescription ? ', ' : ''}`
            : '';
    }
    
    _renderMethodDescription(logEntry){
        return logEntry.triggerMethodDescription
            ? html`${msg('method:', { id: 'quarkus-scheduler-log-method-label' })} ${logEntry.triggerMethodDescription}`
            : '';
    }
    
    _renderIcon(logEntry){
        if (logEntry.success){
            return html`<vaadin-icon icon="font-awesome-solid:circle-info" class="icon-info text-normal"></vaadin-icon>`;
        } else {
            return html`<vaadin-icon icon="font-awesome-solid:radiation" class="text-error"></vaadin-icon>`;
        }
    }
    
    _addLogEntry(logEntry){
        this._logEntries = [
            ...this._logEntries,
            logEntry
        ];
        this._scrollToBottom();
    }
    
    _toggleOnOffClicked(e){
        if(e){
            this._connect();
        }else{
            this._disconnect();
        }
    }
    
    _zoomOut(){
        this._zoom = parseFloat(parseFloat(this._zoom) - parseFloat(this._increment)).toFixed(2);
    }
    
    _zoomIn(){
        this._zoom = parseFloat(parseFloat(this._zoom) + parseFloat(this._increment)).toFixed(2);
    }
    
    _clearLog(){
        this._logEntries = [];
    }
    
    _toggleFollowLog(e){
        this._followLog = e;
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
}
customElements.define('qwc-scheduler-log', QwcSchedulerLog);
