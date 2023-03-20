import { LitElement, html} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import { until } from 'lit/directives/until.js';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { unsafeHTML } from 'lit-html/directives/unsafe-html.js';


export class QwcSmallryeReactiveMessagingChannels extends LitElement {

    jsonRpc = new JsonRpc("SmallRyeReactiveMessaging");

    static properties = {
        "_channels": {state: true, type: Array}
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
        return html`${until(this._renderChannelTable(), html`<span>Loading channels...</span>`)}`;
    }

    _renderChannelTable() {
        if (this._channels) {
            return html`
                <vaadin-grid .items="${this._channels}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header="Channel"
                                        ${columnBodyRenderer(this._channelNameRenderer, [])}>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header="Publisher"
                                        ${columnBodyRenderer(this._channelPublisherRenderer, [])}>>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header="Subscriber(s)"
                                        ${columnBodyRenderer(this._channelSubscriberRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
        }
    }

    _channelNameRenderer(channel) {
        return html`<strong>${ channel.name }</strong>`
    }

    _channelSubscriberRenderer(channel) {
        const consumers = channel.consumers;
        if (consumers) {
            if (consumers.length === 1) {
                return this._renderComponent(consumers[0]);
            } else if (consumers.length > 1) {
                return html`
                  <ul>
                    ${consumers.map(item => html`<li>${this._renderComponent(item)}</li>`)}
                  </ul>
                `;
            } else {
                return html`<em>No subscribers</em>`
            }
        }
    }

    _channelPublisherRenderer(channel) {
        const publisher = channel.publisher;
        if (publisher) {
            return this._renderComponent(publisher);
        }
    }

    _renderComponent(component) {
        switch (component.type) {
            case "PUBLISHER":
                return html`<vaadin-icon icon="font-awesome-solid:right-from-bracket" title="publisher"></vaadin-icon> ${unsafeHTML(component.description)}`
            case "SUBSCRIBER":
                return html`<vaadin-icon icon="font-awesome-solid:right-to-bracket" title="subscriber"></vaadin-icon> ${unsafeHTML(component.description)}`
            case "PROCESSOR":
                return html`<vaadin-icon icon="font-awesome-solid:arrows-turn-to-dots" title="processor" />"></vaadin-icon> ${unsafeHTML(component.description)}`
            case "CONNECTOR":
                return html`<vaadin-icon icon="font-awesome-solid:plug" title="connector"></vaadin-icon> ${unsafeHTML(component.description)}`
            case "EMITTER":
                return html`<vaadin-icon icon="font-awesome-solid:syringe" title="emitter"></vaadin-icon> ${unsafeHTML(component.description)}`
            case "CHANNEL":
                return html`<vaadin-icon icon="font-awesome-solid:syringe" title="channel"></vaadin-icon> ${unsafeHTML(component.description)}`
        }
    }
}
customElements.define('qwc-smallrye-reactive-messaging-channels', QwcSmallryeReactiveMessagingChannels);
