import { EchartsAbstractCanvas } from './echarts-abstract-canvas.js';

/**
 * Single-reading dial: where one value sits between a known minimum and maximum. This is the
 * plain gauge, as opposed to `echarts-gauge-grade`, which scores a percentage against coloured
 * bands - here the scale is the caller's own (bytes, threads, open files), so the ends of the
 * arc and the reading in the middle are both run through `valueFormatter`.
 * See https://echarts.apache.org/examples/en/editor.html?c=gauge-progress
 *
 * `valueFormatter` is a function, so it has to be set as a property (`.valueFormatter=${fn}`)
 * rather than an attribute. A dial only ever shows the latest reading: pair it with a chart if
 * the history matters too.
 */
class EchartsGauge extends EchartsAbstractCanvas {

    static get properties() {
        return {
            value: { type: Number },
            min: { type: Number },
            max: { type: Number },
            valueFormatter: { attribute: false },
            boundFormatter: { attribute: false },
        };
    }

    constructor() {
        super();
        this.value = 0;
        this.min = 0;
        this.max = 1;
        this.valueFormatter = null;
        this.boundFormatter = null;
        this.primaryTextColor = "--lumo-body-text-color";
        this.secondaryTextColor = "--lumo-secondary-text-color";
        this.arcColor = "--lumo-primary-color";
        this.trackColor = "--lumo-contrast-10pct";
    }

    getOption() {
        const textColor = this._color(this.primaryTextColor);
        const secondaryColor = this._color(this.secondaryTextColor);
        const arcColor = this._color(this.arcColor);
        const trackColor = this._color(this.trackColor);
        const format = this.valueFormatter ?? (v => String(v));
        const formatBound = this.boundFormatter ?? format;
        const min = this.min;

        // Everything ECharts sizes in pixels has to be scaled by hand, or a maximized card draws a
        // dial the height of the page with the same 16px reading in the middle of it. The radius
        // follows min(width, height), so the type scales against that too, never below the size
        // that suits a card in the grid.
        const extent = Math.min(this._width ?? 0, this._height ?? 0);
        const detailFont = Math.max(16, Math.round(extent * 0.09));
        const labelFont = Math.max(10, Math.round(extent * 0.05));
        const arcWidth = Math.max(10, Math.round(extent * 0.035));

        const serie = {
            type: 'gauge',
            // Half a dial rather than the default three-quarter one: a card is much wider than it
            // is tall, and a semicircle wastes none of that height on an arc that curls back down.
            startAngle: 180,
            endAngle: 0,
            center: ['50%', '72%'],
            // Under 100%: the arc is twice as wide as it is tall, so a radius that fills the height
            // would run off both sides of a box this narrow, taking the end label with it.
            radius: '82%',
            min: min,
            max: this.max,
            // One label, on the end that carries information. Where the arc starts is not in
            // question, and intermediate ticks on a dial this small cannot be read anyway.
            splitNumber: 1,
            axisLine: { lineStyle: { width: arcWidth, color: [[1, trackColor]] } },
            progress: { show: true, width: arcWidth, itemStyle: { color: arcColor } },
            // The filled arc already says how far along the scale the value is, and a needle at
            // this size only crosses the reading printed inside the dial.
            pointer: { show: false },
            axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: {
                color: secondaryColor,
                fontSize: labelFont,
                // Inside the arc: far enough in to clear the band itself, plus the height of the
                // text, or a bigger dial draws the label straddling its own end.
                distance: -(labelFont + arcWidth),
                formatter: value => (value === min ? '' : formatBound(value)),
            },
            detail: {
                // No count-up animation: the card header shows the same reading, and a dial still
                // settling towards it disagrees with the header for as long as it runs.
                valueAnimation: false,
                offsetCenter: [0, '-10%'],
                fontSize: detailFont,
                color: textColor,
                formatter: format,
            },
            data: [{ value: this.value }],
        };
        return { series: [serie] };
    }

    /** Lumo custom properties have to be resolved against the host before ECharts sees them. */
    _color(value) {
        return value.startsWith('--')
            ? getComputedStyle(this.shadowRoot.host).getPropertyValue(value)
            : value;
    }

    // Matches echarts-line: skip the redraw while the tab is hidden, the page re-seeds on return.
    updated(changedProps) {
        const redraw = changedProps.has('value') || changedProps.has('min') || changedProps.has('max')
            || changedProps.has('valueFormatter') || changedProps.has('boundFormatter');
        if (redraw && this._chart && !document.hidden) {
            this.reload();
        }
    }
}
customElements.define('echarts-gauge', EchartsGauge);
