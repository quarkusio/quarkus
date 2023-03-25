import {LitElement, html, css} from 'lit';
import {devServices} from 'devui-data';
import '@vaadin/details';
import '@vaadin/vertical-layout';
import '@vaadin/icon';

/**
 * This component shows the Dev Services Page
 */
export class QwcDevServices extends LitElement {
    static styles = css`
      vaadin-icon {
        margin-right: 1em;
        margin-left: 0.5em;
      }

      vaadin-details {
        margin-bottom: 2em;
      }

      .dev-service-config {
        margin-left: 2em;
      }
      .no-dev-services {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;
        border: 1px solid var(--lumo-contrast-20pct);
        border-radius: 9px;
        padding: 30px;
        margin: 30px;
      }
      .no-dev-services a {
        color: var(--lumo-contrast-90pct);
      }
    `;


    static properties = {
        _services: {state: true}
    };

    constructor() {
        super();
        this._services = devServices;
    }

    render() {
        if (this._services && this._services.length>0) {
            const items = [];
            for (let i = 0; i < this._services.length; i++) {
                const ds = this._services[i];
                const item = html`
                    <vaadin-details opened theme="filled">
                        <div slot="summary">${ds.name}</div>
                        <vaadin-vertical-layout>
                            ${this._getContainerName(ds)}
                            ${this._getContainerImage(ds)}
                            ${this._getNetwork(ds)}
                            ${this._getExposedPorts(ds)}
                            <span><vaadin-icon icon="font-awesome-solid:gear"></vaadin-icon>Config:</span>
                            <div class="dev-service-config">
                            ${this._getConfig(ds)}
                            </div>
                        </vaadin-vertical-layout>
                    </vaadin-details>
                `
                items.push(item);
            }

            return html`${items}`;
        } else {
            return html`<p class="no-dev-services">
                    <span>You do not have any Dev Services running</span>
                    <a href="https://quarkus.io/guides/dev-services" target="_blank">Read more about Dev Services</a>
                </p>
            `
        }
    }

    _getContainerName(devService) {
        if (devService.containerInfo) {
            return html`<span><vaadin-icon
                    icon="font-awesome-solid:box-open"></vaadin-icon>${devService.containerInfo.names[0]} (${devService.containerInfo.shortId})</span>`;
        }
    }

    _getContainerImage(devService) {
        if (devService.containerInfo) {
            return html`<span><vaadin-icon
                    icon="font-awesome-solid:layer-group"></vaadin-icon>${devService.containerInfo.imageName}</span>`;
        }
    }

    _getNetwork(devService) {
        if (devService.containerInfo) {
            if (devService.containerInfo.networks) {
                return html`<span><vaadin-icon
                        icon="font-awesome-solid:network-wired"></vaadin-icon>${devService.containerInfo.networks.join(', ')}</span>`;
            }
        }
    }

    _getExposedPorts(devService) {
        if (devService.containerInfo) {
            if (devService.containerInfo.exposedPorts) {
                let ports = devService.containerInfo.exposedPorts;
                const p = ports
                    .filter(p => p.ip)
                    .map(p => p.ip + ":" + p.publicPort + "->" + p.privatePort + "/" + p.type)
                    .join(', ');
                return html`<span><vaadin-icon icon="font-awesome-solid:diagram-project"></vaadin-icon>${p}</span>`;
            }
        }
    }

    _getConfig(devService) {
        if (devService.configs) {
            const list = [];
            for (const [key, value] of Object.entries(devService.configs)) {
                list.push(html`<li><code>${key}</code>=<code>${value}</code></li>`);
            }
            return html`${list}`;
        }
    }

}

customElements.define('qwc-dev-services', QwcDevServices);