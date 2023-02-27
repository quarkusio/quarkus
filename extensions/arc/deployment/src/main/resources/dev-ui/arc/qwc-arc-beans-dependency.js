import { LitElement, html, css} from 'lit';
import { arcState } from './arc-state.js';
import { dependencyGraphs } from 'arc-data';
import 'qui-d3-force';

/**
 * This component shows the Arc Beans Dependency
 */
export class QwcArcBeansDependency extends LitElement {
    
    static styles = css`
        .d3Container {
            width: 100%;
            height: 100%;
            max-width: 100%;
            max-height: 100%;
        }
    `;

    static properties = {
        _heading: {state: true},
        _nodes: {state: true},
        _links: {state: true},
    };

    constructor() {
        super();

        this._refresh();

        this._types = [
            { name: 'root', icon: 'font-awesome-solid:circle', description: 'root', color: 'var(--lumo-error-text-color)'},
            { name: 'leaf', icon: 'font-awesome-solid:circle', color: 'var(--lumo-contrast)'},
            { name: 'directDependency', icon: 'font-awesome-solid:arrow-right', description: 'direct dependencies', color: '#1f77b4'},
            { name: 'directDependent', icon: 'font-awesome-solid:arrow-right', description: 'direct dependents', color: '#ff7f0e'},
            { name: 'dependency', icon: 'font-awesome-solid:arrow-right', description: 'dependencies', color: '#2ca02c'},
            { name: 'lookup', icon: 'font-awesome-solid:arrow-right', description: 'potential dependency', subtext: 'programmatic lookup', color: '#d62728'},
            { name: 'producer', icon: 'font-awesome-solid:arrow-right', description: 'declaring bean of a producer', color: '#9467bd'}
        ];
    }

    render() {
        return html`
        <div class="d3Container" >
            <qui-d3-force  
                @code-clicked="${(e) => { this._nodeClicked(e) }}" 
                @back-clicked="${() => { this._backClicked() }}" 
                .nodes=${this._nodes}
                .links=${this._links}
                .types=${this._types}
                .title=${this._heading}>
            </qui-d3-force>
        </div>`;
    }

    _refresh(){
        let d = dependencyGraphs[arcState.beanId];

        this._nodes = [];
        this._links = [];
        
        d.nodes.forEach(n => {
            let type = "leaf";
            if(n.id === arcState.beanId){
                type = "root";        
                this._heading = n.providerType.name;
            }

            this._nodes.push({
                id: n.id,
                kind: n.kind,
                description: n.description,
                type: type
            });
        });

        d.links.forEach(l => {
            this._links.push({
                source: l.source,
                target: l.target,
                type: l.type
            });
        });
    }

    _backClicked() {
        arcState.clear();
    }

    _nodeClicked(e) {
        if(e.detail.type === "leaf"){
            arcState.beanId = e.detail.id;
            this._refresh();
        }
    }

}
customElements.define('qwc-arc-beans-dependency', QwcArcBeansDependency);