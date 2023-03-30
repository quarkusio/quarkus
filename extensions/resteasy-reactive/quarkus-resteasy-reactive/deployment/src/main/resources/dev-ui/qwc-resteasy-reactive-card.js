import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { pages } from 'build-time-data';
import { JsonRpc } from 'jsonrpc';
import 'echarts-gauge-grade';
import '@vaadin/icon';

export class QwcResteasyReactiveCard extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
        .graph {
            height: 200px;
        }
        .extensionLink {
            color: var(--lumo-contrast);
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
        description: {attribute: true},
        guide: {attribute: true},
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
                return html`<div class="graph" @click=${this.hotReload}>
                                <echarts-gauge-grade 
                                    percentage="${this._latestScores.score}"
                                    percentageFontSize="14"
                                    sectionColors="--lumo-error-color,--lumo-warning-color,--lumo-success-color">
                                </echarts-gauge-grade>
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
        return html`<a class="extensionLink" href="${this._pages[0].id}">
                <vaadin-icon class="icon" icon="${this._pages[0].icon}"></vaadin-icon>
                ${this._pages[0].title}
            </a>`;
    }
    
    hotReload(){
        this._latestScores = null;
        this.jsonRpc.getEndpointScores().then(endpointScores => {
            this._latestScores = endpointScores.result;
        });
    }
}
customElements.define('qwc-resteasy-reactive-card', QwcResteasyReactiveCard);