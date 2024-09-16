import { pages } from 'build-time-data';
import { LitElement, css, html } from 'lit';
import 'qwc/qwc-extension-link.js';

export class QwcLgtmCard extends LitElement {
    
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
    }

    connectedCallback() {
        super.connectedCallback();
    }

    render() {
        return html`<div class="card-content" slot="content">
            <div class="identity">
                <div class="logo">
                    <img src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPCEtLSBHZW5lcmF0b3I6IEFkb2JlIElsbHVzdHJhdG9yIDIwLjEuMCwgU1ZHIEV4cG9ydCBQbHVnLUluIC4gU1ZHIFZlcnNpb246IDYuMDAgQnVpbGQgMCkgIC0tPgo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkxheWVyXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4IgoJIHdpZHRoPSIzNTFweCIgaGVpZ2h0PSIzNjVweCIgdmlld0JveD0iMCAwIDM1MSAzNjUiIHN0eWxlPSJlbmFibGUtYmFja2dyb3VuZDpuZXcgMCAwIDM1MSAzNjU7IiB4bWw6c3BhY2U9InByZXNlcnZlIj4KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4KCS5zdDB7ZmlsbDp1cmwoI1NWR0lEXzFfKTt9Cjwvc3R5bGU+CjxnIGlkPSJMYXllcl8xXzFfIj4KPC9nPgo8bGluZWFyR3JhZGllbnQgaWQ9IlNWR0lEXzFfIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjE3NS41IiB5MT0iMzAlIiB4Mj0iMTc1LjUiIHkyPSI5OSUiPgoJPHN0b3AgIG9mZnNldD0iMCIgc3R5bGU9InN0b3AtY29sb3I6I0YwNUEyOCIvPgoJPHN0b3AgIG9mZnNldD0iMSIgc3R5bGU9InN0b3AtY29sb3I6I0ZCQ0EwQSIvPgo8L2xpbmVhckdyYWRpZW50Pgo8cGF0aCBjbGFzcz0ic3QwIiBkPSJNMzQyLDE2MS4yYy0wLjYtNi4xLTEuNi0xMy4xLTMuNi0yMC45Yy0yLTcuNy01LTE2LjItOS40LTI1Yy00LjQtOC44LTEwLjEtMTcuOS0xNy41LTI2LjgKCWMtMi45LTMuNS02LjEtNi45LTkuNS0xMC4yYzUuMS0yMC4zLTYuMi0zNy45LTYuMi0zNy45Yy0xOS41LTEuMi0zMS45LDYuMS0zNi41LDkuNGMtMC44LTAuMy0xLjUtMC43LTIuMy0xCgljLTMuMy0xLjMtNi43LTIuNi0xMC4zLTMuN2MtMy41LTEuMS03LjEtMi4xLTEwLjgtM2MtMy43LTAuOS03LjQtMS42LTExLjItMi4yYy0wLjctMC4xLTEuMy0wLjItMi0wLjMKCWMtOC41LTI3LjItMzIuOS0zOC42LTMyLjktMzguNmMtMjcuMywxNy4zLTMyLjQsNDEuNS0zMi40LDQxLjVzLTAuMSwwLjUtMC4zLDEuNGMtMS41LDAuNC0zLDAuOS00LjUsMS4zYy0yLjEsMC42LTQuMiwxLjQtNi4yLDIuMgoJYy0yLjEsMC44LTQuMSwxLjYtNi4yLDIuNWMtNC4xLDEuOC04LjIsMy44LTEyLjIsNmMtMy45LDIuMi03LjcsNC42LTExLjQsNy4xYy0wLjUtMC4yLTEtMC40LTEtMC40Yy0zNy44LTE0LjQtNzEuMywyLjktNzEuMywyLjkKCWMtMy4xLDQwLjIsMTUuMSw2NS41LDE4LjcsNzAuMWMtMC45LDIuNS0xLjcsNS0yLjUsNy41Yy0yLjgsOS4xLTQuOSwxOC40LTYuMiwyOC4xYy0wLjIsMS40LTAuNCwyLjgtMC41LDQuMgoJQzE4LjgsMTkyLjcsOC41LDIyOCw4LjUsMjI4YzI5LjEsMzMuNSw2My4xLDM1LjYsNjMuMSwzNS42YzAsMCwwLjEtMC4xLDAuMS0wLjFjNC4zLDcuNyw5LjMsMTUsMTQuOSwyMS45YzIuNCwyLjksNC44LDUuNiw3LjQsOC4zCgljLTEwLjYsMzAuNCwxLjUsNTUuNiwxLjUsNTUuNmMzMi40LDEuMiw1My43LTE0LjIsNTguMi0xNy43YzMuMiwxLjEsNi41LDIuMSw5LjgsMi45YzEwLDIuNiwyMC4yLDQuMSwzMC40LDQuNQoJYzIuNSwwLjEsNS4xLDAuMiw3LjYsMC4xbDEuMiwwbDAuOCwwbDEuNiwwbDEuNi0wLjFsMCwwLjFjMTUuMywyMS44LDQyLjEsMjQuOSw0Mi4xLDI0LjljMTkuMS0yMC4xLDIwLjItNDAuMSwyMC4yLTQ0LjRsMCwwCgljMCwwLDAtMC4xLDAtMC4zYzAtMC40LDAtMC42LDAtMC42bDAsMGMwLTAuMywwLTAuNiwwLTAuOWM0LTIuOCw3LjgtNS44LDExLjQtOS4xYzcuNi02LjksMTQuMy0xNC44LDE5LjktMjMuMwoJYzAuNS0wLjgsMS0xLjYsMS41LTIuNGMyMS42LDEuMiwzNi45LTEzLjQsMzYuOS0xMy40Yy0zLjYtMjIuNS0xNi40LTMzLjUtMTkuMS0zNS42bDAsMGMwLDAtMC4xLTAuMS0wLjMtMC4yCgljLTAuMi0wLjEtMC4yLTAuMi0wLjItMC4yYzAsMCwwLDAsMCwwYy0wLjEtMC4xLTAuMy0wLjItMC41LTAuM2MwLjEtMS40LDAuMi0yLjcsMC4zLTQuMWMwLjItMi40LDAuMi00LjksMC4yLTcuM2wwLTEuOGwwLTAuOQoJbDAtMC41YzAtMC42LDAtMC40LDAtMC42bC0wLjEtMS41bC0wLjEtMmMwLTAuNy0wLjEtMS4zLTAuMi0xLjljLTAuMS0wLjYtMC4xLTEuMy0wLjItMS45bC0wLjItMS45bC0wLjMtMS45CgljLTAuNC0yLjUtMC44LTQuOS0xLjQtNy40Yy0yLjMtOS43LTYuMS0xOC45LTExLTI3LjJjLTUtOC4zLTExLjItMTUuNi0xOC4zLTIxLjhjLTctNi4yLTE0LjktMTEuMi0yMy4xLTE0LjkKCWMtOC4zLTMuNy0xNi45LTYuMS0yNS41LTcuMmMtNC4zLTAuNi04LjYtMC44LTEyLjktMC43bC0xLjYsMGwtMC40LDBjLTAuMSwwLTAuNiwwLTAuNSwwbC0wLjcsMGwtMS42LDAuMWMtMC42LDAtMS4yLDAuMS0xLjcsMC4xCgljLTIuMiwwLjItNC40LDAuNS02LjUsMC45Yy04LjYsMS42LTE2LjcsNC43LTIzLjgsOWMtNy4xLDQuMy0xMy4zLDkuNi0xOC4zLDE1LjZjLTUsNi04LjksMTIuNy0xMS42LDE5LjZjLTIuNyw2LjktNC4yLDE0LjEtNC42LDIxCgljLTAuMSwxLjctMC4xLDMuNS0wLjEsNS4yYzAsMC40LDAsMC45LDAsMS4zbDAuMSwxLjRjMC4xLDAuOCwwLjEsMS43LDAuMiwyLjVjMC4zLDMuNSwxLDYuOSwxLjksMTAuMWMxLjksNi41LDQuOSwxMi40LDguNiwxNy40CgljMy43LDUsOC4yLDkuMSwxMi45LDEyLjRjNC43LDMuMiw5LjgsNS41LDE0LjgsN2M1LDEuNSwxMCwyLjEsMTQuNywyLjFjMC42LDAsMS4yLDAsMS43LDBjMC4zLDAsMC42LDAsMC45LDBjMC4zLDAsMC42LDAsMC45LTAuMQoJYzAuNSwwLDEtMC4xLDEuNS0wLjFjMC4xLDAsMC4zLDAsMC40LTAuMWwwLjUtMC4xYzAuMywwLDAuNi0wLjEsMC45LTAuMWMwLjYtMC4xLDEuMS0wLjIsMS43LTAuM2MwLjYtMC4xLDEuMS0wLjIsMS42LTAuNAoJYzEuMS0wLjIsMi4xLTAuNiwzLjEtMC45YzItMC43LDQtMS41LDUuNy0yLjRjMS44LTAuOSwzLjQtMiw1LTNjMC40LTAuMywwLjktMC42LDEuMy0xYzEuNi0xLjMsMS45LTMuNywwLjYtNS4zCgljLTEuMS0xLjQtMy4xLTEuOC00LjctMC45Yy0wLjQsMC4yLTAuOCwwLjQtMS4yLDAuNmMtMS40LDAuNy0yLjgsMS4zLTQuMywxLjhjLTEuNSwwLjUtMy4xLDAuOS00LjcsMS4yYy0wLjgsMC4xLTEuNiwwLjItMi41LDAuMwoJYy0wLjQsMC0wLjgsMC4xLTEuMywwLjFjLTAuNCwwLTAuOSwwLTEuMiwwYy0wLjQsMC0wLjgsMC0xLjIsMGMtMC41LDAtMSwwLTEuNS0wLjFjMCwwLTAuMywwLTAuMSwwbC0wLjIsMGwtMC4zLDAKCWMtMC4yLDAtMC41LDAtMC43LTAuMWMtMC41LTAuMS0wLjktMC4xLTEuNC0wLjJjLTMuNy0wLjUtNy40LTEuNi0xMC45LTMuMmMtMy42LTEuNi03LTMuOC0xMC4xLTYuNmMtMy4xLTIuOC01LjgtNi4xLTcuOS05LjkKCWMtMi4xLTMuOC0zLjYtOC00LjMtMTIuNGMtMC4zLTIuMi0wLjUtNC41LTAuNC02LjdjMC0wLjYsMC4xLTEuMiwwLjEtMS44YzAsMC4yLDAtMC4xLDAtMC4xbDAtMC4ybDAtMC41YzAtMC4zLDAuMS0wLjYsMC4xLTAuOQoJYzAuMS0xLjIsMC4zLTIuNCwwLjUtMy42YzEuNy05LjYsNi41LTE5LDEzLjktMjYuMWMxLjktMS44LDMuOS0zLjQsNi00LjljMi4xLTEuNSw0LjQtMi44LDYuOC0zLjljMi40LTEuMSw0LjgtMiw3LjQtMi43CgljMi41LTAuNyw1LjEtMS4xLDcuOC0xLjRjMS4zLTAuMSwyLjYtMC4yLDQtMC4yYzAuNCwwLDAuNiwwLDAuOSwwbDEuMSwwbDAuNywwYzAuMywwLDAsMCwwLjEsMGwwLjMsMGwxLjEsMC4xCgljMi45LDAuMiw1LjcsMC42LDguNSwxLjNjNS42LDEuMiwxMS4xLDMuMywxNi4yLDYuMWMxMC4yLDUuNywxOC45LDE0LjUsMjQuMiwyNS4xYzIuNyw1LjMsNC42LDExLDUuNSwxNi45YzAuMiwxLjUsMC40LDMsMC41LDQuNQoJbDAuMSwxLjFsMC4xLDEuMWMwLDAuNCwwLDAuOCwwLDEuMWMwLDAuNCwwLDAuOCwwLDEuMWwwLDFsMCwxLjFjMCwwLjctMC4xLDEuOS0wLjEsMi42Yy0wLjEsMS42LTAuMywzLjMtMC41LDQuOQoJYy0wLjIsMS42LTAuNSwzLjItMC44LDQuOGMtMC4zLDEuNi0wLjcsMy4yLTEuMSw0LjdjLTAuOCwzLjEtMS44LDYuMi0zLDkuM2MtMi40LDYtNS42LDExLjgtOS40LDE3LjEKCWMtNy43LDEwLjYtMTguMiwxOS4yLTMwLjIsMjQuN2MtNiwyLjctMTIuMyw0LjctMTguOCw1LjdjLTMuMiwwLjYtNi41LDAuOS05LjgsMWwtMC42LDBsLTAuNSwwbC0xLjEsMGwtMS42LDBsLTAuOCwwCgljMC40LDAtMC4xLDAtMC4xLDBsLTAuMywwYy0xLjgsMC0zLjUtMC4xLTUuMy0wLjNjLTctMC41LTEzLjktMS44LTIwLjctMy43Yy02LjctMS45LTEzLjItNC42LTE5LjQtNy44CgljLTEyLjMtNi42LTIzLjQtMTUuNi0zMi0yNi41Yy00LjMtNS40LTguMS0xMS4zLTExLjItMTcuNGMtMy4xLTYuMS01LjYtMTIuNi03LjQtMTkuMWMtMS44LTYuNi0yLjktMTMuMy0zLjQtMjAuMWwtMC4xLTEuM2wwLTAuMwoJbDAtMC4zbDAtMC42bDAtMS4xbDAtMC4zbDAtMC40bDAtMC44bDAtMS42bDAtMC4zYzAsMCwwLDAuMSwwLTAuMWwwLTAuNmMwLTAuOCwwLTEuNywwLTIuNWMwLjEtMy4zLDAuNC02LjgsMC44LTEwLjIKCWMwLjQtMy40LDEtNi45LDEuNy0xMC4zYzAuNy0zLjQsMS41LTYuOCwyLjUtMTAuMmMxLjktNi43LDQuMy0xMy4yLDcuMS0xOS4zYzUuNy0xMi4yLDEzLjEtMjMuMSwyMi0zMS44YzIuMi0yLjIsNC41LTQuMiw2LjktNi4yCgljMi40LTEuOSw0LjktMy43LDcuNS01LjRjMi41LTEuNyw1LjItMy4yLDcuOS00LjZjMS4zLTAuNywyLjctMS40LDQuMS0yYzAuNy0wLjMsMS40LTAuNiwyLjEtMC45YzAuNy0wLjMsMS40LTAuNiwyLjEtMC45CgljMi44LTEuMiw1LjctMi4yLDguNy0zLjFjMC43LTAuMiwxLjUtMC40LDIuMi0wLjdjMC43LTAuMiwxLjUtMC40LDIuMi0wLjZjMS41LTAuNCwzLTAuOCw0LjUtMS4xYzAuNy0wLjIsMS41LTAuMywyLjMtMC41CgljMC44LTAuMiwxLjUtMC4zLDIuMy0wLjVjMC44LTAuMSwxLjUtMC4zLDIuMy0wLjRsMS4xLTAuMmwxLjItMC4yYzAuOC0wLjEsMS41LTAuMiwyLjMtMC4zYzAuOS0wLjEsMS43LTAuMiwyLjYtMC4zCgljMC43LTAuMSwxLjktMC4yLDIuNi0wLjNjMC41LTAuMSwxLjEtMC4xLDEuNi0wLjJsMS4xLTAuMWwwLjUtMC4xbDAuNiwwYzAuOS0wLjEsMS43LTAuMSwyLjYtMC4ybDEuMy0wLjFjMCwwLDAuNSwwLDAuMSwwbDAuMywwCglsMC42LDBjMC43LDAsMS41LTAuMSwyLjItMC4xYzIuOS0wLjEsNS45LTAuMSw4LjgsMGM1LjgsMC4yLDExLjUsMC45LDE3LDEuOWMxMS4xLDIuMSwyMS41LDUuNiwzMSwxMC4zCgljOS41LDQuNiwxNy45LDEwLjMsMjUuMywxNi41YzAuNSwwLjQsMC45LDAuOCwxLjQsMS4yYzAuNCwwLjQsMC45LDAuOCwxLjMsMS4yYzAuOSwwLjgsMS43LDEuNiwyLjYsMi40YzAuOSwwLjgsMS43LDEuNiwyLjUsMi40CgljMC44LDAuOCwxLjYsMS42LDIuNCwyLjVjMy4xLDMuMyw2LDYuNiw4LjYsMTBjNS4yLDYuNyw5LjQsMTMuNSwxMi43LDE5LjljMC4yLDAuNCwwLjQsMC44LDAuNiwxLjJjMC4yLDAuNCwwLjQsMC44LDAuNiwxLjIKCWMwLjQsMC44LDAuOCwxLjYsMS4xLDIuNGMwLjQsMC44LDAuNywxLjUsMS4xLDIuM2MwLjMsMC44LDAuNywxLjUsMSwyLjNjMS4yLDMsMi40LDUuOSwzLjMsOC42YzEuNSw0LjQsMi42LDguMywzLjUsMTEuNwoJYzAuMywxLjQsMS42LDIuMywzLDIuMWMxLjUtMC4xLDIuNi0xLjMsMi42LTIuOEMzNDIuNiwxNzAuNCwzNDIuNSwxNjYuMSwzNDIsMTYxLjJ6Ii8+Cjwvc3ZnPgo="
                                       alt="LGTM" 
                                       title="LGTM"
                                       width="32" 
                                       height="32">
                </div>
                <div class="description">OpenTelemetry LGTM Stack (Loki for logs, Grafana for visualization, Tempo for traces, and Mimir for metrics)</div>
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
customElements.define('qwc-lgtm-card', QwcLgtmCard);