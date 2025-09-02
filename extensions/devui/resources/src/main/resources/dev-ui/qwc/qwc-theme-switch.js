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
        _selectedThemeIndex: {state: true},
    };

    constructor() {
        super();
        this._restoreThemePreference();
        window.addEventListener('storage-changed', (event) => {
            this._storageChange(event);
        });
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
        
        return html`<div class="themeButtons" @wheel=${this._onWheel}>
                        ${this.themes.map((theme) =>
                            html`${this._renderButton(theme)}`
                        )}                        
                    </div>`;
    }

    _renderButton(theme){
        let selectedTheme = this.themes[this._selectedThemeIndex];
        
        if(selectedTheme.id === theme.id){
            return html`<div class="themeBlock selected">
                            <vaadin-icon icon="${theme.icon}"></vaadin-icon>
                            <span>${theme.name}</span>
                        </div>`;
        }else{
            return html`<div class="themeBlock selectable" @click="${() => this._selectTheme(theme)}">
                            <vaadin-icon icon="${theme.icon}"></vaadin-icon>
                            <span>${theme.name}</span>
                        </div>`;
        }
    }

    _storageChange(event){
        if(event.detail.method === "remove" && event.detail.key.startsWith("qwc-theme-switch-")){
            this._restoreThemePreference();
            this._changeToSelectedThemeIndex();
        }
    }

    _onWheel(event) {
        event.preventDefault();
        if (event.deltaY < 0) {
            this._selectedThemeIndex = (this._selectedThemeIndex + 1) % this.themes.length;
            this._changeToSelectedThemeIndex();
        } else {
            this._selectedThemeIndex = (this._selectedThemeIndex - 1 + this.themes.length) % this.themes.length;    
            this._changeToSelectedThemeIndex();
        }
    }

    _selectTheme(theme){
        this._selectedThemeIndex = theme.id;
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