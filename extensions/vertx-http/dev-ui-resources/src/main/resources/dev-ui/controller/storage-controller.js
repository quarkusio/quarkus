/**
 * Storage for extensions
 */
export class StorageController {
    
    host;
    _pre;
    constructor(host) {
        (this.host = host).addController(this);
        this._pre = host.tagName.toLowerCase() + "-";
    }

    set(key, value){
        localStorage.setItem(this._pre + key, value);
    }
    
    has(key){
        return localStorage.getItem(this._pre + key) === null;
    }
    
    get(key){
        return localStorage.getItem(this._pre + key);
    }
    
    remove(key){
        localStorage.removeItem(this._pre + key);
    }
    
}