import { LitElement, html, css } from 'lit';

/**
 * This component can we used to show if there is not data to show
 */
export class QwcNoData extends LitElement {
    static styles = css`
        .nodata {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
            border: 1px dashed var(--lumo-contrast-15pct);
            border-radius: var(--devui-radius-lg, 12px);
            padding: 40px 30px;
            margin: 30px;
            color: var(--lumo-contrast-50pct);
            font-size: var(--lumo-font-size-s);
            animation: devui-fade-in var(--devui-transition-slow, 0.3s ease);
        }
        @keyframes devui-fade-in {
            from { opacity: 0; transform: translateY(4px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .nodata a {
            color: var(--lumo-primary-text-color);
            text-decoration: none;
            transition: opacity var(--devui-transition-fast, 0.15s ease);
        }
        .nodata a:hover {
            opacity: 0.8;
        }
    `;


    static properties = {
        message: {attribute: true},
        link: {attribute: true},
        linkText: {attribute: true}
    };

    constructor() {
        super();
        this.message = "No data to display";
        this.link = null;
        this.linkText = "See the documentation for more information";
    }

    render() {
        return html`
            <div class="nodata">
                <span>${this.message}</span>
                ${this._renderLink()}
                <slot></slot>
            </div>
        `;
    }
    
    _renderLink(){
        if(this.link){
            return html`<a href="${this.link}" target="_blank"> ${this.linkText}</a>`;
        }
    }
}

customElements.define('qwc-no-data', QwcNoData);