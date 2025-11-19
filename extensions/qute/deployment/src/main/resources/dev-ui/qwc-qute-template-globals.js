import { LitElement, html, css } from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import { templateGlobals } from 'build-time-data';
import 'qui-alert';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the TemplateGlobal.
 */
export class QwcQuteTemplateGlobals extends LitElement {

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
            <qui-alert>
                ${msg(
                    'Global variables are accessible in every template, i.e. a global variable named foo can be used like: {foo}.',
                    { id: 'quarkus-qute-template-globals-description' }
                )}
            </qui-alert>

            <vaadin-grid 
                .items="${templateGlobals}"
                class="templates-table"
                theme="no-border"
                all-rows-visible>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Name', { id: 'quarkus-qute-name' })}"
                    ${columnBodyRenderer(this._renderName, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Target', { id: 'quarkus-qute-target' })}"
                    ${columnBodyRenderer(this._renderTarget, [])}
                    resizable>
                </vaadin-grid-column>

            </vaadin-grid>
        `;
    }

    _renderTarget(global) {
        return html`<code>${global.target}</code>`;
    }

    _renderName(global) {
        return html`<code>${global.name}</code>`;
    }
}
customElements.define('qwc-qute-template-globals', QwcQuteTemplateGlobals);
