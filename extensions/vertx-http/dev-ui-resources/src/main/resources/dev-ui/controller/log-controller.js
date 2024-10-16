/**
 * Control buttons for the log(s) at the bottom
 */
export class LogController {
    static _controllers = new Map();
    static listener;
    
    host;
    tab;
    items = [];
    
    constructor(host) {
        (this.host = host).addController(this);
        this.tab = host.title;
    }

    hostConnected() {
        LogController._controllers.set(this.tab, this);
    }

    hostDisconnected() {
        LogController._controllers.delete(this.tab);
    }
    
    addItem(title, icon, color, callback){
        var item = {
            component: this._createItem(icon, title, color),
            callback: callback,
            isToggle: false
        };
        this.items.push(item);
        return this;
    }
    
    addToggle(title, selected, callback){
        var item = {
            component: this._createToggle(title, selected),
            callback: callback,
            isToggle: true
        };
        this.items.push(item);
        return this;
    }
    
    addFollow(title, selected, callback){
        var item = {
            component: this._createFollow(title, selected),
            callback: callback,
            isToggle: true
        };
        this.items.push(item);
        return this;
    }
    
    done(){
        LogController.listener.loaded();
    }

    _createItem(icon, title, color) {
        var style = `font-size: small;cursor: pointer;color: ${color};background-color: transparent;`;
        const item = document.createElement('vaadin-context-menu-item');
        const vaadinicon = document.createElement('vaadin-icon');
        item.setAttribute('aria-label', `${title}`);
        vaadinicon.setAttribute('icon', `${icon}`);
        vaadinicon.setAttribute('style', `${style}`);
        vaadinicon.setAttribute('title', `${title}`);
        item.appendChild(vaadinicon);
        return item;
    }
    
    _createToggle(title, selected){
        var color = "var(--lumo-tertiary-text-color)";
        var icon = "font-awesome-solid:toggle-off";
        if(selected){
            color = "var(--lumo-primary-color)";
            icon = "font-awesome-solid:toggle-on";
        }
        return this._createItem(icon,title,color);
    }
    
    _createFollow(title, selected){
        var color = "var(--lumo-tertiary-text-color)";
        var icon = "font-awesome-regular:circle";
        if(selected){
            color = "var(--lumo-success-color)";
            icon = "font-awesome-regular:circle-dot";
        }
        return this._createItem(icon,title,color);
    }
    
    static addListener(listener){
        LogController.listener = listener;
    }

    static getItemsForTab(tabName){
        
        if(LogController._controllers.has(tabName)){
            return LogController._controllers.get(tabName).items;
        }else {
            return [];
        }
    }
    
    static fireCallback(e){
        if(e.detail.value.isToggle){
            let iconComponent = e.detail.value.component.firstChild.firstChild;

            if(iconComponent.icon.endsWith('-on')){
                // switching off
                iconComponent.icon = "font-awesome-solid:toggle-off";
                iconComponent.style.color = "var(--lumo-tertiary-text-color)";
                e.detail.value.callback(false);
            }else if(iconComponent.icon.endsWith('-off')){
                // switching on
                iconComponent.icon = "font-awesome-solid:toggle-on";
                iconComponent.style.color = "var(--lumo-primary-color)";
                e.detail.value.callback(true); 
            }else if(iconComponent.icon.endsWith('circle-dot')){
                // switching off
                iconComponent.icon = "font-awesome-regular:circle";
                iconComponent.style.color = "var(--lumo-tertiary-text-color)";
                e.detail.value.callback(false);
            }else if(iconComponent.icon.endsWith('circle')){
                // switching on
                iconComponent.icon = "font-awesome-regular:circle-dot";
                iconComponent.style.color = "var(--lumo-success-color)";
                e.detail.value.callback(true); 
            }
            
        }else{
            e.detail.value.callback(e); 
        }
        
    }
}