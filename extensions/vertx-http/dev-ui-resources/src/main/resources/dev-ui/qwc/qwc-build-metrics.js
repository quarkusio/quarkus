import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';

import { JsonRpc } from 'jsonrpc';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/text-field';
import '@vaadin/vertical-layout';
import '@vaadin/horizontal-layout';

/**
 * This component shows the Build Metrics
 */
export class QwcBuildMetrics extends QwcHotReloadElement {

  jsonRpc = new JsonRpc("devui-build-metrics", true);

  static styles = css`
      .build-steps {
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }
      
      .build-items {
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      vaadin-grid-sort-column {
        color: red;
      }

      .summary {
        margin-bottom: 15px;
      }

      .datatable {
        width: 100%;
      }`;

  static properties = {
    _buildMetrics: { state: true },
    _filteredSteps: {state: true, type: Array},
    _filteredItems: {state: true, type: Array}
  };

  constructor() {
    super();
    this.hotReload();
  }

  hotReload(){
    this.jsonRpc.getBuildMetrics().then(e => {
      this._buildMetrics = e.result;
      this._filteredSteps = this._buildMetrics.records;
      this._filteredItems = this._buildMetrics.items;
    });
  }  

  render() {
      if (this._buildMetrics && this._filteredSteps && this._filteredItems) {
        return html`
            <vaadin-tabsheet class="fullHeight">
                <vaadin-tabs slot="tabs">
                    <vaadin-tab id="steps-tab">
                        <span>Build steps</span>
                    </vaadin-tab>
                    <vaadin-tab id="items-tab">
                        <span>Build items</span>
                    </vaadin-tab>
                </vaadin-tabs>

                <div class="fullHeight" tab="steps-tab">${this._renderBuildSteps()}</div>
                <div class="fullHeight" tab="items-tab">${this._renderBuildItems()}</div>
            </vaadin-tabsheet>
        `;
      }else {
        return html`<span>Loading build metrics...</span>`;
      }
  }
  
  _match(value, term) {
    if (!value) {
      return false;
    }
    return value.toLowerCase().includes(term.toLowerCase());
  }

  _filterSteps(e) {
    const searchTerm = (e.detail.value || '').trim();
    if (searchTerm === '') {
      this._filteredSteps = this._buildMetrics.records;
      return;
    }

    this._filteredSteps = this._buildMetrics.records.filter((record) => {
      return this._match(record.stepId, searchTerm);
    });
  }
  
   _filterItems(e) {
    const searchTerm = (e.detail.value || '').trim();
    if (searchTerm === '') {
      this._filteredItems = this._buildMetrics.items;
      return;
    }

    this._filteredItems = this._buildMetrics.items.filter((record) => {
      return this._match(record.stepId, searchTerm);
    });
  }

  _renderBuildSteps() {
      return html`<div class="build-steps">
            <div class="summary">Executed <strong>${this._buildMetrics.records.length}</strong> build steps on <strong>${Object.keys(this._buildMetrics.threadSlotRecords).length}</strong> threads in <strong>${this._buildMetrics.duration} ms</strong>.</div>
            <vaadin-text-field
                    placeholder="Filter"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filterSteps(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filteredSteps}" class="datatable" theme="row-stripes" all-rows-visible>
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
  
  _renderBuildItems() {
      return html`<div class="build-items">
            <div class="summary">Produced <strong>${this._buildMetrics.itemsCount}</strong> build items.</div>
            <vaadin-text-field
                    placeholder="Filter"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filterSteps(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filteredItems}" class="datatable" theme="row-stripes" all-rows-visible>
                <vaadin-grid-sort-column resizable
                                    header="Build item"
                                    path="class"
                                    ${columnBodyRenderer(this._itemClassRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Count"
                                    path="count">
                </vaadin-grid-sort-column>
            </vaadin-grid></div>`;
  }

  _stepIdRenderer(record) {
    return html`<code>${record.stepId}</code>`;
  }

  _itemClassRenderer(item) {
    return html`<code>${item.class}</code>`;
  }
}
customElements.define('qwc-build-metrics', QwcBuildMetrics);
