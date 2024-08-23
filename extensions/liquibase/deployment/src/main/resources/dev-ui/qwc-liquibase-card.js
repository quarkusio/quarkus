import { LitElement, html, css} from 'lit';
import { pages } from 'build-time-data';
import 'qwc/qwc-extension-link.js';

const NAME = "Liquibase";
export class QwcLiquibaseCard extends LitElement {

    static styles = css`
      .identity {
        display: flex;
        justify-content: flex-start;
      }
      .description {
        padding-bottom: 10px;
      }
      .logo {
        padding-bottom: 10px;
        margin-right: 5px;
      }
      .card-content {
        color: var(--lumo-contrast-90pct);
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
        padding: 2px 2px;
        height: 100%;
      }
      .card-content slot {
        display: flex;
        flex-flow: column wrap;
        padding-top: 5px;
      }
    `;

    static properties = {
        extensionName: {attribute: true},
        description: {attribute: true},
        guide: {attribute: true},
        namespace: {attribute: true},
    };

    constructor() {
        super();
        if(!this.extensionName){
            this.extensionName = NAME;
        }
    }

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<div class="card-content" slot="content">
            <div class="identity">
                <div class="logo">
                    <img src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayIgdmVyc2lvbj0iMS4xIiBpZD0iTGF5ZXJfMSIgeD0iMHB4IiB5PSIwcHgiIHZpZXdCb3g9IjUwMCAyMDAgNTAwIDEwMCIgc3R5bGU9ImVuYWJsZS1iYWNrZ3JvdW5kOm5ldyAwIDAgMTIwMCA2MDA7IiB3aWR0aD0iNjRweCIgaGVpZ2h0PSI2NHB4IiB4bWw6c3BhY2U9InByZXNlcnZlIj4NCgk8c3R5bGUgdHlwZT0idGV4dC9jc3MiPiAuc3Qwe2ZpbGw6IzI5NjJGRjt9IDwvc3R5bGU+DQoJCTxnPg0KCQkJPGVsbGlwc2UgY2xhc3M9InN0MCIgY3g9Ijc1MC4zIiBjeT0iNzIuNiIgcng9IjE4MC45IiByeT0iNzIuNiIvPg0KCQkJPHBhdGggY2xhc3M9InN0MCIgZD0iTTc1NC40LDM5MS4zQzY5Ni42LDQwNS4xLDYzNyw0MTkuNCw1OTksNDQ0LjZjLTE5LTExLjgtMjkuNy0yNS40LTI5LjctMzguNGMwLTU3LjQsOTUuOS03OS4xLDE4OC42LTEwMC4xIGM2Ni44LTE1LjEsMTM1LTMwLjYsMTczLjItNjEuN3Y1MC42QzkzMS4yLDM0OC45LDg0MS4zLDM3MC41LDc1NC40LDM5MS4zeiIvPg0KCQkJPHBhdGggY2xhc3M9InN0MCIgZD0iTTc1MS4yLDI3Ni4xYy03MC40LDE1LjktMTQyLjMsMzIuMi0xODEuOSw2Ni4ydi0xMS4yYzAtMTAwLDEwOS43LTEyNS42LDIxNS44LTE1MC41IGM3Ny4xLTE4LDExNC42LTI3LjUsMTQ2LjEtNTIuOHY1NkM5MzEuMiwyMzUuNCw4MzkuNywyNTYuMSw3NTEuMiwyNzYuMXoiLz4NCgkJCTxwYXRoIGNsYXNzPSJzdDAiIGQ9Ik05MzEuMiwzNTcuOGMtMzcuNiwzMS43LTEwNC4zLDQ3LjgtMTY5LjYsNjMuNGMtNDYuOCwxMS4yLTk0LjcsMjIuNy0xMjguNiwzOS40IGMzMC43LDExLDcwLjksMTguMiwxMTcuMywxOC4yYzEwNS41LDAsMTgwLjktMzcuOCwxODAuOS03Mi42VjM1Ny44eiIvPg0KCQk8L2c+DQo8L3N2Zz4g"
                                       alt="${NAME}" 
                                       title="${NAME}"
                                       width="32" 
                                       height="32">
                </div>
                <div class="description">${this.description}</div>
            </div>
            ${this._renderCardLinks()}
        </div>
        `;
    }

    _renderCardLinks(){
        return html`${pages.map(page => html`
                            <qwc-extension-link slot="link"
                                namespace="${this.namespace}"
                                extensionName="${this.extensionName}"
                                iconName="${page.icon}"
                                displayName="${page.title}"
                                staticLabel="${page.staticLabel}"
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                path="${page.id}"
                                ?embed=${page.embed}
                                externalUrl="${page.metadata.externalUrl}"
                                webcomponent="${page.componentLink}" >
                            </qwc-extension-link>
                        `)}`;
    }

}
customElements.define('qwc-liquibase-card', QwcLiquibaseCard);