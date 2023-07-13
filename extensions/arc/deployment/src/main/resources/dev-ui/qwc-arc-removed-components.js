import { LitElement, html, css} from 'lit';

import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/grid';
import '@vaadin/vertical-layout';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import 'qui-badge';
import { removedBeans } from 'build-time-data';
import { removedDecorators } from 'build-time-data';
import { removedInterceptors } from 'build-time-data';
import 'qui-ide-link';

/**
 * This component shows the Arc RemovedComponents
 */
export class QwcArcRemovedComponents extends LitElement {
    static styles = css`
        .fullHeight {
          height: 100%;
        }
        code {
          font-size: 85%;
        }

        .annotation {
          color: var(--lumo-contrast-50pct);
        }

        .producer {
          color: var(--lumo-primary-text-color);
        }
    `;

    static properties = {
        _removedBeans: {state: true},
        _removedDecorators: {state: true},
        _removedInterceptors: {state: true},
    };

    constructor() {
        super();
        this._removedBeans = removedBeans;
        this._removedDecorators = removedDecorators;
        this._removedInterceptors = removedInterceptors;
    }

    render() {
        return html`
            <vaadin-tabsheet class="fullHeight">
                <vaadin-tabs slot="tabs">
                    <vaadin-tab id="beans-tab">
                        <span>Removed beans</span>
                        <qui-badge small><span>${this._removedBeans.length}</span></qui-badge>
                    </vaadin-tab>
                    <vaadin-tab id="decorators-tab">
                        <span>Removed decorators</span>
                        <qui-badge small><span>${this._removedDecorators.length}</span></qui-badge>
                    </vaadin-tab>
                    <vaadin-tab id="interceptors-tab">
                        <span>Removed interceptors</span>
                        <qui-badge small><span>${this._removedInterceptors.length}</span></qui-badge>
                    </vaadin-tab>
                </vaadin-tabs>

                <div class="fullHeight" tab="beans-tab">${this._renderRemovedBeans()}</div>
                <div class="fullHeight" tab="decorators-tab">${this._renderRemovedDecorators()}</div>
                <div class="fullHeight" tab="interceptors-tab">${this._renderRemovedInterceptors()}</div>
            </vaadin-tabsheet>
        `;
    }

    _renderRemovedBeans(){
        
        if (this._removedBeans.length > 0) {

            return html`
                <vaadin-grid .items="${this._removedBeans}" theme="no-border" class="fullHeight">
                    <vaadin-grid-column auto-width
                        header="Bean"
                        ${columnBodyRenderer(this._beanRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Kind"
                        ${columnBodyRenderer(this._kindRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    
                </vaadin-grid>`;
            
        } else {
            return html`<qui-badge level='contrast'><span>No beans removed</span></qui-badge>`;
        }
    }

    _renderRemovedDecorators(){
        if (this._removedDecorators.length > 0) {
            return html`TODO: Not yet implemented`;
            
        } else {
            return html`<qui-badge level='contrast'><span>No decorators removed</span></qui-badge>`;
        }
    }

    _renderRemovedInterceptors(){
        if (this._removedInterceptors.length > 0) {                 	
            return html`
                <vaadin-grid .items="${this._removedInterceptors}" theme="no-border" class="fullHeight">
                    <vaadin-grid-column auto-width
                        header="Interceptor"
                        ${columnBodyRenderer(this._interceptorRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Bindings"
                        ${columnBodyRenderer(this._bindingsRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    
                </vaadin-grid>`;
            
        } else {
            return html`<qui-badge level='contrast'><span>No interceptors removed</span></qui-badge>`;
        }
    }

    _interceptorRenderer(bean) {
        return html`${this._nameRenderer(bean.interceptorClass)}`;
    }

    _bindingsRenderer(bean) {
        return html`<vaadin-vertical-layout>
            ${bean.bindings.map(binding =>
                html`${this._simpleNameRenderer(binding)}`
            )}
        </vaadin-vertical-layout>`;
    }

    _beanRenderer(bean) {
        return html`<vaadin-vertical-layout>
      <code class="annotation">@${bean.scope.simpleName}</code>
      ${bean.nonDefaultQualifiers.map(qualifier =>
            html`${this._simpleNameRenderer(qualifier)}`
        )}
      <qui-ide-link fileName='${bean.providerType.name}'
                        lineNumber=0><code>${bean.providerType.name}</code></qui-ide-link>
      </vaadin-vertical-layout>`;
    }

    _kindRenderer(bean) {
      return html`
        <vaadin-vertical-layout>
          ${this._kindBadgeRenderer(bean)}
          ${this._kindClassRenderer(bean)}
        </vaadin-vertical-layout>
    `;
    }

    _kindBadgeRenderer(bean){
        let kind = this._camelize(bean.kind);
        let level = null;
  
        if(bean.kind.toLowerCase() === "field"){
          kind = "Producer field";
          level = "success";
        }else if(bean.kind.toLowerCase() === "method"){
            kind = "Producer method";
            level = "success";
        }else if(bean.kind.toLowerCase() === "synthetic"){
          level = "contrast";
        }
        
        return html`
          ${level
            ? html`<qui-badge level='${level}' small><span>${kind}</span></qui-badge>` 
            : html`<qui-badge small><span>${kind}</span></qui-badge>`
          }`;
      }
  
      _kindClassRenderer(bean){
        return html`
            ${bean.declaringClass
              ? html`<code class="producer">${bean.declaringClass.simpleName}.${bean.memberName}()</code>`
              : html`<code class="producer">${bean.memberName}</code>`
            }
        `;
      }

      _simpleNameRenderer(name) {
        return html`<code class="annotation">${name.simpleName}</code>`;
      }

      _nameRenderer(name) {
        return html`<code class="producer">${name.name}</code>`;
      }

      _camelize(str) {
        return str.replace(/(?:^\w|[A-Z]|\b\w|\s+)/g, function (match, index) {
            if (+match === 0)
                return "";
            return index === 0 ? match.toUpperCase() : match.toLowerCase();
        });
      }
}
customElements.define('qwc-arc-removed-components', QwcArcRemovedComponents);