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
 * This component shows the Build Items
 */
export class QwcBuildItems extends QwcHotReloadElement {

  jsonRpc = new JsonRpc("devui-build-metrics", true);

  static styles = css`
      .build-items {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      vaadin-grid {
        height: 100%;
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
    _buildItems: { state: true },
    _count: { state: false },
    _filtered: {state: true, type: Array}
  };

  constructor() {
    super();
    this.hotReload();
  }

  hotReload(){
    this.jsonRpc.getBuildItems().then(e => {
      this._buildItems = e.result.items;
      this._count = e.result.itemsCount;
      this._filtered = this._buildItems;
    });
  }  

  render() {
      if (this._buildItems && this._filtered) {
          return this._render();
      }else {
          return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Loading build items...</div>
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
      this._filtered = this._buildItems;
      return;
    }

    this._filtered = this._buildItems.filter((item) => {
      return this._match(item.class, searchTerm);
    });
  }

  _render() {
    return html`<div class="build-items">
            <div class="summary">Produced <strong>${this._count}</strong> build items of <strong>${this._buildItems.length}</strong> types.</div>
            <vaadin-text-field
                    placeholder="Filter"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filter(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filtered}" class="datatable" theme="row-stripes">
                <vaadin-grid-sort-column resizable
                                    header="Build item"
                                    path="class"
                                    ${columnBodyRenderer(this._classRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="Count"
                                    path="count">
                </vaadin-grid-sort-column>
            </vaadin-grid></div>`;
  }

  _classRenderer(item) {
    return html`<code>${item.class}</code>`;
  }
}
customElements.define('qwc-build-items', QwcBuildItems);