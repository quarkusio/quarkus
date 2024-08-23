import { extensions } from 'devui-data';
import { menuItems } from 'devui-data';
import { footerTabs } from 'devui-data';
import { applicationInfo } from 'devui-data';
import { welcomeData } from 'devui-data';
import { ideInfo } from 'devui-data';
import { allConfiguration } from 'devui-data';
import { connectionState } from 'connection-state';
import { LitState } from 'lit-element-state';

/**
 * This keeps track of the build time data of Dev UI
 * TODO: Import map needs to be reloaded too
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
            welcomeData: welcomeData,
            allConfiguration: allConfiguration,
            ideInfo: ideInfo,
        };
    }
    
    reload(){
        
        if(connectionState.current.isConnected){
            import(`devui/devui-data.js?${Date.now()}`).then(newDevUIData => {

                // Check Card changes
                if(newDevUIData.extensions.active !== devuiState.cards.active ||
                        newDevUIData.extensions.inactive !== devuiState.cards.inactive ){ // TODO: Do a finer check if something changed
                    devuiState.cards = newDevUIData.extensions;
                }

                // Check Menu changes
                if(newDevUIData.menuItems !== devuiState.menuItems){ // TODO: Do a finer check if something changed
                    devuiState.menu = newDevUIData.menuItems;
                }

                // Check Footer changes
                if(newDevUIData.footerTabs !== devuiState.footerTabs){ // TODO: Do a finer check if something changed
                    devuiState.footer = newDevUIData.footerTabs;
                } 
                
                // Check application info for updates
                if(newDevUIData.applicationInfo !== devuiState.applicationInfo){
                    devuiState.applicationInfo = newDevUIData.applicationInfo;
                    document.title = "Dev UI | " + devuiState.applicationInfo.applicationName + " " + devuiState.applicationInfo.applicationVersion; 
                }
                
                // Check welcome data
                if(newDevUIData.welcomeData !== devuiState.welcomeData){
                    devuiState.welcomeData = newDevUIData.welcomeData;
                }
                
                // Check ide info for updates
                if(newDevUIData.ideInfo !== devuiState.ideInfo){
                    devuiState.ideInfo = newDevUIData.ideInfo;
                }

                // Check configuration for updates
                if(newDevUIData.allConfiguration !== devuiState.allConfiguration){
                    devuiState.allConfiguration = newDevUIData.allConfiguration;
                }
            });
        }
    }
}

export const devuiState = new DevUIState();