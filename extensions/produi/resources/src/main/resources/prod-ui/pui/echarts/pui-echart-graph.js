import { PuiEchart } from './pui-echart.js';

/**
 * Directed node-link graph wrapper, built on the ECharts 'graph' series. Data is
 * passed as attributes:
 *  - nodes: JSON array of { id, label, type, subTopology } objects. 'type' is one
 *           of source / processor / sink / store and drives the node colour and shape.
 *  - edges: JSON array of { source, target } objects referencing node ids.
 *  - title: chart title (optional)
 *
 * A force layout positions the nodes (drag to reposition, scroll to zoom); edges
 * are drawn with an arrow head so the data-flow direction is clear. Registered as
 * the bare specifier 'pui-echart-graph' via the Prod UI esbuild alias so extension
 * page components can import it without knowing the shell directory layout.
 */
export class PuiEchartGraph extends PuiEchart {

    static properties = {
        title: { type: String },
        nodes: { type: String },
        edges: { type: String }
    };

    // Fixed category order; a node's 'type' maps to an index into this array.
    static CATEGORIES = ['source', 'processor', 'sink', 'store'];

    getOption() {
        const textColor = this.cssColor('--lumo-body-text-color', '#333');
        const mutedColor = this.cssColor('--lumo-secondary-text-color', '#888');
        const surfaceColor = this.cssColor('--lumo-base-color', '#ffffff');
        const axisColor = this.cssColor('--lumo-contrast-20pct', '#cccccc');
        const lineColor = this.cssColor('--lumo-contrast-30pct', '#bbbbbb');

        // Role palette: prefer theme colours, fall back to a fixed set so the roles
        // stay distinguishable even when a Lumo variable is not defined.
        const roleColor = {
            source: this.cssColor('--lumo-success-color', '#54b04a'),
            processor: this.cssColor('--lumo-primary-color', '#4695eb'),
            sink: '#9b59b6',
            store: '#e8a33d'
        };
        const roleSymbol = {
            source: 'triangle',
            processor: 'roundRect',
            sink: 'pin',
            store: 'diamond'
        };

        const categories = PuiEchartGraph.CATEGORIES.map(name => ({
            name,
            itemStyle: { color: roleColor[name] },
            symbol: roleSymbol[name]
        }));

        const nodes = this._parse(this.nodes);
        const edges = this._parse(this.edges);

        const data = nodes.map(n => {
            const type = PuiEchartGraph.CATEGORIES.includes(n.type) ? n.type : 'processor';
            return {
                id: n.id,
                name: n.id,
                category: PuiEchartGraph.CATEGORIES.indexOf(type),
                symbol: roleSymbol[type],
                symbolSize: type === 'processor' ? 34 : 26,
                label: { formatter: () => n.label || n.id },
                // Carried through for the tooltip.
                _label: n.label || n.id,
                _type: type,
                _subTopology: n.subTopology || ''
            };
        });

        const links = edges.map(e => ({ source: e.source, target: e.target }));

        return {
            backgroundColor: 'transparent',
            textStyle: { color: textColor },
            title: this.title
                ? { text: this.title, textStyle: { color: textColor, fontSize: 14, fontWeight: 'normal' } }
                : undefined,
            tooltip: {
                backgroundColor: surfaceColor,
                borderColor: axisColor,
                textStyle: { color: textColor },
                formatter: (params) => {
                    if (params.dataType === 'edge') {
                        return `${params.data.source} &rarr; ${params.data.target}`;
                    }
                    const d = params.data;
                    const sub = d._subTopology ? `<br/>Sub-topology: ${d._subTopology}` : '';
                    return `<b>${d._label}</b><br/>${d._type}${sub}`;
                }
            },
            legend: [{
                data: PuiEchartGraph.CATEGORIES,
                textStyle: { color: textColor },
                top: this.title ? 26 : 2
            }],
            series: [{
                type: 'graph',
                layout: 'force',
                roam: true,
                draggable: true,
                categories,
                data,
                links,
                edgeSymbol: ['none', 'arrow'],
                edgeSymbolSize: 8,
                force: { repulsion: 260, edgeLength: 110, gravity: 0.08 },
                label: { show: true, position: 'right', color: textColor, fontSize: 11 },
                lineStyle: { color: lineColor, width: 1.5, opacity: 0.7, curveness: 0.05 },
                emphasis: { focus: 'adjacency', lineStyle: { width: 3 } }
            }]
        };
    }

    _parse(json) {
        try {
            return JSON.parse(json || '[]');
        } catch (e) {
            return [];
        }
    }
}
customElements.define('pui-echart-graph', PuiEchartGraph);
