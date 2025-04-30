import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';
/**
 * This wraps the Graph with force layout echart into a component
 * see https://echarts.apache.org/examples/en/editor.html?c=graph-webkit-dep
 */
class EchartsForceGraph extends EchartsAbstractCanvas {

    static get properties() {
        return {
            edgeLength: {type: Number},
            repulsion: {type: Number},
            nodes: {type: String},
            links: {type: String},
            colors: {type: String},
            showLegend: {type: Boolean},
            categories: {type: String}, // Array passed in
            primaryTextColor: { type: String },
            _categories: {state: false} // Objects created for graph
        };
    }

    constructor() {
        super();
        this.edgeLength = 60;
        this.repulsion = 300;
        this.nodes = null;
        this.links = null;
        this.colors = null;
        this.primaryTextColor = "--lumo-body-text-color";
        this.showLegend = true;
        this.categories = null;
        this._categories = null;
    }

    connectedCallback() {
      super.connectedCallback();
      this._categories = [];
      let cats = JSON.parse(this.categories);
      for (var i = 0; i < cats.length; i++) {
        let cat = new Object();
        cat.name = cats[i];
        cat.keyword = new Object();
        this._categories.push(cat);
      }
    }

    getOption(){
        let textColor = this.primaryTextColor;
        if(textColor.startsWith('--')){
            textColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(textColor);
        }

        const option = new Object();
      
        // Legend
        option.legend = new Object();
        option.legend.show = this.showLegend;
        option.legend.data = JSON.parse(this.categories);
        option.legend.orient = 'vertical';
        option.legend.right = 20;
        option.legend.top = 20;
        option.legend.textStyle = new Object();
        option.legend.textStyle.color = textColor;
       
        if(this.colors) {
          option.color = JSON.parse(this.colors);
        }
        // Series
        const serie = new Object();

        serie.type = 'graph';
        serie.layout = 'force';
        serie.animation = true;
        // source -> target
        serie.edgeSymbol = ['', 'arrow'];
        serie.label = new Object();
        serie.label.position = 'right';
        serie.label.show = true;
        serie.label.textStyle = new Object();
        serie.label.textStyle.color = textColor;
        serie.label.triggerEvent = true;

        serie.draggable = true;

        serie.data = JSON.parse(this.nodes);

        serie.categories = this._categories;
        
        serie.force = new Object();
        serie.force.edgeLength = this.edgeLength;
        serie.force.repulsion = this.repulsion;
        
        serie.edges = JSON.parse(this.links);

        serie.lineStyle = new Object();
        // Use the color of the "source" by default
        serie.lineStyle.color = 'source';
        serie.lineStyle.curveness = 0.3;
        
        option.series = [];
        option.series.push(serie);

        return option;
    }

    chartClicked(e){
      const echartsClick = new CustomEvent("echarts-click", {
          detail: e,
          bubbles: true,
          cancelable: true,
          composed: false,
      });
      this.dispatchEvent(echartsClick);
    }
}
customElements.define('echarts-force-graph', EchartsForceGraph);