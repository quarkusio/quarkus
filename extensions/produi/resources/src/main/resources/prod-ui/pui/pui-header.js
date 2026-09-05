import { LitElement, html, css } from 'lit';
import { applicationName, applicationVersion } from '../produi-app-info.js';

const QUARKUS_BLUE = 'hsla(213, 94%, 58%, 1)';
const QUARKUS_RED = 'hsla(4, 90%, 58%, 1)';
const QUARKUS_DARK = 'hsla(220, 13%, 10%, 1)';
const QUARKUS_LIGHT = 'hsla(220, 14%, 97%, 1)';

export class PuiHeader extends LitElement {

    static styles = css`
        :host {
            display: block;
            height: 50px;
            background: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }
        .header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 100%;
            padding: 0 16px;
        }
        .left {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .logo svg {
            width: 28px;
            height: 28px;
        }
        .logo-text {
            font-size: 15px;
            font-weight: 600;
            color: var(--lumo-header-text-color);
            cursor: pointer;
        }
        .title {
            font-size: 15px;
            font-weight: 500;
            color: var(--lumo-header-text-color);
        }
        .breadcrumb-sep {
            padding: 0 8px;
            color: var(--lumo-contrast-30pct);
            font-weight: 300;
        }
        .subtitle {
            font-size: 14px;
            font-weight: 400;
            color: var(--lumo-contrast-50pct);
        }
        .right {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .app-info {
            font-size: 12px;
            color: var(--lumo-contrast-40pct);
            white-space: nowrap;
        }
        .sub-tabs {
            display: flex;
            gap: 4px;
        }
        .sub-tab {
            padding: 6px 12px;
            font-size: 13px;
            cursor: pointer;
            border-radius: 4px;
            color: var(--lumo-secondary-text-color);
            border: none;
            background: none;
        }
        .sub-tab:hover {
            background: var(--lumo-contrast-5pct);
        }
        .sub-tab.active {
            color: var(--lumo-primary-text-color);
            background: var(--lumo-primary-color-10pct);
            font-weight: 500;
        }
        .theme-toggle {
            cursor: pointer;
            background: none;
            border: 1px solid var(--lumo-contrast-20pct);
            border-radius: 50%;
            width: 30px;
            height: 30px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--lumo-secondary-text-color);
            font-size: 14px;
        }
        .theme-toggle:hover {
            background: var(--lumo-contrast-5pct);
            color: var(--lumo-body-text-color);
        }
    `;

    static properties = {
        _dark: { state: true },
        pageTitle: { type: String },
        subTitle: { type: String },
        subPages: { type: Array },
        activeSubPage: { type: String }
    };

    constructor() {
        super();
        this._dark = localStorage.getItem('pui-theme') === 'dark';
        this.pageTitle = null;
        this.subTitle = null;
        this.subPages = [];
        this.activeSubPage = null;
    }

    render() {
        const center = this._dark ? QUARKUS_LIGHT : QUARKUS_DARK;
        return html`
            <div class="header">
                <div class="left">
                    <div class="logo">${this._renderLogo(center)}</div>
                    <span class="logo-text" @click=${this._goHome}>Prod UI</span>
                    ${this.pageTitle ? html`
                        <span class="breadcrumb-sep">/</span>
                        <span class="title">${this.pageTitle}</span>
                    ` : ''}
                    ${this.subTitle ? html`
                        <span class="breadcrumb-sep">/</span>
                        <span class="subtitle">${this.subTitle}</span>
                    ` : ''}
                </div>
                <div class="right">
                    ${this.subPages && this.subPages.length > 1 ? html`
                        <div class="sub-tabs">
                            ${this.subPages.map(sp => html`
                                <button class="sub-tab ${sp.componentLink === this.activeSubPage ? 'active' : ''}"
                                        @click=${() => this._selectSubPage(sp)}>
                                    ${sp.title}
                                </button>
                            `)}
                        </div>
                    ` : html`
                        <span class="app-info">${applicationName} ${applicationVersion}</span>
                    `}
                    <button class="theme-toggle" @click=${this._toggleTheme}
                            title="Toggle dark/light theme">&#9681;</button>
                </div>
            </div>
        `;
    }

    _goHome() {
        this.dispatchEvent(new CustomEvent('navigate-home'));
    }

    _selectSubPage(subPage) {
        this.dispatchEvent(new CustomEvent('navigate-subpage', { detail: subPage }));
    }

    _renderLogo(center) {
        return html`
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
                <polygon fill="${QUARKUS_BLUE}" points="669.34 180.57 512 271.41 669.34 362.25 669.34 180.57"/>
                <polygon fill="${QUARKUS_RED}" points="354.66 180.57 354.66 362.25 512 271.41 354.66 180.57"/>
                <polygon fill="${center}" points="669.34 362.25 512 271.41 354.66 362.25 512 453.09 669.34 362.25"/>
                <polygon fill="${QUARKUS_BLUE}" points="188.76 467.93 346.1 558.76 346.1 377.09 188.76 467.93"/>
                <polygon fill="${QUARKUS_RED}" points="346.1 740.44 503.43 649.6 346.1 558.76 346.1 740.44"/>
                <polygon fill="${center}" points="346.1 377.09 346.1 558.76 503.43 649.6 503.43 467.93 346.1 377.09"/>
                <polygon fill="${QUARKUS_BLUE}" points="677.9 740.44 677.9 558.76 520.57 649.6 677.9 740.44"/>
                <polygon fill="${QUARKUS_RED}" points="835.24 467.93 677.9 377.09 677.9 558.76 835.24 467.93"/>
                <polygon fill="${center}" points="520.57 649.6 677.9 558.76 677.9 377.09 520.57 467.93 520.57 649.6"/>
                <path fill="${QUARKUS_BLUE}" d="M853.47,1H170.53C77.29,1,1,77.29,1,170.53V853.47C1,946.71,77.29,1023,170.53,1023h467.7L512,716.39,420.42,910H170.53C139.9,910,114,884.1,114,853.47V170.53C114,139.9,139.9,114,170.53,114H853.47C884.1,114,910,139.9,910,170.53V853.47C910,884.1,884.1,910,853.47,910H705.28l46.52,113H853.47c93.24,0,169.53-76.29,169.53-169.53V170.53C1023,77.29,946.71,1,853.47,1Z"/>
            </svg>
        `;
    }

    _toggleTheme() {
        this._dark = !this._dark;
        localStorage.setItem('pui-theme', this._dark ? 'dark' : 'light');
        applyTheme(this._dark);
    }
}

const LIGHT_THEME = {
    '--lumo-base-color': 'hsla(220, 14%, 97%, 1)',
    '--lumo-contrast': 'hsla(220, 13%, 10%, 1)',
    '--lumo-primary-color': QUARKUS_BLUE,
    '--lumo-primary-text-color': QUARKUS_BLUE,
    '--lumo-primary-contrast-color': 'hsla(0, 0%, 100%, 1)',
    '--lumo-primary-color-10pct': 'hsla(213, 94%, 58%, 0.1)',
    '--lumo-error-color': QUARKUS_RED,
    '--lumo-error-text-color': 'hsla(3, 90%, 42%, 1)',
    '--lumo-header-text-color': 'hsla(0, 0%, 13%, 1)',
    '--lumo-body-text-color': 'hsla(0, 0%, 20%, 0.94)',
    '--lumo-secondary-text-color': 'hsla(0, 0%, 40%, 0.69)',
    '--lumo-tertiary-text-color': 'hsla(0, 0%, 50%, 0.52)',
    '--lumo-disabled-text-color': 'hsla(0, 0%, 60%, 0.26)',
    '--lumo-warning-color': 'hsla(30, 100%, 50%, 1)',
    '--lumo-success-color': 'hsla(145, 72%, 30%, 1)',
    '--lumo-contrast-5pct': 'hsla(0, 0%, 13%, 0.05)',
    '--lumo-contrast-10pct': 'hsla(0, 0%, 13%, 0.10)',
    '--lumo-contrast-20pct': 'hsla(0, 0%, 13%, 0.20)',
    '--lumo-contrast-30pct': 'hsla(0, 0%, 13%, 0.30)',
    '--lumo-contrast-40pct': 'hsla(0, 0%, 13%, 0.40)',
    '--lumo-contrast-50pct': 'hsla(0, 0%, 13%, 0.50)',
    '--lumo-contrast-60pct': 'hsla(0, 0%, 13%, 0.60)',
    '--lumo-contrast-70pct': 'hsla(0, 0%, 13%, 0.70)',
    '--lumo-contrast-80pct': 'hsla(0, 0%, 13%, 0.80)',
    '--lumo-contrast-90pct': 'hsla(0, 0%, 13%, 0.90)',
    '--quarkus-blue': QUARKUS_BLUE,
    '--quarkus-red': QUARKUS_RED,
    '--quarkus-center': QUARKUS_DARK,
};

const DARK_THEME = {
    '--lumo-base-color': 'hsla(220, 13%, 10%, 1)',
    '--lumo-contrast': 'hsla(220, 14%, 97%, 1)',
    '--lumo-primary-color': QUARKUS_BLUE,
    '--lumo-primary-text-color': QUARKUS_BLUE,
    '--lumo-primary-contrast-color': 'hsla(0, 0%, 100%, 1)',
    '--lumo-primary-color-10pct': 'hsla(213, 94%, 58%, 0.1)',
    '--lumo-error-color': QUARKUS_RED,
    '--lumo-error-text-color': 'hsla(3, 90%, 63%, 1)',
    '--lumo-header-text-color': 'hsla(0, 0%, 100%, 1)',
    '--lumo-body-text-color': 'hsla(0, 0%, 90%, 0.9)',
    '--lumo-secondary-text-color': 'hsla(0, 0%, 70%, 0.7)',
    '--lumo-tertiary-text-color': 'hsla(0, 0%, 60%, 0.5)',
    '--lumo-disabled-text-color': 'hsla(0, 0%, 50%, 0.32)',
    '--lumo-warning-color': 'hsla(30, 100%, 50%, 1)',
    '--lumo-success-color': 'hsla(145, 65%, 42%, 1)',
    '--lumo-contrast-5pct': 'hsla(0, 0%, 100%, 0.05)',
    '--lumo-contrast-10pct': 'hsla(0, 0%, 100%, 0.10)',
    '--lumo-contrast-20pct': 'hsla(0, 0%, 100%, 0.20)',
    '--lumo-contrast-30pct': 'hsla(0, 0%, 100%, 0.30)',
    '--lumo-contrast-40pct': 'hsla(0, 0%, 100%, 0.40)',
    '--lumo-contrast-50pct': 'hsla(0, 0%, 100%, 0.50)',
    '--lumo-contrast-60pct': 'hsla(0, 0%, 100%, 0.60)',
    '--lumo-contrast-70pct': 'hsla(0, 0%, 100%, 0.70)',
    '--lumo-contrast-80pct': 'hsla(0, 0%, 100%, 0.80)',
    '--lumo-contrast-90pct': 'hsla(0, 0%, 100%, 0.90)',
    '--quarkus-blue': QUARKUS_BLUE,
    '--quarkus-red': QUARKUS_RED,
    '--quarkus-center': QUARKUS_LIGHT,
};

export function applyTheme(dark) {
    const theme = dark ? DARK_THEME : LIGHT_THEME;
    for (const [key, value] of Object.entries(theme)) {
        document.body.style.setProperty(key, value);
    }
    // The theme is a set of inline CSS custom properties on <body>; components that paint to a
    // canvas (e.g. ECharts) cannot pick up the change via CSS alone, so broadcast it for them.
    window.dispatchEvent(new CustomEvent('pui-theme-changed', { detail: { dark } }));
}

customElements.define('pui-header', PuiHeader);
