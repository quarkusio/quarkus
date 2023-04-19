
import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import { templateGlobals } from 'build-time-data';


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


    render() {
            return html`
                <qui-alert>Global variables are accessible in every template, i.e. a global variable named <code>foo</code> can be used like: <code>{foo}</code>.</qui-alert>
                <vaadin-grid .items="${templateGlobals}" class="templates-table" theme="no-border" all-rows-visible>
                    <vaadin-grid-column auto-width
                        header="Name"
                        ${columnBodyRenderer(this._renderName, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Target"
                        ${columnBodyRenderer(this._renderTarget, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }
     
    _renderTarget(global) {
        return html`
            <code>${global.target}</code>
        `;
    }
    
    _renderName(global) {
        return html`
            <code>${global.name}</code>
        `;
    }
    
}
customElements.define('qwc-qute-template-globals', QwcQuteTemplateGlobals);
