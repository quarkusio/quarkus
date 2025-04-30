
import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import { templates } from 'build-time-data';


/**
 * This component shows the templates.
 */
export class QwcQuteTemplates extends LitElement {
    
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


    render() {
            return html`
                <vaadin-grid .items="${templates}" class="templates-table" theme="no-border" all-rows-visible>
                    <vaadin-grid-column auto-width
                        header="Template"
                        ${columnBodyRenderer(this._renderPath, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Type-safe Template Method"
                        ${columnBodyRenderer(this._renderCheckedTemplate, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Parameter Declarations"
                        ${columnBodyRenderer(this._renderParamDeclarations, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Fragment Identifiers"
                        ${columnBodyRenderer(this._renderFragments, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }
     
    _renderPath(template) {
        return html`
            <code>${template.path}</code>
        `;
    }
    
    _renderCheckedTemplate(template) {
        return html`
            <code>${template.checkedTemplate}</code>
        `;
    }
    
    _renderParamDeclarations(template) {
        return template.paramDeclarations ? html`<ul>
            ${ template.paramDeclarations.map(pd =>
                html`<li><code>${pd}</code></li>`
            )}</ul>`: html``;
    }
    _renderFragments(template) {
        return template.fragmentIds ? html`<ul>
            ${ template.fragmentIds.map(frag =>
                html`<li><code>${frag}</code></li>`
            )}</ul>`: html``;
    }
        
    
}
customElements.define('qwc-qute-templates', QwcQuteTemplates);