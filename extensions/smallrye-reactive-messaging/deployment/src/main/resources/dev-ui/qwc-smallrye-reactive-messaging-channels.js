import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { unsafeHTML } from 'lit-html/directives/unsafe-html.js';


export class QwcSmallryeReactiveMessagingChannels extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .datatable {
            height: 100%;
            padding-bottom: 10px;
        }
    
        .smaller {
            font-size: var(--lumo-font-size-s);
        }
    `;

    static properties = {
        _channels: {state: true, type: Array}
    }

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getInfo().then(jsonRpcResponse => {
            this._channels = [];
            jsonRpcResponse.result.forEach(c => {
                this._channels.push(c);
            });
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        if (this._channels) {
            return this._renderChannelTable();
        } else {
            return html`<span>Loading channels...</span>`;
        }
    }

    _renderChannelTable() {
        return html`
                <vaadin-grid .items="${this._channels}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header="Channel"
                                        ${columnBodyRenderer(this._channelNameRenderer, [])}>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header="Publisher(s)"
                                        ${columnBodyRenderer(this._channelPublisherRenderer, [])}>>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header="Subscriber(s)"
                                        ${columnBodyRenderer(this._channelSubscriberRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

    _channelNameRenderer(channel) {
        return html`${ channel.name }`
    }

    _channelSubscriberRenderer(channel) {
        const consumers = channel.consumers;
        if (consumers) {
            if (consumers.length === 1) {
                return this._renderComponent(consumers[0]);
            } else if (consumers.length > 1) {
                return html`
                  <ul class="smaller">
                    ${consumers.map(item => html`<li>${this._renderComponent(item)}</li>`)}
                  </ul>
                `;
            } else {
                return html`<em>No subscribers</em>`
            }
        }
    }

    _channelPublisherRenderer(channel) {
        const publishers = channel.publishers;
        if (publishers) {
            if (publishers.length === 1) {
                return this._renderComponent(publishers[0]);
            } else if (publishers.length > 1) {
                return html`
                  <ul class="smaller">
                    ${publishers.map(item => html`<li>${this._renderComponent(item)}</li>`)}
                  </ul>
                `;
            } else {
                return html`<em>No publishers</em>`
            }
        }
    }

    _renderComponent(component) {
        switch (component.type) {
            case "PUBLISHER":
                return html`<span class="smaller"><vaadin-icon icon="font-awesome-solid:right-from-bracket" title="publisher"></vaadin-icon> ${unsafeHTML(component.description)}</span>`;
            case "SUBSCRIBER":
                return html`<span class="smaller"><vaadin-icon icon="font-awesome-solid:right-to-bracket" title="subscriber"></vaadin-icon> ${unsafeHTML(component.description)}</span>`;
            case "PROCESSOR":
                return html`<span class="smaller"><vaadin-icon icon="font-awesome-solid:arrows-turn-to-dots" title="processor"></vaadin-icon> ${unsafeHTML(component.description)}</span>`;
            case "CONNECTOR":
                return html`<span class="smaller"><vaadin-icon icon="font-awesome-solid:plug" title="connector"></vaadin-icon> ${unsafeHTML(component.description)}</span>`;
            case "EMITTER":
                return html`<span class="smaller"><vaadin-icon icon="font-awesome-solid:syringe" title="emitter"></vaadin-icon> ${unsafeHTML(component.description)}</span>`;
            case "CHANNEL":
                return html`<span class="smaller"><vaadin-icon icon="font-awesome-solid:syringe" title="channel"></vaadin-icon> ${unsafeHTML(component.description)}</span>`;
        }
    }
}
customElements.define('qwc-smallrye-reactive-messaging-channels', QwcSmallryeReactiveMessagingChannels);
