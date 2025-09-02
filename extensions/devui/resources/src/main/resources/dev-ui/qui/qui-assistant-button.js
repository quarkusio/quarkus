import {LitElement, html, css} from 'lit';
import '@vaadin/button';
import '@vaadin/icon';

export class QuiAssistantButton extends LitElement {

    static styles = css`
        vaadin-button {
            color: var(--quarkus-assistant);
            --lumo-button-size: var(--lumo-size-s);
            background: var(--vaadin-button-tertiary-background);
            --vaadin-button-min-width: 0;
        }
    `;

    static properties = {
        disabled: { type: Boolean, reflect: true },
        title: { type: String }
    };

    constructor() {
        super();
        this.disabled = false;
        this.title = '';
    }
    
    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`
            <vaadin-button
                ?disabled=${this.disabled}
                title=${this.title}
                @click=${(e) => this._handleClick(e)}>
                    <vaadin-icon icon="font-awesome-solid:robot"></vaadin-icon>
                    <slot></slot>
            </vaadin-button>
          `;
    }
    
    _handleClick(e) {
        this.dispatchEvent(new CustomEvent('click', {
            detail: e,
            bubbles: true,
            composed: true
        }));
    }
}

customElements.define('qui-assistant-button', QuiAssistantButton);
