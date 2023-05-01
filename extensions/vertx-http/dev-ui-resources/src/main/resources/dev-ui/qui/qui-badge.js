import { LitElement, html, css} from 'lit';
import '@vaadin/icon';

/**
 * Badge UI Component based on the vaadin theme one
 * see https://vaadin.com/docs/latest/components/badge
 */
export class QuiBadge extends LitElement {
    static styles = css`
        [theme~="badge"] {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            box-sizing: border-box;
            padding: 0.4em calc(0.5em + var(--lumo-border-radius-s) / 4);
            color: var(--lumo-primary-text-color);
            background-color: var(--lumo-primary-color-10pct);
            border-radius: var(--lumo-border-radius-s);
            font-family: var(--lumo-font-family);
            font-size: var(--lumo-font-size-s);
            line-height: 1;
            font-weight: 500;
            text-transform: initial;
            letter-spacing: initial;
            min-width: calc(var (--lumo-line-height-xs) * 1em + 0.45em);
        }
        [theme~="success"] {
            color: var(--lumo-success-text-color);
            background-color: var(--lumo-success-color-10pct);
        }
        [theme~="error"] {
            color: var(--lumo-error-text-color);
            background-color: var(--lumo-error-color-10pct);
        }
        [theme~="warning"] {
            color: var(--lumo-warning-text-color);
            background-color: var(--lumo-warning-color-10pct);
        }
        [theme~="contrast"] {
            color: var(--lumo-contrast-80pct);
            background-color: var(--lumo-contrast-5pct);
        }
        [theme~="small"] {
            font-size: var(--lumo-font-size-xxs);
            line-height: 1;
        }
        [theme~="tiny"] {
            font-size: var(--lumo-font-size-xxs);
            line-height: 1;
            padding: 0.2em calc(0.2em + var(--lumo-border-radius-s) / 4);
        }
        [theme~="primary"] {
            color: var(--lumo-primary-contrast-color);
            background-color: var(--lumo-primary-color);
        }
        [theme~="successprimary"] {
            color: var(--lumo-success-contrast-color);
            background-color: var(--lumo-success-color);
        }
        [theme~="warningprimary"] {
            color: var(--lumo-warning-contrast-color);
            background-color: var(--lumo-warning-color);
        }
        [theme~="errorprimary"] {
            color: var(--lumo-error-contrast-color);
            background-color: var(--lumo-error-color);
        }
        [theme~="contrastprimary"] {
            color: var(--lumo-base-color);
            background-color: var(--lumo-contrast);
        }
        [theme~="pill"] {
            --lumo-border-radius-s: 1em;
        }
    `;

    static properties = {
        background: {type: String},
        color: {type: String},
        icon: {type: String},
        level: {type: String},
        small: {type: Boolean},
        tiny: {type: Boolean},
        primary: {type: Boolean},
        pill: {type: Boolean},
        clickable: {type: Boolean},
        _theme: {attribute: false},
        _style: {attribute: false},
    };

    constructor(){
        super();
        this.icon = null;
        this.level = null;
        this.background = null;
        this.color = null;
        this.small = false;
        this.primary = false;
        this.pill = false;
        this.clickable = false;
        
        this._theme = "badge";
        this._style = "";
    }

    connectedCallback() {
        super.connectedCallback()
        
        if(this.level){
            this._theme = this._theme + " " + this.level;
        }
        if(this.primary){
            if(this.level){
                this._theme = this._theme + "primary";
            }else{
                this._theme = this._theme + " primary";
            }
        }
        
        if(this.small && !this.tiny){
            this._theme = this._theme + " small";
        }
        if(this.tiny){
            this._theme = this._theme + " tiny";
        }
        
        if(this.pill){
            this._theme = this._theme + " pill";
        }
        
        if(this.background){
            this._style = this._style + "background: " + this.background + ";";
        }
        if(this.color){
            this._style = this._style + "color: " + this.color + ";";
        }
        if(this.clickable){
            this._style = this._style + "cursor: pointer";
        }
      }
      
    render() {
        return html`<span theme='${this._theme}' style='${this._style}'>
                ${this._renderIcon()}
                <slot></slot>
            </span>`;
    }

    _renderIcon(){
        if(this.icon){
            return html`<vaadin-icon icon='${this.icon}' style='padding: var(--lumo-space-xs);'></vaadin-icon>`;
        }
    }

}
customElements.define('qui-badge', QuiBadge);