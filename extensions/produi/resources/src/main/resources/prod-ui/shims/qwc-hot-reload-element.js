// qwc-hot-reload-element shim for Prod UI
// Dev UI's QwcHotReloadElement reacts to live-reload connection events. In
// production there is no live reload, so this provides a plain LitElement base
// with no-op hot-reload hooks, letting shared Dev UI components be reused as-is.
import { LitElement } from 'lit';
export * from 'lit';

export class QwcHotReloadElement extends LitElement {

    hotReload() {
        // no-op in production; components typically call this themselves on connect
    }

    forceRestart(message = "") {
        // no-op in production
    }
}
