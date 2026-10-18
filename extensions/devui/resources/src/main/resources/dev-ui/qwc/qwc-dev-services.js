import {css, html, QwcHotReloadElement} from 'qwc-hot-reload-element';
import {JsonRpc} from 'jsonrpc';
import {devServices} from 'devui-data';
import '@vaadin/icon';
import 'qui-themed-code-block';
import '@qomponent/qui-card';
import 'qwc-no-data';
import { notifier } from 'notifier';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Dev Services Page
 */
export class QwcDevServices extends QwcHotReloadElement {
    jsonRpc = new JsonRpc("devui-dev-services", false);

    static styles = css`
        .cards {
            height: 100%;
            padding-right: 10px;
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        .configHeader {
            padding: 10px 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            color: var(--lumo-contrast-60pct);
            font-size: var(--lumo-font-size-s);
        }
        .copyButtons {
            display: flex;
            gap: 5px;
            color: var(--lumo-contrast-40pct);
            align-items: center;
        }
        qui-badge{
            cursor: pointer;
        }

        .containerDetails {
            padding: 8px 16px;
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .row {
            padding: 5px 10px;
            display: flex;
            align-items: center;
            gap: 10px;
            border-radius: 6px;
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-contrast-80pct);
        }

        .row:hover {
            background-color: var(--lumo-contrast-5pct);
        }

        .row vaadin-icon {
            color: var(--lumo-contrast-50pct);
            --vaadin-icon-size: var(--lumo-font-size-m);
            flex-shrink: 0;
        }

        .config {
            padding: 4px 16px 12px 16px;
            margin: 0 12px 12px 12px;
            background: var(--lumo-contrast-5pct);
            border-radius: 8px;
        }

        .content {
            padding: 12px 0;
        }

        .description {
            padding: 0 16px 10px 16px;
            color: var(--lumo-contrast-50pct);
        }
    `;

    static properties = {
        _services: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._services = devServices;
    }

    connectedCallback() {
        super.connectedCallback();

        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getDevServices().then(jsonRpcResponse => {
            this._services = jsonRpcResponse.result;
        });
    }

    render() {
        if (this._services && this._services.length>0) {
            return html`<div class="cards">
                            ${this._services.map(devService => this._renderCard(devService))}
                        </div>`;
        } else {
            return html`<qwc-no-data message="${msg('You do not have any Dev Services running.', { id: 'devservice-no-services' })}"
                                    link="https://quarkus.io/guides/dev-services"
                                    linkText="${msg('Read more about Dev Services', { id: 'devservice-read-more' })}">
                </qwc-no-data>
            `;
        }
    }

    _renderCard(devService){
        return html`<qui-card header="${devService.name}">
                        <div slot="content" class="content">
                            ${this._renderDescription(devService)}
                            ${this._renderContainerDetails(devService)}
                            ${this._renderConfigDetails(devService)}
                        </div>
                    </qui-card>`;
    }

    _renderDescription(devService){
        if(devService.description){
            return html`<div class="description">${devService.description}</div>`;
        }
    }

    _renderContainerDetails(devService){
        if (devService.containerInfo) {
            return html`
                <div class="containerDetails">
                    ${this._getContainerName(devService)}
                    ${this._getContainerImage(devService)}
                    ${this._getNetwork(devService)}
                    ${this._getExposedPorts(devService)}
                </div>`;
        }
    }

    _renderConfigDetails(devService){
        if (devService.configs) {
            let properties = this._configToText(devService);
            return html`<div class="configHeader">${msg('Config', { id: 'devservice-config' })}:
                            <div class="copyButtons">
                                ${msg('Make a copy for', { id: 'devservice-make-copy-for' })}:
                                <qui-badge
                                    title="${msg('Copy config for test environment', { id: 'devservice-copy-config-test' })}"
                                    @click="${() => this._copyForTest(devService)}">
                                        <span>${msg('Test', { id: 'devservice-test' })}</span>
                                </qui-badge>
                                <qui-badge
                                    title="${msg('Copy config for prod environment', { id: 'devservice-copy-config-prod' })}"
                                    @click="${() => this._copyForProd(devService)}">
                                        <span>${msg('Prod', { id: 'devservice-prod' })}</span>
                                </qui-badge>
                            </div>
                        </div>
                        <div class="config">
                            <qui-themed-code-block
                                mode='properties'
                                content='${properties}'>
                            </qui-themed-code-block>
                        </div>`;
        }
    }

    _getContainerName(devService) {
        return html`<div class="row"><vaadin-icon
                    icon="font-awesome-solid:box-open"></vaadin-icon>${devService.containerInfo.names[0]} (${devService.containerInfo.shortId})</div>`;
    }

    _getContainerImage(devService) {
        return html`<div class="row"><vaadin-icon
                    icon="font-awesome-solid:layer-group"></vaadin-icon>${devService.containerInfo.imageName}</div>`;
    }

    _getNetwork(devService) {
        if (devService.containerInfo.networks) {
            const networks = Object.entries(devService.containerInfo.networks)
                .map(([key, aliases]) => {
                    if (!aliases || aliases.length === 0) {
                        return key;
                    }
                    return `${key} (${aliases.join(", ")})`;
                })
                .join(", ");
            return html`<div class="row"><vaadin-icon
                    icon="font-awesome-solid:network-wired"></vaadin-icon>${networks}</div>`;
        }
    }

    _getExposedPorts(devService) {
        if (devService.containerInfo.exposedPorts) {
            let ports = devService.containerInfo.exposedPorts;

            const p = ports
                .filter(p => p.publicPort !== null)
                .map(p => p.ip + ":" + p.publicPort + "->" + p.privatePort + "/" + p.type)
                .join(', ');

            return html`<div class="row"><vaadin-icon icon="font-awesome-solid:diagram-project"></vaadin-icon>${p}</div>`;
        }
    }

    _copyForTest(devService){
        let properties = this._configToText(devService, "%test.");
        this._copyToClipboard(properties);
    }

    _copyForProd(devService){
        let properties = this._configToText(devService, "%prod.");
        this._copyToClipboard(properties);
    }

    _configToText(devService, pre = ""){
        const list = [];
        for (const [key, value] of Object.entries(devService.configs)) {
            list.push(pre + key + "=" + value + "\n");
        }

        return ''.concat(...list).trim();
    }

    _copyToClipboard(text) {
        navigator.clipboard.writeText(text)
            .then(() => {
                notifier.showInfoMessage(msg('Copied to clipboard', { id: 'devservice-copied' }));
            })
            .catch(err => {
                notifier.showErrorMessage(msg('Clipboard write failed', { id: 'devservice-copy-failed' }) + " [" + err + "]");
            });
    }

}

customElements.define('qwc-dev-services', QwcDevServices);
