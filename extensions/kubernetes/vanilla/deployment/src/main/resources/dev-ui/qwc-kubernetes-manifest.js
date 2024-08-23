import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@quarkus-webcomponents/codeblock';
import '@vaadin/icon';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/progress-bar';

export class QwcKubernetesManifest extends observeState(LitElement) {

    jsonRpc = new JsonRpc(this);

    // Component style
    static styles = css`
      .codeBlock {
        display:flex;
        gap: 10px;
        flex-direction: column;
        padding-left: 10px;
        padding-right: 10px;
      }
    `;

    // Component properties
    static properties = {
        // Name -> Content
        _manifests: {state: true, type: Map},
        _message: {state: true}
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this._message = "Generating Kubernetes manifests...";
        this.jsonRpc.generateManifests().then(jsonRpcResponse => {
            const data = JSON.parse(jsonRpcResponse.result);
            var m = new Map();
            for (const key in data) {
                m.set(key, data[key]);
            }
            this._manifests = m;
            this._message = "No manifests generated.";
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        if (this._manifests) {
            if (this._manifests.size == 0) {
                return html`
                <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                    <div>${this._message}</div>
                </div>
                `;
            }

            return html`
                <vaadin-tabsheet>
                    <vaadin-tabs slot="tabs">
                        ${this._renderFileName()}
                    </vaadin-tabs>
                    ${this._renderContent()}
                </vaadin-tabsheet>
              `;
        }

        return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>${this._message}</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
    }

    _renderFileName(){
        let keys = Array.from( this._manifests.keys() );
        return html`${keys.map((key) =>
                    html`<vaadin-tab id="${key}">${key}</vaadin-tab>`
                )}`;
    }

    _renderContent(){
        let keys = Array.from( this._manifests.keys() );
        return html`${keys.map((key) =>
                    html`<div tab="${key}">${this._renderYaml(key)}</div>`
                )}`;
    }

    _renderYaml(key){
        let yaml = this._manifests.get(key);
        
        return html`<div class="codeBlock">
                            <qui-code-block 
                                mode="yaml"
                                content="${yaml}"
                                theme="${themeState.theme.name}">
                            </qui-code-block>
                        </div>`;
    }

}
customElements.define('qwc-kubernetes-manifest', QwcKubernetesManifest);
