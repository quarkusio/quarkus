import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import 'echarts-force-graph';
import '@vaadin/button';
import '@vaadin/checkbox';
import '@vaadin/checkbox-group';
import '@vaadin/progress-bar';

/**
 * This component shows the Build Step Graph
 */
export class QwcBuildStepGraph extends LitElement {

    static styles = css`
        .top-bar {
            display: flex;
            align-items: baseline;
            gap: 20px;
            padding-left: 20px;
            justify-content: space-between;
            padding-right: 20px;
        }
    
        .top-bar h4 {
            color: var(--lumo-contrast-60pct);
        }
    `;

    static properties = {
        stepId: {type: String},
        extensionName: {type: String}, // TODO: Add 'pane' concept in router to register internal extension pages.
        _edgeLength: {type: Number, state: true},
        _dependencyGraph: {state: true},
        _categories: {state: false},
        _colors: {state: false},
        _nodes: {state: true},
        _links: {state: false},
        _showSimpleDescription: {state: false}
    };

    constructor() {
        super();
        this.stepId = null;
        this._dependencyGraph = null;
        this._categories =     ['root'   , 'direct dependencies', 'direct dependents'];
        this._categoriesEnum = ['root'   , 'directDependency'   , 'directDependent'];
        this._colors =         ['#ee6666', '#5470c6'            , '#fac858'];
        this._edgeLength = 250;
        this._nodes = null;
        this._links = null;
        this._showSimpleDescription = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc = new JsonRpc(this.extensionName);
        this._fetchDependencyGraph();    
    }

    _fetchDependencyGraph(){
        if(this.stepId){
            this.jsonRpc.getDependencyGraph({buildStepId: this.stepId}).then(jsonRpcResponse => {
                this._dependencyGraph = jsonRpcResponse.result;
                this._createNodes();
            });
        }
    }

    _createNodes() {
        if (this._dependencyGraph) {
            let dependencyGraphNodes = this._dependencyGraph.nodes;
            let dependencyGraphLinks = this._dependencyGraph.links;

            this._links = []
            this._nodes = []

            for (var l = 0; l < dependencyGraphLinks.length; l++) {
                const sourceNode = dependencyGraphNodes.find(node => node.stepId === dependencyGraphLinks[l].source);
                const targetNode = dependencyGraphNodes.find(node => node.stepId === dependencyGraphLinks[l].target);
                //console.log('Adding link: ' + sourceNode.stepId + ' -> ' + targetNode.stepId);

                // We need to make sure that the nodes are added first,
                // because the node index is used as a link source/target
                const catindex = this._categoriesEnum.indexOf(dependencyGraphLinks[l].type);
                const sourceIdx = this._addToNodes(sourceNode, catindex);
                const targetIdx = this._addToNodes(targetNode, catindex);

                const link = new Object();
                link.source = sourceIdx;
                link.target = targetIdx;
                this._links.push(link);
            }
        }
    }
    
    _addToNodes(dependencyGraphNode, catindex){
        const newNode = this._createNode(dependencyGraphNode);
        let index = this._nodes.findIndex(item => item.name === newNode.name);
        if (index < 0 ) {
            if(dependencyGraphNode.stepId === this.stepId){
                newNode.category = 0; // root
            }else {
                newNode.category = catindex;
            }
            //console.log('Adding node: ' + newNode.name);
            return this._nodes.push(newNode) - 1;
        }
        return index;
    }

    _createNode(node){
        let nodeObject = new Object();
        if(this._showSimpleDescription.length>0){
            nodeObject.name = node.simpleName;
        }else{
            nodeObject.name = node.stepId;
        }
        nodeObject.value = node.stepId == this.stepId ? 20 : 10;
        nodeObject.symbolSize = nodeObject.value;
        nodeObject.id = node.stepId;
        nodeObject.description = node.simpleName;
        return nodeObject;
    }

    render() {
        if(this.stepId && this._dependencyGraph){
            return html`${this._renderTopBar()}
                        <echarts-force-graph width="400px" height="400px"
                            edgeLength=${this._edgeLength} 
                            categories="${JSON.stringify(this._categories)}"
                            colors="${JSON.stringify(this._colors)}"
                            nodes="${JSON.stringify(this._nodes)}"
                            links="${JSON.stringify(this._links)}"
                            @echarts-click=${this._echartClicked}>
                        </echarts-force-graph>`;
        } else if(this.stepId) {
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Loading Dependency Graph...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        } else {
            return html`<span>No build step provided</span>`;
        }
    }
    
    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${this._backAction}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>${this.stepId}</h4>
                        <div>
                            ${this._renderCheckbox()}
                            
                            <vaadin-button theme="icon" aria-label="Zoom in" @click=${this._zoomIn}>
                                <vaadin-icon icon="font-awesome-solid:magnifying-glass-plus"></vaadin-icon>
                            </vaadin-button>
                            <vaadin-button theme="icon" aria-label="Zoom out" @click=${this._zoomOut}>
                                <vaadin-icon icon="font-awesome-solid:magnifying-glass-minus"></vaadin-icon>
                            </vaadin-button>
                        </div>
                    </div>`;
    }
    
    _renderCheckbox(){
        return html`<vaadin-checkbox-group
                        .value="${this._showSimpleDescription}"
                        @value-changed="${(event) => {
                            this._showSimpleDescription = event.detail.value;
                            this._createNodes();
                        }}">
                        <vaadin-checkbox value="0" label="Simple description"></vaadin-checkbox>
                    </vaadin-checkbox-group>`;
    }
    
    _backAction(){
        const back = new CustomEvent("build-steps-graph-back", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false,
        });
        this.dispatchEvent(back);
    }
    
    _zoomIn(){
        this._edgeLength = this._edgeLength + 20;
    }

    _zoomOut(){
        if (this._edgeLength > 20){
            this._edgeLength = this._edgeLength - 20;
        }else {
            this._edgeLength = 20;
        }
    }
    
    _echartClicked(e){
        this.stepId = e.detail.id;
        this._fetchDependencyGraph();
    }
}
customElements.define('qwc-build-step-graph', QwcBuildStepGraph);