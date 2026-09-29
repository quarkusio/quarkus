import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';

/**
 * Bucket distribution chart: one horizontal bar per bucket, showing how many observations fell
 * into it. Unlike the other wrappers here this is deliberately NOT a time series - a histogram
 * answers "how were the measurements spread", which a line over time cannot show.
 *
 * `buckets` is a JSON string of [{ label, count }, ...] in bucket order, lowest first. Bars run
 * horizontally because bucket labels are ranges ("10ms - 25ms") that never fit under a vertical
 * bar, and the y-axis is a category axis so the buckets keep their order rather than being
 * positioned by value.
 * See https://echarts.apache.org/examples/en/editor.html?c=bar-y-category
 */
class EchartsHistogram extends EchartsAbstractCanvas {

    static get properties() {
        return {
            buckets: { type: String },
            name: { type: String },
        };
    }

    constructor() {
        super();
        this.buckets = "[]";
        this.name = "observations";
        this.primaryTextColor = "--lumo-body-text-color";
        this.barColor = "--lumo-primary-color";
    }

    getOption() {
        let textColor = this.primaryTextColor;
        if (textColor.startsWith('--')) {
            textColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(textColor);
        }
        let barColor = this.barColor;
        if (barColor.startsWith('--')) {
            barColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(barColor);
        }

        let parsed = [];
        try {
            parsed = JSON.parse(this.buckets);
        } catch (e) {
            parsed = [];
        }

        const option = {};
        option.tooltip = { trigger: "axis", axisPointer: { type: "shadow" } };
        option.grid = { top: "5%", left: "3%", right: "6%", bottom: "5%", containLabel: true };
        option.xAxis = {
            type: "value",
            minInterval: 1, // counts are whole numbers: no "2.5 observations" gridline
            axisLine: { lineStyle: { color: textColor } },
            axisLabel: { color: textColor },
        };
        // Reversed so the lowest bucket sits at the top, reading down the way the list does.
        option.yAxis = {
            type: "category",
            inverse: true,
            data: parsed.map(b => b.label),
            axisLine: { lineStyle: { color: textColor } },
            axisLabel: { color: textColor },
        };
        option.series = [{
            name: this.name,
            type: "bar",
            color: barColor,
            data: parsed.map(b => b.count),
        }];
        return option;
    }

    // Matches echarts-line: skip the redraw while the tab is hidden, the page re-seeds on return.
    updated(changedProps) {
        if (changedProps.has('buckets') && this._chart && !document.hidden) {
            this.reload();
        }
    }
}
customElements.define('echarts-histogram', EchartsHistogram);
