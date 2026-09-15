import { LitElement, html, css } from 'lit';
import * as echarts from 'echarts';

/**
 * Abstract base for Prod UI ECharts wrapper components. It owns the chart lifecycle
 * (init, resize, dispose and option updates); concrete subclasses only implement
 * getOption(). This mirrors Dev UI's echarts-abstract-canvas, adapted for the Prod UI
 * bundle: esbuild resolves the bare 'echarts' specifier from org.mvnpm:echarts, so there
 * is no import map, and Prod UI has no theme-state signal to observe.
 */
export class PuiEchart extends LitElement {

    static styles = css`
        :host { display: block; width: 100%; height: 100%; overflow: hidden; }
        .canvas { width: 100%; height: 100%; }
    `;

    constructor() {
        super();
        this._chart = null;
        this._resizeHandler = () => {
            if (this._chart) {
                this._chart.resize();
            }
        };
        // Prod UI applies its theme as inline CSS custom properties on <body> and broadcasts a
        // 'pui-theme-changed' event; canvas charts have to re-read the colors and repaint themselves.
        this._themeHandler = () => this.reload();
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('resize', this._resizeHandler);
        window.addEventListener('pui-theme-changed', this._themeHandler);
    }

    disconnectedCallback() {
        window.removeEventListener('resize', this._resizeHandler);
        window.removeEventListener('pui-theme-changed', this._themeHandler);
        if (this._observer) {
            this._observer.disconnect();
            this._observer = null;
        }
        if (this._chart) {
            this._chart.dispose();
            this._chart = null;
        }
        super.disconnectedCallback();
    }

    firstUpdated() {
        super.firstUpdated();
        // A page can hold hundreds of charts; ECharts init is not free, so defer it until the
        // component actually scrolls into view. Off-screen charts then cost nothing until needed.
        this._observer = new IntersectionObserver((entries) => {
            if (entries.some(entry => entry.isIntersecting)) {
                this._observer.disconnect();
                this._observer = null;
                this._init();
            }
        });
        this._observer.observe(this);
    }

    _init() {
        if (this._chart) {
            return;
        }
        const container = this.shadowRoot.querySelector('.canvas');
        this._chart = echarts.init(container);
        this._chart.setOption(this.getOption());
    }

    updated() {
        // Any reactive property change re-runs the option build and fully replaces it (notMerge).
        if (this._chart) {
            this._chart.setOption(this.getOption(), true);
        }
    }

    // Rebuild the option (re-reading theme colors) and repaint. Used on theme changes.
    reload() {
        if (this._chart) {
            this._chart.setOption(this.getOption(), true);
        }
    }

    render() {
        return html`<div class="canvas"></div>`;
    }

    // Resolve a Lumo CSS custom property to a concrete color so charts match the app theme.
    cssColor(name, fallback) {
        const value = getComputedStyle(this).getPropertyValue(name);
        return value && value.trim() ? value.trim() : fallback;
    }

    // Format a number compactly: large values are abbreviated (1500 -> 1.5K, 2_000_000 -> 2M) and
    // small/fractional values are rounded to a few significant digits so raw floats such as
    // 0.07285620613449256 do not spill across a label or gauge detail.
    abbreviate(value) {
        if (!Number.isFinite(value)) {
            return String(value);
        }
        const abs = Math.abs(value);
        if (abs >= 1e9) {
            return (value / 1e9).toFixed(1).replace(/\.0$/, '') + 'B';
        }
        if (abs >= 1e6) {
            return (value / 1e6).toFixed(1).replace(/\.0$/, '') + 'M';
        }
        if (abs >= 1e3) {
            return (value / 1e3).toFixed(1).replace(/\.0$/, '') + 'K';
        }
        if (value === 0 || Number.isInteger(value)) {
            return String(value);
        }
        if (abs >= 1) {
            return value.toFixed(2).replace(/\.?0+$/, '');
        }
        // Fractions below 1: keep three significant digits (0.0728..., 0.943, 4.97e-6).
        return Number(value.toPrecision(3)).toString();
    }

    getOption() {
        throw new Error("getOption() must be implemented by a subclass");
    }
}
