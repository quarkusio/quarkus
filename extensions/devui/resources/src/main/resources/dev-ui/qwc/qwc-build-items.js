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
import { msg, str, updateWhenLocaleChanges } from 'localization';

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
      }
      
      .build-item-detail {
        padding: 1em;
      }
      
      .caption {
        font-weight: bold;
      }
      
      `;

  static properties = {
    _enabled: {state: true},
    _buildItems: { state: true },
    _count: { state: false },
    _filtered: {state: true, type: Array},
    _detailsOpenedItem: {state: true, type: Array}
  };

  constructor() {
    super();
    updateWhenLocaleChanges(this);
    this.hotReload();
  }

  hotReload(){
    this.jsonRpc.getBuildItems().then(e => {
      this._enabled = e.result.enabled;
      this._buildItems = e.result.items;
      this._count = e.result.itemsCount;
      this._filtered = this._buildItems;
    });
  }  

  render() {
      if (!this._enabled) {
              return html`
              Build metrics not enabled.
              `;
      } else if (this._buildItems && this._filtered) {
          return this._render();
      } else {
          return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>${msg('Loading build items...', { id: 'buildmetrics-loading-items' })}</div>
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
    const count = this._count;
    const length = this._buildItems.length;
    
    return html`<div class="build-items">
            <div class="summary">${msg(str`Produced ${count} build items of ${length} types.`,
                                        {id: 'buildmetrics-items-produced'}
                                )}</div>
            <vaadin-text-field
                    placeholder="${msg('Filter', { id: 'buildmetrics-filter' })}"
                    style="width: 100%;"
                    @value-changed="${(e) => this._filter(e)}">
                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
            </vaadin-text-field>
            <vaadin-grid .items="${this._filtered}" 
                .detailsOpenedItems="${this._detailsOpenedItem}"
                @active-item-changed="${(event) => {
                                const buildItem = event.detail.value;
                                this._detailsOpenedItem = buildItem ? [buildItem] : [];
                                }}"
                ${gridRowDetailsRenderer(this._buildItemDetailRenderer, [])}
                class="datatable" 
                theme="row-stripes">
                <vaadin-grid-sort-column resizable
                                    header="${msg('Build item', { id: 'buildmetrics-item' })}"
                                    path="class"
                                    ${columnBodyRenderer(this._classRenderer, [])}>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column auto-width resizable flex-grow="0"
                                    header="${msg('Count', { id: 'buildmetrics-count' })}"
                                    path="count">
                </vaadin-grid-sort-column>
            </vaadin-grid></div>`;
  }
  
  _buildItemDetailRenderer(item) {
        if (!item.topProducers) {
            return html`<div class="build-item-detail">
                Top producers not collected. Use <code>-Dquarkus.builder.metrics.extended-capture=true</code> to get more information.
                </div>` 
          }
          return html`
                    <div class="build-item-detail">
                        <div class="caption">Top 10 producers out of ${item.totalProducers} total</div>
                        <vaadin-grid 
                          .items="${item.topProducers}"
                          all-rows-visible
                          theme="no-border">
                                        <vaadin-grid-column auto-width resizable flex-grow="0"
                                            header="${msg('Build step', { id: 'buildmetrics-buildstep' })}"
                                            ${columnBodyRenderer(this._renderBuildStep, [])}>
                                        </vaadin-grid-column>
                                        <vaadin-grid-column auto-width resizable flex-grow="0"
                                            header="${msg('Count', { id: 'buildmetrics-count' })}"
                                            path="count">
                                        </vaadin-grid-column>
                        </vaadin-grid>
                        </div>
         `
  }

  _renderBuildStep(producer) {
    return html`<code>${producer.stepId}</code>`;
  }

  
  _classRenderer(item) {
    return html`<code>${item.class}</code>`;
  }
  
}
customElements.define('qwc-build-items', QwcBuildItems);