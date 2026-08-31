import { LitElement, html, css } from 'lit';
import { observabilitySignals } from 'devui-data';
import { RouterController } from 'router-controller';
import '@vaadin/icon';

/**
 * Standalone "Observability" section landing page. Lists the telemetry signals
 * (from build-time data) contributed by backend extensions and links to each
 * signal's detail page (e.g. the OpenTelemetry traces waterfall).
 */
export class QwcObservabilitySection extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            padding: 20px;
            gap: 15px;
        }
        .intro {
            color: var(--lumo-secondary-text-color);
        }
        .signals {
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
        }
        .signal {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 15px 20px;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
            cursor: pointer;
            min-width: 180px;
        }
        .signal:hover {
            background-color: var(--lumo-contrast-5pct);
            border-color: var(--lumo-primary-color-50pct);
        }
        .signal vaadin-icon {
            color: var(--lumo-primary-text-color);
        }
        .signal .title {
            font-size: var(--lumo-font-size-l);
        }
    `;

    static properties = {
        _signals: { state: true },
    };

    constructor() {
        super();
        this.routerController = new RouterController(this);
        this._signals = observabilitySignals ?? [];
    }

    render() {
        return html`
            <span class="intro">Explore the telemetry captured by your application in dev mode.</span>
            <div class="signals">
                ${this._signals.map(signal => html`
                    <div class="signal" @click=${() => this._open(signal)}>
                        <vaadin-icon icon="${signal.icon}"></vaadin-icon>
                        <span class="title">${signal.title}</span>
                    </div>`)}
            </div>`;
    }

    _open(signal) {
        // Route to the signal's detail page by its page id, e.g. "quarkus-opentelemetry/traces"
        this.routerController.goToPath('/' + signal.pageId);
    }
}
customElements.define('qwc-observability-section', QwcObservabilitySection);
