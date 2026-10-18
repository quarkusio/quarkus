import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/button';
import '@vaadin/checkbox';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Arc Invocation Trees
 */
export class QwcArcInvocationTrees extends LitElement {
  jsonRpc = new JsonRpc(this);
  
  static styles = css`
        .menubar {
            display: flex;
            justify-content: flex-start;
            align-items: center;
            padding-left: 5px;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .arctable {
            height: 100%;
            padding-bottom: 10px;
        }
        ul li::before {
            content: "└─ ";
        }
        ul {
            list-style: none;
        }
        code {
          font-size: 90%;
        }
        `;

    static properties = {
        _invocations: {state: true},
        _filterOutQuarkusBeans: {state: true}
    };
  
    constructor() {
        super();
        updateWhenLocaleChanges(this);
    }

    connectedCallback() {
        super.connectedCallback();
        this._filterOutQuarkusBeans = true;
        this._refresh();
    }
  
    render() {
        if (this._invocations) {
            return this._renderInvocations();
        } else {
            return html`<span>
                ${msg('Loading ArC invocation trees...', { id: 'quarkus-arc-loading-invocation-trees' })}
            </span>`;
        }
    }
  
    _renderInvocations(){
        return html`<div class="menubar">
                    <vaadin-button theme="small" @click=${() => this._refresh()} class="button">
                        <vaadin-icon icon="font-awesome-solid:rotate"></vaadin-icon> 
                        ${msg('Refresh', { id: 'quarkus-arc-refresh' })}
                    </vaadin-button> 
                    <vaadin-button theme="small" @click=${() => this._clear()} class="button">
                        <vaadin-icon icon="font-awesome-solid:trash-can"></vaadin-icon> 
                        ${msg('Clear', { id: 'quarkus-arc-clear' })}
                    </vaadin-button> 
                    <vaadin-checkbox
                        theme="small"
                        .checked="${this._filterOutQuarkusBeans}"
                        label=${msg('Filter out Quarkus beans', { id: 'quarkus-arc-filter-out-quarkus-beans' })}
                        @change=${() => this._toggleFilter()}>
                    </vaadin-checkbox>
                </div>
                <vaadin-grid .items="${this._invocations}" class="arctable" theme="no-border">
                    <vaadin-grid-column auto-width
                        header=${msg('Start', { id: 'quarkus-arc-start' })}
                        path="startTime"
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column width="70%"
                        header=${msg('Invocations', { id: 'quarkus-arc-invocations' })}
                        ${columnBodyRenderer(this._invocationsRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }
    
     _invocationsRenderer(invocation) {
        return html`
            <ul>
            ${this._invocationRenderer(invocation)}
            </ul>
        `;
    }
    
    _invocationRenderer(invocation) {
        return html`
            <li>
            <qui-badge small><span>${invocation.kind.toLowerCase()}</span></qui-badge>
            <code>${invocation.methodName}</code>
            <qui-badge small primary><span>${invocation.duration === 0 ? '< 1' : invocation.duration} ms</span></qui-badge>
            <ul>
                ${invocation.children.map(child =>
                    html`${this._invocationRenderer(child)}`
                )}
            </ul>
            </li>
        `;
    }
    
    _refresh(){
        this.jsonRpc.getLastInvocations().then(invocations => {
            if (this._filterOutQuarkusBeans) {
                this._invocations = invocations.result.filter(i => !i.quarkusBean);
            } else {
                this._invocations = invocations.result;
            }
        });
    }
    
    _clear(){
        this.jsonRpc.clearLastInvocations().then(invocations => {
            this._invocations = invocations.result;
        });
    }

    _toggleFilter(){
        this._filterOutQuarkusBeans = !this._filterOutQuarkusBeans;
        this._refresh();
    }
}
customElements.define('qwc-arc-invocation-trees', QwcArcInvocationTrees);
