import {LitElement, html, css} from 'lit';

import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@vanillawc/wc-codemirror';
import '@vanillawc/wc-codemirror/mode/yaml/yaml.js';
import '@vanillawc/wc-codemirror/mode/properties/properties.js';
import '@vanillawc/wc-codemirror/mode/javascript/javascript.js';

export class QuiCodeBlock extends observeState(LitElement) {

    static styles = css``;

    static properties = {
        mode: {type: String}, // yaml / js / etc
        src: {type: String}, // src (optional)
        content: {type: String} // content (optional)
    };

    constructor() {
        super();
        this.mode = null;
        this.src = null;
        this.content = null;
    }
    
    render() {
        let currentPath = window.location.pathname;
        currentPath = currentPath.substring(0, currentPath.indexOf('/dev'));
                
        if(this.src){
            return html`<wc-codemirror mode='${this.mode}'
                                    src='${this.src}'
                                    theme='base16-${themeState.theme.name}'
                                    readonly>
                            <link rel='stylesheet' href='${currentPath}/_static/wc-codemirror/theme/base16-${themeState.theme.name}.css'>
                        </wc-codemirror>`;
        }else if(this.content){
            return html`<wc-codemirror mode='${this.mode}'
                                    theme='base16-${themeState.theme.name}'
                                    readonly>
                            <link rel='stylesheet' href='${currentPath}/_static/wc-codemirror/theme/base16-${themeState.theme.name}.css'>
                            <script type='wc-content'>${this.content}</script>
                        </wc-codemirror>`;
        }
    }

}

customElements.define('qui-code-block', QuiCodeBlock);
