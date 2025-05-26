import { LitElement, html, css} from 'lit';
import { StorageController } from 'storage-controller';
import '@vaadin/popover';
import '@vaadin/vertical-layout';
import { popoverRenderer } from '@vaadin/popover/lit.js';


/**
 * This is the menu action on the Extensions menu
 */
export class QwcExtensionsMenuAction extends LitElement {
    
    storageController = new StorageController(this);
    
    static styles = css`
            .actionBtn{
                color: var(--lumo-contrast-25pct);
            }
       `;

    static properties = {
        _selectedFilters: {state: true, type: Array}
    }

    constructor() {
        super();
        this._filteritems = ["Favorites","Active","Inactive"];
        
    }

    connectedCallback() {
        super.connectedCallback();
        this._selectedFilters = this._getStoredFilters();
    }

    render(){
        return html`<vaadin-button id="filterButton" theme="icon tertiary small" aria-label="Filter" title="Filter extension cards" class="actionBtn">
                        <vaadin-icon icon="font-awesome-solid:filter"></vaadin-icon>
                    </vaadin-button>
                    <vaadin-popover
                        for="filterButton"
                        .position="bottom-start"
                        .position="${this.position}"
                        ${popoverRenderer(this._renderFilters)}
                    ></vaadin-popover>`;
    }

    _renderFilters = () => {
    return html`
            <vaadin-list-box multiple
                .selectedValues="${this._selectedFilters}"
                @selected-values-changed="${this._onFilterChange}">
                    ${this._filteritems.map(filter => this._renderFilter(filter))}
            </vaadin-list-box>
        `;
    }

    _renderFilter(filter){
        return html`<vaadin-item>${filter}</vaadin-item>`;
    }

    _getStoredFilters(){
        const storedFilters = JSON.parse(this.storageController.get?.('selectedFilters'));
        const selectedFilters = Array.isArray(storedFilters) && storedFilters.length > 0 ? storedFilters : this._filteritems;
        const selectedIndexes = selectedFilters.map(option => this._filteritems.indexOf(option));
        return selectedIndexes;
    }

    _setStoredFilters(selectedFilters){
        this.storageController.set('selectedFilters', JSON.stringify(selectedFilters));
    }
    
    _onFilterChange(event) {
        const selectedIndexes = event.detail.value;
        this._selectedFilters = selectedIndexes;

        const selectedFilters = selectedIndexes.map(i => this._filteritems[i]);

        // Save to storage
        this._setStoredFilters(selectedFilters);

        // Fire custom event
        this.dispatchEvent(new CustomEvent('extensions-filters-changed', {
            detail: { filters: selectedFilters },
            bubbles: true,
            composed: true
        }));
    }
    
}
customElements.define('qwc-extensions-menu-action', QwcExtensionsMenuAction);
