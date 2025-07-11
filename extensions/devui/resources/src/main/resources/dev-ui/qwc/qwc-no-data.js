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
            gap: 10px;
            border: 1px solid var(--lumo-contrast-20pct);
            border-radius: 9px;
            padding: 30px;
            margin: 30px;
        }
        .nodata a {
            color: var(--lumo-contrast-90pct);
        }
    `;


    static properties = {
        message: {attribute: true},
        link: {attribute: true},
        linkText: {attribute: true},
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