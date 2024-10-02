import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@quarkus-webcomponents/codeblock';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';
import '@vaadin/tabs';
import '@vaadin/split-layout';
import 'qui-badge';
import 'qui-ide-link';
import '@vaadin/button';

/**
 * This component shows the Grpc Services
 */
export class QwcGrpcServices extends observeState(QwcHotReloadElement) { 
    jsonRpc = new JsonRpc(this);
    streamsMap = new Map();

    static styles = css`
        .table {
            height: 100%;
            padding-bottom: 10px;
        }
        
        vaadin-icon{
            width: var(--lumo-icon-size-s);
            height: var(--lumo-icon-size-s);
        }
        .methods {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }
        .inputOutput {
            display: flex;
            width: 100%;
            gap: 5px;
        }
        .input, .output{
            width: 100%;
        }
    
        .progress-short {
            width: 250px;
        }
        .streamButtons {
            display: flex;
            justify-content: space-between;
        }
    
    `;

    static properties = {
        _services: {state: true},
        _streamsMap: {state: true, type: Map},
        _detailsOpenedItem: {state: true, type: Array},
        _testerContent: {state: true},
        _testerButtons: {state: true}
    };

    constructor() { 
        super();
        this._detailsOpenedItem = [];
        this._streamsMap = new Map();
        this._testerContent = '';
        this._testerButtons = '';
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    disconnectedCallback() {
        for (let value of this._streamsMap.values()){
            value.cancel();
        }
        super.disconnectedCallback();
    }

    hotReload(){
        this.jsonRpc.getServices().then(jsonRpcResponse => { 
            this._services = jsonRpcResponse.result;
            this._forceUpdate();
        });
    }

    render() { 
        if(this._services){
            return html`<vaadin-grid .items="${this._services}" class="table" theme="no-border"
                                .detailsOpenedItems="${this._detailsOpenedItem}"
                                @active-item-changed="${(event) => {
                                        const prop = event.detail.value;
                                        this._detailsOpenedItem = prop ? [prop] : [];
                                    }
                                }"
                            ${gridRowDetailsRenderer(this._testerRenderer, [])}
                        >

                    <vaadin-grid-column width="25px"
                        header="Status"
                        ${columnBodyRenderer(this._statusRenderer, [])}>
                    </vaadin-grid-column>

                    <vaadin-grid-sort-column auto-width
                        header="Name"
                        path="name"
                        ${columnBodyRenderer(this._nameRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column auto-width
                        header="Implementation Class"
                        path="serviceClass"            
                        ${columnBodyRenderer(this._serviceClassRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>
            
                    <vaadin-grid-column auto-width
                        header="Methods"
                        ${columnBodyRenderer(this._methodsRenderer, [])}>
                    </vaadin-grid-column>
            
                    <vaadin-grid-column width="25px"
                        header=""
                        ${columnBodyRenderer(this._testRenderer, [])}>
                    </vaadin-grid-column>
            
                </vaadin-grid>`;    
        }else{
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }
    
    _statusRenderer(service){
        if(service.status === "SERVING"){
            return html`<vaadin-icon style="color: var(--lumo-success-text-color);" icon="font-awesome-solid:check"></vaadin-icon>`;
        }else if(service.status === "NOT_SERVING"){
            return html`<vaadin-icon style="color: var(--lumo-error-text-color);" icon="font-awesome-solid:circle-exclamation"></vaadin-icon>`;
        }else {
            return html`<vaadin-icon icon="font-awesome-solid:circle-question"></vaadin-icon>`;
        }
    }
    
    _nameRenderer(service){
        return html`<code>${service.name}</code>`;
    }
    
    _serviceClassRenderer(service){
        return html`<qui-ide-link fileName='${service.serviceClass}'
                        lineNumber=0><code>${service.serviceClass}</code></qui-ide-link>`;
    }
    
    _methodsRenderer(service){
        return html`<div class="methods">${service.methods.map(method =>
            html`${this._methodRenderer(method)}`
        )}</div>`;
    }
    
    _methodRenderer(method){
        return html`<span><qui-badge level="contrast" pill small><span>${method.type}</span></qui-badge> ${method.bareMethodName}</span>`;
    }
    
    _testRenderer(service){
        if(service.hasTestableMethod){
             return html`<vaadin-icon icon="font-awesome-solid:chevron-down"></vaadin-icon>`;
        }
    }
    
    _testerRenderer(service){
        
        if(service.methods.length > 1 ){
            
            return html`<vaadin-tabs @selected-changed="${(e) => this._tabSelectedChanged(service, e.detail.value)}">
                            ${service.methods.map(method =>
                                html`${this._methodTesterTabHeadingRenderer(service.name, method)}`
                            )}
                        </vaadin-tabs>
                        <div>
                            ${this._testerContent}
                            ${this._testerButtons}
                        </div>`;
        } else {
            return html`
                ${this._methodTesterRenderer(service, service.methods[0])}
                ${this._renderCommandButtons(service, service.methods[0])}
            `;
        }
    }
    
    _tabSelectedChanged(service, n){
        let method = service.methods[n];
        this._testerContent = this._methodTesterRenderer(service, method);
        this._testerButtons = this._renderCommandButtons(service, method);
        this._forceUpdate();
    }
    
    _methodTesterTabHeadingRenderer(serviceName,method) {
        return html`<vaadin-tab id="${this._id(serviceName,method)}">
                        <span>${method.bareMethodName}</span> 
                         <span><qui-badge level="contrast" pill small><span>${method.type}</span></qui-badge><span>
                    </vaadin-tab>`;
    }
    
    _methodTesterRenderer(service, method){
            return html`<vaadin-split-layout>
                            <master-content style="width: 50%;">
                                <qui-code-block @keydown=${(e) => this._keypress(e, service, method)}
                                    id='${this._requestId(service.name, method)}'
                                    mode='json'
                                    content='${method.prototype}'
                                    value='${method.prototype}'
                                    theme='${themeState.theme.name}'
                                    editable>
                                </qui-code-block>
                            </master-content>
                            <detail-content style="width: 50%;">
                                <qui-code-block
                                    id='${this._responseId(service.name, method)}'
                                    mode='json'
                                    content='\n\n\n\n'
                                    theme='${themeState.theme.name}'>
                                </qui-code-block>
                            </detail-content>
                        </vaadin-split-layout>`;
    }
    
    _renderCommandButtons(service, method){
        if(this._streamsMap.size >=0){
            if(method.type == 'UNARY' || method.type == 'SERVER_STREAMING'){
                return html`<vaadin-button theme="secondary error" @click=${() => this._default(service.name, method)}>Reset</vaadin-button>
                            <vaadin-button theme="secondary success" @click=${() => this._test(service, method)}>Send</vaadin-button>`;
            }else if(this._isRunning(service.name, method)){
                return html`<vaadin-button theme="secondary error" @click=${() => this._default(service.name, method)}>Reset</vaadin-button>
                            <vaadin-button theme="secondary success" @click=${() => this._test(service, method)}>Send</vaadin-button>
                            <vaadin-button theme="secondary error" @click=${() => this._disconnect(service, method)}>Disconnect</vaadin-button>
                            <vaadin-progress-bar class="progress-short" indeterminate></vaadin-progress-bar>`;
            }else {
                return html`<vaadin-button theme="secondary success" @click=${() => this._test(service, method)}>Send</vaadin-button>`;
            }
        }
    }
    
    _keypress(e, service, method){
        if(method.type == 'UNARY' || method.type == 'SERVER_STREAMING' || !this._isRunning(service.name, method)){
            if ((e.keyCode == 10 || e.keyCode == 13) && e.ctrlKey){ // ctlr-enter
                this._test(service, method);
            }
        }
    }
    
    _isRunning(serviceName, method){
        let id = this._id(serviceName, method);
        return this._streamsMap.has(id);
    }
    
    _id(serviceName, method){
       return serviceName + "_" + method.bareMethodName + "_" + method.type; 
    }
    
    _clear(serviceName, method){
        this._requestTextArea(serviceName, method).clear();
        this._responseTextArea(serviceName, method).clear();
    }
    
    _default(serviceName, method){
        let requestTextArea = this._requestTextArea(serviceName, method);
        requestTextArea.content = '';
        let pv = JSON.parse(method.prototype);
        let prettyJson = JSON.stringify(pv, null, 2);
        requestTextArea.populatePrettyJson(prettyJson);
    }
    
    _test(service, method){
        let requestTextArea = this._requestTextArea(service.name, method);
        let content = requestTextArea.getAttribute('value');
        let id = this._id(service.name, method);
        let responseTextArea = this._responseTextArea(service.name, method);
        if(method.type == 'UNARY'){
            this.jsonRpc.testService({
                id: id,
                serviceName: service.name,
                methodName: method.bareMethodName,
                methodType: method.type,
                content: content
            }).then(jsonRpcResponse => {
                this._responseTextArea(service.name, method).populatePrettyJson(this._prettyJson(jsonRpcResponse.result));
            });
        }else{
            if(this._isRunning(service.name, method)){
                this.jsonRpc.streamService({
                    id: id,
                    serviceName: service.name,
                    methodName: method.bareMethodName,
                    isRunning: true,
                    content: content
                });
                // this._streamsMap.get(id).cancel();
                // this._streamsMap.delete(id);
                // this._clear(service.name, method);
                // this._default(service.name, method);
            }else{
                // starting a new stream, clear the response area
                responseTextArea.content = null;
                let cancelable = this.jsonRpc.streamService({
                    id: id,
                    serviceName: service.name,
                    methodName: method.bareMethodName,
                    isRunning: false,
                    content: content
                }).onNext(jsonRpcResponse => {
                    if (responseTextArea.content == null) {
                        responseTextArea.populatePrettyJson(this._prettyJson(jsonRpcResponse.result));
                    } else {
                        responseTextArea.populatePrettyJson(responseTextArea.content + '\n' + this._prettyJson(jsonRpcResponse.result));
                    }
                });
                if (method.type == 'BIDI_STREAMING' || method.type == 'CLIENT_STREAMING') {
                    this._streamsMap.set(id, cancelable);
                }
            }
            this._testerButtons = this._renderCommandButtons(service, method);
            this._forceUpdate();
        }
    }

    _disconnect(service, method){
        let id = this._id(service.name, method);
        this.jsonRpc.disconnectService({
            id: id,
        });
        this._streamsMap.delete(id);
        this._testerButtons = this._renderCommandButtons(service, method);
        this._forceUpdate();
    }
    
    _forceUpdate(){
        if(this._detailsOpenedItem.length > 0){
            let itemZero = this._detailsOpenedItem[0];
            this._detailsOpenedItem = [];
            this._detailsOpenedItem.push(itemZero);
        }
    }
    
    _requestTextArea(serviceName, method){
        return this.shadowRoot.getElementById(this._requestId(serviceName, method));
    }
    
    _responseTextArea(serviceName, method){
        return this.shadowRoot.getElementById(this._responseId(serviceName, method));
    }
    
    _requestId(serviceName, method){
        return serviceName + '/' + method.bareMethodName + '_request';
    }
    
    _responseId(serviceName, method){
        return serviceName + '/' + method.bareMethodName + '_response';
    }

    _prettyJson(content){
        return JSON.stringify(JSON.parse(content), null, 2);
    }
}
customElements.define('qwc-grpc-services', QwcGrpcServices);