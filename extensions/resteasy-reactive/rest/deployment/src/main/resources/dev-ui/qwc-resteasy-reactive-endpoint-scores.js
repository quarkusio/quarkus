import {css, html, QwcHotReloadElement} from 'qwc-hot-reload-element';
import {JsonRpc} from 'jsonrpc';

import '@vaadin/details';
import '@vaadin/horizontal-layout';
import 'echarts-gauge-grade';
import 'qui-badge';
import 'qwc-no-data';

/**
 * This component shows the Rest Easy Reactive Endpoint scores
 */
export class QwcResteasyReactiveEndpointScores extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        
        .heading{
            display: flex;
            gap: 20px;
            width: 100em;
            padding: 20px;
            background: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }
        .details {
            display: flex;
            flex-direction: column;
            gap: 20px;
        }
        .diagnostics {
            display: flex;
            justify-content: space-evenly;
            gap: 20px;
            height: 350px;
        }
        .diagnosticsText {
            display: flex;
            justify-content: space-evenly;
            gap: 20px;
        }
        .diagnostic {
            display: flex;
            flex-direction: column;
            gap: 10px;
            width: 100%;
        }
        .diagnosticText {
            display: flex;
            flex-direction: column;
            gap: 10px;
            width: 33%;
            overflow-wrap: break-word;
        }
    
        .information {
            border-top: 1px solid var(--lumo-contrast-10pct);
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .message {
            text-align: center;
            padding-left: 20px;
            padding-right: 20px;
            color: var(--lumo-contrast-70pct);
        }
        .infoTable {
            border: none;
        }
        .col1{
            text-align: right;
            width: 200px;
            font-weight: bolder;
        }
        
        .httpMethod{
            color: var(--lumo-primary-text-color);
        }
    `;

    static properties = {
        _latestScores: {state: true}
    };

    constructor() {
        super();
        this._latestScores = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    render() {
        if(this._latestScores){
            if(this._latestScores.endpoints){
                return html`${this._latestScores.endpoints.map(endpoint=>{
                    return html`${this._renderEndpoint(endpoint)}`;
                })}`;
            }
        }
        return html`<qwc-no-data message="You do not have any REST endpoints." 
                                    link="https://quarkus.io/guides/resteasy-reactive"
                                    linkText="Learn how to write REST Services with Quarkus REST">
                </qwc-no-data>
            `;
    }

    _renderEndpoint(endpoint){
        let level = this._getLevel(endpoint.score);
        
        return html`
            <vaadin-details opened theme="reverse">
                
                <div class="heading" slot="summary">
                    <qui-badge level='${level}'><span>${endpoint.score}/100</span></qui-badge>
                    <div>
                        <code class="httpMethod">${endpoint.httpMethod}</code> <code>${endpoint.fullPath}</code>
                    </div>
                    
                </div>
                
                <div class="details">
                    ${this._renderDiagnostics(endpoint.diagnostics)}
                    ${this._renderInformation(endpoint)}
                </div>
          </vaadin-details>`;
    }

    _renderDiagnostics(diagnostics){
        const map = new Map(Object.entries(diagnostics));
        const graphTemplates = [];
        const textTemplates = [];
        for (let [key, value] of map) {
            graphTemplates.push(html`${this._renderDiagnosticGraph(value, key)}`);
            textTemplates.push(html`${this._renderDiagnosticText(value)}`);
        }

        return html`<div class="diagnostics">
                        ${graphTemplates}
                    </div>
                    <div class="diagnosticsText">
                        ${textTemplates}
                    </div>`;
    }

    _renderDiagnosticGraph(diagnostic, heading){
        let score = diagnostic[0].score;
        let level = this._getLevel(score);
        
        return html`<div class="diagnostic">
                        <echarts-gauge-grade 
                            title="${heading}" 
                            percentage="${score}"
                            sectionColors="--lumo-${level}-color">
                        </echarts-gauge-grade>    
                    </div>`;
    }
    
    _renderDiagnosticText(diagnostic){
        let whatToDo = html``;
        
        return html`<div class="diagnosticText">
                        <div class="message">${diagnostic[0].message}</div>
                    </div>`;
    }
    
    _renderInformation(endpoint){
        return html`<div class="information">
                        <table class="infoTable">
                            ${this._renderMediaType("Produces", endpoint.producesHeaders)}
                            ${this._renderMediaType("Consumes", endpoint.consumesHeaders)}
                    
                            <tr>
                                <td class="col1">Resource Class:</td>
                                <td>${endpoint.className}</td>
                            </tr>
                        </table>
                    </div>`;
    }

    _renderMediaType(type,mediaType) {
        if(mediaType && mediaType.length>0){
            return html`<tr>
                            <td class="col1">${type}:</td>
                            <td>${mediaType.map(mt=>
                                html`<qui-badge><span>${mt}</span></qui-badge>`
                            )}</td>
                        </tr>`;
        }
    }

    _getLevel(score){
        let level = "error";
        if(score === 66){
            level = "warning";
        }else if(score === 100){
            level = "success";
        }
        return level;
    }

    hotReload(){
        this._latestScores = null;
        this.jsonRpc.getEndpointScores().then(endpointScores => {
            this._latestScores = endpointScores.result;
        });
    }

}
customElements.define('qwc-resteasy-reactive-endpoint-scores', QwcResteasyReactiveEndpointScores);