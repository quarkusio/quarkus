import { LitElement, html, css} from 'lit';
import { RouterController } from 'router-controller';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@qomponent/qui-code-block';

/**
 * This component renders build time data in raw json format
 */
export class QwcDataRawPage extends observeState(LitElement) {
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
                        <qui-code-block 
                            mode='json'
                            content='${json}'
                            theme='${themeState.theme.name}'
                            showLineNumbers>
                        </qui-code-block>
            </div>`;
    }

}
customElements.define('qwc-data-raw-page', QwcDataRawPage);