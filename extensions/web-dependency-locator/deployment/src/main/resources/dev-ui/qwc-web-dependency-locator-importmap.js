import {LitElement, html, css} from 'lit';
import {importMap} from 'build-time-data';

import '@quarkus-webcomponents/codeblock';
import { msg, updateWhenLocaleChanges } from 'localization';

export class QwcWebDependencyLocatorImportmap extends LitElement {

    static styles = css`
        :host{
            display: flex;
            flex-direction: column;
            gap: 15px;
            padding: 10px;
            height: 100%;
        }
    `;

    static properties = {
        _importMap: {type: String}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._importMap = importMap;
    }

    render() {
        return html`
            ${msg('To use this in your app, add this to the head of your main html:', { id: 'quarkus-web-dependency-locator-usage-instruction' })}
            <div class="codeBlock">
                <qui-code-block
                    mode='javascrip'
                    content='<script src="/_importmap/generated_importmap.js"></script>'>
                </qui-code-block>
            </div>
            
            ${msg('Here is the generated import map:', { id: 'quarkus-web-dependency-locator-generated-importmap' })}
            <div class="codeBlock">
                <qui-code-block
                    mode='json'
                    content='${this._importMap}'>
                </qui-code-block>
            </div>
        `;
    }
}

customElements.define('qwc-web-dependency-locator-importmap', QwcWebDependencyLocatorImportmap);