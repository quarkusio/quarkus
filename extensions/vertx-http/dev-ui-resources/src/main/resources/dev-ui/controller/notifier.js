import { Notification } from '@vaadin/notification';
import { html} from 'lit';
import '@vaadin/icon';
import '@vaadin/horizontal-layout';

/*
 * Show toast messages.
 * TODO: Implement duration
 */
class Notifier {

    showInfoMessage(message, position = "bottom-start") {
        this.showMessage("font-awesome-solid:circle-info", "primary", message, null, position);
    }

    showSuccessMessage(message, position = "bottom-start") {
        this.showMessage("font-awesome-solid:circle-check", "success", message, null, position);
    }

    showWarningMessage(message, position = "bottom-start") {
        this.showMessage("font-awesome-solid:triangle-exclamation", "contrast", message, "color: var(--lumo-warning-text-color);", position);
    }

    showErrorMessage(message, position = "bottom-start") {
        this.showMessage("font-awesome-solid:circle-exclamation", "error", message, null, position);
    }

    showContrastMessage(message, position = "bottom-start") {
        this.showMessage("font-awesome-solid:apostrophe", "contrast", message, null, position);
    }

    showMessage(icon, theme, message, extrastyle, position = "bottom-start") {

        const notification = Notification.show(html`<vaadin-horizontal-layout theme="spacing" style="align-items: center;${extrastyle}">
                                                        <vaadin-icon icon="${icon}"></vaadin-icon> <span>${message}</span>
                                                    </vaadin-horizontal-layout>`, {
            position: position,
        });

        notification.setAttribute('theme', theme);
    }
}

export const notifier = new Notifier();