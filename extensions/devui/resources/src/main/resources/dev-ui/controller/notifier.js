import { Notification } from '@vaadin/notification';
import { html} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import '@vaadin/icon';
import '@vaadin/horizontal-layout';

/*
 * Show toast messages.
 * TODO: Implement duration
 */
class Notifier {

    showPrimaryInfoMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-primary-contrast-color)";
        this.showMessage("font-awesome-solid:circle-info", "primary", message, color, duration, position);
    }

    showInfoMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-primary-text-color)";
        this.showMessage("font-awesome-solid:circle-info", "contrast", message, color, duration, position);
    }

    showPrimarySuccessMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-primary-contrast-color)";
        this.showMessage("font-awesome-solid:circle-check", "success", message, color, duration, position);
    }
    
    showSuccessMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-success-text-color)";
        this.showMessage("font-awesome-solid:circle-check", "contrast", message, color, duration, position);
    }

    showPrimaryWarningMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-primary-contrast-color)";
        this.showMessage("font-awesome-solid:triangle-exclamation", "contrast", message, color, duration, position);
    }
    
    showWarningMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-warning-text-color)";
        this.showMessage("font-awesome-solid:triangle-exclamation", "contrast", message, color, duration, position);
    }

    showPrimaryErrorMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-primary-contrast-color)";
        this.showMessage("font-awesome-solid:circle-exclamation", "error", message, color, duration, position);
    }

    showErrorMessage(message, position, duration = 5) {
        if(position === null)position = "bottom-start";
        let color = "var(--lumo-error-text-color)";
        this.showMessage("font-awesome-solid:circle-exclamation", "contrast", message, color, duration, position);
    }

    showMessage(icon, theme, message, color, duration, position = "bottom-start") {

        let d = duration * 1000;

        const notification = Notification.show(html`<vaadin-horizontal-layout theme="spacing" style="align-items: center;color:${color};">
                                                        <vaadin-icon icon="${icon}"></vaadin-icon> <span>${unsafeHTML(message)}</span>
                                                    </vaadin-horizontal-layout>`, {
            position: position,
            duration: d,
            theme: theme,
        });
    }
}

export const notifier = new Notifier();