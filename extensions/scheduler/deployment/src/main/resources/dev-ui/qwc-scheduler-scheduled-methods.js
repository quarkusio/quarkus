import { LitElement, html, css} from 'lit';
import { observeState } from 'lit-element-state'; 
import { assistantState } from 'assistant-state'; 
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { notifier } from 'notifier';
import { JsonRpc } from 'jsonrpc';
import MarkdownIt from 'markdown-it';
import '@vaadin/grid';
import '@vaadin/text-field';
import '@vaadin/dialog';
import '@vaadin/progress-bar';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the scheduled methods.
 */
export class QwcSchedulerScheduledMethods extends observeState(LitElement) {
    
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
         _schedulerRunning: {state: true},
         _selectedCron: {state: true},
         _interpretDialogOpened: {state: true},
         _interpretCronLoading: {state: true},
         _interpretResult: {state: true}
    };
    
    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._interpretDialogOpened = false;
        this._interpretCronLoading = false;
        this._selectedCron = null;
        this._interpretResult = null;
        this.md = new MarkdownIt();
    }
    
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
                });
            }); 
    }
    
    disconnectedCallback() {
        super.disconnectedCallback();
        this._runningStatusStream.cancel();
    }

    render() {
        if (this._scheduledMethods){
            return this._renderScheduledMethods();
        } else {
            return html`<span>
                ${msg('Loading scheduled methods...', {
                    id: 'quarkus-scheduler-loading-scheduled-methods'
                })}
            </span>`;
        }
    }

    _renderScheduledMethods(){
        let schedulerButton;
        if (this._schedulerRunning) {
            schedulerButton = html`
                <vaadin-button
                    class="scheduler"
                    theme="tertiary"
                    @click=${() => this._pauseScheduler()}>
                    <vaadin-icon icon="font-awesome-solid:circle-pause"></vaadin-icon>
                    ${msg('Pause scheduler', { id: 'quarkus-scheduler-pause-scheduler' })}
                </vaadin-button>`;
        } else {
            schedulerButton = html`
                <vaadin-button
                    class="scheduler"
                    theme="tertiary"
                    @click=${() => this._resumeScheduler()}>
                    <vaadin-icon icon="font-awesome-solid:circle-play"></vaadin-icon>
                    ${msg('Resume scheduler', { id: 'quarkus-scheduler-resume-scheduler' })}
                </vaadin-button>`;
        }

        const searchBox = html`
            <vaadin-text-field
                class="searchField"
                .placeholder=${msg('Search', { id: 'quarkus-scheduler-search' })}
                @value-changed="${e => {
                    const searchTerm = (e.detail.value || '').trim().toLowerCase();
                    this._filteredScheduledMethods =
                        this._scheduledMethods.filter(method => this._matchesTerm(method, searchTerm));
                }}">
                <vaadin-icon
                    slot="prefix"
                    icon="font-awesome-solid:magnifying-glass">
                </vaadin-icon>
            </vaadin-text-field>
        `;

        return html`
            ${this._renderLoadingDialog()}
            ${this._renderInterpretDialog()}
            <div class="topBar">
                ${searchBox}
                ${schedulerButton}
            </div>
            <vaadin-grid
                .items="${this._filteredScheduledMethods}"
                class="schedules-table"
                theme="no-border">
                <vaadin-grid-column
                    auto-width
                    .header=${msg('Scheduled Method', { id: 'quarkus-scheduler-scheduled-method' })}
                    ${columnBodyRenderer(this._methodRenderer, [])}
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    .header=${msg('Triggers', { id: 'quarkus-scheduler-triggers' })}
                    ${columnBodyRenderer(this._scheduleRenderer, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }
    
    _scheduleRenderer(scheduledMethod) {
        if (scheduledMethod.schedules.length > 1) {
            const triggers = scheduledMethod.schedules.map(s => this._trigger(scheduledMethod, s));
            return html`<ul>
                ${triggers.map(trigger => html`<li>${trigger}</li>`)}
            </ul>`;
        } else {
            return this._trigger(scheduledMethod, scheduledMethod.schedules[0]);
        }
    }
    
    _trigger(scheduledMethod, schedule) {
        let trigger;
        if (schedule.identity) {
            if (schedule.running) {
                trigger = html`
                    <vaadin-button
                        theme="small"
                        @click=${() => this._pauseJob(schedule.identity)}>
                        <vaadin-icon icon="font-awesome-solid:circle-pause"></vaadin-icon>
                        ${msg('Pause', { id: 'quarkus-scheduler-pause' })}
                    </vaadin-button>`;
            } else {
                trigger = html`
                    <vaadin-button
                        theme="small"
                        @click=${() => this._resumeJob(schedule.identity)}>
                        <vaadin-icon icon="font-awesome-solid:circle-play"></vaadin-icon>
                        ${msg('Resume', { id: 'quarkus-scheduler-resume' })}
                    </vaadin-button>`;
            }    
        }
        if (schedule.cron) {
            trigger = schedule.cronConfig
                ? html`${trigger}
                        <code>${schedule.cron}</code>
                        ${msg('configured as', { id: 'quarkus-scheduler-configured-as' })}
                        <code>${schedule.cronConfig}</code>
                        ${this._renderInterpretCron(schedule.cronConfig)}`
                : html`${trigger}
                        <code>${schedule.cron}</code>
                        ${this._renderInterpretCron(schedule.cron)}`;  
        } else {
            trigger = schedule.everyConfig
                ? html`${trigger}
                        ${msg('Every', { id: 'quarkus-scheduler-every' })}
                        <code>${schedule.every}</code>
                        ${msg('configured as', { id: 'quarkus-scheduler-configured-as' })}
                        <code>${schedule.everyConfig}</code>`
                : html`${trigger}
                        ${msg('Every', { id: 'quarkus-scheduler-every' })}
                        <code>${schedule.every}</code>`;
        }
        if (schedule.identity) {
            trigger = schedule.identityConfig
                ? html`${trigger}
                        ${msg('with identity', { id: 'quarkus-scheduler-with-identity' })}
                        <code>${schedule.identity}</code>
                        ${msg('configured as', { id: 'quarkus-scheduler-configured-as' })}
                        <code>${scheduledMethod.identityConfig}</code>`
                : html`${trigger}
                        ${msg('with identity', { id: 'quarkus-scheduler-with-identity' })}
                        <code>${schedule.identity}</code>`;
        }
        if (schedule.delayed > 0) {
            trigger = html`${trigger}
                (${msg('with delay', { id: 'quarkus-scheduler-with-delay' })}
                ${schedule.delayed} ${schedule.delayedUnit})`;
        } else if (schedule.delayed) {
            trigger = schedule.delayedConfig
                ? html`${trigger}
                        (${msg('delayed for', { id: 'quarkus-scheduler-delayed-for' })}
                        <code>${schedule.delayed}</code>
                        ${msg('configured as', { id: 'quarkus-scheduler-configured-as' })}
                        <code>${schedule.delayedConfig}</code>)`
                : html`${trigger}
                        (${msg('delayed for', { id: 'quarkus-scheduler-delayed-for' })}
                        <code>${schedule.delayed}</code>)`;
        }
        return trigger;
    }

    _renderInterpretCron(cron){
        if (assistantState.current.isConfigured && !this._interpretCronLoading) {
            return html`
                <qui-assistant-button
                    .title=${`${msg('Interpret', { id: 'quarkus-scheduler-interpret' })} ${cron}`}
                    @click="${(e) => this._interpretCron(e, cron)}">
                </qui-assistant-button>`;
        }
    }
    
    _interpretCron(e, cron){
        document.body.style.cursor = 'wait';
        this._selectedCron = cron;
        this._interpretCronLoading = true;
        
        this.jsonRpc.interpretCron({ cron: this._selectedCron }).then(jsonResponse => {
            this._interpretCronLoading = false;
            document.body.style.cursor = 'default';
            this._interpretResult = jsonResponse.result.markdown;
            this._interpretDialogOpened = true;
        });
    }
    
    _renderLoadingDialog(){
        return html`
            <vaadin-dialog
              .opened="${this._interpretCronLoading}"
              @closed="${() => {
                this._interpretCronLoading = false;
              }}"
              ${dialogRenderer(
                () => html`
                    <label class="text-secondary" id="pblabel">
                        ${msg(
                            'Talking to the Dev Assistant ... please wait',
                            { id: 'quarkus-scheduler-talking-to-dev-assistant' }
                        )}
                    </label>
                    <vaadin-progress-bar
                        indeterminate
                        aria-labelledby="pblabel">
                    </vaadin-progress-bar>`,
                []
              )}
            ></vaadin-dialog>`;
    }
    
    _renderInterpretDialog(){
        return html`
            <vaadin-dialog
              .headerTitle=${this._selectedCron
                ? `${msg('Interpret', { id: 'quarkus-scheduler-interpret' })} "${this._selectedCron}"`
                : msg('Interpret cron', { id: 'quarkus-scheduler-interpret-cron-title' })
              }
              .opened="${this._interpretDialogOpened}"
              @closed="${() => {
                  this._closeInterpretDialog();
              }}"
              ${dialogRenderer(() => html`${this._renderInterpretDialogContents()}`, [])}
              ${dialogHeaderRenderer(
                () => html`
                  <vaadin-button theme="tertiary" @click="${this._closeInterpretDialog}">
                    <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                  </vaadin-button>
                `,
                []
              )}
            ></vaadin-dialog>`;
    }

    _closeInterpretDialog(){
        this._interpretDialogOpened = false;
        this._selectedCron = null;
        this._interpretResult = null;
    }

    _renderInterpretDialogContents(){
        const htmlContent = this.md.render(this._interpretResult);
        return html`${unsafeHTML(htmlContent)}`;
    }

    _methodRenderer(scheduledMethod) {
      return html`
        <vaadin-button
            theme="small"
            @click=${() => this._executeMethod(scheduledMethod.methodDescription)}>
            <vaadin-icon icon="font-awesome-solid:bolt"></vaadin-icon>
            ${msg('Execute', { id: 'quarkus-scheduler-execute' })}
        </vaadin-button>
        <code>${scheduledMethod.declaringClassName}.${scheduledMethod.methodName}()</code>
    `;
    }
    
    _pauseJob(identity) {
        this.jsonRpc.pauseJob({ identity }).then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message);
            }
        });
    }
    
    _resumeJob(identity) {
        this.jsonRpc.resumeJob({ identity }).then(jsonResponse => {
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
        this.jsonRpc.executeJob({ methodDescription }).then(jsonResponse => {
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
            return (s.identity && s.identity.toLowerCase().includes(searchTerm)) ||
                (s.identityConfig && s.identityConfig.toLowerCase().includes(searchTerm)) ||
                (s.cron && s.cron.toLowerCase().includes(searchTerm)) ||
                (s.cronConfig && s.cronConfig.toLowerCase().includes(searchTerm)) ||
                (s.every && s.every.toLowerCase().includes(searchTerm)) ||
                (s.everyConfig && s.everyConfig.toLowerCase().includes(searchTerm));
        }).length > 0;
    }
    
}
customElements.define('qwc-scheduler-scheduled-methods', QwcSchedulerScheduledMethods);
