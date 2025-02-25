import { LitState } from 'lit-element-state';

/**
 * This keeps state of the JsonRPC Connection
 */
class ConnectionState extends LitState {
    
    constructor() {
        super();
    }

    static get stateVars() {
        return {
            current: {}
        };
    }
    
    disconnected(serverUri){
        const newState = new Object();
        newState.name = "disconnected";
        newState.icon = "plug-circle-exclamation";
        newState.color = "var(--lumo-error-color)";
        newState.message = "Disconnected from " + serverUri;
        newState.serverUri = serverUri;
        newState.isConnected = false;
        newState.isDisconnected = true;
        newState.isConnecting = false;
        newState.isHotreloading = false;
        connectionState.current = newState;
        document.body.style.cursor = 'wait';
    }
    
    connecting(serverUri){
        const newState = new Object();
        newState.name = "connecting";
        newState.icon = "plug-circle-bolt";
        newState.color = "var(--lumo-warning-color)";
        newState.message = "Connecting to " + serverUri;
        newState.serverUri = serverUri;
        newState.isConnected = false;
        newState.isDisconnected = true;
        newState.isConnecting = true;
        newState.isHotreloading = false;
        connectionState.current = newState;
        document.body.style.cursor = 'progress';
    }
    
    hotreload(serverUri){
        const newState = new Object();
        newState.name = "hotreload";
        newState.icon = "plug-circle-bolt";
        newState.color = "var(--lumo-primary-color)";
        newState.message = "Hot reloading " + serverUri;
        newState.serverUri = serverUri;
        newState.isConnected = false;
        newState.isDisconnected = false;
        newState.isConnecting = false;
        newState.isHotreloading = true;
        connectionState.current = newState;
        document.body.style.cursor = 'progress'; 
    }
    
    connected(serverUri){
        const newState = new Object();
        newState.name = "connected";
        newState.icon = "plug-circle-check";
        newState.color = "var(--lumo-success-color)";
        newState.message = "Connected to " + serverUri;
        newState.serverUri = serverUri;
        newState.isConnected = true;
        newState.isDisconnected = false;
        newState.isConnecting = false;
        newState.isHotreloading = false;
        connectionState.current = newState;
        document.body.style.cursor = 'default';
    }
}

export const connectionState = new ConnectionState();