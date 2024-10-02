import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/vertical-layout';
import '@vaadin/icon';
import '@vaadin/details';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import '@qomponent/qui-badge';
import 'qui-ide-link';


import 'echarts-horizontal-stacked-bar';

/**
 * This component shows the Continuous Testing Page
 */
export class QwcContinuousTesting extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this, false);
    
    static styles = css`
        :host {
            height: 100%;
            display: flex;
            flex-direction: column;
        }
        .menubar{
            border-bottom: 1px solid var(--lumo-contrast-5pct);
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        
        .results {
            display: flex;
            gap: 10px;
            justify-content: space-between;
            height: 100%;
        }
        .warning {
            color: var(--lumo-warning-text-color);
        }
        .successful {
            color: var(--lumo-success-text-color);
        }
        .contrast {
            color: var(--lumo-contrast-70pct);
        }
        .failed {
            color: var(--lumo-error-text-color);
        }
        .resultTable {
            display: flex;
            flex-direction: column;
            overflow: hidden;
            margin-left: 40px;
            margin-top: 10px;
            height: 100%;
        }
        
        .progress {
            padding-right: 20px;
            width: 50%;
        }

        .total {
            padding-right: 20px;
            text-align: center;
        }
        .ideIcon {
            color: var(--lumo-contrast-40pct);
            font-size: small;
        }
    `;

    static properties = {
        _state: {state: true},
        _results: {state: true},
        _busy: {state: true},
        _detailsOpenedItem: {state: true, type: Array},
        _displayTags: {state: true, type: Boolean},
    };
  
    constructor() {
        super();
        this._busy = false;
        this._detailsOpenedItem = [];
        this._chartTitles = ["passed", "failed", "skipped"];
        this._chartColors = ['--lumo-success-text-color', '--lumo-error-text-color', '--lumo-contrast-70pct'];
        this._displayTags = true;
    }

    connectedCallback() {
        super.connectedCallback();
        this._lastKnownState();
        this._createObservers();
    }
    
    disconnectedCallback() {
        this._cancelObservers();    
        super.disconnectedCallback();
    }

    _createObservers(){
        this._streamStateObserver = this.jsonRpc.streamTestState().onNext(jsonRpcResponse => {
            this._state = JSON.parse(jsonRpcResponse.result);
        });
        this._streamResultsObserver = this.jsonRpc.streamTestResults().onNext(jsonRpcResponse => {
            this._results = jsonRpcResponse.result;
        });
    }

    _cancelObservers(){
        this._streamStateObserver.cancel();
        this._streamResultsObserver.cancel();
    }

    _lastKnownState(){
        // Get last known
        this.jsonRpc.lastKnownState().then(jsonRpcResponse => {
            this._state = JSON.parse(jsonRpcResponse.result);
        });
        this.jsonRpc.lastKnownResults().then(jsonRpcResponse => {
            if(jsonRpcResponse.result){
                this._results = jsonRpcResponse.result;
            }
        });
    }

    hotReload(){
        this._streamStateObserver.cancel();
        this._lastKnownState();
        this._createObservers();
    }

    render() {
        let results = this._prepareResultsToRender();
        return html`
            ${this._renderMenuBar(results)}
            ${this._renderResults(results)}
            ${this._renderBarChart()}
        `;
    }

    _prepareResultsToRender() {
        if(this._state && this._state.running && this._results && this._results.results) {
            let itemsByState = {
                passing: [],
                failing: [],
                skipped: [],
            }
            Object
                .values(this._results.results)
                .forEach(item => {
                    itemsByState.passing.push(...item.passing.filter( obj => obj.test === true ));
                    itemsByState.failing.push(...item.failing.filter( obj => obj.test === true ));
                    itemsByState.skipped.push(...item.skipped.filter( obj => obj.test === true ));
                });
            let items = itemsByState.failing.concat(
                itemsByState.passing,
                itemsByState.skipped
            );
            let hasTags = items.find( item => item.tags && item.tags.length > 0 );
            return {
                items: items,
                meta: {
                    hasTags: hasTags,
                    failing: itemsByState.failing.length,
                    passing: itemsByState.passing.length,
                    skipped: itemsByState.skipped.length,
                },
            }
        } else {
            return {
                items: [],
                meta: {
                    hasTags: false,
                    failing: 0,
                    passing: 0,
                    skipped: 0,
                },
            };
        }
    }

    _renderMenuBar(results){
        if(this._state){
            return html`<div class="menubar">
                <div>
                    ${this._renderStopStartButton()}
                    ${this._renderRunAllButton()}
                    ${this._renderRunFailedButton()}
                    ${this._renderToggleBrokenOnly()}
                    ${this._renderToggleDisplayTags(results)}
                </div>
                ${this._renderBusyIndicator()}
            </div>`;
        }else{
            return html`<div class="menubar">
                ${this._renderStartButton()}
            </div>`;
        }
    }

    _renderBarChart(){
        if(this._state && this._state.running && this._state.run > 0){
            let values = [this._state.passed, this._state.failed, this._state.skipped];
            return html`<echarts-horizontal-stacked-bar name = "Tests"
                            sectionTitles="${this._chartTitles.toString()}" 
                            sectionValues="${values.toString()}"
                            sectionColors="${this._chartColors.toString()}">
                        </echarts-horizontal-stacked-bar>`;
        }
    }

    _renderStateChart(){
        let values = [this._state.passed, this._state.failed, this._state.skipped];
        return html`<echarts-pie name = "Tests"
                            sectionTitles="${this._chartTitles.toString()}" 
                            sectionValues="${values.toString()}"
                            sectionColors="${this._chartColors.toString()}">
                        </echarts-pie>`;
    }

    _renderBusyIndicator(){
        if(this._state && this._state.inProgress){
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }else if(this._results && this._state && this._state.running){

             return html`<span class="total">
                         Total time: 
                         <qui-badge><span>${this._results.totalTime}ms</span></qui-badge>
                     </span>`
        }
        
    }

    _renderResults(results){
        if(results.items.length > 0){

            return html`
                <vaadin-grid .items="${results.items}" class="resultTable" theme="no-border"
                                .detailsOpenedItems="${this._detailsOpenedItem}"
                            @active-item-changed="${(event) => {
                            const prop = event.detail.value;
                            this._detailsOpenedItem = prop ? [prop] : [];
                        }}"
                        ${gridRowDetailsRenderer(this._descriptionRenderer, [])}
                >
                ${
                    this._displayTags && results.meta.hasTags 
                    ? html`<vaadin-grid-sort-column path="tags" header="Tags" ${columnBodyRenderer((prop) => this._tagsRenderer(prop), [])}></vaadin-grid-sort-column>`
                    : ''
                }
                <vaadin-grid-sort-column path="testClass" header="Test Class" ${columnBodyRenderer((prop) => this._testRenderer(prop), [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="displayName" header="Name" ${columnBodyRenderer((prop) => this._nameRenderer(prop), [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="time" header="Time" ${columnBodyRenderer((prop) => this._timeRenderer(prop), [])}>></vaadin-grid-sort-column>
            </vaadin-grid>`;

        }else{
            return html`No tests`;
        }
    }

    _descriptionRenderer(prop) {
        
        let problems = prop.problems;
        if(problems){
            return html`<div class="lineDetails">
                        ${this._renderProblems(problems)}
                    </div>`;
        }

    }

    _renderProblems(problems){
        return html`<div class="problems">
                        ${problems.map((problem) => 
                            html`${this._renderProblem(problem)}`
                        )}
                    </div>`;
    }

    _renderProblem(problem){
        if(problem){
            return html`<pre class="failed">${problem.message} ${this._renderStacktrace(problem.stackTrace)}<pre>`;
        }
    }

    _renderStacktrace(stackTraces){
        return html`${stackTraces.map((stackTrace) => 
            html`<br><qui-ide-link fileName='${stackTrace.className}'
                        lineNumber=${stackTrace.lineNumber}>${stackTrace.className}#${stackTrace.methodName}(${stackTrace.fileName}:${stackTrace.lineNumber})</qui-ide-link>`
        )}`;
    }

    _tagToColor(tag){
        // Step 0: two strings with the last char differing by 1 should render to totally different colors
        const tagValue = tag + tag;
        // Step 1: Convert the string to a numeric hash value
        let hash = 0;
        for (let i = 0; i < tagValue.length; i++) {
            hash = tagValue.charCodeAt(i) + ((hash << 5) - hash);
        }

        // Step 2: Convert the numeric hash value to a hex color code
        let color = '#';
        const normalizeFactor = 0.2; // cut 20% light and dark values
        for (let i = 0; i < 3; i++) {
            const value = Math.round(((hash >> (i * 8)) & 0xFF) * (1-2*normalizeFactor) + 255*normalizeFactor);
            color += ('00' + value.toString(16)).slice(-2);
        }

        return color;
    }

    _tagsRenderer(testLine){
        return html`${testLine.tags.map((tag, index) => {
            const color = this._tagToColor(tag);
            return html`<qui-badge small pill color="${color}" background="${color}40">
                            <span>${"io.quarkus.test.junit.QuarkusTest" === tag ? "Q" : tag}</span>
                        </qui-badge> `;
        })}`;
    }

    _testRenderer(testLine){
        let level = testLine.testExecutionResult.status.toLowerCase();

        return html`<qui-ide-link fileName='${testLine.testClass}'
                        lineNumber=0><vaadin-icon class="ideIcon" icon="font-awesome-solid:code"></vaadin-icon></qui-ide-link>
                    <span class="${level}">
                        ${testLine.testClass}
                    </span>`;
    }

    _nameRenderer(testLine){
        var dn = testLine.displayName;
        var hi = dn.lastIndexOf('#');
        var n = dn.substring(hi + 1);
        let level = testLine.testExecutionResult.status.toLowerCase();
        return html`<span class="${level}">
                        ${n}
                    </span>`;
    }

    _timeRenderer(testLine){
        return html`<span>
                        <qui-badge small><span>${testLine.time}ms</span></qui-badge>
                    </span>`;        
    }

    _detailsRenderer(prop) {
        return html`<p>${prop}</p>
        <hr/>
        <p>${this._detailsOpenedItem}</p>`;
    }


    _renderStopStartButton(){        
        if(this._state && this._state.running){
            return this._renderStopButton();
        }
        return this._renderStartButton();
    }

    _renderStartButton(){
        return html`<vaadin-button id="start-cnt-testing-btn" theme="tertiary" @click="${this._start}" ?disabled=${this._busy}>
                        <vaadin-icon icon="font-awesome-solid:play"></vaadin-icon>
                        Start
                    </vaadin-button>`;
    }

    _renderStopButton(){
        return html`<vaadin-button id="stop-cnt-testing-btn" theme="tertiary" @click="${this._stop}" ?disabled=${this._state.inProgress || this._busy}>
                        <vaadin-icon icon="font-awesome-solid:stop"></vaadin-icon>
                        Stop
                    </vaadin-button>`;
    }

    _renderRunAllButton(){
        if(this._state && this._state.running){
            return html`<vaadin-button id="run-all-cnt-testing-btn" theme="tertiary" @click="${this._runAll}" ?disabled=${this._state.inProgress || this._busy}>
                            <vaadin-icon icon="font-awesome-solid:person-running"></vaadin-icon>
                            Run all
                        </vaadin-button>`;
        }
    }

    _renderRunFailedButton(){
        if(this._state && this._state.running && this._state.failed > 0){
            return html`<vaadin-button id="run-failed-cnt-testing-btn" theme="tertiary" @click="${this._runFailed}" ?disabled=${this._state.inProgress || this._busy}>
                            <vaadin-icon class="warning" icon="font-awesome-solid:person-falling-burst" ></vaadin-icon>
                            Run failed
                        </vaadin-button>`;
        }
    }

    _renderToggleBrokenOnly(){
        if(this._state && this._state.running){
            return html`<vaadin-checkbox id="run-failed-cnt-testing-chk" theme="small"
                                    @change="${this._toggleBrokenOnly}"
                                    ?checked=${this._state.isBrokenOnly}
                                    ?disabled=${this._state.inProgress || this._busy}
                                    label="Only run failing tests">
                        </vaadin-checkbox>`;
        }
    }

    _renderToggleDisplayTags(results) {
        if(this._state && this._state.running){
            return html`<vaadin-checkbox id="display-tags-cnt-testing-chk" theme="small"
                                         @change="${this._toggleDisplayTags}"
                                         ?checked=${this._displayTags}
                                         ?disabled=${this._state.inProgress || this._busy || !results.meta.hasTags}
                                         label="Display tags (if available)">
            </vaadin-checkbox>`;
        }
    }

    _start(){
        if(!this._busy){
            this._busy = true;
            this.jsonRpc.start().then(jsonRpcResponse => {
                this._busy = false;
            });
        }
    }

    _stop(){
        if(!this._busy){
            this._busy = true;
            this.jsonRpc.stop().then(jsonRpcResponse => {
                this._busy = false;
            });
        }
    }

    _runAll(){
        this._busy = true;
        this.jsonRpc.runAll().then(jsonRpcResponse => {
            this._busy = false;
        });
    }

    _runFailed(){
        this._busy = true;
        this.jsonRpc.runFailed().then(jsonRpcResponse => {
            this._busy = false;
        });
    }

    _toggleBrokenOnly(){
        this._busy = true;
        this.jsonRpc.toggleBrokenOnly().then(jsonRpcResponse => {
            this._busy = false;
        });
    }

    _toggleDisplayTags(){
        this._displayTags = !this._displayTags;
    }
}
customElements.define('qwc-continuous-testing', QwcContinuousTesting);