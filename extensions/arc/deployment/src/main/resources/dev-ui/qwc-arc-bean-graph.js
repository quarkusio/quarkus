import { LitElement, html, css} from 'lit';
import { dependencyGraphs } from 'build-time-data';
import 'echarts-force-graph';
import '@vaadin/button';
import '@vaadin/checkbox';
import '@vaadin/checkbox-group';

/**
 * This component shows the Arc Bean Graph
 */
export class QwcArcBeanGraph extends LitElement {

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
        beanId: {type: String},
        beanDescription: {type: String},
        _edgeLength: {type: Number, state: true},
        _dependencyGraphs: {state: true},
        _categories: {state: false},
        _colors: {state: false},
        _nodes: {state: true},
        _links: {state: false},
        _showSimpleDescription: {state: false}
    };

    constructor() {
        super();
        this.beanId = null;
        this.beanDescription = null;
        this._dependencyGraphs = dependencyGraphs;
        this._categories =     ['root'   , 'dependencies', 'declaring bean of a producer', 'lookup dependency'];
        this._categoriesEnum = ['root'   , 'dependency'  , 'producer'                    , 'lookup'];
        this._colors =         ['#ee6666', '#91cc75'     , '#73c0de'                     , '#fac858'];
        this._edgeLength = 250;
        this._nodes = null;
        this._links = null;
        this._showSimpleDescription = [];
    }

    connectedCallback() {
        super.connectedCallback();
        if(this.beanId){
            this._createNodes();
        }
    }

    _createNodes(){
        let dependencyGraphsNodes = this._dependencyGraphs[this.beanId].nodes;
        let dependencyGraphsLinks = this._dependencyGraphs[this.beanId].links;
        
        this._links = []
        this._nodes = []
        for (var l = 0; l < dependencyGraphsLinks.length; l++) {
            const sourceNode = dependencyGraphsNodes.find(node => node.id === dependencyGraphsLinks[l].source);
            const targetNode = dependencyGraphsNodes.find(node => node.id === dependencyGraphsLinks[l].target);
            //console.log('Adding link: ' + sourceNode.description + ' -> ' + targetNode.description);
            
            // We need to make sure that the nodes are added first,
            // because the node index is used as a link source/target
            const catindex = this._categoriesEnum.indexOf(dependencyGraphsLinks[l].type);
            const sourceIdx = this._addToNodes(sourceNode,catindex);
            const targetIdx = this._addToNodes(targetNode,catindex);
            
            const link = new Object();
            link.source = sourceIdx;
            link.target = targetIdx;
            this._links.push(link);
        }
        
    }

    _addToNodes(dependencyGraphsNode, catindex){
        const newNode = this._createNode(dependencyGraphsNode);
        let index = this._nodes.findIndex(item => item.name === newNode.name);
        if (index < 0 ) {
            if(dependencyGraphsNode.id === this.beanId){
                newNode.category = 0; // root
            }else {
                newNode.category = catindex;
            }
            return this._nodes.push(newNode) - 1;
        }
        return index;
    }

    _createNode(node){
        let nodeObject = new Object();
        if(this._showSimpleDescription.length > 0){
            nodeObject.name = node.simpleDescription;
        }else{
            nodeObject.name = node.description;
        }
        nodeObject.value = node.id == this.beanId ? 20 : 10;
        nodeObject.symbolSize = nodeObject.value;
        nodeObject.id = node.id;
        nodeObject.description = node.description;
        return nodeObject;
    }

    render() {
        if(this.beanId){
            return html`${this._renderTopBar()}
                        <echarts-force-graph width="400px" height="400px"
                            edgeLength=${this._edgeLength} 
                            categories="${JSON.stringify(this._categories)}"
                            colors="${JSON.stringify(this._colors)}"
                            nodes="${JSON.stringify(this._nodes)}"
                            links="${JSON.stringify(this._links)}"
                            repulsion=500
                            @echarts-click=${this._echartClicked}>
                        </echarts-force-graph>`;
        }else{
            return html`<span>No bean id provided</span>`;
        }
    }
    
    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${this._backAction}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>${this.beanDescription}</h4>
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
        const back = new CustomEvent("arc-beans-graph-back", {
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
        this.beanId = e.detail.id;
        this.beanDescription = e.detail.description;
        this._createNodes();
    }
}
customElements.define('qwc-arc-bean-graph', QwcArcBeanGraph);