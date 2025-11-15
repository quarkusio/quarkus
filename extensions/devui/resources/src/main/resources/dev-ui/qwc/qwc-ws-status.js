import { LitElement, html} from 'lit';
import { connectionState } from 'connection-state';
import { observeState } from 'lit-element-state';

/**
 * This component shows the status of the Web socket connection
 */
export class QwcWsStatus extends observeState(LitElement) {

    render() {
        return html`<vaadin-icon title="${connectionState.current.message}" style="color:${connectionState.current.color}" icon="font-awesome-solid:${connectionState.current.icon}"></vaadin-icon>`;
    }
}

customElements.define('qwc-ws-status', QwcWsStatus);