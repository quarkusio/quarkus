import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';
/**
 * This wraps the Pie echart into a component
 * see https://echarts.apache.org/examples/en/editor.html?c=pie-simple
 */
class EchartsPie extends EchartsAbstractCanvas {

    static get properties() {
        return {
            name: {type: String}, 
            sectionTitles: { type: String},
            sectionValues: { type: String},
            sectionColors: { type: String},
        };
    }

    constructor() {
        super();
        this.name = "Pie";
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
        const data = [];

        for (var cc = 0; cc < sectionTitlesArray.length; cc++) {
            let name = sectionTitlesArray[cc];
            let value = sectionValuesArray[cc];
            const d = new Object();
            d.name = name;
            d.value = value;

            d.label = new Object();
            d.label.color = textColor;

            data.push(d);
        }

        const pieOption = new Object();

        pieOption.tooltip = new Object();
        pieOption.tooltip.trigger = "item";
        // TODO: Make this an option to switch on
        // pieOption.legend = new Object();
        // pieOption.legend.orient = "horizontal";
        // pieOption.legend.left = "center";
        // pieOption.legend.top = "bottom";
        const serie = new Object();
        serie.name = this.name;

        serie.type = "pie";
        serie.radius = "50%";
        serie.color = colors;
        serie.data = data;
        serie.emphasis = new Object();
        serie.emphasis.itemStyle = new Object();
        serie.emphasis.itemStyle.shadowBlur = 10;
        serie.emphasis.itemStyle.shadowOffsetX = 0;


        pieOption.series = [];
        pieOption.series.push(serie);

        return pieOption;

    }

}
customElements.define('echarts-pie', EchartsPie);