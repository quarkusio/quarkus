import { LitElement, html, css } from 'lit';
import 'echarts/dist/echarts.min.js';
import { themeState } from 'theme-state';

/**
 * This is an abstract components used as a base for all echarts components
 */
class EchartsAbstractCanvas extends LitElement {
    static get styles() {
        return css`
        :host {
            display: block;
            width: 100%;
            height: 100%;
            overflow: hidden;
        }
      `;
    }

    static properties = {
        _width: {state: true},
        _height: {state: true},
        _chart: {state: true},
        _option: {state: true},
    };

    constructor() {
        super();
        this._chart = null;
    }

    connectedCallback() {
        super.connectedCallback();
        // Observe the element rather than the window: a chart can be resized without the window
        // being touched at all - a card maximized, a panel expanded, a layout column collapsed -
        // and the canvas is sized in pixels, so it has to be told.
        this._resizeObserver = new ResizeObserver(() => this._handleResize());
        this._resizeObserver.observe(this);

        this.themeStateObserver = () => this.reload();
        themeState.addObserver(this.themeStateObserver);
    }

    disconnectedCallback() {
        this._resizeObserver.disconnect();
        themeState.removeObserver(this.themeStateObserver);
        super.disconnectedCallback();
    }

    render() {
        if(!this._height){
            this._height = parseFloat(getComputedStyle(this).getPropertyValue('height'), 10) - 20;
        }
        if(!this._width){
            this._width = parseFloat(getComputedStyle(this).getPropertyValue('width'), 10) - 20;
        }
        
        if(this._chart && this._chart !== null){
            this._option = this.getOption();
            this._chart.setOption(this._option);
        }
        return html`<div class="canvasContainer" style="width:${this._width}px;height:${this._height}px;"></div>`;
    }

    chartClicked(e){
        // Can be implemented in the implementation
    }

    _handleResize(e){
        this._width = parseFloat(getComputedStyle(this).getPropertyValue('width'), 10) - 20;
        this._height = parseFloat(getComputedStyle(this).getPropertyValue('height'), 10) - 20;

        // The observer delivers an initial measurement before firstUpdated has built the chart.
        if(this._chart){
            this._chart.resize({width: this._width, height: this._height});
        }
    }

    firstUpdated(){
        super.firstUpdated();
        let canvasContainer = this.shadowRoot.querySelector('.canvasContainer');
        this._chart = echarts.init(canvasContainer);
        this._option = this.getOption();
        this._chart.setOption(this._option);

        this._chart.on('click', params => {
            this.chartClicked(params.data);
        });
    }

    getOption(){
        throw new Error("Method 'getOption()' must be implemented.");
    }

    reload(){
        this._option = this.getOption();
        this._chart.setOption(this._option, true);
        this.requestUpdate();
    }

}

export { EchartsAbstractCanvas };
