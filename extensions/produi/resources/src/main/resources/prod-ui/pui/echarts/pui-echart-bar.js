import { PuiEchart } from './pui-echart.js';

/**
 * Bar chart wrapper (vertical or horizontal). Data is passed as attributes:
 *  - title:      chart title (optional)
 *  - categories: JSON array of category-axis labels
 *  - series:     JSON array of { name, data:[...] } objects
 *  - valueName:  value-axis label (optional)
 *  - horizontal: render horizontally (category on the y-axis) - better for long labels
 *
 * Registered as the bare specifier 'pui-echart-bar' via the Prod UI esbuild alias so
 * extension page components can import it without knowing the shell directory layout.
 */
export class PuiEchartBar extends PuiEchart {

    static properties = {
        title: { type: String },
        categories: { type: String },
        series: { type: String },
        valueName: { type: String },
        horizontal: { type: Boolean }
    };

    getOption() {
        const textColor = this.cssColor('--lumo-body-text-color', '#333');
        const mutedColor = this.cssColor('--lumo-secondary-text-color', '#888');
        const axisColor = this.cssColor('--lumo-contrast-20pct', '#cccccc');
        const splitColor = this.cssColor('--lumo-contrast-10pct', '#eeeeee');
        const barColor = this.cssColor('--lumo-primary-color', '#4695eb');
        const surfaceColor = this.cssColor('--lumo-base-color', '#ffffff');

        const categories = this._parse(this.categories);
        const series = this._parse(this.series);
        const singleSeries = series.length === 1;

        const categoryAxis = {
            type: 'category',
            data: categories,
            axisLine: { lineStyle: { color: axisColor } },
            axisTick: { lineStyle: { color: axisColor } },
            axisLabel: { color: mutedColor }
        };
        const valueAxis = {
            type: 'value',
            name: this.valueName || '',
            nameTextStyle: { color: mutedColor },
            axisLine: { show: false },
            axisLabel: { color: mutedColor, formatter: (v) => this.abbreviate(v) },
            splitLine: { lineStyle: { color: splitColor } }
        };

        return {
            backgroundColor: 'transparent',
            textStyle: { color: textColor },
            title: this.title
                ? { text: this.title, textStyle: { color: textColor, fontSize: 14, fontWeight: 'normal' } }
                : undefined,
            tooltip: {
                trigger: 'axis',
                axisPointer: { type: 'shadow' },
                backgroundColor: surfaceColor,
                borderColor: axisColor,
                textStyle: { color: textColor },
                valueFormatter: (v) => this.abbreviate(v)
            },
            legend: singleSeries
                ? undefined
                : { textStyle: { color: textColor }, top: this.title ? 26 : 2 },
            grid: { top: this.title ? 52 : 24, left: '3%', right: '5%', bottom: '3%', containLabel: true },
            xAxis: this.horizontal ? valueAxis : categoryAxis,
            yAxis: this.horizontal ? categoryAxis : valueAxis,
            series: series.map(s => ({
                name: s.name,
                type: 'bar',
                emphasis: { focus: 'series' },
                // A single series looks better in the Quarkus primary colour; multi-series keep the palette.
                itemStyle: singleSeries ? { color: barColor, borderRadius: 2 } : { borderRadius: 2 },
                data: s.data
            }))
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
customElements.define('pui-echart-bar', PuiEchartBar);
