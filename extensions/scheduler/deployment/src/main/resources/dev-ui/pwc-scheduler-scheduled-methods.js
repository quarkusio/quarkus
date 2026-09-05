import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the scheduled methods. It lists each scheduled method
 * with its triggers (cron / every), description and running/paused status. Unlike
 * the Dev UI component it offers no pause, resume or manual execution - it is
 * purely a listing that reflects the current scheduler state.
 */
export class PwcSchedulerScheduledMethods extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
            gap: 20px;
            padding: 10px;
        }
        .toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        code {
            font-size: 85%;
        }
        .status {
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            font-size: var(--lumo-font-size-xs);
        }
        .running {
            background-color: var(--lumo-success-color-10pct);
            color: var(--lumo-success-text-color);
        }
        .paused {
            background-color: var(--lumo-contrast-10pct);
            color: var(--lumo-secondary-text-color);
        }
        .schedules {
            margin: 0;
            padding-left: 18px;
        }
        .trigger {
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-xs);
        }
        .description {
            color: var(--lumo-contrast-50pct);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _data: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
        this._streamSubscription = this.jsonRpc.streamRunningStatus().onNext(() => {
            this._load();
        });
    }

    disconnectedCallback() {
        this._streamSubscription?.cancel();
        super.disconnectedCallback();
    }

    _load() {
        this.jsonRpc.getData().then(jsonRpcResponse => {
            this._data = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._data) {
            return html`<span class="empty">Loading scheduled methods...</span>`;
        }
        const methods = this._data.methods ?? [];
        return html`
            <div class="toolbar">
                <span>
                    Scheduler:
                    ${this._data.schedulerRunning
                        ? html`<span class="status running">Running</span>`
                        : html`<span class="status paused">Paused</span>`}
                </span>
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${methods.length === 0
                ? html`<span class="empty">No scheduled methods.</span>`
                : html`
                    <vaadin-grid .items="${methods}" theme="no-border row-stripes compact" all-rows-visible>
                        <vaadin-grid-sort-column auto-width header="Method" path="methodName" frozen
                            ${columnBodyRenderer(this._methodRenderer, [])}></vaadin-grid-sort-column>
                        <vaadin-grid-column auto-width header="Triggers"
                            ${columnBodyRenderer(this._schedulesRenderer, [])}></vaadin-grid-column>
                    </vaadin-grid>`}`;
    }

    _methodRenderer(method) {
        return html`<code>${method.declaringClassName}#${method.methodName}</code>
            ${method.methodDescription
                ? html`<div class="description">${method.methodDescription}</div>`
                : html``}`;
    }

    _schedulesRenderer(method) {
        const schedules = method.schedules ?? [];
        if (schedules.length === 0) {
            return html``;
        }
        return html`<ul class="schedules">
            ${schedules.map(schedule => html`<li>
                <span class="trigger">${this._triggerText(schedule)}</span>
                ${schedule.identity
                    ? (schedule.running === false
                        ? html`&nbsp;<span class="status paused">Paused</span>`
                        : html`&nbsp;<span class="status running">Running</span>`)
                    : html``}
                ${schedule.description
                    ? html`&nbsp;<span class="description">${schedule.description}</span>`
                    : html``}
            </li>`)}
        </ul>`;
    }

    _triggerText(schedule) {
        if (schedule.cron) {
            return `cron: ${schedule.cron}`;
        }
        if (schedule.every) {
            return `every: ${schedule.every}`;
        }
        return "-";
    }
}
customElements.define('pwc-scheduler-scheduled-methods', PwcSchedulerScheduledMethods);
