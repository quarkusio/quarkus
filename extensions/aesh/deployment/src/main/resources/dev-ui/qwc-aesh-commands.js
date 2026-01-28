import { LitElement, html, css } from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/text-field';
import '@vaadin/badge';

/**
 * Dev UI page showing all discovered aesh commands.
 * Uses build-time data only -- no JsonRPC needed.
 */
export class QwcAeshCommands extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100%;
        }
        .topBar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 10px;
        }
        .searchField {
            width: 30%;
        }
        .mode-badge {
            font-size: 14px;
            padding: 4px 12px;
            border-radius: 4px;
            background: var(--lumo-primary-color-10pct);
            color: var(--lumo-primary-text-color);
        }
        code {
            font-size: 85%;
        }
        .tag {
            display: inline-block;
            font-size: 12px;
            padding: 2px 6px;
            border-radius: 3px;
            margin-right: 4px;
        }
        .tag-top {
            background: var(--lumo-success-color-10pct);
            color: var(--lumo-success-text-color);
        }
        .tag-cli {
            background: var(--lumo-primary-color-10pct);
            color: var(--lumo-primary-text-color);
        }
        .tag-group {
            background: var(--lumo-contrast-10pct);
            color: var(--lumo-contrast-80pct);
        }
        .sub-commands {
            padding-left: 16px;
            font-size: 13px;
            color: var(--lumo-contrast-60pct);
        }
    `;

    static properties = {
        _commands: { state: true },
        _filteredCommands: { state: true },
        _mode: { state: true }
    };

    constructor() {
        super();
        this._commands = [];
        this._filteredCommands = [];
        this._mode = '';
    }

    connectedCallback() {
        super.connectedCallback();
        // Build-time data is injected via custom element attributes by Dev UI framework
        this._mode = this.jsondata?.mode || '';
        this._commands = this.jsondata?.commands || [];
        this._filteredCommands = this._commands;
    }

    render() {
        return html`
            <div class="topBar">
                <vaadin-text-field
                    class="searchField"
                    placeholder="Search commands..."
                    @value-changed="${e => this._filter(e.detail.value)}">
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
                <span class="mode-badge">Mode: ${this._mode}</span>
            </div>
            <vaadin-grid .items="${this._filteredCommands}" theme="no-border">
                <vaadin-grid-sort-column
                    auto-width
                    header="Command"
                    path="name"
                    resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-column
                    auto-width
                    header="Description"
                    path="description"
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="Type"
                    ${columnBodyRenderer(this._typeRenderer, [])}
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="Class"
                    ${columnBodyRenderer(this._classRenderer, [])}
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="Sub-commands"
                    ${columnBodyRenderer(this._subCommandsRenderer, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    _typeRenderer(cmd) {
        const tags = [];
        if (cmd.topCommand) {
            tags.push(html`<span class="tag tag-top">@TopCommand</span>`);
        }
        if (cmd.cliCommand) {
            tags.push(html`<span class="tag tag-cli">@CliCommand</span>`);
        }
        if (cmd.groupCommand) {
            tags.push(html`<span class="tag tag-group">Group</span>`);
        }
        return html`${tags}`;
    }

    _classRenderer(cmd) {
        return html`<code>${cmd.className}</code>`;
    }

    _subCommandsRenderer(cmd) {
        if (cmd.subCommands && cmd.subCommands.length > 0) {
            return html`
                <div class="sub-commands">
                    ${cmd.subCommands.map(sc => html`<div><code>${sc}</code></div>`)}
                </div>`;
        }
        return html``;
    }

    _filter(term) {
        const search = (term || '').trim().toLowerCase();
        if (!search) {
            this._filteredCommands = this._commands;
            return;
        }
        this._filteredCommands = this._commands.filter(cmd =>
            cmd.name.toLowerCase().includes(search) ||
            (cmd.description && cmd.description.toLowerCase().includes(search)) ||
            cmd.className.toLowerCase().includes(search)
        );
    }
}

customElements.define('qwc-aesh-commands', QwcAeshCommands);
