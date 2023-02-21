import { LitElement, html, css} from 'lit';
import { RouterController } from 'router-controller';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * This component renders build time data in a table
 */
export class QwcDataTablePage extends LitElement {
    
    static styles = css`
        .datatable {
            height: 100%;
            padding-bottom: 10px;
        }
    `;

    static properties = {
        _buildTimeDataKey: {attribute: false},
        _buildTimeData: {attribute: false},
        _cols: {attribute: false},
    };

    connectedCallback() {
        super.connectedCallback();
        var extensionId = RouterController.currentExtensionId();
        if(extensionId){

            var metadata = RouterController.currentMetaData();
            if(metadata){

                this._buildTimeDataKey = metadata.buildTimeDataKey;
                
                let modulePath = extensionId + "-data";

                import(modulePath)
                .then(obj => {
                    this._buildTimeData = obj[this._buildTimeDataKey];

                    if(metadata.cols){
                        this._cols = metadata.cols.split(',');
                    }else{
                        this._autodetectCols();
                    }
                });
            }
        }
    }
    
    
    render() {
        if(this._cols){
            const colTemplates = [];

            for (const col of this._cols) {
                colTemplates.push(html`<vaadin-grid-sort-column path="${col}" resizable></vaadin-grid-sort-column>`);
            }

            return html`<vaadin-grid .items="${this._buildTimeData}" class="datatable" theme="no-border">
                ${colTemplates}
            </vaadin-grid>`;
        }
    }

    _autodetectCols(){
        if(this._buildTimeData){
            var row = this._buildTimeData[0];
            if(row){
                this._cols = Object.getOwnPropertyNames(row);
            }else{
                this._cols = [];
            }
        }
    }

}
customElements.define('qwc-data-table-page', QwcDataTablePage);