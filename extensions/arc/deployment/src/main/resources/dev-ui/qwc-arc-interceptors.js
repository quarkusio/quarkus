import { LitElement, html, css} from 'lit';
import { interceptors } from 'build-time-data';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';
import 'qui-badge';

/**
 * This component shows the Arc Interceptors
 */
export class QwcArcInterceptors extends LitElement {
  
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
        `;

    static properties = {
        _interceptors: {attribute: false}
    };
  
    constructor() {
        super();
        this._interceptors = interceptors;
    }
  
    render() {
        if(this._interceptors){
            return html`
            <vaadin-grid .items="${this._interceptors}" class="arctable" theme="no-border">
              <vaadin-grid-sort-column path="interceptorClass.name" auto-width
                header="Interceptor Class"
                ${columnBodyRenderer(this._classRenderer, [])}
                resizable>
              </vaadin-grid-sort-column>

              <vaadin-grid-sort-column path="priority" auto-width
                header="Priority"
                ${columnBodyRenderer(this._priorityRenderer, [])}
                resizable>
              </vaadin-grid-sort-column>

              <vaadin-grid-column auto-width
                header="Bindings"
                ${columnBodyRenderer(this._bindingsRenderer, [])}
                resizable>
              </vaadin-grid-column>

              <vaadin-grid-column auto-width
                header="Interception Types"
                ${columnBodyRenderer(this._typeRenderer, [])}
                resizable>
              </vaadin-grid-column>
            </vaadin-grid>
            `;
        }
    }

    _classRenderer(bean){
        return html`
            <code>${bean.interceptorClass.name}</code>
        `;
    }

    _priorityRenderer(bean){
        return html`
            <qui-badge small pill><span>${bean.priority}</span></qui-badge>
        `;
    }

    _bindingsRenderer(bean){
        return html`
        <vaadin-vertical-layout>
            ${bean.bindings.map(binding=>
                html`<code class="annotation" title="${binding.name}">${binding.simpleName}</code>`
            )}
        </vaadin-vertical-layout>`;
    }

    _typeRenderer(bean){
        const typeTemplates = [];
        bean.intercepts.forEach((interceptionType) => typeTemplates.push(html`<code class="annotation">${this._printIntercepterType(interceptionType)}</code>`));
        return html`
        <vaadin-vertical-layout>
            ${typeTemplates}
        </vaadin-vertical-layout>`;
    }

    _printIntercepterType(str){
        const p = str.split("_");
        let f = "@";
        p.forEach(w => {
            f = f + this._camelize(w);
        });
        return f;
    }

    _camelize(str) {
        return str.replace(/(?:^\w|[A-Z]|\b\w|\s+)/g, function (match, index) {
            if (+match === 0)
                return "";
            return index === 0 ? match.toUpperCase() : match.toLowerCase();
        });
    }
}
customElements.define('qwc-arc-interceptors', QwcArcInterceptors);