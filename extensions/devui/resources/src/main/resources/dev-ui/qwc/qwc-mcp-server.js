import { LitElement, html, css} from 'lit';

/**
 * This component show details on the MCP Server
 */
export class QwcMCPServer extends LitElement {
    static styles = css`
        
    `;

    static properties = {

    }

    constructor() {
        super();    
    }

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html` TODO: Here show details on the MCP Server and how it can be used`;
    }
}
customElements.define('qwc-mcp-server', QwcMCPServer);