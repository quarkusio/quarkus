import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Bean Validation constraint metadata. For each
 * validated class it shows the class-level constraints and the per-property
 * constraints (the constraint annotation and its declarative attributes). It
 * offers no actions - nothing is validated and no instance is created.
 */
export class PwcHibernateValidator extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
            padding: 10px;
            gap: 15px;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .class {
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
            padding: 15px;
        }
        .class-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;
        }
        .class-header h3 {
            margin: 0;
        }
        .class-constraints {
            margin-bottom: 10px;
        }
        .section-title {
            font-size: var(--lumo-font-size-s);
            font-weight: 600;
            color: var(--lumo-secondary-text-color);
            margin: 4px 0;
        }
        .constraints {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
        }
        .constraint {
            background-color: var(--lumo-contrast-5pct);
            border-radius: var(--lumo-border-radius-s);
            padding: 0 6px;
            font-family: monospace;
            font-size: 85%;
        }
        .cascaded {
            color: var(--lumo-primary-text-color);
            font-size: 80%;
        }
        code {
            font-size: 85%;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _classes: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getConstrainedClasses().then(jsonRpcResponse => {
            this._classes = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._classes) {
            return html`<span class="empty">Loading constraint metadata...</span>`;
        }
        if (this._classes.length === 0) {
            return html`<span class="empty">No constrained classes.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._classes.map(c => this._renderClass(c))}`;
    }

    _renderClass(constrainedClass) {
        return html`
            <div class="class">
                <div class="class-header">
                    <vaadin-icon icon="font-awesome-solid:shield-halved"></vaadin-icon>
                    <h3><code>${constrainedClass.className}</code></h3>
                </div>
                ${constrainedClass.classConstraints.length > 0
                    ? html`<div class="class-constraints">
                        <div class="section-title">Class-level constraints</div>
                        ${this._renderConstraints(constrainedClass.classConstraints)}
                      </div>`
                    : html``}
                ${constrainedClass.properties.length > 0
                    ? html`<div class="section-title">Properties</div>
                        <vaadin-grid .items=${constrainedClass.properties} theme="row-stripes compact" all-rows-visible>
                            <vaadin-grid-sort-column path="propertyName" header="Property" auto-width resizable
                                ${columnBodyRenderer(p => html`<code>${p.propertyName}</code>`, [])}>
                            </vaadin-grid-sort-column>
                            <vaadin-grid-column header="Constraints" auto-width resizable
                                ${columnBodyRenderer(p => this._renderConstraints(p.constraints), [])}>
                            </vaadin-grid-column>
                            <vaadin-grid-column header="Cascaded" auto-width resizable
                                ${columnBodyRenderer(p => p.cascaded
                                    ? html`<span class="cascaded">@Valid</span>` : html``, [])}>
                            </vaadin-grid-column>
                        </vaadin-grid>`
                    : html``}
            </div>`;
    }

    _renderConstraints(constraints) {
        if (!constraints || constraints.length === 0) {
            return html``;
        }
        return html`<div class="constraints">
            ${constraints.map(c => html`<span class="constraint">${c}</span>`)}
        </div>`;
    }
}
customElements.define('pwc-hibernate-validator', PwcHibernateValidator);
