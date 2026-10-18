import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Rest Easy Reactive Exception mappers
 */
export class QwcResteasyReactiveExceptionMappers extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        .datatable {
            height: 100%;
            padding-bottom: 10px;
            border: none;
        }`;

    static properties = {
        _exceptionMappers: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._exceptionMappers = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getExceptionMappers().then(exceptionMappers => {
            this._exceptionMappers = exceptionMappers.result;
        });
    }

    render() {
        if (this._exceptionMappers) {
            return html`
                <vaadin-grid .items="${this._exceptionMappers}" class="datatable" theme="row-stripes">
                    <vaadin-grid-sort-column
                        header="${msg('Priority', { id: 'quarkus-rest-priority' })}"
                        path="priority"
                        resizable
                        auto-width>
                    </vaadin-grid-sort-column>
                    <vaadin-grid-sort-column
                        header="${msg('Exception', { id: 'quarkus-rest-exception' })}"
                        path="name"
                        resizable
                        auto-width>
                    </vaadin-grid-sort-column>
                    <vaadin-grid-sort-column
                        header="${msg('Mapper', { id: 'quarkus-rest-mapper' })}"
                        path="className"
                        resizable
                        auto-width>
                    </vaadin-grid-sort-column>
                </vaadin-grid>
            `;
        }
    }
}
customElements.define('qwc-resteasy-reactive-exception-mappers', QwcResteasyReactiveExceptionMappers);
