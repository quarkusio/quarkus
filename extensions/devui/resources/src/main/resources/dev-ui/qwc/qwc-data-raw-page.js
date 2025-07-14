import { LitElement, html, css} from 'lit';
import { RouterController } from 'router-controller';
import 'qui-themed-code-block';

/**
 * This component renders build time data in raw json format
 */
export class QwcDataRawPage extends LitElement {
    routerController = new RouterController(this);
    
    static styles = css`
        .codeBlock {
            display:flex;
            gap: 10px;
            flex-direction: column;
            padding-left: 10px;
            padding-right: 10px;
        }
    `;

    static properties = {
        _buildTimeDataKey: {attribute: false},
        _buildTimeData: {attribute: false},
    };

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        
        var page = this.routerController.getCurrentPage();
        if(page && page.metadata){
            this._buildTimeDataKey = page.metadata.buildTimeDataKey;

            let modulePath = page.namespace + "-data";

            import(modulePath)
            .then(obj => {
                this._buildTimeData = obj[this._buildTimeDataKey]; // TODO: Just use obj and allow multiple keys ?
            });
        }
    }
    
    
    render() {

        var json = JSON.stringify(this._buildTimeData, null, '\t');

        return html`<div class="codeBlock">
                        <qui-themed-code-block
                            mode='json'
                            content='${json}'
                            showLineNumbers>
                        </qui-themed-code-block>
            </div>`;
    }

}
customElements.define('qwc-data-raw-page', QwcDataRawPage);