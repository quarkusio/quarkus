import { LitState } from 'lit-element-state';

class ArcState extends LitState {

    constructor() {
        super();
    }

    static get stateVars() {
        return {
            component: "qwc-arc-beans-grid",
            beanId: null
        };
    }

    clear(){
        arcState.component = "qwc-arc-beans-grid";
        arcState.beanId = null;
    }
}

export const arcState = new ArcState();