import { LitElement, html, css } from 'lit';
import { dependencies } from '../produi-dependencies-data.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/text-field';
import './pui-empty-state.js';

export class PuiDependencies extends LitElement {

    static styles = css`
        :host { display: block; height: 100%; }
        .container { display: flex; flex-direction: column; height: 100%; }
        .search { padding: 0 0 12px; }
        .search vaadin-text-field { width: 400px; }
        .grid { flex: 1; }
        .gav { font-family: monospace; font-size: 13px; }
        .direct { color: var(--lumo-primary-text-color); font-weight: 500; }
        .transitive { color: var(--lumo-secondary-text-color); }
    `;

    static properties = {
        _deps: { state: true },
        _filtered: { state: true }
    };

    constructor() {
        super();
        if (dependencies) {
            this._deps = dependencies.nodes.map(node => {
                const parts = node.id.split(':');
                const link = dependencies.links.find(l => l.target === node.id);
                return {
                    groupId: parts[0] || '',
                    artifactId: parts[1] || '',
                    version: parts[parts.length - 1] || '',
                    direct: link ? link.direct : node.id === dependencies.rootId,
                    id: node.id
                };
            });
            this._filtered = this._deps;
        }
    }

    render() {
        if (!this._deps || this._deps.length === 0) {
            return html`<pui-empty-state kind="empty" heading="No dependencies"
                message="No dependency information is available."></pui-empty-state>`;
        }
        return html`
            <div class="container">
                <div class="search">
                    <vaadin-text-field
                        placeholder="Search dependencies..."
                        clear-button-visible
                        @input=${this._onSearch}>
                    </vaadin-text-field>
                </div>
                <vaadin-grid .items="${this._filtered}" class="grid" theme="no-border row-stripes">
                    <vaadin-grid-sort-column auto-width header="Group ID" path="groupId"></vaadin-grid-sort-column>
                    <vaadin-grid-sort-column auto-width header="Artifact ID" path="artifactId"></vaadin-grid-sort-column>
                    <vaadin-grid-sort-column auto-width header="Version" path="version"></vaadin-grid-sort-column>
                </vaadin-grid>
            </div>`;
    }

    _onSearch(e) {
        const query = e.target.value?.toLowerCase() || '';
        if (!query) {
            this._filtered = this._deps;
        } else {
            this._filtered = this._deps.filter(d =>
                d.groupId.toLowerCase().includes(query) ||
                d.artifactId.toLowerCase().includes(query) ||
                d.version.toLowerCase().includes(query)
            );
        }
    }
}
customElements.define('pui-dependencies', PuiDependencies);
