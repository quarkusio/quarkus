import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { pages } from 'build-time-data';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import 'qwc/qwc-extension-link.js';

export class QwcResteasyReactiveCard extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
        .score {
            font-size: 4em;
            text-align: center;
            color: var(--lumo-primary-text-color);
            text-shadow: 2px 1px 0 var(--lumo-contrast-10pct);
        }
    
        .extensionLink {
            color: var(--lumo-contrast) !important;
            font-size: small;
            cursor: pointer;
            text-decoration: none;
        }
        .extensionLink:hover {
            filter: brightness(80%);
        }
        a, a:link, a:visited, a:hover, a:active{
            color: var(--lumo-primary-color);
        }
    `;
    
    static properties = {
        extensionName: {attribute: true},
        description: {attribute: true},
        guide: {attribute: true},
        namespace: {attribute: true},
        _pages: {state: false},
        _latestScores: {state: true},
    };
    
    constructor() {
        super();
        this._pages = pages;
        this._latestScores = null;
    }
    
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getEndpointScores().then(endpointScores => {
            this._latestScores = endpointScores.result;
        });
    }
    
    render() {
        
        if(this._latestScores){
            if(this._latestScores.endpoints){
                return html`<div class="score" @click=${this.hotReload}>
                                ${this._latestScores.score} %
                            </div>
            ${this._renderPagesLinks()}`;
            }else{
                return html`${this.description}
                            <p>No endpoints detected. 
                            <a href="${this.guide}" target="_blank">Learn how you can add Endpoints</a></p>`;
            }
        }
    }
    
    _renderPagesLinks(){
        return html`${this._pages.map(page => html`
                            <qwc-extension-link slot="link"
                                namespace="${this.namespace}"
                                extensionName="${this.name}"
                                iconName="${page.icon}"
                                displayName="${page.title}"
                                staticLabel="${page.staticLabel}"
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                path="${page.id}"
                                ?embed=${page.embed}
                                externalUrl="${page.metadata.externalUrl}"
                                webcomponent="${page.componentLink}" >
                            </qwc-extension-link>
                        `)}`;
    }
    
    hotReload(){
        this._latestScores = null;
        this.jsonRpc.getEndpointScores().then(endpointScores => {
            this._latestScores = endpointScores.result;
        });
    }
}
customElements.define('qwc-resteasy-reactive-card', QwcResteasyReactiveCard);