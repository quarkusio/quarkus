import { LitElement, html, css } from 'lit';
import { StorageController } from 'storage-controller';
import '@vaadin/combo-box';
import { comboBoxRenderer } from '@vaadin/combo-box/lit.js';
import { setLocale } from 'localization';
import { locales } from 'devui-locales-data';

/**
 * Basic language switch
 */
export class QwcLanguageSwitch extends LitElement {
    storageControl = new StorageController(this);
    
    static styles = css`
        .languages {
            display: flex;
            gap: 15px;
        }
    `;

    static properties = {
        flagsVersion: {type: String},
        _language: {state: true}
    };

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        let preferedLang = this.storageControl.get("language-preference");
        if(!preferedLang){
            preferedLang = this._getDefaultLocale();
        }
        this._language = preferedLang;
        this.updateComplete.then(() => {
            const cb = this.renderRoot.querySelector('vaadin-combo-box');
            if (cb) cb.value = this._language;
        });
        this.changeTo(this._language);
    }

    render() {
        return html`<div class="languages">
                        <vaadin-combo-box
                            .itemLabelPath=${'name'}
                            .itemValuePath=${'code'} 
                            .items=${locales}
                            style="--vaadin-combo-box-overlay-width: 16em"
                            @value-changed=${this._languageChanged}
                            ${comboBoxRenderer(this._languageRenderer, [])}
                        ></vaadin-combo-box>
                        ${this._renderSelectedFlag()}
                    </div>`;
    }

    _renderSelectedFlag(){
        if(this._language){
            return this._renderFlag(this._language);
        }
    }

    _languageRenderer(language){
        return html`
            <div style="display: inline-flex;align-items: center;gap: 0.5rem;">
                ${this._renderFlag(language.code)}
                <span>${language.name}</span>
            </div>    
        `;
    }

    _renderFlag(language){
        const cc = language.split('-')[1].toLowerCase();
        const src = cc ? new URL(`./../../_static/flag-icons/${this.flagsVersion}/flags/4x3/${cc}.svg`, import.meta.url).href : null;
        return html`${src ? html`<img class="flag" width="30" src=${src} alt=${cc} />` : ''}`;
    }

    _languageChanged(e) {
        const language = e.detail.value;
        if (!language) return;
        this._language = language;
        this.storageControl.set('language-preference', this._language);
        this.changeTo(this._language);
    }

    _getDefaultLocale(){
        const browserPref = Array.isArray(navigator.languages) && navigator.languages.length
            ? navigator.languages
            : [navigator.language].filter(Boolean);
        
        
        if(browserPref && browserPref.length>0){
            const browserLang = browserPref[0].toLowerCase();
            
            for (const lang of locales) {
                const supported = lang.code.toLowerCase();
                if (browserLang === supported || browserLang.startsWith(supported) || supported.startsWith(browserLang)) {
                    return lang.code;
                }

                const baseBrowserLang = browserLang.split('-')[0];
                const baseSupportedLang = supported.split('-')[0];
                if (baseBrowserLang === baseSupportedLang) {
                    return lang.code;
                }
            }            
        }
        return "en-GB"; // Default
    }

    async changeTo(languageCode) {
        try {
            await setLocale(languageCode);
        } catch (e) {
            console.error('[i18n] setLocale failed', languageCode, e);
        }
    }

}
customElements.define('qwc-language-switch', QwcLanguageSwitch);