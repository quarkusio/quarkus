import { LitState } from 'lit-element-state';

/**
 * This keeps state of the Assistant
 */
class AssistantState extends LitState {
    
    constructor() {
        super();
        this.notAvailable();
    }

    static get stateVars() {
        return {
            current: {}
        };
    }
    
    notAvailable(){
        const newState = new Object();
        newState.name = "notAvailable";
        newState.message = "No assistant is available";
        newState.isAvailable = false;
        newState.isConfigured = false;
        this.current = newState;
    }
    
    available(){
        const newState = new Object();
        newState.name = "available";
        newState.message = "Assistant is available, but not configured";
        newState.isAvailable = true;
        newState.isConfigured = false;
        this.current = newState;
    }
    
    ready(){
        const newState = new Object();
        newState.name = "ready";
        newState.message = "Assistant is available, and configured";
        newState.isAvailable = true;
        newState.isConfigured = true;
        this.current = newState;
    }
}

export const assistantState = new AssistantState();