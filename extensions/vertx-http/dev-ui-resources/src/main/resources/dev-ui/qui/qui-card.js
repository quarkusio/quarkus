import { LitElement, html, css} from 'lit';

/**
 * Card UI Component 
 */
export class QuiCard extends LitElement {
    
    static styles = css`
        .card {
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: 4px;
            filter: brightness(90%);
            
        }

        .card-header {
            font-size: var(--lumo-font-size-l);
            line-height: 1;
            height: 25px;
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            padding: 10px 10px;
            background-color: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }

        .card-footer {
            height: 20px;
            padding: 10px 10px;
            color: var(--lumo-contrast-50pct);
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            visibility:hidden;
        }`;

    static properties = {
        title: {type: String},
        width: {state: true},
        _hasFooter: {state: true},
    };

    constructor(){
        super();
        this.width = "100%";
        this._hasFooter = false;
    }

    connectedCallback() {
        super.connectedCallback()
    }
    
    render() {
        return html`<div class="card" style="width: ${this.width};">
                ${this._headerTemplate()}
                <slot name="content"></slot>
                ${this._footerTemplate()}
            </div>`;
    }

    firstUpdated(){
        const footerSlot = this.shadowRoot.querySelector("#footer");
        if (footerSlot && footerSlot.assignedNodes().length>0){
            console.log('No content is available')
            this._hasFooter = true;
        }
    }

    _headerTemplate() {
        return html`<div class="card-header">
                        <div>${this.title}</div>
                    </div>
          `;
    }

    _footerTemplate() {
        if(this._hasFooter){
            return html`
                <div class="card-footer">
                    <slot id="footer" name="footer"></slot>
                </div>
            `;
        }
    }

}
customElements.define('qui-card', QuiCard);