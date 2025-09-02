import { devuiState } from 'devui-state';

/**
 * Storage for extensions
 */
export class StorageController {
    
    host;
    _pre;
    constructor(host, perApp=false) {
        // First check if host is a String
        if (typeof host === 'string' || host instanceof String){
            this._pre = host + "-";
        }else {
            (this.host = host).addController(this);
            this._pre = host.tagName.toLowerCase() + "-";
        }
        
        if(perApp){
            this._pre = this._pre + devuiState.applicationInfo.applicationName + "-";
        }
    }

    set(key, value){
        localStorage.setItem(this._pre + key, value);
        window.dispatchEvent(new CustomEvent('storage-changed', {
            detail: { method: 'set', key: this._pre + key, value: value }
        }));
    }
    
    has(key){
        return localStorage.getItem(this._pre + key) !== null;
    }
    
    get(key){
        return localStorage.getItem(this._pre + key);
    }
    
    remove(key){
        localStorage.removeItem(this._pre + key);
        window.dispatchEvent(new CustomEvent('storage-changed', {
            detail: { method: 'remove', key: this._pre + key}
        }));
    }
}