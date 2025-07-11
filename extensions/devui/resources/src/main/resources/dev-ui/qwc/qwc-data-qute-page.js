import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { RouterController } from 'router-controller';

/**
 * This component renders build time data using qute
 */
export class QwcDataQutePage extends LitElement {
    routerController = new RouterController(this);
    
    static styles = css``;

    static properties = {
        _htmlFragment: {attribute: false},
    };

    connectedCallback() {
        super.connectedCallback();
        var page = this.routerController.getCurrentPage();
        if(page && page.metadata){
            this._htmlFragment = page.metadata.htmlFragment;
        }
    }
    
    render() {
        return html`${unsafeHTML(this._htmlFragment)}`;
    }
}
customElements.define('qwc-data-qute-page', QwcDataQutePage);