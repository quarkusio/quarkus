import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';
/**
 * This wraps the Horizontal Stacked Bar echart into a component
 * see https://echarts.apache.org/examples/en/editor.html?c=bar-y-category-stack
 */
class EchartsHorizontalStackedBar extends EchartsAbstractCanvas {

    static get properties() {
        return {
            name: {type: String},
            height: {type: String},
            sectionTitles: { type: String},
            sectionValues: { type: String},
            sectionColors: { type: String},
        };
    }

    constructor() {
        super();
        this.name = "Bar";
        this.height = "40px";
        this.sectionTitles = "";
        this.sectionValues = "";
        this.sectionColors = "grey";
        this.primaryTextColor = "--lumo-body-text-color";
    }

    getOption(){

        let textColor = this.primaryTextColor;
        if(textColor.startsWith('--')){
            textColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(textColor);
        }

        const sectionColorsArray = this.sectionColors.split(',');
        const colors = [];

        for (var cc = 0; cc < sectionColorsArray.length; cc++) {
            let colorString = sectionColorsArray[cc];
            if(colorString.startsWith("--")){
                colorString = getComputedStyle(this.shadowRoot.host).getPropertyValue(colorString);
            }
            colors.push(colorString);
        }

        const sectionTitlesArray = this.sectionTitles.split(',');
        const sectionValuesArray = this.sectionValues.split(',');

        const option = new Object();
        // Tooltip
        option.tooltip = new Object();
        option.tooltip.trigger = "axis";
        // Legend
        option.legend = new Object();
        option.legend.show = false;

        // Grid
        option.grid = new Object();
        option.grid.left = '3%';
        option.grid.right = '4%';
        option.grid.bottom = '3%';
        option.grid.containLabel = false;
        // AxisLabel
        option.axisLabel = new Object();
        option.axisLabel.color = textColor;
        // X Axis
        option.xAxis = new Object();
        option.xAxis.type = 'value';
        // Y Axis
        option.yAxis = new Object();
        option.yAxis.type = 'category';
        option.yAxis.data = [this.name];
        option.yAxis.show = false;
        // Height
        option.height = this.height;

        // Series
        option.series = [];

        for (var count = 0; count < sectionTitlesArray.length; count++) {
            let title = sectionTitlesArray[count];
            let value = sectionValuesArray[count];
            let color = colors[count];

            const serie = new Object();
            serie.name = title;
            serie.type = 'bar';
            serie.stack = 'total';
            serie.data = [value];
            serie.color =  color;
            option.series.push(serie);
        }

        return option;
    }

}
customElements.define('echarts-horizontal-stacked-bar', EchartsHorizontalStackedBar);
