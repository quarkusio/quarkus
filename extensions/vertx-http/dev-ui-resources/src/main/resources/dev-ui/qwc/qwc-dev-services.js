import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { devServices } from 'devui-data';
import '@vaadin/icon';
import 'qui-code-block';
import 'qui-card';
import 'qwc-no-data';

/**
 * This component shows the Dev Services Page
 */
export class QwcDevServices extends QwcHotReloadElement {
    static styles = css`
        .cards {
            height: 100%;
            padding-right: 10px;
            display: flex;
            flex-direction: column;
            gap: 10px;
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
    `;

    static properties = {
        _services: {state: true}
    };

    constructor() {
        super();
        this._services = devServices;
    }

    hotReload(){
        import(`devui/devui-data.js?${Date.now()}`).then(newDevUIData => {
            this._services = newDevUIData.devServices;
        });
    }

    render() {
        if (this._services && this._services.length>0) {
            return html`<div class="cards">
                            ${this._services.map(devService => this._renderCard(devService))}  
                        </div>`;
        } else {
            return html`<qwc-no-data message="You do not have any Dev Services running." 
                                    link="https://quarkus.io/guides/dev-services"
                                    linkText="Read more about Dev Services">
                </qwc-no-data>
            `;
        }
    }

    _renderCard(devService){
        return html`<qui-card title="${devService.name}">
                        <div slot="content">
                            ${this._renderContainerDetails(devService)}
                            ${this._renderConfigDetails(devService)}
                        </div>
                    </qui-card>`;
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