import { LitElement, html } from 'lit';
import { forceSimulation, forceLink, forceManyBody, forceCenter } from 'd3-force';
import { select } from 'd3-selection';
import { drag } from 'd3-drag';
import { zoom, zoomIdentity } from 'd3-zoom';
import ZoomEvent from 'd3-zoom/src/event.js';
import 'qui-icon-menu';
import '@vaadin/icon';

/**
 * Wrapping D3 Force 
 */
export class QuiD3Force extends LitElement {

  static properties = {
    title: { state: true },
    nodes: { state: true },
    links: { state: true },
    types: { type: Array },
  }

  constructor() {
    super();
    this.title = "";
    this.nodes = [];
    this.links = [];
    this.types = [];
    this.draggedNode = null;
    this.svg = null;
  }

  connectedCallback() {
    super.connectedCallback();

    this.svg = select(this.shadowRoot.querySelector('svg'));

    this.zoomBehavior = zoom()
      .scaleExtent([0.1, 10])
      .on('zoom', (e) => {
        console.log("------> ZOOM !" + e);
        const transform = new ZoomEvent().transform;
        this.svg.select('#nodes').attr('transform', transform);
        this.svg.select('#links').attr('transform', transform);
      });

    
  }

  updated() {
    const { nodes, links, types } = this;

    const width = this.clientWidth;
    const height = this.clientHeight;
    
    const svg = select(this.shadowRoot.querySelector('svg'));

    const linkGroup = svg.select('#links');
    const nodeGroup = svg.select('#nodes');
    const defsGroup = svg.select('#defs');

    linkGroup.html("");
    nodeGroup.html("");
    defsGroup.html("");

    defsGroup.selectAll("marker")
    .data(types)
    .join("marker")
      .attr("id", d => `arrow-${d.name}`)
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 15)
      .attr("refY", -0.5)
      .attr("markerWidth", 8)
      .attr("markerHeight", 8)
      .attr("orient", "auto")
      .append("path")
      .attr("fill", d => d.color)
      .attr("d", "M0,-5L10,0L0,5");

    const link = linkGroup
      .selectAll('path')
      .data(links)
      .join('path')
      .attr('stroke', d => types.find(type => type.name === d.type).color)
      .attr('fill', 'none')
      .attr('marker-end', d => `url(#arrow-${d.type})`)
      .attr('d', link => {
        const dx = link.target.x - link.source.x;
        const dy = link.target.y - link.source.y;
        const dr = Math.sqrt(dx * dx + dy * dy);
        return `M${link.source.x},${link.source.y}A${dr},${dr} 0 0,1 ${link.target.x},${link.target.y}`;
      });

    const node = nodeGroup
      .selectAll('g')
      .data(nodes, d => d.id)
      .join('g')
      .call(drag()
        .on('start', event => {
          this.draggedNode = event.subject;
        })
        .on('drag', event => {
          this.draggedNode.x = event.x;
          this.draggedNode.y = event.y;
          this.simulation.alpha(0.5).restart();
          this.updateNodePositions(node, link);
        })
        .on('end', () => {
          this.draggedNode.fx = null;
          this.draggedNode.fy = null;
          this.draggedNode = null;
        })
      );

    node.append('circle')
      .attr('r', 5)
      .attr('fill', d => types.find(type => type.name === d.type).color);
      
    node.append('text')
      .text(d => d.description)
      .attr('x', 12)
      .attr('y', 4)
      .attr('class',d => d.type)
      .on('click', (event, d) => {
        this.nodeClicked(d);
      });

    this.simulation = forceSimulation(nodes)
      .force('link', forceLink(links)
        .id(d => d.id)
        .distance(100)
      )
      .force('charge', forceManyBody().strength(-200))
      .force('center', forceCenter(width / 3 , height / 3))
      .on('tick', () => {
        link.attr('d', link => {
          const dx = link.target.x - link.source.x;
          const dy = link.target.y - link.source.y;
          const dr = Math.sqrt(dx * dx + dy * dy);
          return `M${link.source.x},${link.source.y}A${dr},${dr} 0 0,1 ${link.target.x},${link.target.y}`;
        });
        node.attr('transform', d => `translate(${d.x},${d.y})`);
      });

    this.simulation.nodes(nodes);
    this.simulation.force('link').links(links);
    this.simulation.alpha(1).restart();

    this.svg.call(this.zoomBehavior);
  }

  updateNodePositions(nodes, links) {
    links.attr('d', link => {
      const dx = link.target.x - link.source.x;
      const dy = link.target.y - link.source.y;
      const dr = Math.sqrt(dx * dx + dy * dy);
      return `M${link.source.x},${link.source.y}A${dr},${dr} 0 0,1 ${link.target.x},${link.target.y}`;
    });
    nodes.attr('transform', d => `translate(${d.x},${d.y})`);
  }

  nodeClicked(node) {
      let event = new CustomEvent('code-clicked', {
        detail: node
      });

      this.dispatchEvent(event);
  }

  render() {

    return html`
      <style>
        :host {
          display: flex;
          width: 100%;
          height: 100%;
          flex-direction: column;
        }

        .heading {
          display: flex;
          gap: 15px;
          align-items: center;
        }

        .title {
          font-size: 22px;
        }

        .center {
          display: flex;
          width: 100%;
          height: 100%;
        }

        .legend {
          display: flex;
          flex-direction: column;
          gap: 10px;
          padding: 30px;
          border-left: 1px solid var(--lumo-contrast-5pct);
          width: 300px;
        }

        svg {
          width: 100%;
          height: 100%;
        }

        .subText {
          font-size: small;
          color: var(--lumo-contrast-50pct);
        }

        .leaf {
          fill: var(--lumo-primary-color);
          cursor: pointer;
        }

        .leaf:hover {
          text-decoration-line: underline;
          text-decoration-thickness: 1px;
        }

        .root {
          fill: var(--lumo-primary-color);
        }

      </style>
      <div class="heading">
        <qui-icon-menu>
            <vaadin-icon title="Back" icon="font-awesome-solid:chevron-left" @click=${() => this._back()}></vaadin-icon>
            <vaadin-icon title="Zoom In" icon="font-awesome-solid:magnifying-glass-plus" @click=${() => this._zoomIn()}></vaadin-icon>
            <vaadin-icon title="Zoom Out" icon="font-awesome-solid:magnifying-glass-minus" @click=${() => this._zoomOut()}></vaadin-icon>
            <slot></slot>
        </qui-icon-menu>
        <span class="title">${this.title}</span>
      </div>
      <div class="center">
        <svg>
          <defs id="defs"></defs>
          <g id="links"></g>
          <g id="nodes"></g>
        </svg>
        <div class="legend">
          ${this.types.map((type) =>
            this._renderLegendItem(type)
          )}
        </div>
      </div>
    `;
  }

  _renderLegendItem(type){
    if(type.description){
      return html`<div>
          <vaadin-icon icon="${type.icon}" style="color: ${type.color};font-size: x-small;"></vaadin-icon>
          ${type.description}
          ${this._renderLegendSubText(type)}
        </div>`;
    }
  }

  _renderLegendSubText(type){
    if(type.subtext){
      return html`<br><span class="subText">${type.subtext}</span>`;
    }
  }

  _back(){
    this.dispatchEvent(new Event('back-clicked'));
  }

  _zoomIn(){
    this.svg.call(this.zoomBehavior.scaleBy, 1.5);
    this.requestUpdate();
  }

  _zoomOut(){
    this.svg.call(this.zoomBehavior.scaleBy, 1 / 1.5);
    this.requestUpdate();
  }
}

customElements.define('qui-d3-force', QuiD3Force);