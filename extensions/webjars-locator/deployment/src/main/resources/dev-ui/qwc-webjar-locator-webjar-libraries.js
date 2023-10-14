import {LitElement, html, css} from 'lit';
import {webJarLibraries} from 'build-time-data';
import '@vaadin/tabsheet';
import '@vaadin/tabs';
import '@vaadin/grid';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid/vaadin-grid-tree-column.js';
import {notifier} from 'notifier';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';


export class QwcWebjarLocatorWebjarLibraries extends LitElement {

    static styles = css`
        .full-height {
            height: 100%;
        }
    `;

    static properties = {
        _webJarLibraries: {},
    };

    constructor() {
        super();
        this._webJarLibraries = webJarLibraries;
    }

    render() {
        return html`
            <vaadin-tabsheet class="full-height">
                <vaadin-tabs slot="tabs">
                    ${this._webJarLibraries.map(webjar => html`
                        <vaadin-tab id="${webjar.webJarName}">
                            ${webjar.webJarName + " (" + webjar.version + ")"}
                        </vaadin-tab>`)}
                </vaadin-tabs>

                ${this._webJarLibraries.map(webjar => this._renderLibraryAssets(webjar))}


            </vaadin-tabsheet>
        `;
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
            <div tab="${library.webJarName}" class="full-height">
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

customElements.define('qwc-webjar-locator-webjar-libraries', QwcWebjarLocatorWebjarLibraries)