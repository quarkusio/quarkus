import { LitElement, html, css } from 'lit';
import { themeState } from 'theme-state';
import { StorageController } from 'storage-controller';
import '@vaadin/button';
import { msg, updateWhenLocaleChanges } from 'localization';

export class QwcThemeSwitch extends LitElement {
    storageControl = new StorageController(this);

    themes = [
        { id: 0, key: 'desktop', icon: 'font-awesome-solid:desktop' },
        { id: 1, key: 'light',   icon: 'font-awesome-solid:sun' },
        { id: 2, key: 'dark',    icon: 'font-awesome-solid:moon' }
    ];

    static styles = css`
        .themeButtons { 
            display: flex; 
            gap: 20px; 
            padding-bottom: 15px; 
        }
        .themeBlock { 
            border-radius: 8px; 
            padding: 10px; 
            display: flex; 
            flex-direction: column; 
            align-items: center; 
            gap: 4px; 
            width: 60px; 
        }
        .selected { 
            border: 3px solid var(--lumo-contrast-10pct); 
        }
        .selectable { 
            border: 3px solid var(--lumo-base-color); 
            cursor: pointer; 
        }
        .selectable:hover { 
            border: 3px solid var(--lumo-contrast-10pct); 
        }
    `;

    static properties = {
        _selectedThemeIndex: { state: true },
        _desktopTheme: { state: false }
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._restoreThemePreference();
        window.addEventListener('storage-changed', (event) => this._storageChange(event));
    }

    connectedCallback() {
        super.connectedCallback();
        this._desktopTheme = 'dark'; // default
        if(window.matchMedia){
            if(window.matchMedia('(prefers-color-scheme: light)').matches){
                this._desktopTheme = 'light';
            }

            // watch OS theme changes when in Desktop mode
            window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', e => {
                this._desktopTheme = e.matches ? 'light' : 'dark';
                if (this._selectedThemeIndex === 0) this._changeToSelectedThemeIndex();
            });
        }
        
        this._changeToSelectedThemeIndex();
    }

    render() {
        return html`
            <div class="themeButtons" @wheel=${this._onWheel}>
                ${this.themes.map((theme) => this._renderButton(theme))}
            </div>
        `;
    }

    _labelFor(key) {
        switch (key) {
            case 'desktop': return msg('Workspace', { id: 'theme-desktop-label' });
            case 'light':   return msg('Light',     { id: 'theme-light-label' });
            case 'dark':    return msg('Dark',      { id: 'theme-dark-label' });
            default:        return key;
        }
    }

    _renderButton(theme) {
        const selected = this.themes[this._selectedThemeIndex].id === theme.id;
        const label = this._labelFor(theme.key);
        const content = html`
            <vaadin-icon icon="${theme.icon}"></vaadin-icon>
            <span>${label}</span>
        `;
        return selected
            ? html`<div class="themeBlock selected" aria-current="true">${content}</div>`
            : html`<div class="themeBlock selectable" @click=${() => this._selectTheme(theme)}>${content}</div>`;
    }

    _storageChange(event) {
        if (event.detail.method === 'remove' && event.detail.key.startsWith('qwc-theme-switch-')) {
            this._restoreThemePreference();
            this._changeToSelectedThemeIndex();
        }
    }

    _onWheel(event) {
        event.preventDefault();
        this._selectedThemeIndex = event.deltaY < 0
            ? (this._selectedThemeIndex + 1) % this.themes.length
            : (this._selectedThemeIndex - 1 + this.themes.length) % this.themes.length;
        this._changeToSelectedThemeIndex();
    }

    _selectTheme(theme) {
        this._selectedThemeIndex = theme.id;
        this._changeToSelectedThemeIndex();
    }

    _changeToSelectedThemeIndex() {
        const theme = this.themes[this._selectedThemeIndex];
        this.storageControl.set('theme-preference', theme.id);
        if (theme.key === 'desktop') {
            themeState.changeTo(this._desktopTheme); // follow OS
        } else {
            themeState.changeTo(theme.key);
        }
    }

    _restoreThemePreference() {
        const stored = this.storageControl.get('theme-preference');
        this._selectedThemeIndex = (stored ?? 0) * 1; // ensure number
    }
}
customElements.define('qwc-theme-switch', QwcThemeSwitch);
