import { LitElement, html, css} from 'lit';
import {ring} from 'ldrs';
import { msg, updateWhenLocaleChanges } from 'localization';

ring.register();

/**
 * This is the menu action on the Continuous Testing menu
 */
export class QwcContinuousTestingMenuAction extends LitElement {
    
    static styles = css`
            .actionBtn{
                color: var(--lumo-contrast-25pct);
            }
            .ring {
                padding-right: 5px;
                padding-top: 5px;
            }
       `;

    static properties = {
        _ctState: {state : true}
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._ctState = "stopped";
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('continuous-testing-state-change', this._onStateChanged);   
    }
    
    disconnectedCallback() {
        window.removeEventListener('continuous-testing-state-change', this._onStateChanged);
        super.disconnectedCallback();
    }

    _onStateChanged = (event) => {
        this._ctState = event.detail.state;
    }

    render(){
        let icon = "stop";
        let title = msg('Stop', { id: 'continuoustesting-stop' });
        if(this._ctState === "stopped"){
            icon = "play";
            title = msg('Start', { id: 'continuoustesting-start' });
        }

        if(this._ctState === "stopped" || this._ctState === "started"){
            return html`<vaadin-button 
                            title="${title} ${msg('Continuous Testing', { id: 'continuoustesting-title' })} " class="actionBtn"
                            id="start-cnt-testing-btn" 
                            theme="icon tertiary small" 
                            @click="${this._startStopClicked}">
                        <vaadin-icon icon="font-awesome-solid:${icon}"></vaadin-icon>
                    </vaadin-button>`;
        }else{
            return html`<l-ring size="26" stroke="2" color="var(--lumo-contrast-25pct)" class="ring"></l-ring>`;
        }
    }
    
    _startStopClicked(e){
        this.dispatchEvent(new CustomEvent('continuous-testing-start-stop', {
            detail: { requested: "start/stop" },
            bubbles: true,
            composed: true
        }));
    }
    
    
}
customElements.define('qwc-continuous-testing-menu-action', QwcContinuousTestingMenuAction);
