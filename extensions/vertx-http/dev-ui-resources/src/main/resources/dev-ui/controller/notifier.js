import { Notification } from '@vaadin/notification';
import { html} from 'lit';
import '@vaadin/icon';
import '@vaadin/horizontal-layout';

/*
 * Show toast messages.
 * TODO: Implement duration
 */
class Notifier {

    showInfoMessage(message, position = "bottom-start", duration = 5) {
        this.showMessage("font-awesome-solid:circle-info", "primary", message, position, duration);
    }

    showSuccessMessage(message, position = "bottom-start", duration = 5) {
        this.showMessage("font-awesome-solid:circle-check", "success", message, position, duration);
    }

    showWarningMessage(message, position = "bottom-start", duration = 5) {
        this.showMessage("font-awesome-solid:triangle-exclamation", "contrast", message, position, duration);
    }

    showErrorMessage(message, position = "bottom-start", duration = 5) {
        this.showMessage("font-awesome-solid:circle-exclamation", "error", message, position, duration);
    }

    showMessage(icon, theme, message, position = "bottom-start") {

        const notification = Notification.show(html`<vaadin-horizontal-layout theme="spacing" style="align-items: center;">
                                                        <vaadin-icon icon="${icon}"></vaadin-icon> <span>${message}</span></vaadin-horizontal-layout>`, {
            position: position,
        });

        notification.setAttribute('theme', theme);
    }
}

export const notifier = new Notifier();