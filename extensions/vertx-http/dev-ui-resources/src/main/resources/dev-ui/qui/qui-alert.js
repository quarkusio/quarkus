import {LitElement, html, css} from 'lit';
import '@vaadin/icon';

export class QuiAlert extends LitElement {

    static styles = css`
        .alert {
            padding: 1rem 1rem;
            margin: 1rem;
            border: 1px solid transparent;
            border-radius: 0.375rem;
            position: relative;
            display: flex;
            justify-content: space-between;
        }

        .info {
            background-color: var(--lumo-primary-color-10pct);
            color: var(--lumo-primary-text-color);
        }
        .success {
            background-color: var(--lumo-success-color-10pct);
            color: var(--lumo-success-text-color);
        }
        .warning {
            background-color: var(--lumo-warning-color-10pct);
            color: var(--lumo-warning-text-color);
        }  
        .error {
            background-color: var(--lumo-error-color-10pct);
            color: var(--lumo-error-text-color);
        }  
      
        .infoprimary {
            background-color: var(--lumo-primary-color);
            color: var(--lumo-primary-contrast-color);
        }
        .successprimary {
            background-color: var(--lumo-success-color);
            color: var(--lumo-success-contrast-color);
        }
        .warningprimary {
            background-color: var(--lumo-warning-color);
            color: var(--lumo-warning-contrast-color);
        }  
        .errorprimary {
            background-color: var(--lumo-error-color);
            color: var(--lumo-error-contrast-color);
        }
      
        .layout {
            display: flex;
            flex-direction: column;
            width: 100%;
        }
       
       .content {
            display: flex;
            gap: 10px;
            align-items: center;
            width: 100%;
        }
    
        .center {
            justify-content: center;
        }
    
        .close {
            cursor: pointer;
        }
    
        .title {
            font-size: 1.4em;
            padding-bottom: 10px;
        }
    `;

    static properties = {
        // Tag attributes
        title: {type: String}, // Optional title
        level: {type: String}, // Level (info, success, warning, error) - default info
        icon: {type: String}, // Icon
        size: {type: String}, // Font size - default large
        showIcon: {type: Boolean}, // Use default icon if none is supplied - default false
        permanent: {type: Boolean}, // disallow dismissing - default false
        primary: {type: Boolean}, // Primary - default false
        center: {type: Boolean}, // Center - default false
        // Internal state
        _dismissed: {type: Boolean, state: true}
    };

    constructor() {
        super();
        this.title = null;
        this.level = "info";
        this.icon = null;
        this.size = "large";
        this.showIcon = false;
        this.permanent = false;
        this.primary = false;
        this.center = false;
        this._dismissed - false;
    }
    render() {
        if (!this._dismissed) {
            let theme = this.level;    
            if(this.primary){
                theme = theme + "primary";
            }
            
            let contentClass="content";
            if(this.center){
                contentClass = contentClass + " center";
            }
            return html`
                <div class="alert ${theme}" style="font-size:${this.size};" role="alert">
                    <div class="layout">
                        ${this._renderTitle()}
                        <div class="${contentClass}">
                            ${this._renderIcon()}    
                            <slot></slot>
                        </div>
                    </div>
                    ${this._renderClose()}
                </div>`;
        }
    }

    _renderIcon(){
        if(this.icon){
            // User provided icon
            return html`<vaadin-icon icon="${this.icon}"></vaadin-icon>`;
        }else if (this.showIcon){
            // Default icon
            if(this.level === "info"){
                return html`<vaadin-icon icon="font-awesome-solid:circle-info"></vaadin-icon>`;
            }else if(this.level === "success"){
                return html`<vaadin-icon icon="font-awesome-solid:check"></vaadin-icon>`;
            }else if(this.level === "warning"){
                return html`<vaadin-icon icon="font-awesome-solid:triangle-exclamation"></vaadin-icon>`;
            }else if(this.level === "error"){
                return html`<vaadin-icon icon="font-awesome-solid:circle-exclamation"></vaadin-icon>`;
            }
        }
    }

    _renderTitle(){
        if(this.title){
            return html`<div class="title">${this.title}</div>`;
        }
    }

    _renderClose(){
        if (!this.permanent) {
            return html`<vaadin-icon class="close" icon='font-awesome-solid:xmark' @click="${this._dismiss}"></vaadin-icon>`;
        }
    }

    _dismiss() {
        this._dismissed = true;
    }

    

}

customElements.define('qui-alert', QuiAlert);
