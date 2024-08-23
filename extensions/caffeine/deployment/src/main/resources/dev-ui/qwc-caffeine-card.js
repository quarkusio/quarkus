import { LitElement, html, css} from 'lit';
import { pages } from 'build-time-data';
import 'qwc/qwc-extension-link.js';

const NAME = "Caffeine";
export class QwcCaffeineCard extends LitElement {

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
        padding: 10px 10px;
        height: 100%;
      }
      .card-content slot {
        display: flex;
        flex-flow: column wrap;
        padding-top: 5px;
      }
    `;

    static properties = {
        description: {type: String}
    };

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<div class="card-content" slot="content">
            <div class="identity">
                <div class="logo">
                    <img src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPCEtLSBHZW5lcmF0b3I6IEFkb2JlIElsbHVzdHJhdG9yIDIxLjAuMCwgU1ZHIEV4cG9ydCBQbHVnLUluIC4gU1ZHIFZlcnNpb246IDYuMDAgQnVpbGQgMCkgIC0tPgo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9ItCh0LvQvtC5XzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4IgoJIHZpZXdCb3g9IjAgMCAyODEuOCAyNzMuOCIgc3R5bGU9ImVuYWJsZS1iYWNrZ3JvdW5kOm5ldyAwIDAgMjgxLjggMjczLjg7IiB4bWw6c3BhY2U9InByZXNlcnZlIiBmaWxsPSJ3aGl0ZSI+CjxnPgoJPHJlY3QgeD0iMTg2LjciIHk9IjEyMS42IiB3aWR0aD0iMi40IiBoZWlnaHQ9IjQ1LjYiLz4KCTxyZWN0IHg9IjQzLjQiIHk9Ijc3LjIiIHRyYW5zZm9ybT0ibWF0cml4KDAuNTAwNCAtMC44NjU4IDAuODY1OCAwLjUwMDQgLTYxLjI0NTQgODYuNzc3NSkiIHdpZHRoPSIyLjQiIGhlaWdodD0iMzguNSIvPgoJPHJlY3QgeD0iMTI2LjUiIHk9IjIyMi4zIiB3aWR0aD0iMi40IiBoZWlnaHQ9IjM3LjQiLz4KCTxwb2x5Z29uIHBvaW50cz0iMjY3LjEsMTQ1LjEgMjY3LjcsMTQ0LjQgMjY3LjEsMTQzLjcgMjY3LjEsMTQzLjcgMjY2LjUsMTQyLjggMjY2LDE0Mi4xIDI2NiwxNDIuMSAyNDcuMSwxMTYuMSAyNDUuMiwxMTcuNSAKCQkyNjQsMTQzLjUgMjY0LDE0My41IDI2NC40LDE0NCAyNjQuNywxNDQuNCAyNDUuOSwxNzAuMyAyNDcuOCwxNzEuNyAyNjYsMTQ2LjcgMjY2LDE0Ni43IDI2NiwxNDYuNiAyNjcuMSwxNDUuMSAJIi8+Cgk8cmVjdCB4PSIyMzIuOSIgeT0iMTUyLjkiIHRyYW5zZm9ybT0ibWF0cml4KDAuNTg3NiAtMC44MDkxIDAuODA5MSAwLjU4NzYgLTIzLjA2MjQgMjYyLjg2NTgpIiB3aWR0aD0iMjYuOSIgaGVpZ2h0PSIyLjQiLz4KCTxwb2x5Z29uIHBvaW50cz0iMjE0LjksMTAyLjIgMTc3LjcsMTE0LjMgMTM0LDg5LjEgMTM0LDUwLjIgMTMxLjcsNTAuMiAxMzEuNyw4Ny43IDEyOC41LDg1LjkgMTI3LjcsODUuNCAxMjYuOCw4NS45IDEyMy43LDg3LjcgCgkJMTIzLjcsNTAuMiAxMjEuMyw1MC4yIDEyMS4zLDg5LjEgOTMuNywxMDUuMSA5NC45LDEwNy4xIDEyNi41LDg4LjkgMTI2LjUsODguOSAxMjcuNyw4OC4yIDEyOC44LDg4LjkgMTI4LjgsODguOCAxNzYuMywxMTYuMyAKCQkxNzYuMywxNzIuNSAxNDMuNiwxOTEuNSAxNDQuOCwxOTMuNSAxNzcuNywxNzQuNSAyMTQuOSwxODYuNiAyMTUuNiwxODQuMyAxNzguNywxNzIuMyAxNzguNywxMTYuNSAyMTUuNiwxMDQuNSAJIi8+Cgk8cmVjdCB4PSIyMjYuOCIgeT0iNTguOCIgdHJhbnNmb3JtPSJtYXRyaXgoMC4zMDk0IC0wLjk1MDkgMC45NTA5IDAuMzA5NCAxMTEuODk3MyAyNzQuMDMyNSkiIHdpZHRoPSIzNS43IiBoZWlnaHQ9IjIuNCIvPgoJPHBvbHlnb24gcG9pbnRzPSI3MC40LDEwNy4xIDcwLjUsMTA3LjEgODQuOSwxMjkuNSA4OC43LDEyOS41IDg4LjcsMTAwLjggODUuMSwxMDAuOCA4NS4xLDEyMy4xIDg1LjEsMTIzLjEgNzAuNywxMDAuOCA2Ni44LDEwMC44IAoJCTY2LjgsMTI5LjUgNzAuNCwxMjkuNSAJIi8+Cgk8cG9seWdvbiBwb2ludHM9Ijc5LDE3MS4yIDc5LDEzNS45IDc2LjYsMTM1LjkgNzYuNiwxNjYuNSA0Mi42LDE4Ni4xIDQzLjksMTg4LjIgNzYuNiwxNjkuMyA3Ni42LDE3MS4yIDc2LjYsMTczLjIgNzYuNiwxNzMuOSAKCQk3OC45LDE3NS4zIDc4LjksMTc1LjMgODAuNiwxNzYuMiA0Ny44LDE5NS4xIDQ5LDE5Ny4yIDgzLDE3Ny42IDExMC41LDE5My41IDExMS43LDE5MS41IDc5LDE3Mi41IAkiLz4KCTxwb2x5Z29uIHBvaW50cz0iMTM1LjEsMjA5LjUgMTM1LDIwOS41IDEyMC42LDE4Ny4yIDExNi43LDE4Ny4yIDExNi43LDIxNS45IDEyMC4zLDIxNS45IDEyMC4zLDE5My41IDEyMC4zLDE5My41IDEzNC43LDIxNS45IAoJCTEzOC42LDIxNS45IDEzOC42LDE4Ny4yIDEzNS4xLDE4Ny4yIAkiLz4KCTxwYXRoIGQ9Ik0zNC45LDE4OC44Yy0zLjEtMS44LTctMS44LTctMS44Yy0zLjksMC03LDEuOC03LDEuOGMtMy4yLDEuOC00LjksNS4yLTQuOSw1LjJjLTEuOCwzLjQtMS44LDcuOC0xLjgsNy44CgkJYzAsNC4zLDEuOCw3LjcsMS44LDcuN2MxLjgsMy40LDQuOSw1LjIsNC45LDUuMmMzLjEsMS44LDcuMSwxLjgsNy4xLDEuOGMzLjksMCw3LTEuOCw3LTEuOGMzLjEtMS44LDQuOS01LjIsNC45LTUuMgoJCWMxLjgtMy40LDEuOC03LjgsMS44LTcuOGMwLTQuNS0xLjgtNy44LTEuOC03LjhDMzguMSwxOTAuNiwzNC45LDE4OC44LDM0LjksMTg4Ljh6IE0zNi40LDIwOGMtMS4yLDIuNi0zLjUsNC0zLjUsNAoJCWMtMi4yLDEuNC01LDEuNC01LDEuNGMtMi44LDAtNS4xLTEuNC01LjEtMS40Yy0yLjItMS40LTMuNC00LTMuNC00Yy0xLjMtMi42LTEuMy02LjEtMS4zLTYuMWMwLTMuNiwxLjItNi4zLDEuMi02LjMKCQljMS4yLTIuNiwzLjQtNCwzLjQtNGMyLjItMS40LDUuMS0xLjQsNS4xLTEuNGMyLjksMCw1LjEsMS40LDUuMSwxLjRjMi4yLDEuNCwzLjQsNCwzLjQsNGMxLjIsMi42LDEuMiw2LjIsMS4yLDYuMgoJCUMzNy43LDIwNS40LDM2LjQsMjA4LDM2LjQsMjA4eiIvPgoJPHBhdGggZD0iTTEyMC42LDQxLjljMy4yLDEuOCw3LjEsMS44LDcuMSwxLjhjMy45LDAsNy0xLjgsNy0xLjhjMy4xLTEuOCw0LjktNS4yLDQuOS01LjJjMS44LTMuNCwxLjgtNy44LDEuOC03LjgKCQljMC00LjUtMS44LTcuOC0xLjgtNy44Yy0xLjgtMy4zLTQuOS01LjItNC45LTUuMmMtMy4xLTEuOC03LTEuOC03LTEuOGMtMy45LDAtNywxLjgtNywxLjhjLTMuMiwxLjgtNC45LDUuMi00LjksNS4yCgkJYy0xLjgsMy40LTEuOCw3LjgtMS44LDcuOGMwLDQuMywxLjgsNy43LDEuOCw3LjdDMTE3LjUsNDAuMSwxMjAuNiw0MS45LDEyMC42LDQxLjl6IE0xMTkuMSwyMi44YzEuMi0yLjYsMy41LTQsMy41LTQKCQljMi4yLTEuNCw1LjEtMS40LDUuMS0xLjRjMi45LDAsNS4xLDEuNCw1LjEsMS40YzIuMiwxLjQsMy41LDQsMy41LDRjMS4yLDIuNiwxLjIsNi4yLDEuMiw2LjJjMCwzLjYtMS4yLDYuMi0xLjIsNi4yCgkJYy0xLjIsMi42LTMuNSw0LTMuNSw0Yy0yLjIsMS40LTUsMS40LTUsMS40Yy0yLjgsMC01LTEuNC01LTEuNGMtMi4yLTEuNC0zLjUtNC0zLjUtNGMtMS4yLTIuNi0xLjItNi4xLTEuMi02LjEKCQlDMTE3LjksMjUuNSwxMTkuMSwyMi44LDExOS4xLDIyLjh6Ii8+Cgk8cG9seWdvbiBwb2ludHM9IjIzOS43LDE5OC41IDIzOS42LDE5OC41IDIyNS4zLDE3Ni4yIDIyMS4zLDE3Ni4yIDIyMS4zLDIwNC45IDIyNC45LDIwNC45IDIyNC45LDE4Mi41IDIyNSwxODIuNSAyMzkuNCwyMDQuOSAKCQkyNDMuMywyMDQuOSAyNDMuMywxNzYuMiAyMzkuNywxNzYuMiAJIi8+Cgk8cG9seWdvbiBwb2ludHM9IjIyNC45LDg5LjMgMjI1LDg5LjMgMjM5LjQsMTExLjcgMjQzLjMsMTExLjcgMjQzLjMsODMgMjM5LjcsODMgMjM5LjcsMTA1LjMgMjM5LjYsMTA1LjMgMjI1LjMsODMgMjIxLjMsODMgCgkJMjIxLjMsMTExLjcgMjI0LjksMTExLjcgCSIvPgo8L2c+Cjwvc3ZnPgo="
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
                                extensionName="${NAME}"
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
customElements.define('qwc-caffeine-card', QwcCaffeineCard);