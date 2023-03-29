import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import { until } from 'lit/directives/until.js';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@vanillawc/wc-codemirror';
import '@vanillawc/wc-codemirror/mode/yaml/yaml.js';
import '@vanillawc/wc-codemirror/mode/properties/properties.js';
import '@vanillawc/wc-codemirror/mode/javascript/javascript.js';
import '@vaadin/icon';

export class QwcKubernetesManifest extends observeState(LitElement)  {

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
        "_manifests": {state: true, type: Map}
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.generateManifests().then(jsonRpcResponse => {
            console.log(jsonRpcResponse.result);
            const  data = JSON.parse(jsonRpcResponse.result);
            this._manifests = new Map();
            for (const key in data) {
                this._manifests.set(key, data[key]);
            }
            this.requestUpdate();
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        return html`${until(this._renderManifests(), html`<span>Generating Kubernetes manifests...</span>`)}`;
    }

    // View / Templates

    _renderManifests() {
        if (this._manifests) {
            let full = [];
            for (let [filename, content] of this._manifests) {
                console.log("rendering ", filename, content)
                full.push(html`
                    <div class="manifest">
                        <h3>${filename}</h3>
                        <div class="codeBlock">
                            <wc-codemirror mode='yaml'
                                           theme='base16-${themeState.theme.name}'
                                           readonly>
                                <link rel="stylesheet"
                                      href="/_static/wc-codemirror/theme/base16-${themeState.theme.name}.css">
                                ${content}
                            </wc-codemirror>
                        </div>
                    </div>`);
            }
            return full;
        }
    }

}
customElements.define('qwc-kubernetes-manifest', QwcKubernetesManifest);
