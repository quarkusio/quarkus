import { LitElement, html, css} from 'lit';
import { connectionState } from 'connection-state';
import { observeState } from 'lit-element-state';
import { dynamicMsg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the status of the Web socket connection
 */
export class QwcWsStatus extends observeState(LitElement) {

    static styles = css`
        :host {
            display: inline-flex;
            align-items: center;
        }
        vaadin-icon {
            width: var(--lumo-icon-size-s);
            height: var(--lumo-icon-size-s);
            transition: color var(--devui-transition-fast, 0.15s ease);
        }
    `;

    constructor() {
        super();
        updateWhenLocaleChanges(this);
    }

    render() {
        return html`<vaadin-icon title="${dynamicMsg('connection', connectionState.current.message)} ${connectionState.current.serverUri}" style="color:${connectionState.current.color}" icon="font-awesome-solid:${connectionState.current.icon}"></vaadin-icon>`;
    }
}

customElements.define('qwc-ws-status', QwcWsStatus);