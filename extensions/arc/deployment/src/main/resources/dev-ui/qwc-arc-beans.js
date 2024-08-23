import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { beans } from 'build-time-data';
import { beanIdsWithDependencyGraphs } from 'build-time-data';
import '@vaadin/grid';
import '@vaadin/vertical-layout';
import 'qui-badge';
import 'qui-ide-link';
import '@vaadin/icon';
import './qwc-arc-bean-graph.js';

/**
 * This component shows the Arc Beans
 */
export class QwcArcBeans extends LitElement {

    static styles = css`
        .arctable {
          height: 100%;
          padding-bottom: 10px;
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
        _beans: {state: true},
        _beanIdsWithDependencyGraphs: {state: true},
        _selectedBean: {state: true}
    };

    constructor() {
        super();
        this._beans = beans;
        this._beanIdsWithDependencyGraphs = beanIdsWithDependencyGraphs;
        this._selectedBean = null;
    }

    render() {
        if (this._beans) {
            if(this._selectedBean){
                return this._renderBeanGraph();
            }else{
                return this._renderBeanList();
            }
        } else {
            return html`No beans found`;
        }
    }

    _renderBeanList(){
        return html`<vaadin-grid .items="${this._beans}" class="arctable" theme="no-border">
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

                    <vaadin-grid-column auto-width
                        header="Associated Interceptors"
                        ${columnBodyRenderer(this._interceptorsRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column
                        frozen-to-end
                        auto-width
                        flex-grow="0"
                        ${columnBodyRenderer(this._graphIconRenderer, [])}
                      ></vaadin-grid-column>
            
                </vaadin-grid>`;
    }

    _renderBeanGraph(){
        return html`<qwc-arc-bean-graph class="graph"
                        beanId="${this._selectedBean.id}"
                        beanDescription="${this._selectedBean.description}"
                        @arc-beans-graph-back=${this._showBeanList}></qwc-arc-bean-graph>`;
    }

    _beanRenderer(bean) {
        return html`<vaadin-vertical-layout>
                        <code class="annotation">@${bean.scope.simpleName}</code>
                        ${bean.nonDefaultQualifiers.map(qualifier =>
                            html`${this._qualifierRenderer(qualifier)}`
                        )}
                        <qui-ide-link fileName='${bean.providerType.name}'
                            lineNumber=0><code>${bean.providerType.name}</code></qui-ide-link>
      
                    </vaadin-vertical-layout>`;
    }

    _kindRenderer(bean) {
        return html`<vaadin-vertical-layout>
                        ${this._kindBadgeRenderer(bean)}
                        ${this._kindClassRenderer(bean)}
                    </vaadin-vertical-layout>`;
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
            }`;
    }

    _interceptorsRenderer(bean) {
        if (bean.interceptors && bean.interceptors.length > 0) {
            return html`<vaadin-vertical-layout>
                          ${bean.interceptorInfos.map(interceptor =>
                                html`<div>
                                        <code>${interceptor.interceptorClass.name}</code> 
                                        <qui-badge class="${bean.kind.toLowerCase()}" small pill><span>${interceptor.priority}</span></qui-badge>
                                    </div>`
                          )}
                        </vaadin-vertical-layout>`;
        }
    }

    _qualifierRenderer(qualifier) {
        return html`<code class="annotation">${qualifier.simpleName}</code>`;
    }

    _graphIconRenderer(bean){
        if(this._beanIdsWithDependencyGraphs.includes(bean.id)){
            return html`<vaadin-icon class="graph-icon" icon="font-awesome-solid:diagram-project" @click=${() => this._showGraph(bean)}></vaadin-icon>`;
        }
    }

    _showGraph(bean){
        this._selectedBean = bean;
    }
    
    _showBeanList(){
        this._selectedBean = null;
    }
    
    _camelize(str) {
        return str.replace(/(?:^\w|[A-Z]|\b\w|\s+)/g, function (match, index) {
            if (+match === 0)
                return "";
            return index === 0 ? match.toUpperCase() : match.toLowerCase();
        });
    }
}
customElements.define('qwc-arc-beans', QwcArcBeans);