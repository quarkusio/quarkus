import { extensions } from 'devui-data';
import { menuItems } from 'devui-data';
import { footerTabs } from 'devui-data';
import { applicationInfo } from 'devui-data';
import { connectionState } from 'connection-state';
import { LitState } from 'lit-element-state';

/**
 * This keeps track of the build time data of Dev UI
 * 
 * TODO: Find a way to abstract this so that any build time data can reuse this in an easy way
 * TODO: Import map needs to be reloaded too
 * TODO: Hot reload should trigger this too (not only ws connection drops)
 */
class DevUIState extends LitState {
    
    constructor() {
        super();
        document.title = "Dev UI | " + applicationInfo.applicationName + " " + applicationInfo.applicationVersion; 
        this.connectionStateObserver = () => this.reload();
        connectionState.addObserver(this.connectionStateObserver);
    }

    static get stateVars() {
        return {
            cards: extensions,
            menu: menuItems,
            footer: footerTabs,
            applicationInfo: applicationInfo,
        };
    }
    
    reload(){
        
        if(connectionState.current.isConnected){
            import(`devui/devui-data.js?${Date.now()}`).then(devUIData => {

                // Check Card changes
                if(devUIData.extensions.active !== devuiState.cards.active ||
                        devUIData.extensions.inactive !== devuiState.cards.inactive ){ // TODO: Do a finer check if something changed
                    devuiState.cards = devUIData.extensions;
                }

                // Check Menu changes
                if(devUIData.menuItems !== devuiState.menuItems){ // TODO: Do a finer check if something changed
                    devuiState.menu = devUIData.menuItems;
                }

                // Check Footer changes
                if(devUIData.footerTabs !== devuiState.footerTabs){ // TODO: Do a finer check if something changed
                    devuiState.footer = devUIData.footerTabs;
                } 
                
                // Check application info for updates
                if(devUIData.applicationInfo !== devuiState.applicationInfo){
                    devuiState.applicationInfo = devUIData.applicationInfo;
                    document.title = "Dev UI | " + devuiState.applicationInfo.applicationName + " " + devuiState.applicationInfo.applicationVersion; 
                } 
            });
        }
    }
}

export const devuiState = new DevUIState();