import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/text-field';

/**
 * This component shows the scheduled methods.
 */
export class QwcSchedulerScheduledMethods extends LitElement {
    
    jsonRpc = new JsonRpc(this);

    static styles = css`
       :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100%;
        }
    
        .schedules-table {
          padding-bottom: 10px;
        }

        code {
          font-size: 85%;
        }

        .annotation {
          color: var(--lumo-contrast-50pct);
        }
        vaadin-button {
            cursor:pointer;
        }
    
        .topBar {
            display: flex;
            justify-content: space-between;
        }
        .searchField {
            width: 30%;
            padding-left: 20px;
        }
        .scheduler {
            padding-right: 20px;
        }
        
        `;

    static properties = {
         _scheduledMethods: {state: true},
         _filteredScheduledMethods: {state: true},
         _schedulerRunning: {state: true}
    };
    
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getData()
            .then(jsonResponse => {
                this._scheduledMethods = jsonResponse.result.methods;
                this._schedulerRunning = jsonResponse.result.schedulerRunning;
                this._filteredScheduledMethods = this._scheduledMethods;
            })
            .then(() => {
                this._runningStatusStream = this.jsonRpc.streamRunningStatus().onNext(jsonResponse => {
                    const identity = jsonResponse.result.id;
                    if (identity === "quarkus_scheduler") {
                        this._schedulerRunning = jsonResponse.result["running"];
                    } else {
                        this._scheduledMethods =  this._scheduledMethods.map(sm => {
                            sm.schedules.forEach(schedule => {
                                if (schedule.identity === identity) {
                                    schedule.running = jsonResponse.result["running"];
                                }
                                }); 
                            return sm; 
                            });
                        this._filteredScheduledMethods = this._scheduledMethods;
                    }
            }
        )}); 
    }
    
    disconnectedCallback() {
        super.disconnectedCallback();
        this._runningStatusStream.cancel();
    }

    render() {
        if (this._scheduledMethods){
            return this._renderScheduledMethods();
        } else {
            return html`<span>Loading scheduled methods...</span>`;
        }
    }

    _renderScheduledMethods(){
        let schedulerButton;
        if (this._schedulerRunning) {
            schedulerButton = html`<vaadin-button class="scheduler" theme="tertiary" @click=${() => this._pauseScheduler()}>
                <vaadin-icon icon="font-awesome-solid:circle-pause"></vaadin-icon>
                Pause scheduler</vaadin-button>`;
        } else {
            schedulerButton = html`<vaadin-button class="scheduler" theme="tertiary" @click=${() => this._resumeScheduler()}>
                <vaadin-icon icon="font-awesome-solid:circle-play"></vaadin-icon>
                Resume scheduler</vaadin-button>`;
        }

        const searchBox = html`
            <vaadin-text-field class="searchField"
                placeholder="Search"
                @value-changed="${e => {
            const searchTerm = (e.detail.value || '').trim().toLowerCase();
            this._filteredScheduledMethods = this._scheduledMethods.filter(method => this._matchesTerm(method, searchTerm));
        }}"
            >
                <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
            </vaadin-text-field>
            `

        return html`
                <div class="topBar">
                    ${searchBox}
                    ${schedulerButton}
                </div>
                <vaadin-grid .items="${this._filteredScheduledMethods}" class="schedules-table" theme="no-border">
                    <vaadin-grid-column auto-width
                        header="Scheduled Method"
                        ${columnBodyRenderer(this._methodRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Triggers"
                        ${columnBodyRenderer(this._scheduleRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }
    
    _scheduleRenderer(scheduledMethod) {
        if (scheduledMethod.schedules.length > 1) {
            const triggers = scheduledMethod.schedules.map(s => this._trigger(s));
            return html`<ul>
                ${triggers.map(trigger =>
                    html`<li>${trigger}</li>`
                )}</ul>`;
        } else {
            return this._trigger(scheduledMethod.schedules[0]);
        }
    }
    
    _trigger(schedule) {
        let trigger;
        if (schedule.identity) {
            if (schedule.running) {
                trigger = html`<vaadin-button theme="small" @click=${() => this._pauseJob(schedule.identity)}>
                <vaadin-icon icon="font-awesome-solid:circle-pause"></vaadin-icon>
                Pause</vaadin-button>`;
            } else {
                trigger = html`<vaadin-button theme="small" @click=${() => this._resumeJob(schedule.identity)}>
                <vaadin-icon icon="font-awesome-solid:circle-play"></vaadin-icon>
                Resume</vaadin-button>`;
            }    
        }
        if (schedule.cron) {
            trigger = schedule.cronConfig ? html`${trigger} <code>${schedule.cron}</code> configured as <code>${schedule.cronConfig}</code>` : html`${trigger} <code>${schedule.cron}</code>`;  
        } else {
            trigger = schedule.everyConfig ? html`${trigger} Every <code>${schedule.every}</code> configured as <code>${schedule.everyConfig}</code>` : html`${trigger} Every <code>${schedule.every}</code>`;
        }
        if (schedule.identity) {
            trigger = schedule.identityConfig ? html`${trigger} with identity <code>${schedule.identity}</code> configured as <code>${scheduledMethod.identityConfig}</code>` : html`${trigger} with identity <code>${schedule.identity}</code>`;
        }
        if (schedule.delayed > 0) {
            trigger = html`${trigger} (with delay ${schedule.delayed} ${schedule.delayedUnit})`;
        } else if(schedule.delayed) {
            trigger = schedule.delayedConfig ? html`${trigger} (delayed for <code>${schedule.delayed}</code> configured as <code>${schedule.delayedConfig}</code>)` : html`${trigger} (delayed for <code>${schedule.delayed}</code>)`;
        }
        return trigger;
    }

    _methodRenderer(scheduledMethod) {
      return html`
        <vaadin-button theme="small" @click=${() => this._executeMethod(scheduledMethod.methodDescription)}>
            <vaadin-icon icon="font-awesome-solid:bolt"></vaadin-icon>
            Execute
        </vaadin-button>
        <code>${scheduledMethod.declaringClassName}.${scheduledMethod.methodName}()</code>
    `;
    }
    
    _pauseJob(identity) {
        this.jsonRpc.pauseJob({"identity": identity}).then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message);
            }
        });
    }
    
    _resumeJob(identity) {
        this.jsonRpc.resumeJob({"identity": identity}).then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message, "bottom-stretch");
            }
        });
    }
    
     _pauseScheduler() {
        this.jsonRpc.pauseScheduler().then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message);
            }
        });
    }
    
    _resumeScheduler() {
        this.jsonRpc.resumeScheduler().then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message);
            }
        });
    }
    
    _executeMethod(methodDescription) {
        this.jsonRpc.executeJob({"methodDescription": methodDescription}).then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message);
            }
        });
    }
    
    _matchesTerm(scheduledMethod, searchTerm) {
        if (!searchTerm
          || scheduledMethod.declaringClassName.toLowerCase().includes(searchTerm)
          || scheduledMethod.methodName.toLowerCase().includes(searchTerm)) {
            return true;
        }
        // schedules
        return scheduledMethod.schedules.filter(s => {
            return s.identity && s.identity.toLowerCase().includes(searchTerm) ||
                s.identityConfig && s.identityConfig.toLowerCase().includes(searchTerm) ||
                s.cron && s.cron.toLowerCase().includes(searchTerm) ||
                s.cronConfig && s.cronConfig.toLowerCase().includes(searchTerm) ||
                s.every && s.every.toLowerCase().includes(searchTerm) ||
                s.everyConfig && s.everyConfig.toLowerCase().includes(searchTerm)
            }).length > 0;
    }
    
}
customElements.define('qwc-scheduler-scheduled-methods', QwcSchedulerScheduledMethods);