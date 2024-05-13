import {LitElement, html, css} from 'lit';
import {webDependencyLibraries} from 'build-time-data';
import '@vaadin/tabs';
import '@vaadin/grid';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid/vaadin-grid-tree-column.js';
import {notifier} from 'notifier';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';


export class QwcWebDependencyLocatorLibraries extends LitElement {

    static styles = css`
        :host {
            display: flex;
            height: 100%;
            padding: 10px;
            gap: 20px;
        }
        .full-height {
            height: 100%;
        }
        .tabcontent {
            height: 100%;
            width: 100%;
        }
    `;

    static properties = {
        _webDependencyLibraries: {},
        _selectedWebDependency: {state: true}
    };

    constructor() {
        super();
        this._webDependencyLibraries = webDependencyLibraries;
        this._selectedWebDependency = this._webDependencyLibraries[0];
    }

    render() {
        return html`
                <vaadin-tabs @selected-changed="${this._tabSelectedChanged}" orientation="vertical">
                    ${this._webDependencyLibraries.map(webDependency => html`
                        <vaadin-tab id="${webDependency.webDependencyName}">
                            ${webDependency.webDependencyName + " (" + webDependency.version + ")"}
                        </vaadin-tab>`)}
                </vaadin-tabs>
                ${this._renderLibraryAssets(this._selectedWebDependency)}
        `;
    }

    _tabSelectedChanged(e){
        this._selectedWebDependency = this._webDependencyLibraries[e.detail.value];
    }

    _renderLibraryAssets(library) {
        const dataProvider = function (params, callback) {
            if (params.parentItem === undefined) {
                callback(library.rootAsset.children, library.rootAsset.children.length);
            } else {
                callback(params.parentItem.children, params.parentItem.children.length)
            }
        };

        return html`
            <div tab="${library.webDependencyName}" class="tabcontent">
                <vaadin-grid .itemHasChildrenPath="${'children'}" .dataProvider="${dataProvider}"
                             theme="compact no-border" class="full-height">
                    <vaadin-grid-tree-column path="name"></vaadin-grid-tree-column>
                    <vaadin-grid-column width="5em" header="Copy link" flex-grow="0"
                                        ${columnBodyRenderer(this._assetCopyRenderer, [])}></vaadin-grid-column>
                    <vaadin-grid-column width="6em" header="Open asset" flex-grow="0"
                                        ${columnBodyRenderer(this._assetLinkRenderer, [])}></vaadin-grid-column>
                </vaadin-grid>
            </div>`;
    }

    _assetLinkRenderer(item) {
        if (item.fileAsset) {
            return html`<a href="${item.urlPart}" target="_blank" style="color: var(--lumo-contrast-80pct);">
                <vaadin-icon style="font-size: small;" icon="font-awesome-solid:up-right-from-square" role="link"
                             title="Open link to ${item.name} in a new tab">
                </vaadin-icon>
            </a>`;
        } else {
            return html``;
        }
    }

    _assetCopyRenderer(item) {
        if (item.fileAsset) {
            return html`
                <vaadin-button theme="icon" style="margin: 0; height: 2em;" 
                               @click=${() => {this._onCopyLinkClick(item)}}
                               aria-label="Copy link to ${item.name} to clipboard"
                               title="Copy link to ${item.name} to clipboard">
                    <vaadin-icon style="font-size: small; cursor: pointer" icon="font-awesome-regular:copy"
                                 role="button">
                    </vaadin-icon>
                </vaadin-button>`;
        } else {
            return html``;
        }
    }

    _onCopyLinkClick(item) {
        navigator.clipboard.writeText(item.urlPart);
        notifier.showInfoMessage('URL for ' + item.name + ' copied to clipboard', 'top-end');
    }

}

customElements.define('qwc-web-dependency-locator-libraries', QwcWebDependencyLocatorLibraries)