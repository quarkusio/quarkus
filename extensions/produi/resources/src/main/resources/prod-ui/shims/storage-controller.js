// storage-controller shim for Prod UI
// Mirrors Dev UI's StorageController API (get/set/has/remove) backed by
// localStorage, without the Dev UI state dependency.
export class StorageController {

    _pre;

    constructor(host) {
        if (typeof host === 'string' || host instanceof String) {
            this._pre = host + "-";
        } else {
            host.addController?.(this);
            this._pre = host.tagName.toLowerCase() + "-";
        }
    }

    set(key, value) {
        localStorage.setItem(this._pre + key, value);
    }

    has(key) {
        return localStorage.getItem(this._pre + key) !== null;
    }

    get(key) {
        return localStorage.getItem(this._pre + key);
    }

    remove(key) {
        localStorage.removeItem(this._pre + key);
    }
}
