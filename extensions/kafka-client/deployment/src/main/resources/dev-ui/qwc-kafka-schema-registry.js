import { LitElement, html, css} from 'lit';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Kafka Scheme Registry
 */
export class QwcKafkaSchemeRegistry extends LitElement { 

    static styles = css``;

    static properties = {
        
    };

    constructor() { 
        super();
        updateWhenLocaleChanges(this);
    }

    render() { 
        return html`<span>${msg('TODO: Scheme Registry', { id: 'quarkus-kafka-client-schema-registry-todo' })}</span>`;
    }
}

customElements.define('qwc-kafka-schema-registry', QwcKafkaSchemeRegistry);