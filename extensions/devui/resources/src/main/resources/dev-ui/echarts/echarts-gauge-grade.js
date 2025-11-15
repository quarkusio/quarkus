import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';
/**
 * This wraps the Gauge Grade echart into a component
 * see https://echarts.apache.org/examples/en/editor.html?c=gauge-grade
 */
class EchartsGaugeGrade extends EchartsAbstractCanvas {

    static get properties() {
        return {
            percentage: { type: Number },
            percentageFontSize: { type: Number },
            title: { type: String },
            titleFontSize: { type: Number },
            sectionTitles: { type: String },
            sectionColors: { type: String },
            primaryTextColor: { type: String },
            secondaryTextColor: { type: String },
        };
    }

    constructor() {
        super();
        this.title = null;
        this.titleFontSize = 18;
        this.percentage = 50;
        this.percentageFontSize = 28;
        this.sectionTitles = null;
        this.sectionColors = "grey";
        this.primaryTextColor = "--lumo-body-text-color";
        this.secondaryTextColor = "--lumo-secondary-text-color";
    }

    getOption(){
        let textColor = this.primaryTextColor;
        if(textColor.startsWith('--')){
            textColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(textColor);
        }

        let secondaryColor = this.secondaryTextColor;
        if(secondaryColor.startsWith('--')){
            secondaryColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(secondaryColor);
        }
        
        const sectionColorsArray = this.sectionColors.split(',');
        let sectionColorsLength = sectionColorsArray.length;
        let partSize = (1/sectionColorsLength);
        const colors = [];

        for (var cc = 0; cc < sectionColorsLength; cc++) {
            let p = cc + 1;
            let max = (p * partSize).toFixed(2);
            let colorString = sectionColorsArray[cc];
            if(colorString.startsWith("--")){
                colorString = getComputedStyle(this.shadowRoot.host).getPropertyValue(colorString);
            }
            colors.push([max, colorString]);
        }

        const partsSize = sectionColorsLength + 1;
        const st = this.sectionTitles;
        const serie = new Object();
        serie.type = 'gauge';
        serie.startAngle = 180;
        serie.endAngle = 0;
        serie.center = [];
        serie.center.push('50%');
        serie.center.push('75%');
        serie.radius = '90%';
        serie.min = 0;
        serie.max = 1;
        serie.splitNumber = partsSize*2;

        serie.axisLine = this._createAxisElement(null,6,colors);

        const pointer = new Object();
        pointer.icon = 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z';
        pointer.length = '12%';
        pointer.width = 20;
        pointer.offsetCenter = [];
        pointer.offsetCenter.push(0);
        pointer.offsetCenter.push('-60%');
        
        const itemStyle = new Object();
        itemStyle.color = 'inherit';
        pointer.itemStyle = itemStyle;
        serie.pointer = pointer;

        serie.axisTick = this._createAxisElement(2,12,secondaryColor);
        serie.splitLine = this._createAxisElement(5,20,secondaryColor);

        const axisLabel = new Object();
        axisLabel.color = secondaryColor;
        axisLabel.fontSize = 20;
        axisLabel.distance = -60;
        axisLabel.rotate = 'tangential';
        axisLabel.formatter = function (value) {
            if(st && st!== "null"){

                var titles = st.split(',');
                let numberOfSections = titles.length;


                let partSize = (1/numberOfSections).toFixed(2);

                for (var i = 0; i < numberOfSections; i++) {
                    let part = i + 1;

                    let min = i * partSize;
                    let max = part * partSize;

                    min = min + 0.1;
                    max = max - 0.1;

                    if(value > min && value < max ){
                        return titles[i];
                    }
                }
            }
            return '';
        };

        serie.axisLabel = axisLabel;
        
        
        const title = new Object();
        title.offsetCenter = [0, '-10%'];
        title.fontSize = this.titleFontSize;
        title.color = textColor;
        serie.title = title;

        const detail = new Object();
        detail.fontSize = this.percentageFontSize;
        detail.offsetCenter = [0, '-35%'];
        detail.valueAnimation = true;
        detail.formatter = function (value) {
            return Math.round(value * 100) + '%';
        };
        detail.color = 'inherit';
        serie.detail = detail;
        
        const data1 = new Object();
        data1.name = this.title;
        data1.value = this.percentage/100;
        const data = [data1];
        serie.data = data;

        const gaugeGradeOption = new Object();
        gaugeGradeOption.series = [serie];
        
        return gaugeGradeOption;
        
    }

    _createAxisElement(width, length, color){
        const axisTick = new Object();
        if(length){
            axisTick.length = length;
        }
        const lineStyle = new Object();
        lineStyle.color = color;
        if(width){
            lineStyle.width = width;
        }
        axisTick.lineStyle = lineStyle;
        return axisTick;
    }
}
customElements.define('echarts-gauge-grade', EchartsGaugeGrade);
