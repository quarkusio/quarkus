import { LitElement, html, css } from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import { extensionMethods } from 'build-time-data';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the extension methods.
 */
export class QwcQuteExtensionMethods extends LitElement {

    static styles = css`
       :host {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .templates-table {
          padding-bottom: 10px;
          height: 100%;
        }
        code {
          font-size: 85%;
        }
        .annotation {
          color: var(--lumo-contrast-50pct);
        }
    `;

    constructor() {
        super();
        updateWhenLocaleChanges(this);
    }

    render() {
        return html`
            <vaadin-grid .items="${extensionMethods}"
                         class="templates-table"
                         theme="no-border"
                         all-rows-visible>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Extension Method', { id: 'quarkus-qute-extension-method' })}"
                    ${columnBodyRenderer(this._renderMethod, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Match name', { id: 'quarkus-qute-match-name' })}"
                    ${columnBodyRenderer(this._renderMatchName, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Match type', { id: 'quarkus-qute-match-type' })}"
                    ${columnBodyRenderer(this._renderMatchType, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Namespace', { id: 'quarkus-qute-namespace' })}"
                    ${columnBodyRenderer(this._renderNamespace, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    _renderMethod(method) {
        return html`<code>${method.name}</code>`;
    }

    _renderMatchName(method) {
        if (method.matchRegex) {
            return html`Regex: <code>${method.matchRegex}</code>`;
        } else if (method.matchNames) {
            return html`Names: <code>${method.matchNames}</code>`;
        } else {
            return html`Name: <code>${method.matchName}</code>`;
        }
    }

    _renderMatchType(method) {
        return method.matchType
            ? html`<code>${method.matchType}</code>`
            : html``;
    }

    _renderNamespace(method) {
        return method.namespace
            ? html`<code>${method.namespace}</code>`
            : html``;
    }
    
}
customElements.define('qwc-qute-extension-methods', QwcQuteExtensionMethods);
