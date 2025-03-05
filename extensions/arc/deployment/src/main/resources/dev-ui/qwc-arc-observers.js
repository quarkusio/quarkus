import { LitElement, html, css} from 'lit';
import { observers } from 'build-time-data';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';
import 'qui-badge';
import 'qui-ide-link';

/**
 * This component shows the Arc Observers
 */
export class QwcArcObservers extends LitElement {
  
    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .arctable {
            height: 100%;
            padding-bottom: 10px;
        }

        code {
            font-size: 85%;
        }

        .text {
            font-size: 85%;
        }

        .method {
            color: var(--lumo-primary-text-color);
        }

        .annotation {
            color: var(--lumo-contrast-50pct);
        }
        .filterBar {
            width: 99%;
            margin-left: 5px;
        }
        `;

    static properties = {
        _observers: {attribute: false},
        _filteredObservers: {state: true}
    };
  
    constructor() {
        super();
        this._observers = observers;
        this._filteredObservers = this._observers;
    }
  
    render() {
        if(this._filteredObservers){

            return html`${this._renderFilterBar()}
                <vaadin-grid .items="${this._filteredObservers}" class="arctable" theme="no-border">

                    <vaadin-grid-sort-column path="declaringClass.name" auto-width
                        header="Source"
                        ${columnBodyRenderer(this._sourceRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column path="observedType.name" auto-width
                        header="Observed Type / Qualifiers"
                        ${columnBodyRenderer(this._typeRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column path="priority" auto-width
                        header="Priority"
                        ${columnBodyRenderer(this._priorityRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column path="reception" auto-width
                        header="Reception"
                        ${columnBodyRenderer(this._receptionRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column path="transactionPhase" auto-width
                        header="Transaction Phase"
                        ${columnBodyRenderer(this._transactionPhaseRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                    <vaadin-grid-sort-column path="async" auto-width 
                        header="Async"
                        ${columnBodyRenderer(this._asyncRenderer, [])}
                        resizable>
                    </vaadin-grid-sort-column>

                </vaadin-grid>`;
        }
  }

  _renderFilterBar(){
        return html`<vaadin-text-field
                        placeholder="Search"
                        class="filterBar"
                        @value-changed="${(e) => {
                            const searchTerm = (e.detail.value || '').trim();
                            const matchesTerm = (value) => {
                                if(value){
                                    return value.toLowerCase().includes(searchTerm.toLowerCase());
                                }
                            }
                            if(searchTerm?.trim()){
                                this._filteredObservers = this._observers.filter(
                                    ({ declaringClass, observedType , priority}) => {
                                        return !searchTerm ||
                                            matchesTerm(declaringClass?.name) ||
                                            matchesTerm(observedType?.name) ||
                                            matchesTerm(priority.toString())
                                });
                            }else{
                                this._filteredObservers = this._observers;
                            }
                        }}">
                        <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                    </vaadin-text-field>`;  
  }
  
  _sourceRenderer(bean){
    return html`<qui-ide-link fileName='${bean.declaringClass.name}'><code>${bean.declaringClass.name}</code><code class="method">#${bean.methodName}()</code></qui-ide-link>`;
  }

  _typeRenderer(bean){
    return html`<vaadin-vertical-layout>
      ${bean.qualifiers.map(qualifier=>
        html`${this._qualifierRenderer(qualifier)}`
      )}
      <code>${bean.observedType.name}</code>  
      </vaadin-vertical-layout>`;
  }

  _qualifierRenderer(qualifier){
    if(qualifier){
      return html`<code class="annotation" title="${qualifier.name}">${qualifier.simpleName}</code>`;
    }
  }

  _priorityRenderer(bean){
    return html`
      <qui-badge small pill><span>${bean.priority}</span></qui-badge>
    `;
  }

  _receptionRenderer(bean){
    return html`
      <span class="text">${this._camelize(bean.reception)}</span>  
    `;
  }

  _transactionPhaseRenderer(bean){
    return html`
      <span class="text">${this._camelize(bean.transactionPhase)}</span>  
    `;
  }

  _asyncRenderer(bean){
    if(bean.async !== false){
      return html`
        <qui-badge level="success" icon="font-awesome-solid:check"></qui-badge>
      `;
    }
  }

  _camelize(str) {
    const s = str.replace(/(?:^\w|[A-Z]|\b\w|\s+)/g, function(match, index) {
      if (+match === 0) return "";
      return index === 0 ? match.toUpperCase() : match.toLowerCase();
    });

    return s.replaceAll('_', ' ');
  }
}
customElements.define('qwc-arc-observers', QwcArcObservers);
