import { LitElement, html, css } from 'lit';
import { nothing } from 'lit';
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
        content: {type: String}, // content (optional),
        value: {type: String, reflect: true }, // up to date value
        editable: {type: Boolean} // readonly
    };

    constructor() {
        super();
        this.mode = null;
        this.src = null;
        this.content = null;
        this.value = null;
        this.editable = false;
    }
    
    render() {
        let currentPath = window.location.pathname;
        currentPath = currentPath.substring(0, currentPath.indexOf('/dev'));
        
        return html`<wc-codemirror id="code" mode='${this.mode}'
                                src='${this.src || nothing}'
                                theme='base16-${themeState.theme.name}'
                                ?readonly=${!this.editable}
                                @keyup=${this._persistValue}>
                        <link rel='stylesheet' href='${currentPath}/_static/wc-codemirror/theme/base16-${themeState.theme.name}.css'>
                        ${this._renderContent()}
                    </wc-codemirror>`;
        
    }

    _persistValue(event){
        this.value = event.target.value;
    }

    _renderContent(){
        if(this.content){
            return html`<script type='wc-content'>${this.content}</script>`;
        }
    }
    
    populatePrettyJson(v){
        this.clear();
        let pv = JSON.parse(v);
        v = JSON.stringify(pv, null, 2);
        this.content = v;
        this.value = v;
    }
    
    clear(){
        this.content = "\n\n\n\n";
        this.value = null;
    }
}

customElements.define('qui-code-block', QuiCodeBlock);
