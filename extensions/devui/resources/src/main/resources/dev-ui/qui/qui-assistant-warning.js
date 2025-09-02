import {LitElement, html, css} from 'lit';
import '@qomponent/qui-badge';

export class QuiAssistantWarning extends LitElement {

    static styles = css``;

    static properties = {
        warning: {type: String}
    };

    constructor() {
        super();
        this.warning = "Quarkus assistant can make mistakes. Check responses.";
    }
    
    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<qui-badge style="padding-left: 20px;" text="Warning" level="contrast" color="var(--quarkus-assistant)" icon="robot" tiny>
                            <span>${this.warning}</span>
                        </qui-badge>`;
    }
}

customElements.define('qui-assistant-warning', QuiAssistantWarning);
