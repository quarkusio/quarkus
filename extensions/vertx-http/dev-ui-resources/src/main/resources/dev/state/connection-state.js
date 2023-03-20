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
        connectionState.current = newState;
    }
    
    connecting(serverUri){
        const newState = new Object();
        newState.name = "connecting";
        newState.icon = "plug-circle-bolt";
        newState.color = "var(--lumo-primary-color)";
        newState.message = "Connecting to " + serverUri;
        newState.serverUri = serverUri;
        newState.isConnected = false;
        newState.isDisconnected = true;
        newState.isConnecting = true;
        connectionState.current = newState;
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
        
        connectionState.current = newState;
    }
}

export const connectionState = new ConnectionState();