import { LitElement, html, css} from 'lit';
import { RouterController } from 'router-controller';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import 'qui-themed-code-block';
import '@vaadin/progress-bar';

/**
 * This component loads an external page
 */
export class QwcExternalPage extends LitElement {
    routerController = new RouterController(this);
    
    static styles = css`
        .codeBlock {
            display:flex;
            gap: 10px;
            flex-direction: column;
            padding-left: 10px;
            padding-right: 10px;
        }
        .download {
            padding-left: 6px;
            color: var(--lumo-contrast-50pct);
            font-size: small;
            cursor: pointer;
        }
    `;

    static properties = {
        _externalUrl: {type: String},
        _mode: {type: String},
        _mimeType: {type: String}
    };

    connectedCallback() {
        super.connectedCallback();
        var metadata = this.routerController.getCurrentMetaData();
        if(metadata && metadata.dynamicUrlMethodName){
            let ns = this.routerController.getCurrentNamespace();
            this.jsonRpc = new JsonRpc(ns);
            this.jsonRpc[metadata.dynamicUrlMethodName]().then(jsonRpcResponse => {
                this._externalUrl = jsonRpcResponse.result;
                
                if(metadata.mimeType){
                    this._mimeType = metadata.mimeType;
                    this._deriveModeFromMimeType(this._mimeType);
                }else{
                    this._autoDetectMimeType();
                }
            });
        } else if (metadata && metadata.externalUrl){
            this._externalUrl = metadata.externalUrl;
            if(metadata.mimeType){
                this._mimeType = metadata.mimeType;
                this._deriveModeFromMimeType(this._mimeType);
            }else{
                this._autoDetectMimeType();
            }
        }
    }
    
    render() {
        if(this._mode){
            return this._loadExternal();
        }else {
            return html`
                <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                    <div>Loading content...</div>
                    <vaadin-progress-bar indeterminate></vaadin-progress-bar>
                </div>`;
        }
    }

    _autoDetectMimeType(){
        if(this._externalUrl){
            fetch(this._externalUrl)
                .then((res) => {
                        this._mimeType = res.headers.get('content-type');
                        this._deriveModeFromMimeType(this._mimeType);
                    }
                );
        }
    }

    _deriveModeFromMimeType(mimeType){
        if(mimeType.startsWith('application/yaml')){
            this._mode = "yaml";
        }else if(mimeType.startsWith('application/json')){
            this._mode = "javascript";
        }else if(mimeType.startsWith('text/html')){
            this._mode = "html";
        }else if(mimeType.startsWith('application/pdf')){
            this._mode = "pdf";
        }else{
            this._mode = "properties";
        }
    }

    _loadExternal(){
        if(this._mode){
            if(this._mode == "html" || this._mode == "pdf"){
                return html`<object type='${this._mimeType}'
                                data='${this._externalUrl}' 
                                width='100%' 
                                height='100%'>
                            </object>`;
            } else {
                return html`<div class="codeBlock">
                            <span class="download" @click="${this._download}" title="${this._externalUrl}">
                                <vaadin-icon class="icon" icon="font-awesome-solid:download"></vaadin-icon>
                                Download
                            </span>
                            <qui-themed-code-block 
                                mode='${this._mode}'
                                src='${this._externalUrl}'>
                            </qui-themed-code-block>
                        </div>
                        `;
            }
        }   
    }
    
    _download(e) {
        window.open(this._externalUrl, '_blank').focus();
    }
}
customElements.define('qwc-external-page', QwcExternalPage);
