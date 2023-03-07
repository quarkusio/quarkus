import { LitElement, html, css} from 'lit';
import { pages } from 'resteasy-reactive-data';
import { JsonRpc } from 'jsonrpc';
import 'echarts-gauge-grade';
import '@vaadin/icon';

export class QwcResteasyReactiveCard extends LitElement {
    jsonRpc = new JsonRpc("RESTEasy Reactive");
    
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
    `;
    
    static properties = {
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
            return html`<div class="graph" @click=${this._refresh}>
                <echarts-gauge-grade 
                            percentage="${this._latestScores.score}"
                            percentageFontSize="14"
                            sectionColors="--lumo-error-color,--lumo-warning-color,--lumo-success-color"
                        </echarts-gauge-grade>
            </div>
            ${this._renderPagesLinks()}`;
        }
    }
    
    _renderPagesLinks(){
        return html`<a class="extensionLink" href="${this._pages[0].id}">
                <vaadin-icon class="icon" icon="${this._pages[0].icon}"></vaadin-icon>
                ${this._pages[0].title}
            </a>`;
    }
    
    _refresh(){
        this._latestScores = null;
        this.jsonRpc.getEndpointScores().then(endpointScores => {
            this._latestScores = endpointScores.result;
        });
    }
}
customElements.define('qwc-resteasy-reactive-card', QwcResteasyReactiveCard);