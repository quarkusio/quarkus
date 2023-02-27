import { LitElement, html, css} from 'lit';
import '@vaadin/icon';

/**
 * Menubar UI Component using only icons
 */
export class QuiIconMenu extends LitElement {
    static styles = css`
        .menubar {
            display:flex;
            gap: 10px;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: 25px;
            padding: 6px;
            width: fit-content;
        }

        ::slotted(vaadin-icon) {
            cursor: pointer;
            color: var(--lumo-primary-color-50pct);
            padding: 2px;
        }

        ::slotted(vaadin-icon:hover) {
            color: var(--quarkus-blue);
        }
    `;

    static properties = {
        
    };

    constructor(){
        super();
        
    }

    connectedCallback() {
        super.connectedCallback()
      }

    render() {
        return html`
                <div class="menubar">
                    <slot></slot>
                </div>`;
    }


}
customElements.define('qui-icon-menu', QuiIconMenu);