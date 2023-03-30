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
 * This component shows the Build Items
 */
export class QwcBuildItems extends LitElement {

  jsonRpc = new JsonRpc(this, true, 'build-metrics');

  static styles = css`
      .build-items {
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
      this._filtered = this._buildStepsMetrics.items;
    });
  }

  render() {
    return html`${until(this._render(), html`<span>Loading build items...</span>`)}`;
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
      this._filtered = this._buildStepsMetrics.items;
      return;
    }

    this._filtered = this._buildStepsMetrics.items.filter((item) => {
      return this._match(item.class, searchTerm);
    });
  }

  _render() {
    if (this._buildStepsMetrics && this._filtered) {
      return html`<div class="build-items">
            <div class="summary">Produced <strong>${this._buildStepsMetrics.itemsCount}</strong> build items.</div>
            <vaadin-text-field
                    placeholder="Filter"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filter(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filtered}" style="width: 100%;" class="datatable" theme="row-stripes">
                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Build item"
                                    path="class"
                                    class="cell"
                                    ${columnBodyRenderer(this._classRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Count"
                                    path="count"
                                    class="cell"
                                    ${columnBodyRenderer(this._countRenderer, [])}>
                </vaadin-grid-sort-column>
            </vaadin-grid></div>`;
    }
  }

  _classRenderer(item) {
    return html`<code>${item.class}</code>`;
  }

  _countRenderer(item) {
    return html`${item.count}`;
  }
}
customElements.define('qwc-build-items', QwcBuildItems);