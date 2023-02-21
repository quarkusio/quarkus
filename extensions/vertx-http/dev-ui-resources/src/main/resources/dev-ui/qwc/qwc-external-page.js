import { LitElement, html, css} from 'lit';
import { until } from 'lit/directives/until.js';
import { RouterController } from 'router-controller';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@vanillawc/wc-codemirror';
import '@vanillawc/wc-codemirror/mode/yaml/yaml.js';
import '@vanillawc/wc-codemirror/mode/properties/properties.js';
import '@vanillawc/wc-codemirror/mode/javascript/javascript.js';
import '@vaadin/icon';

/**
 * This component loads an external page
 */
export class QwcExternalPage extends observeState(LitElement)  {
    
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
        var metadata = RouterController.currentMetaData();
        if(metadata){
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
        return html`${until(this._loadExternal(), html`<span>Loading with ${themeState.theme.name} theme</span>`)}`;
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
                            <span class="download" @click="${this._download}">
                                <vaadin-icon class="icon" icon="font-awesome-solid:download"></vaadin-icon>
                                Download
                            </span>
                            <wc-codemirror mode='${this._mode}'
                                        src='${this._externalUrl}'
                                        theme='base16-${themeState.theme.name}'
                                        readonly>
                                <link rel="stylesheet" href="/_static/wc-codemirror/theme/base16-${themeState.theme.name}.css">
                            </wc-codemirror>
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