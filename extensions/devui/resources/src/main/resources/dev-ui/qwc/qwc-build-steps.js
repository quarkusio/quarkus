import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';

import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/text-field';
import '@vaadin/vertical-layout';
import '@vaadin/horizontal-layout';
import '@vaadin/progress-bar';
import '@vaadin/button';
import './qwc-build-step-graph.js';
import './qwc-build-steps-execution-graph.js';

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
      `;

  static properties = {
    _buildMetrics: { state: true },
    _selectedBuildStep: {state: true},
    _showBuildStepsExecutionGraph: {state: true},
    _filtered: {state: true, type: Array}
  };

  constructor() {
    super();
    this._buildMetrics = null;
    this._selectedBuildStep = null;
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
      if (this._buildMetrics && this._filtered) {
        return this._render();
      }else {
          return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Loading build steps...</div>
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

      return html`<div class="build-steps">
            <div class="summary">
                Executed <strong>${this._buildMetrics.records.length}</strong> build steps on <strong>${this._buildMetrics.numberOfThreads}</strong> threads in <strong>${this._buildMetrics.duration} ms</strong>.
                <vaadin-button theme="tertiary" @click="${this._showBuildStepsChart}">
                    <vaadin-icon icon="font-awesome-solid:chart-simple" slot="prefix"></vaadin-icon>
                    Build Steps Concurrent Execution Chart
                </vaadin-button>
            </div>
            <vaadin-text-field
                    placeholder="Filter"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filter(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filtered}" class="datatable" theme="row-stripes">
                <vaadin-grid-sort-column resizable
                                    header="Build step"
                                    path="stepId"
                                    ${columnBodyRenderer(this._stepIdRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Started"
                                    path="started">
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Duration (ms)"
                                    path="duration">
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Thread"
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