import { LitElement, html, css } from 'lit';

/**
 * This component allows users to change the configuration in an online editor
 */
export class QwcConfigurationEditor extends LitElement {

    static styles = css`
    `;

    static properties = {        
    };

    constructor() {
        super();
    }

    render() {
        return html`<span>TODO: Configuration properties editor</span>`;
    }
}

customElements.define('qwc-configuration-editor', QwcConfigurationEditor);