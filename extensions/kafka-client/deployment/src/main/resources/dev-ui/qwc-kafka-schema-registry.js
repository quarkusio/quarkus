import { LitElement, html, css} from 'lit'; 

/**
 * This component shows the Kafka Scheme Registry
 */
export class QwcKafkaSchemeRegistry extends LitElement { 

    static styles = css``;

    static properties = {
        
    };

    constructor() { 
        super();
    }

    render() { 
        return html`<span> TODO: Scheme Registry</span>`;
    }
}

customElements.define('qwc-kafka-schema-registry', QwcKafkaSchemeRegistry);