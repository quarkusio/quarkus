import {css, html, QwcHotReloadElement} from 'qwc-hot-reload-element';
import {JsonRpc} from 'jsonrpc';

import '@vaadin/details';
import '@vaadin/horizontal-layout';
import 'echarts-gauge-grade';
import '@qomponent/qui-badge';
import 'qwc-no-data';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Rest Easy Reactive Endpoint scores
 */
export class QwcResteasyReactiveEndpointScores extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`

        .heading{
            display: flex;
            gap: 20px;
            align-items: center;
            width: 100%;
            padding: 12px 20px;
            box-sizing: border-box;
            background: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-10pct);
            border-radius: 8px 8px 0 0;
        }
        .details {
            display: flex;
            flex-direction: column;
            gap: 20px;
            padding: 10px 0;
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
            padding: 10px 0;
        }
        .message {
            text-align: center;
            padding: 10px 20px;
            color: var(--lumo-contrast-70pct);
            background: var(--lumo-contrast-5pct);
            border-radius: 6px;
            font-size: var(--lumo-font-size-s);
        }
        .infoTable {
            border: none;
        }
        .col1{
            text-align: right;
            width: 200px;
            font-weight: bolder;
        }

        .httpMethod {
            font-weight: 600;
            font-family: monospace;
        }

        .method-GET { color: hsl(142, 70%, 35%); }
        .method-POST { color: hsl(214, 90%, 40%); }
        .method-PUT { color: hsl(35, 90%, 35%); }
        .method-DELETE { color: hsl(0, 75%, 40%); }
        .method-PATCH { color: hsl(280, 65%, 40%); }
        .method-OPTIONS { color: hsl(190, 60%, 35%); }
        .method-HEAD { color: hsl(50, 60%, 35%); }

        .path {
            color: var(--lumo-contrast-70pct);
            font-family: monospace;
        }

        vaadin-details {
            margin-bottom: 10px;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: 10px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06);
            overflow: hidden;
        }

        .infoRow {
            display: flex;
            gap: 10px;
            padding: 6px 10px;
            border-radius: 6px;
        }

        .infoRow:hover {
            background-color: var(--lumo-contrast-5pct);
        }

        .infoLabel {
            font-weight: 600;
            min-width: 150px;
            text-align: right;
            color: var(--lumo-contrast-60pct);
        }
    `;

    static properties = {
        _latestScores: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
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
        return html`<qwc-no-data
                        message="${msg('You do not have any REST endpoints.', { id: 'quarkus-rest-no-rest-endpoints' })}"
                        link="https://quarkus.io/guides/resteasy-reactive"
                        linkText="${msg('Learn how to write REST Services with Quarkus REST', { id: 'quarkus-rest-learn-rest' })}">
                </qwc-no-data>
            `;
    }

    _renderEndpoint(endpoint){
        let level = this._getLevel(endpoint.score);
        let methodClass = `httpMethod method-${endpoint.httpMethod}`;

        return html`
            <vaadin-details opened theme="reverse">

                <div class="heading" slot="summary">
                    <qui-badge level='${level}'><span>${endpoint.score}/100</span></qui-badge>
                    <div>
                        <span class="${methodClass}">${endpoint.httpMethod}</span>
                        <span class="path">${endpoint.fullPath}</span>
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
        return html`<div class="diagnosticText">
                        <div class="message">${diagnostic[0].message}</div>
                    </div>`;
    }

    _renderInformation(endpoint){
        return html`<div class="information">
                        ${this._renderMediaType(msg('Produces', { id: 'quarkus-rest-produces' }), endpoint.producesHeaders)}
                        ${this._renderMediaType(msg('Consumes', { id: 'quarkus-rest-consumes' }), endpoint.consumesHeaders)}
                        <div class="infoRow">
                            <span class="infoLabel">${msg('Resource Class', { id: 'quarkus-rest-resource-class' })}:</span>
                            <span>${endpoint.className}</span>
                        </div>
                    </div>`;
    }

    _renderMediaType(type,mediaType) {
        if(mediaType && mediaType.length>0){
            return html`<div class="infoRow">
                            <span class="infoLabel">${type}:</span>
                            <span>${mediaType.map(mt=>
                                html`<qui-badge><span>${mt}</span></qui-badge>`
                            )}</span>
                        </div>`;
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
