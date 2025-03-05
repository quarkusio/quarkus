import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/button';

/**
 * This component shows the Report Issues Page
 */
export class QwcReportIssues extends LitElement {

    jsonRpc = new JsonRpc("report-issues", true);

    static styles = css`
        .todo {
            padding-left: 10px;
            height: 100%;
        }`;

    constructor() {
        super();
    }

    render() {
        return html`
            <vaadin-horizontal-layout theme="spacing" style="align-items: baseline">
                <vaadin-button @click="${this._reportBug}">Report a bug in upstream Quarkus</vaadin-button>
                <vaadin-button @click="${this._reportFeature}">Request a new feature/enhancement in upstream Quarkus</vaadin-button>
            </vaadin-horizontal-layout>
        `;
    }

    _reportBug(event){
        event.preventDefault();
        this.jsonRpc.reportBug().then(e => {
            window.open(e.result.url, "_blank");
        });
    }

    _reportFeature(event)  {
        event.preventDefault();
        window.open("https://github.com/quarkusio/quarkus/issues/new?assignees=&labels=kind%2Fenhancement&template=feature_request.yml", "_blank");
    }

}
customElements.define('qwc-report-issues', QwcReportIssues);
