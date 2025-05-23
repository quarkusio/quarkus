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
import 'qwc-no-data';
import 'echarts-horizontal-stacked-bar';
import {ring} from 'ldrs';

ring.register();

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

    set _tests(value) {
        this._state = value;
        this._state?.result?.passed?.forEach(item => item.style = "successful");
        this._state?.result?.failed?.forEach(item => item.style = "failed");
        this._state?.result?.skipped?.forEach(item => item.style = "aborted");
    }

    get _tests() {
        return this._state;
    }

    get _testsReceived() {
        return this._tests != null;
    }

    get _testsEnabled() {
        return this._tests?.config?.enabled ?? false;
    }

    get _testsCurrentlyRunning() {
        return this._tests?.inProgress ?? false;
    }

    get _testResult() {
        return this._tests?.result;
    }

    get _hasTestResult() {
        return (this._testResult?.counts?.total ?? 0) > 0
    }

    get _hasFailedTestResult() {
        return (this._testResult?.counts?.failed ?? 0) > 0;
    }

    get _hasTestResultWithTags() {
        return (this._testResult?.tags?.length ?? 0) > 0;
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('continuous-testing-start-stop', this._onStartStopChanged);
        this._lastKnownState();
        this._createObservers();
    }
    
    disconnectedCallback() {
        window.removeEventListener('continuous-testing-start-stop', this._onStartStopChanged);
        this._cancelObservers();    
        super.disconnectedCallback();
    }

    _onStartStopChanged = (event) => {
        if(this._testsEnabled){
            this._stop();
        }else {
            this._start();
        }
    }

    _createObservers(){
        this._streamStateObserver = this.jsonRpc.streamState().onNext(jsonRpcResponse => {
            this._tests = jsonRpcResponse.result;
            this._broadcastState();
        });
    }

    _broadcastState(){
        if(this._tests.inProgress){
            this._fireStateChange("busy");
        }else if(!this._tests.config.enabled){
            this._fireStateChange("stopped");
        }else if(this._tests.config.enabled){
            this._fireStateChange("started");
        }
    }

    _cancelObservers(){
        this._streamStateObserver.cancel();
        if(this._streamResultsObserver){
            this._streamResultsObserver.cancel();
        }
    }

    _lastKnownState(){
        // Get last known
        this.jsonRpc.currentState().then(jsonRpcResponse => {
            this._tests = jsonRpcResponse.result;
            this._broadcastState();
        });
    }

    hotReload(){
        this._streamStateObserver.cancel();
        this._lastKnownState();
        this._createObservers();
    }

    render() {
        return html`
            ${this._renderMenuBar()}
            ${this._renderResults()}
            ${this._renderBarChart()}
        `;
    }

    _renderMenuBar(){
        let items = [];
        if(this._testsEnabled) {
            items.push(... (this._testResult?.failed ?? []));
            items.push(... (this._testResult?.passed ?? []));
            items.push(... (this._testResult?.skipped ?? []));
        }
        if(items.length > 0){
            return html`
            <div class="menubar">
                <div>
                    ${this._renderRunAllButton()}
                    ${this._renderRunFailedButton()}
                    ${this._renderToggleBrokenOnly()}
                    ${this._renderToggleDisplayTags()}
                </div>
                ${this._renderBusyIndicator()}
            </div>`;
        }
    }

    _renderBarChart(){
        if(this._testsEnabled && this._hasTestResult){
            let values = [
                this._testResult.counts?.passed ?? 0,
                this._testResult.counts?.failed ?? 0,
                this._testResult.counts?.skipped ?? 0
            ];
            return html`<echarts-horizontal-stacked-bar name = "Tests"
                            sectionTitles="${this._chartTitles.toString()}" 
                            sectionValues="${values.toString()}"
                            sectionColors="${this._chartColors.toString()}">
                        </echarts-horizontal-stacked-bar>`;
        }
    }

    _renderBusyIndicator(){
        if(this._testsCurrentlyRunning){
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }else if(this._testsEnabled && this._testResult?.totalTime){

             return html`<span class="total">
                         Total time: 
                         <qui-badge><span>${this._testResult?.totalTime}ms</span></qui-badge>
                     </span>`
        }
        
    }

    _renderResults(){
        let items = [];
        if(this._testsEnabled) {
            items.push(... (this._testResult?.failed ?? []));
            items.push(... (this._testResult?.passed ?? []));
            items.push(... (this._testResult?.skipped ?? []));
        }
        if(items.length > 0){

            return html`
                <vaadin-grid .items="${items}" class="resultTable" theme="no-border"
                                .detailsOpenedItems="${this._detailsOpenedItem}"
                            @active-item-changed="${(event) => {
                            const prop = event.detail.value;
                            this._detailsOpenedItem = prop ? [prop] : [];
                        }}"
                        ${gridRowDetailsRenderer(this._descriptionRenderer, [])}
                >
                ${
                    this._displayTags && this._hasTestResultWithTags
                    ? html`<vaadin-grid-sort-column path="tags" header="Tags" ${columnBodyRenderer((prop) => this._tagsRenderer(prop), [])}></vaadin-grid-sort-column>`
                    : ''
                }
                <vaadin-grid-sort-column path="testClass" header="Test Class" ${columnBodyRenderer((prop) => this._testRenderer(prop), [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="displayName" header="Name" ${columnBodyRenderer((prop) => this._nameRenderer(prop), [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="time" header="Time" ${columnBodyRenderer((prop) => this._timeRenderer(prop), [])}>></vaadin-grid-sort-column>
            </vaadin-grid>`;

        }else{
            return html`<qwc-no-data message="Continuous Testing is not running. Click the Start button or press [r] in the console to start." 
                                    link="https://quarkus.io/guides/continuous-testing"
                                    linkText="Read more about Continuous Testing">
                            ${this._renderPlayButton()}
                </qwc-no-data>
            `
        }
    }

    _renderPlayButton(){
        if(!this._busy && !this._testsEnabled){
            return html`<vaadin-button theme="tertiary" id="start-continuous-testing-btn" @click=${this._start}>
                        <vaadin-icon icon="font-awesome-solid:play"></vaadin-icon>
                        Start
                    </vaadin-button>`;
        }else{
            return html`<l-ring size="26" stroke="2" color="var(--lumo-contrast-25pct)" class="ring"></l-ring>`;
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
                        lineNumber='${stackTrace.lineNumber}'>${stackTrace.className}#${stackTrace.methodName}(${stackTrace.fileName}:${stackTrace.lineNumber})</qui-ide-link>`
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
        let level = testLine.style ?? '';
        return html`<qui-ide-link fileName='${testLine.className}'>
                        <vaadin-icon class="ideIcon" icon="font-awesome-solid:code"></vaadin-icon>
                    </qui-ide-link>
                    <span class="${level}">
                        ${testLine.className}
                    </span>`;
    }

    _nameRenderer(testLine){
        var dn = testLine.displayName;
        var hi = dn.lastIndexOf('#');
        var n = dn.substring(hi + 1);
        let level = testLine.style ?? '';
        return html`<span class="${level}">
                        ${n}
                    </span>`;
    }

    _timeRenderer(testLine){
        return html`<span>
                        <qui-badge small><span>${testLine.time}ms</span></qui-badge>
                    </span>`;        
    }

    _renderRunAllButton(){
        if(this._testsEnabled){
            return html`<vaadin-button id="run-all-cnt-testing-btn" theme="tertiary" @click="${this._runAll}" ?disabled=${this._state.inProgress || this._busy}>
                            <vaadin-icon icon="font-awesome-solid:person-running"></vaadin-icon>
                            Run all
                        </vaadin-button>`;
        }
    }

    _renderRunFailedButton(){
        if(this._testsEnabled && this._hasFailedTestResult){
            return html`<vaadin-button id="run-failed-cnt-testing-btn" theme="tertiary" @click="${this._runFailed}" ?disabled=${this._state.inProgress || this._busy}>
                            <vaadin-icon class="warning" icon="font-awesome-solid:person-falling-burst" ></vaadin-icon>
                            Run failed
                        </vaadin-button>`;
        }
    }

    _renderToggleBrokenOnly(){
        if(this._testsEnabled){
            return html`<vaadin-checkbox id="run-failed-cnt-testing-chk" theme="small"
                                    @change="${this._toggleBrokenOnly}"
                                    ?checked=${this._tests.config.brokenOnly}
                                    ?disabled=${this._testsCurrentlyRunning || this._busy}
                                    label="Only run failing tests">
                        </vaadin-checkbox>`;
        }
    }

    _renderToggleDisplayTags() {
        if(this._testsEnabled){
            return html`<vaadin-checkbox id="display-tags-cnt-testing-chk" theme="small"
                                         @change="${this._toggleDisplayTags}"
                                         ?checked=${this._displayTags}
                                         ?disabled=${this._busy || !this._hasTestResultWithTags}
                                         label="Display tags (if available)">
            </vaadin-checkbox>`;
        }
    }

    _start(){
        if(!this._busy){
            this._busy = true;
            this._fireStateChange("busy");
            this.jsonRpc.start().then(jsonRpcResponse => {
                this._busy = false;
            });
        }
    }

    _stop(){
        if(!this._busy){
            this._busy = true;
            this._fireStateChange("busy");
            this.jsonRpc.stop().then(jsonRpcResponse => {
                this._busy = false;
                this._fireStateChange("stopped");
            });
        }
    }

    _fireStateChange(state){
        this.dispatchEvent(new CustomEvent('continuous-testing-state-change', {
            detail: { state: state },
            bubbles: true,
            composed: true
        }));
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
