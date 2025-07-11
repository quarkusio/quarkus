import { LitElement, html, css } from 'lit';
import '@qomponent/qui-code-block';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';

class QuiThemedCodeBlock extends observeState(LitElement) {
   
    static properties = {
        mode: { type: String },
        content: { type: String },
        src: { type: String },
        showLineNumbers: {type: Boolean},
        editable: {type: Boolean},
        value: {type: String, reflect: true }
    };

    constructor() {
        super();
        this.mode = null;
        this.content = '';
        this.showLineNumbers = false;
        this.editable = false;
        this.value = null;
    }
    
    render() {
        return html`
            <qui-code-block
                .mode=${this.mode}
                .content=${this.content}
                .src=${this.src}
                .showLineNumbers=${this.showLineNumbers}
                .editable=${this.editable}
                .value=${this.value}
                theme='${themeState.theme.name}'
                @value-changed=${(e) => this._onValueChanged(e)}
                @shiftEnter=${(e) => this._onShiftEnter(e)}>
                    <slot></slot>
            </qui-code-block>
        `;
    }

    _onValueChanged(e) {
        e.stopPropagation();
        // re-dispatch event so parent can listen on <qui-themed-code-block>
        if (this.value !== e.detail.value) {
            this.value = e.detail.value;
        }
        this.dispatchEvent(new CustomEvent('value-changed', {
            detail: e.detail,
            bubbles: true,
            composed: true
        }));
    }
    
    _onShiftEnter(e) {
        this.dispatchEvent(new CustomEvent('shiftEnter', {
            detail: e.detail,
            bubbles: true,
            composed: true
        }));
    }
}

customElements.define('qui-themed-code-block', QuiThemedCodeBlock);