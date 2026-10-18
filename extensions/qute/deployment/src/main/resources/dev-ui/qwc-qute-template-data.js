import { LitElement, html, css } from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import { templateData } from 'build-time-data';
import 'qui-alert';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the TemplateData.
 */
export class QwcQuteTemplateData extends LitElement {

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
                    'The listed target classes are accessible in templates, i.e. a value resolver is generated for each class so that reflection is not needed to access properties and call methods.',
                    { id: 'quarkus-qute-template-data-description' }
                )}
            </qui-alert>

            <vaadin-grid 
                .items="${templateData}"
                class="templates-table"
                theme="no-border"
                all-rows-visible>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Target Class', { id: 'quarkus-qute-target-class' })}"
                    ${columnBodyRenderer(this._renderTarget, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Namespace', { id: 'quarkus-qute-namespace' })}"
                    ${columnBodyRenderer(this._renderNamespace, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Ignore members', { id: 'quarkus-qute-ignore-members' })}"
                    ${columnBodyRenderer(this._renderIgnores, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Only properties', { id: 'quarkus-qute-only-properties' })}"
                    ${columnBodyRenderer(this._renderProperties, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    _renderTarget(data) {
        return html`<code>${data.target}</code>`;
    }

    _renderNamespace(data) {
        return data.namespace ? html`<code>${data.namespace}</code>` : html``;
    }

    _renderIgnores(data) {
        return data.ignores ? html`<code>${data.ignores}</code>` : html``;
    }

    _renderProperties(data) {
        return data.properties ? html`<code>${data.properties}</code>` : html``;
    }

}
customElements.define('qwc-qute-template-data', QwcQuteTemplateData);
