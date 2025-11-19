import { LitElement, html} from 'lit';
import { connectionState } from 'connection-state';
import { observeState } from 'lit-element-state';
import { dynamicMsg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the status of the Web socket connection
 */
export class QwcWsStatus extends observeState(LitElement) {

    constructor() {
        super();
        updateWhenLocaleChanges(this);
    }

    render() {
        return html`<vaadin-icon title="${dynamicMsg('connection', connectionState.current.message)} ${connectionState.current.serverUri}" style="color:${connectionState.current.color}" icon="font-awesome-solid:${connectionState.current.icon}"></vaadin-icon>`;
    }
}

customElements.define('qwc-ws-status', QwcWsStatus);