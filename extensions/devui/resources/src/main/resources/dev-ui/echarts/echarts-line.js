import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';

/**
 * Generic time-series line chart wrapper. `series` is a JSON string of
 * [{ name, data: [[tsMillis, value], ...], dashed, showSymbol }, ...]. One line per entry,
 * time x-axis.
 * See https://echarts.apache.org/examples/en/editor.html?c=line-simple
 *
 * `step` draws the lines as a staircase, which is how a quantity that only ever jumps between
 * whole values (active tasks, an up-down counter) actually behaves - joining those points with
 * a slope invents intermediate values that never existed. `valueFormatter` is a function, so it
 * has to be set as a property (`.valueFormatter=${fn}`) rather than an attribute; it labels the
 * y-axis and the tooltip, letting the caller keep unit handling in one place.
 */
class EchartsLine extends EchartsAbstractCanvas {

    static get properties() {
        return {
            series: { type: String },
            showLegend: { type: Boolean },
            step: { type: Boolean },
            valueFormatter: { attribute: false },
        };
    }

    constructor() {
        super();
        this.series = "[]";
        this.showLegend = true;
        this.step = false;
        this.valueFormatter = null;
        this.primaryTextColor = "--lumo-body-text-color";
    }

    getOption() {
        let textColor = this.primaryTextColor;
        if (textColor.startsWith('--')) {
            textColor = getComputedStyle(this.shadowRoot.host).getPropertyValue(textColor);
        }

        const format = this.valueFormatter;

        const option = {};
        option.tooltip = { trigger: "axis", valueFormatter: format ?? undefined };
        if (this.showLegend) {
            // Vertical legend on the right so long series names (tag combinations) read on one line
            // each instead of wrapping across the top of the plot. Long names are truncated to keep
            // them inside the reserved gutter (full name shown on hover) so they never overlap the plot.
            option.legend = {
                type: "scroll",
                orient: "vertical",
                right: 0,
                top: "middle",
                textStyle: { color: textColor, width: 240, overflow: "truncate" },
                tooltip: { show: true },
            };
        }
        // Reserve room on the right for the side legend; when hidden the plot uses the full width.
        option.grid = { top: "8%", left: "3%", right: this.showLegend ? "36%" : "4%", bottom: "8%", containLabel: true };
        // hideOverlap drops colliding time labels instead of letting them clash on the narrow x-axis.
        option.xAxis = [{ type: "time", axisLabel: { hideOverlap: true }, axisLine: { lineStyle: { color: textColor } } }];
        option.yAxis = [{
            type: "value",
            axisLine: { lineStyle: { color: textColor } },
            axisLabel: format ? { formatter: format } : {},
        }];

        let parsed = [];
        try {
            parsed = JSON.parse(this.series);
        } catch (e) {
            parsed = [];
        }
        option.series = parsed.map(s => ({
            name: s.name,
            type: "line",
            step: this.step ? "end" : false,
            // Points are hidden by default because a dense series is all line and no dots, but a
            // gappy series needs them: a lone point between two gaps draws nothing on its own.
            showSymbol: s.showSymbol === true,
            // A per-series dash pattern lets a card draw related lines (a mean and its max, say)
            // without the two reading as unrelated measurements.
            lineStyle: s.dashed ? { type: "dashed" } : {},
            data: s.data,
        }));
        return option;
    }

    // Avoid recomputing/redrawing the chart while the tab is hidden; the metrics page
    // re-seeds and calls reload() on becoming visible again. When visible, defer to the
    // base class rendering.
    updated(changedProps) {
        const redraw = changedProps.has('series') || changedProps.has('step')
            || changedProps.has('valueFormatter');
        if (redraw && this._chart && !document.hidden) {
            this.reload();
        }
    }
}
customElements.define('echarts-line', EchartsLine);
