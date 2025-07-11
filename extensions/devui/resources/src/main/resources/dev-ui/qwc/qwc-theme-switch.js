import { LitElement, html, css } from 'lit';
import { themeState } from 'theme-state';
import { StorageController } from 'storage-controller';
import '@vaadin/button';

/**
 * Basic theme switch
 */
export class QwcThemeSwitch extends LitElement {
    storageControl = new StorageController(this);
    
    themes = [
        { id: 0, name: 'Desktop', icon: 'font-awesome-solid:desktop' },
        { id: 1, name: 'Light', icon: 'font-awesome-solid:sun' },
        { id: 2, name: 'Dark', icon: 'font-awesome-solid:moon' }
    ];
    
    static styles = css`
        .themeButton {
            padding-left: 10px;
        }
        .button {
            --vaadin-button-background: var(--lumo-base-color);
        }
    `;

    static properties = {
        _selectedThemeIndex: {state: true},
    };

    constructor() {
        super();
        this._restoreThemePreference();
    }

    connectedCallback() {
        super.connectedCallback();
        this._desktopTheme = "dark"; // default
        if(window.matchMedia){
             if(window.matchMedia('(prefers-color-scheme: light)').matches){
                this._desktopTheme = "light";
             }
            
             // Change theme setting when OS theme change
             window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', e => {
                 if(e.matches){
                    this._desktopTheme = "light";
                 }else{
                    this._desktopTheme = "dark";
                 }
                 if(this._selectedThemeIndex===0){
                     this._changeToSelectedThemeIndex();
                 }
            });
        }
        
        this._changeToSelectedThemeIndex();
    }

    render() {
        let theme = this.themes[this._selectedThemeIndex];
        
        return html`<div class="themeButton">
                        <vaadin-button theme="icon" aria-label="${theme.name}" title="${theme.name} theme" class="button" @click="${this._nextTheme}">
                            <vaadin-icon icon="${theme.icon}"></vaadin-icon>
                        </vaadin-button>
                    </div>`;
    }

    _nextTheme(e){
        this._selectedThemeIndex = (this._selectedThemeIndex + 1) % this.themes.length;
        this._changeToSelectedThemeIndex();
    }

    _changeToSelectedThemeIndex(){
        let theme = this.themes[this._selectedThemeIndex];
        this.storageControl.set('theme-preference', theme.id);
        
        if(theme.id === 0){ // Desktop
            themeState.changeTo(this._desktopTheme);
        }else {
            themeState.changeTo(theme.name.toLowerCase());
        }
        
    }

    _restoreThemePreference() {
        const storedValue = this.storageControl.get("theme-preference");
        if(storedValue){
            this._selectedThemeIndex = storedValue;
        } else {
            this._selectedThemeIndex = 0;
        }
    }


}
customElements.define('qwc-theme-switch', QwcThemeSwitch);