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
      }`;

  static properties = {
    _buildStepsMetrics: { state: true },
    _filtered: {state: true, type: Array}
  };

  constructor() {
    super();
    this.hotReload();
  }

  hotReload(){
    this.jsonRpc.getBuildStepsMetrics().then(e => {
      this._buildStepsMetrics = e.result;
      this._filtered = this._buildStepsMetrics.records;
    });
  }  

  render() {
      if (this._buildStepsMetrics && this._filtered) {
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
      this._filtered = this._buildStepsMetrics.records;
      return;
    }

    this._filtered = this._buildStepsMetrics.records.filter((record) => {
      return this._match(record.stepId, searchTerm);
    });
  }

  _render() {
      return html`<div class="build-steps">
            <div class="summary">Executed <strong>${this._buildStepsMetrics.records.length}</strong> build steps on <strong>${Object.keys(this._buildStepsMetrics.threadSlotRecords).length}</strong> threads in <strong>${this._buildStepsMetrics.duration} ms</strong>.</div>
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
            </vaadin-grid></div>`;
  }

  _stepIdRenderer(record) {
    return html`<code>${record.stepId}</code>`;
  }
}
customElements.define('qwc-build-steps', QwcBuildSteps);