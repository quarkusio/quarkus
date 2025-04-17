import { LitElement, html, css} from 'lit';
import '@vaadin/progress-bar';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@qomponent/qui-code-block';

/**
 * This component show all available tools for MCP clients
 */
export class QwcMCPTools extends observeState(LitElement) {
    tools = new JsonRpc("tools");

    static styles = css`
        
    `;

    static properties = {
        _tools: {state: true}
    }

    constructor() {
        super();

    }

    connectedCallback() {
        super.connectedCallback();
        this._loadTools();
    }

    render() {
        if (this._tools) {    
            return html`${this._renderTools()}`;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching tools...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }

    _renderTools(){
        return html`<vaadin-tabsheet>
                        <vaadin-tabs slot="tabs">
                            <vaadin-tab id="list-tab">List</vaadin-tab>
                            <vaadin-tab id="raw-tab">Raw json</vaadin-tab>
                        </vaadin-tabs>
                        <div tab="list-tab">
                            <vaadin-grid .items="${this._tools.tools}" all-rows-visible>
                                <vaadin-grid-sort-column 
                                    header='Name'
                                    path="name">
                                </vaadin-grid-sort-column>
                                <vaadin-grid-sort-column 
                                    header='Description'
                                    path="description">
                                </vaadin-grid-sort-column>
                            </vaadin-grid>
                        </div>
                        <div tab="raw-tab">
                            <div class="codeBlock">
                                <qui-code-block 
                                    mode='json'
                                    content='${JSON.stringify(this._tools, null, 2)}'
                                    theme='${themeState.theme.name}'
                                    showLineNumbers>
                                </qui-code-block>
                            </div>
                    </vaadin-tabsheet>
                `;
    }

    _loadTools(){
        this.tools.list().then(jsonRpcResponse => {
            this._tools = jsonRpcResponse.result;
        });
    }

}
customElements.define('qwc-mcp-tools', QwcMCPTools);