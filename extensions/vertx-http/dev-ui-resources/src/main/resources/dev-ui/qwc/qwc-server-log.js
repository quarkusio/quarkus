import { QwcAbstractLogElement, html, css} from 'qwc-abstract-log-element';
import { repeat } from 'lit/directives/repeat.js';
import { LogController } from 'log-controller';
import { JsonRpc } from 'jsonrpc';
import { unsafeHTML } from 'lit-html/directives/unsafe-html.js';
import { loggerLevels } from 'devui-data';
import '@vaadin/icon';
import '@vaadin/dialog';
import '@vaadin/select';
import '@vaadin/checkbox';
import '@vaadin/checkbox-group';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';
import '@qomponent/qui-badge';
import 'qui-ide-link';

/**
 * This component represent the Server Log
 */
export class QwcServerLog extends QwcAbstractLogElement {
    
    logControl = new LogController(this);
    jsonRpc = new JsonRpc("devui-logstream", false);
    space = "&nbsp;";

    static styles = css`
        .log {
            width: 100%;
            height: 100%;
            max-height: 100%;
            display: flex;
            flex-direction:column;
        }
        
        a, a:link, a:visited, a:hover, a:active{
            color: var(--lumo-primary-color);
        }
    
        .line {
            margin-top: 10px;
            margin-bottom: 10px;
            border-top: 1px dashed var(--lumo-primary-color);
            color: transparent;
        }
    
        .text-warn {
            color: var(--lumo-warning-color-50pct);
        }
        .text-error{
            color: var(--lumo-error-text-color);
        }
        .text-info{
            
        }
        .icon-info {
            color: var(--lumo-primary-text-color);
        }
    
        .text-debug{
            color: var(--lumo-success-text-color);
        }
        .text-normal{
            color: var(--lumo-success-color-50pct);
        }
        .text-logger{
            color: var(--lumo-primary-color-50pct);
        }
        .text-source{
            color: var(--lumo-primary-text-color);
        }
        .text-file {
            color: var(--lumo-contrast-40pct);
        }
        .text-process{
            color: var(--lumo-primary-text-color);
        }
        .text-thread{
            color: var(--lumo-success-color-50pct);
        }
    `;

    static properties = {
        _messages: {state:true},
        _zoom: {state:true},
        _increment: {state: false},
        _followLog: {state: false},
        _observer: {state:false},
        _levelsDialogOpened: {state: true},
        _columnsDialogOpened: {state: true},
        _selectedColumns: {state: true},
        _allLoggers: {state: true, type: Array},
        _filteredLoggers: {state: true, type: Array},
        _loggerLevels: {state: false, type: Array}
    };

    constructor() {
        super();
        this._loggerLevels = [];
        for (let i in loggerLevels) {
            let loggerLevel = loggerLevels[i];
            this._loggerLevels.push({
                'label': loggerLevel,
                'value': loggerLevel,
            });
        }

        this.logControl
                .addToggle("On/off switch", true, (e) => {
                    this._toggleOnOffClicked(e);
                }).addItem("Log levels", "font-awesome-solid:layer-group", "var(--lumo-tertiary-text-color)", (e) => {
                    this._logLevels();
                }).addItem("Columns", "font-awesome-solid:table-columns", "var(--lumo-tertiary-text-color)", (e) => {
                    this._columns();
                }).addItem("Zoom out", "font-awesome-solid:magnifying-glass-minus", "var(--lumo-tertiary-text-color)", (e) => {
                    this._zoomOut();
                }).addItem("Zoom in", "font-awesome-solid:magnifying-glass-plus", "var(--lumo-tertiary-text-color)", (e) => {
                    this._zoomIn();
                }).addItem("Clear", "font-awesome-solid:trash-can", "var(--lumo-error-color)", (e) => {
                    this._clearLog();
                }).addFollow("Follow log", true , (e) => {
                    this._toggleFollowLog(e);
                }).done();
                
        this._messages = [];
        this._zoom = parseFloat(1.0);
        this._increment = parseFloat(0.05);
        this._followLog = true;
        this._levelsDialogOpened = false;
        this._columnsDialogOpened = false;
        this._selectedColumns = ['0','3','4','5','10','18','19'];
    }

    connectedCallback() {
        super.connectedCallback();
        this._toggleOnOff(true);
        this._history();
        this._loadAllLoggers();
    }
    
    disconnectedCallback() {
        this._toggleOnOff(false);
        super.disconnectedCallback();
    }

    _loadAllLoggers(){
        this.jsonRpc.getLoggers().then(jsonRpcResponse => {
            this._allLoggers = jsonRpcResponse.result;
            this._filteredLoggers = this._allLoggers;
        });
    }

    render() {
        if(this._filteredLoggers){
            return html`
                <vaadin-dialog class="levelsDialog"
                    header-title="Log levels (${this._filteredLoggers.length})"
                    resizable
                    draggable
                    .opened="${this._levelsDialogOpened}"
                    @opened-changed="${(e) => (this._levelsDialogOpened = e.detail.value)}"
                    ${dialogHeaderRenderer(() => html`
                        <vaadin-button theme="tertiary" @click="${() => (this._levelsDialogOpened = false)}">
                            <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                        </vaadin-button>
                    `,
                    []
                    )}
                    ${dialogRenderer(() => this._renderLevelsDialog(), "levels")}
                ></vaadin-dialog>
                <vaadin-dialog class="columnsDialog"
                    header-title="Columns"
                    resizable
                    draggable
                    .opened="${this._columnsDialogOpened}"
                    @opened-changed="${(e) => (this._columnsDialogOpened = e.detail.value)}"
                    ${dialogHeaderRenderer(() => html`
                        <vaadin-button theme="tertiary" @click="${() => (this._columnsDialogOpened = false)}">
                            <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                        </vaadin-button>
                    `,
                    []
                    )}
                    ${dialogRenderer(() => this._renderColumnsDialog(), "columns")}
                ></vaadin-dialog>
                <code class="log" style="font-size: ${this._zoom}em;">
                ${repeat(
                    this._messages,
                    (message) => message.sequenceNumber,
                    (message, index) => html`
                    <div class="logEntry">
                        ${this._renderLogEntry(message)}
                    </div>
                  `
                  )}
                </code>`;
        }
    }

    _renderLogEntry(message){
        if(message.type === "line"){
            return html`<hr class="line"/>`;
        }else if(message.type === "blank"){
            return html`<br/>`;
        }else if(message.type === "help"){
            return html`<br/>
                            The following commands are available:<br/>
                            <br/>
                            == Continuous Testing<br/>
                            <br/>
                            [r] - Resume testing / Re-run all tests<br/>
                            [f] - Re-run failed tests<br/>
                            [b] - Toggle 'broken only' mode, where only failing tests are run<br/>
                            [v] - Print failures from the last test run<br/>
                            [p] - Pause tests<br/>
                            [o] - Toggle test output<br/>
                            <br/>
                            == System<br/>
                            <br/>
                            [s] - Force restart<br/>
                            [i] - Toggle instrumentation based reload <br/>
                            [l] - Toggle live reload<br/>
                            [h] - Show this help<br/>
                            `;
        }else{
            var level = message.level.toUpperCase();
            if (level === "WARNING" || level === "WARN"){
                level = "warn";
            }else if (level === "SEVERE" || level === "ERROR"){
                level = "error";
            }else if (level === "INFO"){
                level = "info";
            }else if (level === "DEBUG"){
                level = "debug";
            }else {
                level = "normal";
            }
            
            return html`
                ${this._renderLevelIcon(level)}
                ${this._renderSequenceNumber(message.sequenceNumber)}
                ${this._renderHostName(message.hostName)}
                ${this._renderDate(message.timestamp)}
                ${this._renderTime(message.timestamp)}
                ${this._renderLevel(level, message.level)}
                ${this._renderLoggerNameShort(message.loggerNameShort)}
                ${this._renderLoggerName(message.loggerName)}
                ${this._renderLoggerClassName(message.loggerClassName)}
                ${this._renderSourceClassNameFull(message.sourceClassNameFull, message.sourceLineNumber)}
                ${this._renderSourceClassNameFullShort(message.sourceClassNameFullShort, message.sourceClassNameFull, message.sourceLineNumber)}
                ${this._renderSourceClassName(message.sourceClassName, message.sourceClassNameFull, message.sourceLineNumber)}
                ${this._renderSourceMethodName(message.sourceMethodName)}
                ${this._renderSourceFileName(message.sourceFileName, message.sourceClassNameFull, message.sourceLineNumber)}
                ${this._renderSourceLineNumber(message.sourceLineNumber)}
                ${this._renderProcessId(message.processId)}
                ${this._renderProcessName(message.processName)}
                ${this._renderThreadId(message.threadId)}
                ${this._renderThreadName(message.threadName)}
                ${this._renderMessage(level, message.formattedMessage, message.stacktrace, message.decoration)}
            `;
        }
    }
    
    _renderLevelIcon(level){
        if(this._selectedColumns.includes('0')){
            if (level === "warn"){
                return html`<vaadin-icon icon="font-awesome-solid:circle-exclamation" class="text-warn"></vaadin-icon>`;
            }else if (level === "error"){
                return html`<vaadin-icon icon="font-awesome-solid:radiation" class="text-error"></vaadin-icon>`;
            }else if (level === "info"){
                return html`<vaadin-icon icon="font-awesome-solid:circle-info" class="icon-info"></vaadin-icon>`;
            }else if (level === "debug"){
                return html`<vaadin-icon icon="font-awesome-solid:bug" class="text-debug"></vaadin-icon>`;
            }else {
                return html`<vaadin-icon icon="font-awesome-solid:circle" class="text-normal"></vaadin-icon>`;
            }
        }
    }
    
    _renderSequenceNumber(sequenceNumber){
        if(this._selectedColumns.includes('1')){
            return html`<qui-badge small><span title='Sequence number'>${sequenceNumber}</span></qui-badge>`;
        }
    }
    
    _renderHostName(hostName){
        if(this._selectedColumns.includes('2')){
            return html`<span title='Host name'>${hostName}</span>`;
        }
    }
    
    _renderDate(timestamp){
        if(this._selectedColumns.includes('3')){
            return html`<span title='Date'>${timestamp.slice(0, 10)}</span>`;
        }
    }
    
    _renderTime(timestamp){
        if(this._selectedColumns.includes('4')){
            return html`<span title='Time'>${timestamp.slice(11, 23).replace(".", ",")}</span>`;
        }
    }
    
    _renderLevel(level, leveldisplay){
        if(this._selectedColumns.includes('5')){
            return html`<span title='Level' class='text-${level}'>${leveldisplay}</span>`;
        }
    }
    
    _renderLoggerNameShort(loggerNameShort){
        if(this._selectedColumns.includes('6')){
            return html`<span title='Logger name (short)' class='text-logger'>[${loggerNameShort}]</span>`;
        }
    }
    
    _renderLoggerName(loggerName){
        if(this._selectedColumns.includes('7')){
            return html`<span title='Logger name' class='text-logger'>[${loggerName}]</span>`;
        }
    }
    
    _renderLoggerClassName(loggerClassName){
        if(this._selectedColumns.includes('8')){
            return html`<span title='Logger class name' class='text-logger'>[${loggerClassName}]</span>`;
        }
    }
    
    _renderSourceClassNameFull(sourceClassNameFull, sourceLineNumber){
        if(this._selectedColumns.includes('9')){
            return html`<qui-ide-link title='Source full class name' 
                        class='text-source'
                        fileName='${sourceClassNameFull}'
                        lineNumber=${sourceLineNumber}>[${sourceClassNameFull}]</qui-ide-link>`;
        }
    }
    
    _renderSourceClassNameFullShort(sourceClassNameFullShort, sourceClassNameFull, sourceLineNumber){
        if(this._selectedColumns.includes('10')){
            return html`<qui-ide-link title='Source full class name (short)' 
                        class='text-source'
                        fileName='${sourceClassNameFull}'
                        lineNumber=${sourceLineNumber}>[${sourceClassNameFullShort}]</qui-ide-link>`;
        }
    }
    
    _renderSourceClassName(sourceClassName, sourceClassNameFull, sourceLineNumber){
        if(this._selectedColumns.includes('11')){
            return html`<qui-ide-link title='Source class name' 
                        class='text-source'
                        fileName='${sourceClassNameFull}'
                        lineNumber=${sourceLineNumber}>[${sourceClassName}]</qui-ide-link>`;
        }
    }
    
    _renderSourceMethodName(sourceMethodName){
        if(this._selectedColumns.includes('12')){
            return html`<span title='Source method name' class='text-source'>${sourceMethodName}</span>`;
        }
    }
    
    _renderSourceFileName(sourceFileName, sourceClassNameFull, sourceLineNumber){
        if(this._selectedColumns.includes('13')){
            return html`<qui-ide-link title='Source file name' 
                        class='text-file'
                        fileName='${sourceClassNameFull}'
                        lineNumber=${sourceLineNumber}>${sourceFileName}</qui-ide-link>`;
        }
    }
    
    _renderSourceLineNumber(sourceLineNumber){
        if(this._selectedColumns.includes('14')){
            return html`<span title='Source line number' class='text-source'>(line:${sourceLineNumber})</span>`;
        }
    }
    
    _renderProcessId(processId){
        if(this._selectedColumns.includes('15')){
            return html`<span title='Process Id' class='text-process'>(${processId})</span>`;
        }
    }
    
    _renderProcessName(processName){
        if(this._selectedColumns.includes('16')){
            return html`<span title='Process name' class='text-process'>(${processName})</span>`;
        }
    }
    
    _renderThreadId(threadId){
        if(this._selectedColumns.includes('17')){
            return html`<span title='Thread Id' class='text-thread'>(${threadId})</span>`;
        }
    }
    
    _renderThreadName(threadName){
        if(this._selectedColumns.includes('18')){
            return html`<span title='Thread name' class='text-thread'>(${threadName})</span>`;
        }
    }
    
    _renderMessage(level, message, stacktrace, decoration){
        if(this._selectedColumns.includes('19')){
            // Clean up Ansi
            message = message.replace(/\u001b\[.*?m/g, "");
            
            // Make links clickable
            if(message.includes("http://")){
                message = this._makeLink(message, "http://");
            }
            if(message.includes("https://")){
                message = this._makeLink(message, "https://");
            }
            
            // Make sure multi line is supported
            message = this._makeMultiLine(message);
        
            if(message){
                return html`<span title="Message" class='text-${level}'>${unsafeHTML(message)}${this._renderDecoration(decoration)}${this._renderStackTrace(stacktrace)}</span>`;
            }   
        }
    }
    
    _renderDecoration(decoration){
        if(decoration){
            decoration = this._makeMultiLine("\n" + decoration + "\n");
            return html`${unsafeHTML(decoration)}`;
        }
    }
    
    _makeMultiLine(message){
        if(message.includes('\n')){
            var htmlifiedLines = [];
            var lines = message.split('\n');
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                line = line.replace(/ /g, '\u00a0');
                if(i === lines.length-1){
                    htmlifiedLines.push(line);
                }else{
                    htmlifiedLines.push(line + '<br/>');
                }
            }
            message = htmlifiedLines.join('');
        }
        return message;
    }
    
    _renderStackTrace(stacktrace){
        if(stacktrace){
            var htmlifiedLines = [];
            for (let i = 0; i < stacktrace.length; i++) {
                let st = stacktrace[i];
                
                if(st.includes('\n')){
                    var lines = st.split('\n');
                    for (var j = 0; j < lines.length; j++) {
                        let line = lines[j].trim();
                        
                        var startWithAt = line.startsWith("at ");
                        line = "<qui-ide-link stackTraceLine='" + line + "'>" + line + "</qui-ide-link>";
                        if(startWithAt){
                            line = this.space + this.space + this.space + this.space + this.space + this.space + line;
                        }
                        htmlifiedLines.push(line + '<br/>');
                    }
                }else{
                    htmlifiedLines.push(st + '<br/>');
                }
            }
            let trace = htmlifiedLines.join(''); 
            return html`: ${unsafeHTML(trace)}`;
        }
    }

    _renderLevelsDialog(){
        if(this._filteredLoggers){
            return html`<vaadin-vertical-layout
                            theme="spacing"
                            style="width: 600px; max-width: 100%; min-width: 300px; height: 100%; align-items: stretch;">

                <vaadin-text-field
                        placeholder="Filter"
                        style="width: 100%;"
                        @value-changed="${(e) => this._filterLoggers(e)}">
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-grid .items="${this._filteredLoggers}" style="width: 100%;" theme="row-stripes">
                    <vaadin-grid-sort-column resizable
                                        header="Name"
                                        path="name">
                    </vaadin-grid-sort-column>

                    <vaadin-grid-column auto-width resizable flex-grow="0"
                                            class="cell"
                                            header="Level"
                                            ${columnBodyRenderer(this._logLevelRenderer, [])}>

                </vaadin-grid>
            </vaadin-vertical-layout>
            `;
        }
    }
    
    _filterLoggers(e) {
        const searchTerm = (e.detail.value || '').trim();
        if (searchTerm === '') {
          this._filteredLoggers = this._allLoggers;
          return;
        }
    
        this._filteredLoggers = this._allLoggers.filter((level) => {    
            let i = this._matchLogger(level.name, searchTerm); 
            return i;
        });
    }

    _matchLogger(value, term) {
        if (!value) {
          return false;
        }
        return value.toLowerCase().includes(term.toLowerCase());
    }

    _logLevelRenderer(logger){
        return html`${this._renderSelect(logger.name, logger.effectiveLevel)}`;
    }

    _renderSelect(loggerName, loggerLevel){
        return html`<vaadin-select class="input-column" 
                            id="${loggerName}" 
                            theme="small" 
                            .items="${this._loggerLevels}" 
                            .value="${loggerLevel}"
                            @change="${this._logLevelSelectChanged}">
                </vaadin-select>`;
    }

    _logLevelSelectChanged(event){
        let name = event.target.id;
        this._updateLogLevel(name, event.target.value);
    }

    _updateLogLevel(name, value){
        this.jsonRpc.updateLogLevel({
            'loggerName': name,
            'levelValue': value
        });
    }

    _renderColumnsDialog(){
        return html`<vaadin-checkbox-group
                            .value="${this._selectedColumns}"
                            @value-changed="${(e) => (this._selectedColumns = e.detail.value)}"
                            theme="vertical">
                        <vaadin-checkbox value="0" label="Level icon"></vaadin-checkbox>
                        <vaadin-checkbox value="1" label=Sequence number"></vaadin-checkbox>
                        <vaadin-checkbox value="2" label="Host name"></vaadin-checkbox>
                        <vaadin-checkbox value="3" label="Date"></vaadin-checkbox>
                        <vaadin-checkbox value="4" label="Time"></vaadin-checkbox>
                        <vaadin-checkbox value="5" label="Level"></vaadin-checkbox>
                        <vaadin-checkbox value="6" label="Logger name (short)"></vaadin-checkbox>
                        <vaadin-checkbox value="7" label="Logger name"></vaadin-checkbox>
                        <vaadin-checkbox value="8" label="Logger class name"></vaadin-checkbox>
                        <vaadin-checkbox value="9" label="Source full class name"></vaadin-checkbox>
                        <vaadin-checkbox value="10" label="Source full class name (short)"></vaadin-checkbox>
                        <vaadin-checkbox value="11" label="Source class name"></vaadin-checkbox>
                        <vaadin-checkbox value="12" label="Source method name"></vaadin-checkbox>
                        <vaadin-checkbox value="13" label="Source file name"></vaadin-checkbox>
                        <vaadin-checkbox value="14" label="Source line number"></vaadin-checkbox>
                        <vaadin-checkbox value="15" label="Process id"></vaadin-checkbox>
                        <vaadin-checkbox value="16" label="Process name"></vaadin-checkbox>
                        <vaadin-checkbox value="17" label="Thread id"></vaadin-checkbox>
                        <vaadin-checkbox value="18" label="Thread name"></vaadin-checkbox>
                        <vaadin-checkbox value="19" label="Message"></vaadin-checkbox>
                    </vaadin-checkbox-group>`;
    }
    
    _makeLink(message, protocol){
        var url = message.substring(message.indexOf(protocol));
        if(url.includes(" ")){
            url = url.substr(0,url.indexOf(' '));
        }
        var link = "<a href='" + url + "' class='text-primary' target='_blank'>" + url + "</a>";

        return message.replace(url, link);    
    }
    
    _toggleOnOffClicked(e){
        this._toggleOnOff(e);
        // Add line on stop
        if(!e){
            var stopEntry = new Object();
            stopEntry.id = Math.floor(Math.random() * 999999);
            stopEntry.type = "line";
            this._addLogEntry(stopEntry);
        }
    }
    
    _toggleOnOff(e){
        if(e){
            this._observer = this.jsonRpc.streamLog().onNext(jsonRpcResponse => {
                this._addLogEntry(jsonRpcResponse.result);
            });
        }else{
            this._observer.cancel();
            this._observer = null;
        }
    }
    
    hotReload(){
        // Stop then start / start then stop
        this._stopStartLog();
        this._stopStartLog();
        this._history();
        this._loadAllLoggers();
    }
    
    _history(){
        this.jsonRpc.history().then(jsonRpcResponse => {
            jsonRpcResponse.result.forEach(entry => {
                this._addLogEntry(entry);
            });
        });
    }
    
    _toggleFollowLog(e){
        this._followLog = e;
        this._scrollToBottom();   
    }
    
    _addLogEntry(entry){
        if(this.doLogEntry(entry)){
            this._messages = [
                ...this._messages,
                entry
            ];
        
            this._scrollToBottom();
        }
    }
    
    doLogEntry(entry){
        return true;
    }
    
    async _scrollToBottom(){
        if(this._followLog){
            await this.updateComplete;
            
            const last = Array.from(
                this.shadowRoot.querySelectorAll('.logEntry')
            ).pop();
            
            if(last){
                last.scrollIntoView({
                    behavior: "smooth",
                    block: "end"
                });
            }
        }
    }
    
    _clearLog(){
        this._messages = [];
    }
    
    _zoomOut(){
        this._zoom = parseFloat(parseFloat(this._zoom) - parseFloat(this._increment)).toFixed(2);
    }
    
    _zoomIn(){
        this._zoom = parseFloat(parseFloat(this._zoom) + parseFloat(this._increment)).toFixed(2);
    }
    
    _logLevels(){
        this._levelsDialogOpened = true;
    }
    
    _columns(){
        this._columnsDialogOpened = true;
    }
    
    _handleZoomIn(event){
        this._zoomIn();
    }
    
    _handleZoomOut(event){
        this._zoomOut();
    }
    
    _handleKeyPress(event) {
        if (event.key === 'Enter') {
            this._keyPressEnter();
        } else if (event.ctrlKey && event.key === 'c') {
            this._stopStartLog();
        } else if (event.key === 's') {
            this.jsonRpc.forceRestart();
        } else if (event.key === 'r') {
            this.jsonRpc.rerunAllTests();
        } else if (event.key === 'f') {
            this.jsonRpc.rerunFailedTests();
        } else if (event.key === 'b') {
            this.jsonRpc.toggleBrokenOnly();
        } else if (event.key === 'v') {
            this.jsonRpc.printFailures();
        } else if (event.key === 'o') {
            this.jsonRpc.toggleTestOutput();
        } else if (event.key === 'i') {
            this.jsonRpc.toggleInstrumentationReload();
        } else if (event.key === 'p') {
            this.jsonRpc.pauseTests();
        } else if (event.key === 'l') {
            this.jsonRpc.toggleLiveReload();
        } else if (event.key === 'h'){
            this._printHelp();
        }
    }
    
    _keyPressEnter(){
        // Create a blank line in the console.
        var blankEntry = new Object();
        blankEntry.id = Math.floor(Math.random() * 999999);
        blankEntry.type = "blank";
        this._addLogEntry(blankEntry);
    }
    
    _printHelp(){
        var helpEntry = new Object();
        helpEntry.id = Math.floor(Math.random() * 999999);
        helpEntry.type = "help";
        this._addLogEntry(helpEntry);
    }
    
    
    _stopStartLog(){
        if(this._observer){
            // stop
            this._toggleOnOffClicked(false);
        }else{
            // start
            this._toggleOnOffClicked(true);
        }
    }
    
}

customElements.define('qwc-server-log', QwcServerLog);
