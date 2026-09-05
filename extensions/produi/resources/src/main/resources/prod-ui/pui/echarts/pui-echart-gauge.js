import { PuiEchart } from './pui-echart.js';

/**
 * Radial gauge wrapper for a single scalar value. Data is passed as attributes:
 *  - title: gauge title (optional)
 *  - value: the numeric value to display
 *  - max:   axis maximum (optional; a "nice" ceiling above the value is derived when absent)
 *  - unit:  base unit shown under the value (optional)
 *
 * Registered as the bare specifier 'pui-echart-gauge' via the Prod UI esbuild alias so
 * extension page components can import it without knowing the shell directory layout.
 */
export class PuiEchartGauge extends PuiEchart {

    static properties = {
        title: { type: String },
        value: { type: Number },
        max: { type: Number },
        unit: { type: String }
    };

    getOption() {
        const textColor = this.cssColor('--lumo-body-text-color', '#333');
        const mutedColor = this.cssColor('--lumo-secondary-text-color', '#888');
        const trackColor = this.cssColor('--lumo-contrast-10pct', '#eeeeee');
        const arcColor = this.cssColor('--lumo-primary-color', '#4695eb');

        const value = Number.isFinite(this.value) ? this.value : 0;
        const max = Number.isFinite(this.max) && this.max > 0 ? this.max : this._niceMax(value);

        return {
            backgroundColor: 'transparent',
            title: this.title
                ? { text: this.title, left: 'center', top: 4,
                    textStyle: { color: textColor, fontSize: 13, fontWeight: 'normal' } }
                : undefined,
            series: [{
                type: 'gauge',
                min: 0,
                max: max,
                center: ['50%', '58%'],
                radius: '82%',
                progress: { show: true, width: 10, itemStyle: { color: arcColor } },
                pointer: { show: true, length: '60%', itemStyle: { color: arcColor } },
                axisLine: { lineStyle: { width: 10, color: [[1, trackColor]] } },
                axisTick: { show: false },
                splitLine: { length: 8, lineStyle: { color: mutedColor, width: 1 } },
                axisLabel: { distance: 12, color: mutedColor, fontSize: 9,
                    formatter: (v) => this.abbreviate(v) },
                anchor: { show: true, size: 8, itemStyle: { color: arcColor } },
                detail: {
                    valueAnimation: true,
                    offsetCenter: [0, '38%'],
                    color: textColor,
                    fontSize: 18,
                    formatter: () => this.abbreviate(value) + (this.unit ? ' ' + this.unit : '')
                },
                data: [{ value: value }]
            }]
        };
    }

    // Round the axis maximum up to a readable 1/2/5 x 10^n ceiling so the needle has headroom.
    // Ratios (value <= 1) simply use a full-scale of 1.
    _niceMax(value) {
        if (value <= 1) {
            return 1;
        }
        const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        const normalized = value / magnitude;
        let nice;
        if (normalized <= 1) {
            nice = 1;
        } else if (normalized <= 2) {
            nice = 2;
        } else if (normalized <= 5) {
            nice = 5;
        } else {
            nice = 10;
        }
        return nice * magnitude;
    }
}
customElements.define('pui-echart-gauge', PuiEchartGauge);
