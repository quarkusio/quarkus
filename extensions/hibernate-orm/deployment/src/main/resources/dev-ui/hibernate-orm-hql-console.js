import {css, html, QwcHotReloadElement} from 'qwc-hot-reload-element';
import {JsonRpc} from 'jsonrpc';
import {StorageController} from 'storage-controller';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/combo-box';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/progress-bar';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import {assistantState} from 'assistant-state';
import 'qui-assistant-warning';
import {observeState} from 'lit-element-state';

export class HibernateOrmHqlConsoleComponent extends observeState(QwcHotReloadElement) {
    jsonRpc = new JsonRpc(this);
    configJsonRpc = new JsonRpc("devui-configuration");
    storageControl = new StorageController(this);

    static styles = css`
        .bordered {
            border: 1px solid var(--lumo-contrast-20pct);
            border-radius: var(--lumo-border-radius-l);
            padding: var(--lumo-space-s) var(--lumo-space-m);
        }

        .dataSources {
            display: flex;
            flex-direction: column;
            gap: 20px;
            height: 100%;
            padding-left: 10px;
        }

        .chat-container {
            display: flex;
            height: 100%;
            flex-direction: column;
            padding-right: 20px;
        }

        .selector-section {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-bottom: 15px;
        }

        .selector-row {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            align-items: flex-end;
        }

        .pu-selector, .entity-selector {
            min-width: 200px;
        }

        .entity-suggestions {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }

        .suggestion-chips {
            display: flex;
            flex-wrap: wrap;
            gap: 5px;
            margin-bottom: 5px;
        }

        .chat-area {
            display: flex;
            flex-direction: column;
            flex: 1;
            overflow-y: auto;
            gap: 10px;
            padding: 15px;
            background-color: var(--lumo-contrast-5pct);
            border-radius: var(--lumo-border-radius-l);
            margin-bottom: 10px;
        }

        .message {
            max-width: 80%;
            padding: 10px 15px;
            border-radius: var(--lumo-border-radius-m);
            margin-bottom: 10px;
        }

        .user-message {
            align-self: flex-end;
            background-color: var(--lumo-primary-color-10pct);
            color: var(--lumo-body-text-color);
        }

        .system-message {
            align-self: flex-start;
            background-color: var(--lumo-contrast-10pct);
            color: var(--lumo-body-text-color);
        }

        .error-message {
            align-self: flex-start;
            background-color: var(--lumo-error-color-10pct);
            color: var(--lumo-error-text-color);
        }

        .chat-input {
            display: flex;
            gap: 10px;
            align-items: center;
        }

        .chat-text-area {
            flex: 1;
        }

        .results-card {
            background-color: var(--lumo-base-color);
            padding: 10px;
            border-radius: var(--lumo-border-radius-m);
            margin-top: 5px;
            border: 1px solid var(--lumo-contrast-20pct);
            display: inline-block;
        }

        .pagination {
            display: flex;
            justify-content: center;
            align-items: center;
            padding-top: 10px;
            font-size: var(--lumo-font-size-s);
        }

        .result-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 5px;
            padding: 5px 5px 0 5px;
            font-size: var(--lumo-font-size-s);
            border-top: 1px solid var(--lumo-contrast-10pct);
        }

        .result-count {
            color: var(--lumo-secondary-text-color);
        }

        .copy-button {
            display: flex;
            align-items: center;
            gap: 4px;
        }
        
        .small-icon {
            height: var(--lumo-icon-size-s);
            width: var(--lumo-icon-size-s);
        }

        .spinner {
            animation: spin 1s linear infinite;
            font-size: var(--lumo-font-size-xl);
            color: var(--lumo-primary-color);
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        .expandable-json {
            display: flex;
            flex-direction: column;
        }

        .json-preview {
            color: var(--lumo-secondary-text-color);
            cursor: pointer;
            padding: 2px 4px;
            border-radius: var(--lumo-border-radius-s);
            width: fit-content;
        }

        .json-preview:hover {
            color: var(--lumo-body-text-color);
        }

        .table-container table {
            border-collapse: collapse;
            border-spacing: 0;
            width: 100%;
        }

        .table-container th {
            text-align: left;
            padding: 8px 12px;
            font-weight: bold;
            background-color: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-20pct);
            white-space: nowrap;
        }

        .table-container td {
            padding: 8px 12px;
            vertical-align: top;
        }

        .table-container tbody tr:nth-child(even) {
            background-color: var(--lumo-contrast-5pct);
        }

        .nested-table {
            width: 100%;
            margin-top: 8px;
            border-left: 2px solid var(--lumo-contrast-10pct);
            padding: 0 10px;
        }
        
        .nested-table table {
            width: 100%;
            border-collapse: collapse;
        }

        .nested-table th {
            text-align: left;
            padding: 4px 8px;
            font-weight: bold;
            background-color: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-20pct);
        }

        .nested-table td {
            padding: 4px 8px;
            vertical-align: top;
        }

        .vertical-separator {
            width: 1px;
            height: 24px;
            background-color: var(--lumo-contrast-20pct);
            margin: 0 8px;
            align-self: center;
        }
    `;

    static properties = {
        _persistenceUnits: {state: true, type: Array},
        _selectedPersistenceUnit: {state: true},
        _entityTypes: {state: true, type: Array},
        _messages: {state: true, type: Array},
        _currentPageNumber: {state: true},
        _currentNumberOfPages: {state: true},
        _allowHql: {state: true},
        _assistantEnabled: {state: true},
        _assistantInteractive: {state: true},
        _loading: {state: true, type: Boolean},
        _expandCounter: {state: true, type: Number},
    }

    constructor() {
        super();
        this._persistenceUnits = [];
        this._selectedPersistenceUnit = null;
        this._entityTypes = [];
        this._messages = [];
        this._currentPageNumber = 1;
        this._currentNumberOfPages = 1;
        this._pageSize = 15;
        this._allowHql = false;
        this._assistantEnabled = this.storageControl.get('assistant-enabled') === 'true';
        this._assistantInteractive = this.storageControl.get('assistant-interactive') === 'true';
        this._loading = false;
        this._expandCounter = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this._loading = true;
        const configPromise = this.configJsonRpc.getAllValues();
        const infoPromise = this.jsonRpc.getInfo();
        Promise.all([configPromise, infoPromise]).then(responses => {
            const configValues = responses[0].result;
            this._allowHql = configValues['quarkus.hibernate-orm.dev-ui.allow-hql'] === 'true';
            const infoResponse = responses[1].result;
            if (infoResponse) {
                this._persistenceUnits = infoResponse.persistenceUnits;
                this._selectPersistenceUnit(this._persistenceUnits[0]);
            }
        }).catch(error => {
            console.error("Failed to fetch configuration or persistence units:", error);
            this._addErrorMessage("Failed to fetch configuration or persistence units: " + error);
        }).finally(() => {
            this._loading = false;
        });
    }

    render() {
        if (this._loading) {
            return this._renderFetchingProgress();
        } else if (this._persistenceUnits && this._persistenceUnits.length > 0) {
            return this._renderChatInterface();
        } else {
            return html`
                <p>No persistence units were found.
                    <vaadin-button @click="${this.hotReload}" theme="small">Check again</vaadin-button>
                </p>`;
        }
    }

    _renderFetchingProgress() {
        return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;">
                <div>Fetching persistence units...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
    }

    _renderChatInterface() {
        return html`
            <div class="dataSources">
                <div class="chat-container bordered">
                    <div class="selector-section">
                        <div class="selector-row">
                            <div class="pu-selector">
                                ${this._renderDatasourcesComboBox()}
                            </div>
                            <div class="entity-selector">
                                ${this._renderEntityTypes()}
                            </div>
                        </div>
                    </div>
                    <div class="chat-area" id="chat-area">
                        ${this._messages.map(msg => this._renderMessage(msg))}
                    </div>
                    ${this._allowHql ? html`
                        <div class="chat-input">
                            <vaadin-text-field class="chat-text-area" placeholder="${this._assistantEnabled ? 'Enter natural language request' : 'Enter an HQL query'}"
                                              id="hql-input"
                                              @keydown="${this._handleKeyDown}"
                                              style="height: 40px;" 
                                              clear-button-visible>
                                <vaadin-tooltip slot="tooltip"
                                                text="${this._assistantEnabled ? 'Enter natural language request' : 'Enter an HQL query'}">
                                </vaadin-tooltip>
                            </vaadin-text-field>
                            ${assistantState.current.isConfigured ? html`
                                <vaadin-button theme="secondary" @click="${this._toggleAssistant}"
                                               style="color: ${this._assistantEnabled ? 'var(--quarkus-assistant)' : 'var(--lumo-secondary-text-color)'}">
                                    <vaadin-icon icon="font-awesome-solid:robot" slot="prefix"></vaadin-icon>
                                    <vaadin-tooltip slot="tooltip" 
                                                    text="${this._assistantEnabled ? 'Disable Hibernate Assistant' : 'Enable Hibernate Assistant'}">
                                    </vaadin-tooltip>
                                    ${this._assistantEnabled ? 'Assistant enabled' : 'Assistant disabled'}
                                </vaadin-button>
                                <vaadin-checkbox 
                                        label="Interactive"
                                        ?disabled="${!this._assistantEnabled}"
                                        ?checked="${this._assistantInteractive}"
                                        @change="${this._onInteractiveChanged}">
                                    <vaadin-tooltip slot="tooltip"
                                                    text="Interactive mode generates a natural language response based on the extracted data"></vaadin-tooltip>
                                </vaadin-checkbox>
                            ` : ''}
                            <div class="vertical-separator"></div>
                            <vaadin-button theme="contrast" @click="${this._clearChat}">
                                <vaadin-icon icon="font-awesome-solid:trash"></vaadin-icon>
                                <vaadin-tooltip slot="tooltip" text="Clear History"></vaadin-tooltip>
                            </vaadin-button>
                            <vaadin-button theme="primary" @click="${this._sendQuery}"
                                           ?disabled="${this._selectedPersistenceUnit?.reactive}">
                                <vaadin-icon icon="font-awesome-solid:play"></vaadin-icon>
                                <vaadin-tooltip slot="tooltip" text="Execute query"></vaadin-tooltip>
                            </vaadin-button>
                        </div>` : ''}
                </div>
            </div>`;
    }

    _clearChat() {
        this._messages = [];
        this._expandCounter = 0;
        this._welcomeMessage(this._selectedPersistenceUnit);
    }

    _toggleAssistant() {
        this._assistantEnabled = !this._assistantEnabled;
        this.storageControl.set('assistant-enabled', this._assistantEnabled);
    }

    _onInteractiveChanged(event) {
        this._assistantInteractive = event.target.checked;
        this.storageControl.set('assistant-interactive', this._assistantInteractive);
    }

    _renderMessage(message) {
        if (message.type === 'loading') {
            return html`
            <div class="message system-message">
                <div style="display: flex; justify-content: center; align-items: center;">
                    <vaadin-icon icon="font-awesome-solid:circle-notch" class="spinner"></vaadin-icon>
                </div>
            </div>`;
        } else if (message.type === 'user') {
            return html`
            <div class="message user-message">
                <div>${message.content}</div>
            </div>`;
        } else if (message.type === 'error') {
            return html`
            <div class="message error-message">
                <div><strong>Error:</strong> ${message.content}</div>
            </div>`;
        } else if (message.type === 'result') {
            return html`
                <div class="message system-message">
                    ${message.message ? html`
                        <div>${message.message}</div>
                        ${this._renderResultsFooter(message)}
                    ` : message.data ? html`
                        ${this._renderResultsWithPagination(message)}
                    ` : ''}
                </div>`;
        } else {
            return html`
            <div class="message system-message">
                <div>${message.content}</div>
            </div>`;
        }
    }

    _renderResultsFooter(message) {
        if (message.results <= 0) {
            return;
        }

        const results = `${message.message ? 'Using ' : ''}${message.results} result${message.results === 1 ? '' : 's'}`;
        return html`
            <div class="result-footer">
                <span class="result-count">${results}</span>
                ${message.assistant ? html`<qui-assistant-warning style="display: inline-block;padding-right: 20px;"/>` : ''}
                <vaadin-button theme="tertiary-inline" class="copy-button"
                               data-query="${message.query}"
                               @click="${(e) => this._copyQueryToClipboard(e, message.query)}">
                    HQL
                    <vaadin-icon class="small-icon" icon="font-awesome-regular:copy"></vaadin-icon>
                    <vaadin-tooltip slot="tooltip" text="${message.query}"></vaadin-tooltip>
                </vaadin-button>
            </div>`;
    }

    _renderResultsWithPagination(message) {
        return html`
            <div class="results-card">
                ${this._renderResultsData(message.data)}
                ${this._renderResultsFooter(message)}
            </div>
            ${message.totalPages > 1 ? html`
                <div class="pagination">
                    <vaadin-button theme="icon tertiary" ?disabled="${message.page === 1}"
                                   @click="${() => this._paginateResults(message.query, message.page - 1)}">
                        <vaadin-icon class="small-icon" icon="font-awesome-solid:chevron-left"></vaadin-icon>
                    </vaadin-button>
                    <span>Page ${message.page} of ${message.totalPages}</span>
                    <vaadin-button theme="icon tertiary" ?disabled="${message.page >= message.totalPages}"
                                   @click="${() => this._paginateResults(message.query, message.page + 1)}">
                        <vaadin-icon class="small-icon" icon="font-awesome-solid:chevron-right"></vaadin-icon>
                    </vaadin-button>
                </div>` : ''}`;
    }

    _copyQueryToClipboard(e, query) {
        e.stopPropagation();
        const targetButton = e.currentTarget;
        navigator.clipboard.writeText(query).then(() => {
            if (targetButton) {
                const icon = targetButton.querySelector('vaadin-icon');
                icon.setAttribute('icon', 'font-awesome-solid:check');
                setTimeout(() => {
                    icon.setAttribute('icon', 'font-awesome-regular:copy');
                }, 2000);
            }
        }).catch(err => {
            console.error('Failed to copy query: ', err);
        });
    }

    _renderResultsData(data) {
        if (!data || data.length === 0) {
            return html`<div>No results found.</div>`;
        }

        return this._renderArrayTable(data, false);
    }

    _formatValue(value) {
        if (value === true) {
            return html`<vaadin-icon style="color: var(--lumo-contrast-50pct);" title="true" icon="font-awesome-regular:square-check"></vaadin-icon>`;
        } else if (value === false) {
            return html`<vaadin-icon style="color: var(--lumo-contrast-50pct);" title="false" icon="font-awesome-regular:square"></vaadin-icon>`;
        } else if (value === null || value === undefined) {
            return html`<span>null</span>`;
        } else if (typeof value === 'object') {
            return this._renderExpandableJson(value);
        } else if (typeof value === 'string' && (value.startsWith('http://') || value.startsWith('https://'))) {
            return html`<a href="${value}" target="_blank">${value}</a>`;
        } else {
            return html`<span>${value}</span>`;
        }
    }

    _renderExpandableJson(value) {
        const isArray = Array.isArray(value);
        const preview = isArray
            ? `Array[${value.length}]`
            : `Object{${Object.keys(value).length} properties}`;

        const expandId = `expand-${++this._expandCounter}`;
        return html`
            <div class="expandable-json">
                <span class="json-preview" @click="${(e) => this._toggleExpand(e, expandId)}">[+] ${preview}</span>
                <div id="${expandId}" class="nested-table" style="display: none">
                    ${isArray
                            ? this._renderArrayTable(value, true) // true means this is nested
                            : this._renderObjectTable(value)}
                </div>
            </div>
        `;
    }

    _toggleExpand(e, expandId) {
        e.stopPropagation();
        const element = this.renderRoot.querySelector(`#${expandId}`);
        const previewEl = e.currentTarget;
        if (element.style.display === 'none') {
            element.style.display = 'block';
            previewEl.textContent = previewEl.textContent.replace('[+]', '[-]');
        } else {
            element.style.display = 'none';
            previewEl.textContent = previewEl.textContent.replace('[-]', '[+]');
        }
    }

    _renderArrayTable(array, isNested = false) {
        if (array.length === 0) {
            return html`<div style="font-style: italic; color: var(--lumo-tertiary-text-color);">[empty array]</div>`;
        }

        // Check if we need a complex table (objects) or a simple list
        const isComplexArray = array.some(item => typeof item === 'object' && item !== null);

        if (isComplexArray) {
            // Get all possible keys from objects in the array
            const keys = new Set();
            array.forEach(item => {
                if (typeof item === 'object' && item !== null) {
                    Object.keys(item).forEach(key => keys.add(key));
                }
            });

            return html`
                <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            ${[...keys].map(key => html`<th>${key}</th>`)}
                        </tr>
                        </thead>
                        <tbody>
                        ${array.map(item => {
                            if (typeof item === 'object' && item !== null) {
                                return html`
                                    <tr>
                                        ${[...keys].map(key => html`
                                            <td>${this._formatValue(item[key])}</td>
                                        `)}
                                    </tr>
                                `;
                            }
                            return '';
                        })}
                        </tbody>
                    </table>
                </div>
            `;
        } else {
            // Simple table for primitive values
            return html`
                <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            <th>Index</th>
                            <th>Value</th>
                        </tr>
                        </thead>
                        <tbody>
                        ${array.map((item, index) => html`
                            <tr>
                                <td style="width: 50px;">${index}</td>
                                <td>${this._formatValue(item)}</td>
                            </tr>
                        `)}
                        </tbody>
                    </table>
                </div>
            `;
        }
    }

    _renderObjectTable(obj) {
        return html`
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Property</th>
                        <th>Value</th>
                    </tr>
                </thead>
                <tbody>
                    ${Object.entries(obj).map(([key, value]) => html`
                        <tr>
                            <td style="width: 30%;">${key}</td>
                            <td>${this._formatValue(value)}</td>
                        </tr>
                    `)}
                </tbody>
            </table>
        </div>
    `;
    }

    _capitalize(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    _renderDatasourcesComboBox() {
        return html`
            <vaadin-combo-box
                    label="Persistence Unit"
                    item-label-path="name"
                    item-value-path="name"
                    .items="${this._persistenceUnits}"
                    .value="${this._persistenceUnits[0]?.name || ''}"
                    @value-changed="${this._onPersistenceUnitChanged}"
                    .allowCustomValue="${false}"
            ></vaadin-combo-box>
        `;
    }

    _renderEntityTypes() {
        return html`
            <div class="entity-suggestions">
                <vaadin-combo-box
                        label="Entity Types"
                        .items="${this._entityTypes}"
                        placeholder="Select entity to use..."
                        @value-changed="${(e) => this._insertEntityName(e.detail.value)}"
                        clear-button-visible
                ></vaadin-combo-box>
            </div>
        `;
    }

    _insertEntityName(entityName) {
        if (entityName) {
            const input = this.renderRoot.querySelector('#hql-input');
            if (input) {
                input.value = `from ${entityName}`;
                input.focus();
            }
        }
    }

    _onPersistenceUnitChanged(event) {
        const selectedValue = event.detail.value;
        this._selectPersistenceUnit(this._persistenceUnits.find(unit => unit.name === selectedValue))
    }

    _selectPersistenceUnit(pu) {
        this._selectedPersistenceUnit = pu;

        if (pu && !pu.reactive) {
            this._entityTypes = pu.managedEntities ? pu.managedEntities.map(entity => entity.name) : [];
        } else {
            this._entityTypes = [];
        }
        this._clearChat();
    }

    _welcomeMessage(pu) {
        if ( pu ) {
            if (pu.reactive) {
                this._addErrorMessage("Reactive persistence units are not supported in this console, please use a blocking one.");
            } else {
                // Add initialization welcome message
                if (this._allowHql) {
                    this._addSystemMessage(`Welcome to the HQL Console! Enter your queries below.`);
                } else {
                    this._addSystemMessage(html`
                        Welcome to the HQL Console!
                        <a href="#" @click="${this._handleAllowHqlChange}"
                           style="color: var(--lumo-primary-color); text-decoration: underline; font-weight: bold;">
                            Enable HQL execution in <code>application.properties</code>
                        </a>.
                    `);
                }
            }
        }
        else {
            this._addErrorMessage("No persistence unit is available.");
        }
    }

    _handleAllowHqlChange() {
        this.configJsonRpc.updateProperty({
            'name': '%dev.quarkus.hibernate-orm.dev-ui.allow-hql',
            'value': 'true'
        }).then(e => {
            this._allowHql = true;
            this._addSystemMessage("HQL execution is now enabled. Enter your queries below.");
        });
    }

    _handleKeyDown(event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            this._sendQuery();
        }
    }

    _sendQuery() {
        const input = this.renderRoot.querySelector('#hql-input');
        const query = input.value.trim();

        if (!query) return;

        this._addUserMessage(html`<strong>${this._assistantEnabled ? 'Message' : 'Query'}: </strong>${query}`);

        this._executeHQL(query, 1, this._assistantEnabled, this._assistantInteractive);
    }

    _executeHQL(query, pageNumber, assistant = false, interactive = false) {
        if (!query || !this._selectedPersistenceUnit) return;

        // Create a loading message instead of setting global loading state
        const loadingMessageIndex = this._messages.length;
        this._messages = [...this._messages, {
            type: 'loading',
            query: query
        }];

        this.jsonRpc.executeHQL({
            persistenceUnit: this._selectedPersistenceUnit.name,
            query: query,
            pageNumber: pageNumber,
            pageSize: this._pageSize,
            assistant: assistant,
            interactive: interactive,
        }).then(response => {
            // Clone the messages array to modify it
            const updatedMessages = [...this._messages];

            const result = response.result;
            const error = response.error && response.error.message ||
                (result && result.error);

            if (error) {
                updatedMessages[loadingMessageIndex] = {
                    type: 'error',
                    content: error
                };
            } else {
                updatedMessages[loadingMessageIndex] = {
                    type: 'result',
                    message: result.message,
                    data: result.data,
                    query: result.query,
                    page: pageNumber,
                    results: result.resultCount,
                    assistant: assistant,
                    totalPages: Math.ceil(result.resultCount / this._pageSize) || 1
                };
            }

            this._messages = updatedMessages;
            this._scrollToBottom();
        }).catch(response => {
            // Replace loading message with error on exception
            const updatedMessages = [...this._messages];
            updatedMessages[loadingMessageIndex] = {
                type: 'error',
                content: response.error && response.error.message || "Failed to execute query."
            };
            this._messages = updatedMessages;
        });
    }

    _paginateResults(query, pageNumber) {
        this._executeHQL(query, pageNumber);
    }

    _addUserMessage(content) {
        this._messages = [...this._messages, {
            type: 'user',
            content
        }];
        this._scrollToBottom();
    }

    _scrollToBottom() {
        setTimeout(() => {
            const chatArea = this.renderRoot.querySelector('#chat-area');
            chatArea.scrollTop = chatArea.scrollHeight;
        }, 100);
    }

    _addSystemMessage(content) {
        this._messages = [...this._messages, {
            type: 'system',
            content
        }];
    }

    _addErrorMessage(content) {
        this._messages = [...this._messages, {
            type: 'error',
            content
        }];
    }

    _addResultMessage(result) {
        this._messages = [...this._messages, {
            type: 'result',
            ...result
        }];
    }

    _getCurrentTime() {
        return new Date().toLocaleTimeString();
    }
}

customElements.define('hibernate-orm-hql-console', HibernateOrmHqlConsoleComponent);