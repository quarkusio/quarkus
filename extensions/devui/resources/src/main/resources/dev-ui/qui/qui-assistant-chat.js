import {LitElement, html, css} from 'lit';
import '@vaadin/button';
import '@vaadin/icon';
import { JsonRpc } from 'jsonrpc';
import { msg, updateWhenLocaleChanges } from 'localization';
import { RouterController } from 'router-controller';

export class QuiAssistantChat extends LitElement {
    
    routerController = new RouterController(this);
    jsonRpc = new JsonRpc("devui-assistant", true);

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
        this.title = msg('Go to the assistant chat screen to continue this discussion.', { id: 'assistant-chat-title' });
    }
    
    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`
            <vaadin-button
                ?disabled=${this.disabled}
                title="${this.title}"
                @click=${(e) => this._navigateToChat(e)}>
                    <vaadin-icon icon="font-awesome-solid:comment-dots"></vaadin-icon>
                    <slot>${msg('Chat', { id: 'assistant-chat-button' })}</slot>
            </vaadin-button>
          `;
    }
    
    _navigateToChat(){
        this.jsonRpc.getLinkToChat().then(jsonRpcResponse => { 
            this._linkToChat = jsonRpcResponse.result;
            this.routerController.goToPath(this._linkToChat);
        });
    }
}

customElements.define('qui-assistant-chat', QuiAssistantChat);
