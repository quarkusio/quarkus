import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';

import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer, gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/text-field';
import '@vaadin/vertical-layout';
import '@vaadin/horizontal-layout';
import '@vaadin/progress-bar';
import '@vaadin/button';
import './qwc-build-step-graph.js';
import './qwc-build-steps-execution-graph.js';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Build Steps
 */
export class QwcBuildSteps extends QwcHotReloadElement {

  jsonRpc = new JsonRpc("devui-build-metrics", true);

  static styles = css`
      .build-steps {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      vaadin-grid {
        height: 100%;
      }

      .summary {
        margin-bottom: 15px;
        display: flex;
        align-items: baseline;
        justify-content: space-between;
      }

      .datatable {
        width: 100%;
      }

      .graph-icon {
        font-size: small;
        color: var(--lumo-contrast-50pct); 
        cursor: pointer;
      }

      .graph {
        display: flex;
        flex-direction: column;
        overflow: hidden;
        height: 100%;
      }
      .build-step-detail {
        padding: 1em;
      }
      `;

  static properties = {
    _buildMetrics: { state: true },
    _selectedBuildStep: {state: true},
    _showBuildStepsExecutionGraph: {state: true},
    _filtered: {state: true, type: Array},
    _detailsOpenedItem: {state: true, type: Array}
  };

  constructor() {
    super();
    updateWhenLocaleChanges(this);
    this._buildMetrics = null;
    this._selectedBuildStep = null;
    this._detailsOpenedItem = [];
    this._showBuildStepsExecutionGraph = false;
    this.hotReload();
  }

  hotReload(){
    this.jsonRpc.getBuildMetrics().then(e => {
      this._buildMetrics = e.result;
      this._filtered = this._buildMetrics.records;
    });
  }  

  render() {
      if (this._buildMetrics && !this._buildMetrics.enabled) {
          return html`
          Build metrics not enabled.
          `;
      } else if (this._buildMetrics && this._filtered) {
        return this._render();
      } else {
          return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>${msg('Loading build steps...', { id: 'buildmetrics-loading-steps' })}</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
      }
  }

  _match(value, term) {
    if (!value) {
      return false;
    }
    return value.toLowerCase().includes(term.toLowerCase());
  }

  _filter(e) {
    const searchTerm = (e.detail.value || '').trim();
    if (searchTerm === '') {
      this._filtered = this._buildMetrics.records;
      return;
    }

    this._filtered = this._buildMetrics.records.filter((record) => {
      return this._match(record.stepId, searchTerm);
    });
  }

  _render() {
    if(this._selectedBuildStep){
      return this._renderBuildStepGraph();
    }else if(this._showBuildStepsExecutionGraph){
        return this._renderBuildStepsExecutionGraph();
    }else{    
      return this._renderBuildStepList();
    }
  }

  _renderBuildStepList(){

    const length = this._buildMetrics.records.length;
    const threads = this._buildMetrics.numberOfThreads;
    const duration = this._buildMetrics.duration;

    return html`<div class="build-steps">
            <div class="summary">
                <div>
                ${msg(
                    str`Executed ${length} build steps on ${threads} threads in ${duration} ms`,
                    {id: 'buildmetrics-summary'}
                )}
                </div>    
                <vaadin-button theme="tertiary" @click="${this._showBuildStepsChart}">
                    <vaadin-icon icon="font-awesome-solid:chart-simple" slot="prefix"></vaadin-icon>
                    ${msg('Build Steps Concurrent Execution Chart', { id: 'buildmetrics-concurrent-chart' })}
                </vaadin-button>
            </div>
            <vaadin-text-field
                    placeholder="${msg('Filter', { id: 'buildmetrics-filter' })}"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filter(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid 
                .items="${this._filtered}" 
                .detailsOpenedItems="${this._detailsOpenedItem}"
                @active-item-changed="${(event) => {
                    const buildStep = event.detail.value;
                    this._detailsOpenedItem = buildStep ? [buildStep] : [];
                    }}"
                ${gridRowDetailsRenderer(this._buildStepDetailRenderer, [])}
                class="datatable" 
                theme="row-stripes">
                <vaadin-grid-sort-column resizable
                                    header="${msg('Build step', { id: 'buildmetrics-step' })}"
                                    path="stepId"
                                    ${columnBodyRenderer(this._stepIdRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="${msg('Started', { id: 'buildmetrics-started' })}"
                                    path="started">
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="${msg('Duration (ms)', { id: 'buildmetrics-duration' })}"
                                    path="duration">
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="${msg('Thread', { id: 'buildmetrics-thread' })}"
                                    path="thread">
                </vaadin-grid-sort-column>
                
                <vaadin-grid-column
                        frozen-to-end
                        auto-width
                        flex-grow="0"
                        ${columnBodyRenderer(this._graphIconRenderer, [])}
                      ></vaadin-grid-column>

            </vaadin-grid></div>`;
  }
  
  _buildStepDetailRenderer(buildStep) {
      if (!buildStep.producedItems) {
        return html`<div class="build-step-detail">
            Produced build items not collected. Use <code>-Dquarkus.builder.metrics.extended-capture=true</code> to get more information.
            </div>` 
      }
      return html`<div class="build-step-detail">
                    <vaadin-grid 
                      .items="${buildStep.producedItems}"
                      all-rows-visible
                      theme="no-border">
                                    <vaadin-grid-column auto-width resizable flex-grow="0"
                                        header="${msg('Produced build item', { id: 'buildmetrics-builditem' })}"
                                        ${columnBodyRenderer(this._renderBuildItem, [])}>
                                    </vaadin-grid-column>
                                    <vaadin-grid-column auto-width resizable flex-grow="0"
                                        header="${msg('Count', { id: 'buildmetrics-count' })}"
                                        path="count">
                                    </vaadin-grid-column>
                    </vaadin-grid>
                    </div>
     `
  }
  
  _renderBuildItem(record) {
    return html`<code>${record.item}</code>`;
  }

  _renderBuildStepGraph(){
      return html`<qwc-build-step-graph class="graph"
                      stepId="${this._selectedBuildStep.stepId}"
                      extensionName="${this.jsonRpc.getExtensionName()}"
                      @build-steps-graph-back=${this._showBuildStepsList}></qwc-build-step-graph>`;
  
  }

  _renderBuildStepsExecutionGraph(){
      return html`<qwc-build-steps-execution-graph class="graph"                      
                      extensionName="${this.jsonRpc.getExtensionName()}"
                      @build-steps-graph-back=${this._showBuildStepsList}></qwc-build-steps-execution-graph>`;
  }  

  _stepIdRenderer(record) {
    return html`<code>${record.stepId}</code>`;
  }

  _graphIconRenderer(buildStep){
    return html`<vaadin-icon class="graph-icon" icon="font-awesome-solid:diagram-project" @click=${() => this._showGraph(buildStep)}></vaadin-icon>`;    
  }

  _showGraph(buildStep){
    this._selectedBuildStep = buildStep;
    this._showBuildStepsExecutionGraph = false;
  }

  _showBuildStepsList(){
    this._selectedBuildStep = null;
    this._showBuildStepsExecutionGraph = false;
  }

  _showBuildStepsChart(){
      this._selectedBuildStep = null;
      this._showBuildStepsExecutionGraph = true;
  }
}
customElements.define('qwc-build-steps', QwcBuildSteps);