import {LitElement, html, css} from 'lit';
import '@qomponent/qui-badge';
import { msg, updateWhenLocaleChanges } from 'localization';

export class QuiAssistantWarning extends LitElement {

    static styles = css``;

    static properties = {
        warning: {type: String}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this.warning = msg('Quarkus assistant can make mistakes. Check responses.', { id: 'assistant-warning-explanation' });
    }
    
    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<qui-badge style="padding-left: 20px;" text="${msg('Warning', { id: 'assistant-warning' })}" level="contrast" color="var(--quarkus-assistant)" icon="robot" tiny>
                            <span>${this.warning}</span>
                        </qui-badge>`;
    }
}

customElements.define('qui-assistant-warning', QuiAssistantWarning);
