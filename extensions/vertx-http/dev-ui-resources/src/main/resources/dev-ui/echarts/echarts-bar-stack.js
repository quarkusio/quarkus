import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';

/**
 * This wraps the Bar Stack echart into a component
 * see https://echarts.apache.org/examples/en/editor.html?c=bar-stack
 */
class EchartsBarStack extends EchartsAbstractCanvas {
    
    static get properties() {
        return {
            xdata:{type: String}, 
            xdataName: {type: String},
            series: { type: String},
            ydataName: {type: String}
        };
    }

    constructor() {
        super();
        
        this.xdata = null;
        this.xdataName = null;
        this.series = null;
        this.primaryTextColor = "--lumo-body-text-color";
    }

    getOption(){

        let textColor = this.primaryTextColor;
        if(textColor.startsWith('--')){
            textColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(textColor);
        }
        const barStackOption = new Object();

        barStackOption.tooltip = new Object();
        barStackOption.tooltip.trigger = "item";
        barStackOption.tooltip.axisPointer= new Object();
        barStackOption.tooltip.axisPointer.type = "shadow";
        barStackOption.tooltip.formatter = function (params) {
            let namesUL = "<ul>";
            for (let i = 0; i < params.data.name.length; i++) {
                namesUL = namesUL + "<li>" + params.data.name[i] + "</li>";
            }
            namesUL = namesUL + "</ul>";
            return `
                 <b>${params.seriesName}</b></br>
                  ${namesUL}`;
        };
        barStackOption.legend = new Object();
        barStackOption.legend.textStyle = new Object();
        barStackOption.legend.textStyle.color = textColor;

        barStackOption.grid = new Object();
        barStackOption.grid.top = "20%";
        barStackOption.grid.left = "3%";
        barStackOption.grid.right = "4%";
        barStackOption.grid.bottom = "3%";
        barStackOption.grid.containLabel = true;

        let xAxis = new Object();
        xAxis.type = "category";

        xAxis.data = this.xdata.split(',');
        xAxis.axisLine = new Object();
        xAxis.axisLine.lineStyle = new Object();
        xAxis.axisLine.lineStyle.color = textColor;
        if(this.xdataName){
            xAxis.name = this.xdataName;
            xAxis.nameLocation = "center";
            xAxis.nameGap = 30;
        }
        barStackOption.xAxis = [];
        barStackOption.xAxis.push(xAxis);

        let yAxis = new Object();
        yAxis.type = "value";
        yAxis.axisLine = new Object();
        yAxis.axisLine.lineStyle = new Object();
        yAxis.axisLine.lineStyle.color = textColor;
        if(this.ydataName){
            yAxis.name = this.ydataName;
            yAxis.nameLocation = "center";
            yAxis.nameGap = 30;
        }

        barStackOption.yAxis = [];
        barStackOption.yAxis.push(yAxis);

        barStackOption.series = [];
        let seriesMap = new Map(Object.entries(JSON.parse(this.series)));
        

        for (let [key, value] of seriesMap) {
            let arr = [];

            for (let i = 0; i < value.length; i++) {
                 let val = value[i];
                 
                 
                 let dataItem = new Object();
                 dataItem.name = val;
                 dataItem.label = new Object();
                 if(val.length > 0){
                    dataItem.value = 1;  
                 }
                 arr.push(dataItem);
            }

            const serie = new Object();
            serie.name = key;
            serie.type = "bar";
            serie.stack = "stack";
            serie.emphasis = new Object();
            serie.emphasis.focus = "series";
            
            serie.data = arr;
            barStackOption.series.push(serie);
        }

        return barStackOption;

    }

}
customElements.define('echarts-bar-stack', EchartsBarStack);