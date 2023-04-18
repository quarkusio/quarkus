import {LitElement, html, css} from 'lit';
import {devServices} from 'devui-data';
import '@vaadin/vertical-layout';
import '@vaadin/icon';
import 'qui-code-block';

/**
 * This component shows the Dev Services Page
 */
export class QwcDevServices extends LitElement {
    static styles = css`
        .cards {
            height: 100%;
        }
        .card {
            display: flex;
            flex-direction: column;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: 4px;
            margin-left: 30px;
            margin-right: 30px;
        }

        .card-header {
            font-size: var(--lumo-font-size-l);
            line-height: 1;
            height: 25px;
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            padding: 10px 10px;
            background-color: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }

        .configHeader {
            padding: 10px;
        }
        .containerDetails{
            padding: 15px;
        }

        .row {
            padding: 3px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .config {
            padding-left: 10px;
            background: var(--lumo-contrast-5pct);
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
            return html`<div class="cards">
                            ${this._services.map(devService => this._renderCard(devService))}  
                        </div>`;
        } else {
            return html`<p class="no-dev-services">
                    <span>You do not have any Dev Services running.</span>
                    <a href="https://quarkus.io/guides/dev-services" target="_blank">Read more about Dev Services</a>
                </p>
            `;
        }
    }

    _renderCard(devService){
        return html`<div class="card">
                        <div class="card-header">${devService.name}</div>
                        ${this._renderContainerDetails(devService)}
                        ${this._renderConfigDetails(devService)}
                    </div>`;
    }

    _renderContainerDetails(devService){
        if (devService.containerInfo) {
            return html`
                <table class="containerDetails">
                    <tr><td>${this._getContainerName(devService)}</td></tr>
                    <tr><td>${this._getContainerImage(devService)}</td></tr>
                    <tr><td>${this._getNetwork(devService)}</td></tr>
                    <tr><td>${this._getExposedPorts(devService)}</td></tr>
                </table>`;
        }
    }

    _renderConfigDetails(devService){
        if (devService.configs) {
            const list = [];
            for (const [key, value] of Object.entries(devService.configs)) {
                list.push(key + "=" + value + "\n");
            }

            let properties = ''.concat(...list);
            return html`<span class="configHeader">Config:</span>
                        <div class="config">
                            <qui-code-block 
                                mode='properties'
                                content='${properties.trim()}'>
                            </qui-code-block>
                        </div>`;
        }
    }

    _getContainerName(devService) {
        return html`<span class="row"><vaadin-icon
                    icon="font-awesome-solid:box-open"></vaadin-icon>${devService.containerInfo.names[0]} (${devService.containerInfo.shortId})</span>`;
    }

    _getContainerImage(devService) {
        return html`<span class="row"><vaadin-icon
                    icon="font-awesome-solid:layer-group"></vaadin-icon>${devService.containerInfo.imageName}</span>`;
    }

    _getNetwork(devService) {
        if (devService.containerInfo.networks) {
            return html`<span class="row"><vaadin-icon
                    icon="font-awesome-solid:network-wired"></vaadin-icon>${devService.containerInfo.networks.join(', ')}</span>`;
        }
    }

    _getExposedPorts(devService) {
        if (devService.containerInfo.exposedPorts) {
            let ports = devService.containerInfo.exposedPorts;
            
            const p = ports
                .map(p => p.ip + ":" + p.publicPort + "->" + p.privatePort + "/" + p.type)
                .join(', ');

            return html`<span class="row"><vaadin-icon icon="font-awesome-solid:diagram-project"></vaadin-icon>${p}</span>`;
        }
    }

}

customElements.define('qwc-dev-services', QwcDevServices);