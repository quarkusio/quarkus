import {LitElement, html, css} from 'lit';
import {unsafeHTML} from 'lit-html/directives/unsafe-html.js';


export class QuiAlert extends LitElement {

    static styles = css`
      .alert {
        background-color: transparent;
        padding: 1rem 1rem;
        margin: 1rem;
        color: inherit;
        border: 1px solid transparent;
        border-radius: 0.375rem;
        position: relative;
      }

      a {
        font-weight: 700;
      }

      .icon {
        width: 1em;
        height: 1em;
        vertical-align: -0.125em;
        fill: currentColor;
        margin-right: .5rem !important;
        flex-shrink: 0 !important;
      }

      .alert-header {
        font-size: calc(1.275rem + .3vw);
      }


      .alert-primary {
        color: var(--lumo-primary-contrast-color);
        background-color: var(--lumo-primary-color);
      }

      .alert-primary a {
        color: #6ea8fe;
      }

      .alert-success {
        color: var(--lumo-success-contrast-color);
        background-color: var(--lumo-success-color);
      }


      .alert-success a {
        color: #75b798;
      }

      .alert-danger {
        color: var(--lumo-error-contrast-color);
        background-color: var(--lumo-error-color);

      }

      .alert-danger a {
        color: #ea868f;
      }

      .alert-warning {
        color: var(--lumo-warning-contrast-color);
        background-color: var(--lumo-warning-color);
        border-color: #664d03;
      }

      .alert-warning a {
        color: #ffda6a;
      }

      .alert-info {
        color: var(--lumo-info-contrast-color);
        background-color: var(--lumo-info-color);
      }

      .alert-info a {
        color: #6edff6;
      }

      .close {
        cursor: pointer;
      }
      
      .alert-dismissible .close {
        position: absolute;
        top: 0;
        right: 0;
        padding: .75rem 1.25rem;
        color: inherit;
        font-size: x-large;
      }
      
      button.close {
        padding: 0;
        background-color: transparent;
        border: 0;
        -webkit-appearance: none;
      }
    `


    static properties = {
        // Tag attributes
        title: {type: String},
        content: {type: String},
        theme: {type: String},
        icon: {type: Boolean},
        dismissible: {type: Boolean},
        // Internal state
        _dismissed: {type: Boolean, state: true}
    };

    render() {
        if (this._dismissed) {
            return '';
        }

        let title = '';
        let close = '';
        let classes = '';
        if (this.title) {
            title = html`<h4 class="alert-heading alert-heading-${this.theme}">${title}</h4>`
        }
        if (this.dismissible) {
            classes = "alert-dismissible";
            close = html`
                <button type="button" @click="${this._dismiss}" class="close" aria-label="Close">
                    <span>&times;</span>
                </button>`
        }
        let icon = this._getIcon();
        return html`
            <div class="alert alert-${this.theme} ${classes}" role="alert">
                ${icon}
                ${title}
                ${unsafeHTML(this.content)}
                ${close}
            </div>`;
    }

    _dismiss() {
        this._dismissed = true;
    }

    _getIcon() {
        if (this.icon) {
            switch (this.type) {
                case "warning":
                case "danger":
                    return html`
                        <svg class="icon" role="img" aria-label="Danger:">
                            <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"></path>
                        </svg>`
                case "info":
                    return html`
                        <svg class="icon" role="img" aria-label="Info:">
                            <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"></path>
                        </svg>`
                case "success":
                    return html`
                        <svg class="icon" role="img" aria-label="Success:">
                            <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"></path>
                        </svg>`
                default:
                    return '';
            }
        } else {
            return '';
        }
    }

}

customElements.define('qui-alert', QuiAlert);
