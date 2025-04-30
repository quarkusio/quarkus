import { LitElement, html, css} from 'lit';
import { root } from 'devui-data';
import { allGavs } from 'devui-data';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/checkbox';
import '@vaadin/checkbox-group';
import '@vaadin/combo-box';
import { JsonRpc } from 'jsonrpc';

/**
 * This component shows the Application dependencies
 */
export class QwcDependencies extends LitElement {

    jsonRpc = new JsonRpc("devui-dependencies");

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .top-bar {
            display: flex;
            align-items: baseline;
            gap: 20px;
            padding-left: 20px;
            justify-content: space-between;
            padding-right: 20px;
        }
        .middle {
            display: flex;
            width:100%;
            height: 100%;
        }
        .allGavs {
            width: 100%;
        }
        .controls {
            display: flex;
            gap: 5px;
        }
        .target {
            width: 40%;
            display: flex;
        }
    `;

    static properties = {
        _edgeLength: {type: Number, state: true},
        _root: {state: true},
        _categories: {state: false},
        _colors: {state: false},
        _nodes: {state: true},
        _links: {state: true},
        _showSimpleDescription: {state: false},
        _showDirectOnly: {state: false},
        _allGavs: {state: false},
        _selectedTarget: {state: true}
    };

    constructor() {
        super();
        this._root = root;
        this._categories =     ['root'   , 'deployment', 'runtime', 'target'];
        this._categoriesEnum = ['root'   , 'deployment', 'runtime', 'target'];
        this._colors =         ['#ee6666', '#5470c6'   , '#91cc75', '#f7c657'];
        this._edgeLength = 120;
        this._nodes = null;
        this._links = null;
        this._showSimpleDescription = ["0"];
        this._showDirectOnly = [];
        this._allGavs = allGavs.map(str => ({ id: str, name: str }));
        this._selectedTarget = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._createNodes();
    }

    _createNodes(){
        let dependencyGraphNodes = this._root.nodes;
        let dependencyGraphLinks = this._root.links;

        this._links = [];
        this._nodes = [];

        for (var l = 0; l < dependencyGraphLinks.length; l++) {
            let linkSpec = dependencyGraphLinks[l];
            let sourceNode = dependencyGraphNodes.find(item => item.id === linkSpec.source);
            let targetNode = dependencyGraphNodes.find(item => item.id === linkSpec.target);
            let catindex = this._categoriesEnum.indexOf(linkSpec.type);
            
            if(this._showDirectOnly.length==0 || (this._showDirectOnly.length>0 && this._isDirect(linkSpec))){
                this._addToNodes(sourceNode, catindex);
                this._addToNodes(targetNode, catindex);
                let link = new Object();
                link.target = this._nodes.findIndex(item => item.id === sourceNode.id);
                link.source = this._nodes.findIndex(item => item.id === targetNode.id);
                this._links.push(link);
            }
        }
    }

    _addToNodes(dependencyGraphNode, catindex){
        let addedNode = this._nodes.find(item => item.id === dependencyGraphNode.id);
        if (!addedNode) {
            let newNode = this._createNode(dependencyGraphNode);
            if(this._isRoot(dependencyGraphNode)){
                newNode.category = 0; // Root
            }else if(this._isTarget(dependencyGraphNode)){
                newNode.category = 3; // Target
            }else {
                newNode.category = catindex;
            }
            this._nodes.push(newNode);
        } else if (addedNode.category > 0 && addedNode.category < catindex) {
           addedNode.category = catindex;
        }
    }

    _isDirect(dependencyGraphLink){
        return dependencyGraphLink.direct || dependencyGraphLink.source === this._root.rootId;
    }

    _isRoot(dependencyGraphNode){
        return dependencyGraphNode.id === this._root.rootId;
    }

    _isTarget(dependencyGraphNode){
        if(this._selectedTarget){
            return dependencyGraphNode.id === this._selectedTarget;
        }
        return false;
    }

    _createNode(node){
        let nodeObject = new Object();
        if(this._showSimpleDescription.length>0){
            nodeObject.name = node.name;
        }else{
            nodeObject.name = node.description;
        }

        nodeObject.value = node.value;
        nodeObject.id = node.id;
        nodeObject.description = node.description;
        return nodeObject;
    }

    render() {
        return html`${this._renderTopBar()}
                        <div class="middle">    
                            <echarts-force-graph width="400px" height="400px"
                                edgeLength=${this._edgeLength}
                                categories="${JSON.stringify(this._categories)}"
                                colors="${JSON.stringify(this._colors)}"
                                nodes="${JSON.stringify(this._nodes)}"
                                links="${JSON.stringify(this._links)}"
                                @echarts-click=${this._echartClicked}>
                            </echarts-force-graph>
                        </div>`;
        
    }

    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <div class="target">
                            ${this._renderPathToTargetCombobox()}
                            <vaadin-button theme="tertiary" @click=${() => this._fetchPathToTarget()}>
                                <vaadin-icon icon="font-awesome-solid:eraser"></vaadin-icon>
                            </vaadin-button>
                        </div>
                        <div class="controls">    
                            ${this._renderDirectOnlyCheckbox()}
                            ${this._renderSimpleDescriptionCheckbox()}
                            
                            <vaadin-button theme="icon" aria-label="Zoom in" @click=${this._zoomIn}>
                                <vaadin-icon icon="font-awesome-solid:magnifying-glass-plus"></vaadin-icon>
                            </vaadin-button>
                            <vaadin-button theme="icon" aria-label="Zoom out" @click=${this._zoomOut}>
                                <vaadin-icon icon="font-awesome-solid:magnifying-glass-minus"></vaadin-icon>
                            </vaadin-button>
                        </div>
                    </div>`;
    }

    _renderPathToTargetCombobox(){
        return html`<vaadin-combo-box
            @change="${(event) => {
                this._togglePathToTarget(event);
            }}"
            class="allGavs"
            placeholder="Show path to ..."
            item-label-path="name"
            item-value-path="id"
            .items="${this._allGavs}"
            value="${this._selectedTarget}"
        ></vaadin-combo-box>`;
    }

    _renderSimpleDescriptionCheckbox(){
        return html`<vaadin-checkbox-group
                        .value="${this._showSimpleDescription}"
                        @value-changed="${(event) => {
                            this._showSimpleDescription = event.detail.value;
                            this._createNodes();
                        }}">
                        <vaadin-checkbox value="0" label="Simple description"></vaadin-checkbox>
                    </vaadin-checkbox-group>`;
    }

    _renderDirectOnlyCheckbox(){
        return html`<vaadin-checkbox-group
                        .value="${this._showDirectOnly}"
                        @value-changed="${(event) => {
                            this._showDirectOnly = event.detail.value;
                            this._createNodes();
                        }}">
                        <vaadin-checkbox value="0" label="Direct Only"></vaadin-checkbox>
                    </vaadin-checkbox-group>`;
    }

    _zoomIn(){
        if(this._edgeLength>20){
            this._edgeLength = this._edgeLength - 20;
        }else{
            this._edgeLength = 20;
        }
    }

    _zoomOut(){
        this._edgeLength = this._edgeLength + 20;
    }

    _togglePathToTarget(event){
        if(event.target.value){
            this._fetchPathToTarget(event.target.value);
        }else{
            this._fetchPathToTarget();
        }
    }
    
    _echartClicked(e){
        this._fetchPathToTarget(e.detail.id);
    }

    _fetchPathToTarget(target){
        if(target){
            this._selectedTarget = target;
            this.jsonRpc.pathToTarget({target: target}).then(jsonRpcResponse => { 
                this._root = jsonRpcResponse.result;
                this._createNodes();    
            });
        }else {
            this._selectedTarget = null;
            this.jsonRpc.pathToTarget().then(jsonRpcResponse => { 
                this._root = jsonRpcResponse.result;
                this._createNodes();
            });
        }
    }
}
customElements.define('qwc-dependencies', QwcDependencies);