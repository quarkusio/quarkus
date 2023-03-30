import { LitElement, html, css } from 'lit';

import { JsonRpc } from 'jsonrpc';
import { until } from 'lit/directives/until.js';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/text-field';
import '@vaadin/vertical-layout';
import '@vaadin/horizontal-layout';

/**
 * This component shows the Build Steps
 */
export class QwcBuildSteps extends LitElement {

  jsonRpc = new JsonRpc(this, true, 'build-metrics');

  static styles = css`
      .build-steps {
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      vaadin-grid {
        height: 100%;
      }

      vaadin-grid-cell-content {
        vertical-align: top;
        width: 100%;
      }

      .summary {
        margin-bottom: 15px;
      }`;

  static properties = {
    _buildStepsMetrics: { state: true },
    _filtered: {state: true, type: Array}
  };

  constructor() {
    super();
    this.jsonRpc.getBuildStepsMetrics().then(e => {
      this._buildStepsMetrics = e.result;
      this._filtered = this._buildStepsMetrics.records;
    });
  }

  render() {
    return html`${until(this._render(), html`<span>Loading build steps...</span>`)}`;
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
    if (this._buildStepsMetrics && this._filtered) {
      return html`<div class="build-steps">
            <div class="summary">Executed <strong>${this._buildStepsMetrics.records.length}</strong> build steps on <strong>${Object.keys(this._buildStepsMetrics.threadSlotRecords).length}</strong> threads in <strong>${this._buildStepsMetrics.duration} ms</strong>.</div>
            <vaadin-text-field
                    placeholder="Filter"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filter(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filtered}" style="width: 100%;" class="datatable" theme="row-stripes">
                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Build step"
                                    path="stepId"
                                    class="cell"
                                    ${columnBodyRenderer(this._stepIdRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Started"
                                    path="started"
                                    class="cell"
                                    ${columnBodyRenderer(this._startedRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Duration (ms)"
                                    path="duration"
                                    class="cell"
                                    ${columnBodyRenderer(this._durationRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-column auto-width resizable flex-grow="0"
                                    header="Thread"
                                    path="thread"
                                    class="cell"
                                    ${columnBodyRenderer(this._threadRenderer, [])}>
                </vaadin-grid-column>
            </vaadin-grid></div>`;
    }
  }

  _stepIdRenderer(record) {
    return html`<code>${record.stepId}</code>`;
  }

  _startedRenderer(record) {
    return html`${record.started}`;
  }

  _durationRenderer(record) {
    return html`${record.duration}`;
  }

  _threadRenderer(record) {
    return html`${record.thread}`;
  }
}
customElements.define('qwc-build-steps', QwcBuildSteps);