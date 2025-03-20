import { LitElement } from 'lit';
import { connectionState } from 'connection-state';
export * from 'lit';

/**
 * This is an abstract component that monitor hot reload events
 */
class QwcHotReloadElement extends LitElement {
    
    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.connectionStateObserver = () => this._connectionStateChange();
        connectionState.addObserver(this.connectionStateObserver);
    }
      
    disconnectedCallback() {
        connectionState.removeObserver(this.connectionStateObserver);
        super.disconnectedCallback();
    }

    _connectionStateChange(){
        if(connectionState.current.isConnected){
            this.hotReload();
        }
    }

    hotReload(){
        throw new Error("Method 'hotReload()' must be implemented.");
    }

    forceRestart(message = ""){
        const event = new CustomEvent('force-restart-event', {
            detail: { message: message },
            bubbles: false, 
            composed: false
        });
        window.dispatchEvent(event);
    }

}

export { QwcHotReloadElement };
