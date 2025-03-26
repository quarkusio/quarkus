import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/progress-bar';
import '@qomponent/qui-code-block';
import '@qomponent/qui-badge';
import { themeState } from 'theme-state';
import { observeState } from 'lit-element-state';
import { notifier } from 'notifier';

export class QwcOpenapiGenerateClient extends observeState(LitElement) {
    jsonRpc = new JsonRpc(this);
    static styles = css`
        :host {
            display: flex;
        }
    
        .heading {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
    
        .generatedcode {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
    
        qui-badge {
            display: flex;
            flex-direction: column;
        }
        .progress {
            padding-top: 50px;
            padding-left: 20px;
            padding-right: 20px;
        }
    `;

    static properties = {
        _codemap: {state: true},
        _visitedTabs: {state: true}
    };

    constructor() {
        super();
        this._visitedTabs = new Set();
        this._codemap = new Map();
    }

    connectedCallback() {
        super.connectedCallback();
    }

    disconnectedCallback() {
        super.disconnectedCallback()
    }

    render() {
        return html`
        <vaadin-tabsheet @selected-changed=${this._selectedTabChanged} style="width: 100%;">
            <vaadin-tabs slot="tabs">
                <vaadin-tab id="quarkus-tab">Quarkus</vaadin-tab>
                <vaadin-tab id="javascript-tab">Javascript</vaadin-tab>
                <vaadin-tab id="typescript-tab">Typescript</vaadin-tab>
                <vaadin-tab id="csharp-tab">C#</vaadin-tab>
                <vaadin-tab id="cplusplus-tab">C++</vaadin-tab>
                <vaadin-tab id="php-tab">PHP</vaadin-tab>
                <vaadin-tab id="python-tab">Python</vaadin-tab>
                <vaadin-tab id="rust-tab">Rust</vaadin-tab>
                <vaadin-tab id="go-tab">Go</vaadin-tab>
            </vaadin-tabs>
            ${this._visitedTabs.has(0)
              ? html`<div tab="quarkus-tab">${this._renderClientTab("Quarkus", "java", "This should use the rest-client-jackson extension and the Jakarta namespace (not javax)")}</div>`
              : ''}
            ${this._visitedTabs.has(1)
              ? html`<div tab="javascript-tab">${this._renderClientTab("Javascript", "js", "")}</div>`
              : ''}
            ${this._visitedTabs.has(2)
              ? html`<div tab="typescript-tab">${this._renderClientTab("Typecript", "ts", "")}</div>`
              : ''}  
            ${this._visitedTabs.has(3)
              ? html`<div tab="csharp-tab">${this._renderClientTab("C#", "cs", "")}</div>`
              : ''}  
            ${this._visitedTabs.has(4)
              ? html`<div tab="cplusplus-tab">${this._renderClientTab("C++", "cpp", "")}</div>`
              : ''}  
            ${this._visitedTabs.has(5)
              ? html`<div tab="php-tab">${this._renderClientTab("PHP", "php", "")}</div>`
              : ''}    
            ${this._visitedTabs.has(6)
              ? html`<div tab="python-tab">${this._renderClientTab("Python", "py", "")}</div>`
              : ''}      
            ${this._visitedTabs.has(7)
              ? html`<div tab="rust-tab">${this._renderClientTab("Rust", "rust", "")}</div>`
              : ''}
            ${this._visitedTabs.has(8)
              ? html`<div tab="go-tab">${this._renderClientTab("Golang", "go", "")}</div>`
              : ''}  
        </vaadin-tabsheet>`;
    }

    _selectedTabChanged(event) {
        this._visitedTabs = new Set([...this._visitedTabs, event.detail.value]);
    }


    _renderClientTab(langName, langMode, extraContext){
        if(this._codemap.has(langName)){
            let code = this._codemap.get(langName);
            return html`<div class="generatedcode">
                            <div class="heading">${langName} code generated from the OpenAPI Schema with AI:
                                <vaadin-button theme="secondary" @click="${() => this._copyGeneratedContent(langName)}"> 
                                    <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                                    Copy
                                </vaadin-button>
                            </div>
                            <qui-code-block 
                                mode='${langMode}'
                                content='${code}'
                                theme='${themeState.theme.name}'
                                showLineNumbers>
                            </qui-code-block>
                        </div>
                        ${this._renderWarning()}`;

        }else {
            this._generateCodeWithAI(langName, extraContext);
            return html`<div class="progress">
                            <label class="text-secondary" id="pblbl">Talking to AI...</label>
                            <vaadin-progress-bar
                              indeterminate
                              aria-labelledby="pblbl"
                              aria-describedby="sublbl"
                            ></vaadin-progress-bar>
                            <span class="text-secondary text-xs" id="sublbl">
                              This can take a while
                            </span>
                        </div>`;
        }
    }

    async _generateCodeWithAI(langName, extraContext) {
        try {
            const jsonRpcResponse = await this.jsonRpc.generateClient({ language: langName, extraContext: extraContext });
            this._codemap.set(langName, jsonRpcResponse.result.code);
            this.requestUpdate();
        } catch (error) {
            console.error('Error generating code:', error);
        }
    }

    _renderWarning(){
        return html`<qui-badge text="Warning" level="warning" icon="warning">
            <span>AI can make mistakes. Check responses.</span>
        </qui-badge>`;
    }

    _copyGeneratedContent(langName){
        if(this._codemap.has(langName)) {
            var content = this._codemap.get(langName);
            navigator.clipboard.writeText(content)
            .then(() => {
                notifier.showInfoMessage("Content copied to clipboard");
            })
            .catch(err => {
                notifier.showErrorMessage("Failed to copy content:" + err);
            });
        } else {
            notifier.showWarningMessage("No content");
            return;
        }
    }
}
customElements.define('qwc-openapi-generate-client', QwcOpenapiGenerateClient);