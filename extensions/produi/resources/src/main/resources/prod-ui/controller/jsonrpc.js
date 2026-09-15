import { jsonRPCSubscriptions } from '../produi-jsonrpc-data.js';
import { jsonRPCMethods } from '../produi-jsonrpc-data.js';
import { pages } from '../produi-pages-data.js';

class Observer {
    constructor(id) {
        this.id = id;
    }

    onNext(callback) {
        this.onNextCallback = callback;
        return this;
    }

    onError(callback) {
        this.onErrorCallback = callback;
        return this;
    }

    cancel() {
        JsonRpc.observerQueue.delete(this.id);
        JsonRpc.cancelSubscription(this.id);
    }
}

/**
 * JsonRPC proxy for Prod UI.
 * Translates method calls to JsonRPC WebSocket messages.
 */
export class JsonRpc {
    static promiseQueue = new Map();
    static observerQueue = new Map();
    static initQueue = [];
    static messageCounter = 0;
    static webSocket;
    static serverUri;
    static retryCount = 0;
    static maxRetries = 10;

    _extensionName;

    constructor(host, logTraffic, serviceIdentifier) {
        if (typeof host === 'string' || host instanceof String) {
            this._extensionName = host;
        } else if (host && host.getAttribute) {
            this._extensionName = host.getAttribute("namespace");
            if (!this._extensionName) {
                // Look up namespace from pages data using the tag name
                const tagName = host.tagName?.toLowerCase();
                if (tagName && JsonRpc._componentMap) {
                    this._extensionName = JsonRpc._componentMap.get(tagName);
                }
            }
        }

        if (serviceIdentifier) {
            this._extensionName = this._extensionName + "-" + serviceIdentifier;
        }

        if (!JsonRpc.webSocket) {
            if (window.location.protocol === "https:") {
                JsonRpc.serverUri = "wss:";
            } else {
                JsonRpc.serverUri = "ws:";
            }
            var path = window.location.pathname;
            var prodUiIdx = path.indexOf('/prod-ui');
            var basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';
            JsonRpc.serverUri += "//" + window.location.host + basePath + "/json-rpc-ws";
            JsonRpc.connect();
        }

        return new Proxy(this, {
            get(target, prop) {
                const origMethod = target[prop];
                if (typeof origMethod === 'undefined') {
                    return function (...args) {
                        var uid = JsonRpc.messageCounter++;
                        let method = this._extensionName + "_" + prop.toString();

                        let params = {};
                        if (args.length > 0) {
                            params = args[0];
                        }

                        var message = {
                            jsonrpc: "2.0",
                            method: method,
                            params: params,
                            id: uid
                        };
                        var payload = JSON.stringify(message);

                        if (jsonRPCSubscriptions.includes(method)) {
                            var observer = new Observer(uid);
                            JsonRpc.observerQueue.set(uid, { observer: observer });
                            JsonRpc.sendMessage(payload);
                            return observer;
                        } else if (jsonRPCMethods.includes(method)) {
                            var _resolve, _reject;
                            var promise = new Promise((resolve, reject) => {
                                _reject = reject;
                                _resolve = resolve;
                            });
                            promise.resolve_ex = (value) => { _resolve(value); };
                            promise.reject_ex = (value) => { _reject(value); };
                            JsonRpc.promiseQueue.set(uid, { promise: promise });
                            JsonRpc.sendMessage(payload);
                            return promise;
                        } else {
                            console.error("Prod UI: method not found: " + method);
                            return Promise.reject("Method not found: " + method);
                        }
                    };
                } else {
                    return Reflect.get(target, prop);
                }
            }
        });
    }

    getExtensionName() {
        return this._extensionName;
    }

    static sendMessage(payload) {
        if (JsonRpc.webSocket.readyState !== WebSocket.OPEN) {
            JsonRpc.initQueue.push(payload);
        } else {
            JsonRpc.webSocket.send(payload);
        }
    }

    static cancelSubscription(id) {
        var message = {
            jsonrpc: "2.0",
            method: "unsubscribe",
            params: {},
            id: id
        };
        JsonRpc.sendMessage(JSON.stringify(message));
    }

    static connect() {
        JsonRpc.webSocket = new WebSocket(JsonRpc.serverUri);

        JsonRpc.webSocket.onopen = function () {
            JsonRpc.retryCount = 0;
            while (JsonRpc.initQueue.length > 0) {
                JsonRpc.webSocket.send(JsonRpc.initQueue.pop());
            }
        };

        JsonRpc.webSocket.onmessage = function (event) {
            var response = JSON.parse(event.data);
            var result = response.result;
            var error = response.error;

            if (!result && error) {
                if (JsonRpc.promiseQueue.has(response.id)) {
                    var saved = JsonRpc.promiseQueue.get(response.id);
                    saved.promise.reject_ex(response);
                    JsonRpc.promiseQueue.delete(response.id);
                } else if (JsonRpc.observerQueue.has(response.id)) {
                    var saved = JsonRpc.observerQueue.get(response.id);
                    if (typeof saved.observer.onErrorCallback === "function") {
                        saved.observer.onErrorCallback(response);
                    }
                }
                return;
            }

            var messageType = result.messageType;

            if (messageType === "Response") {
                if (JsonRpc.promiseQueue.has(response.id)) {
                    var saved = JsonRpc.promiseQueue.get(response.id);
                    saved.promise.resolve_ex({ result: result.object });
                    JsonRpc.promiseQueue.delete(response.id);
                }
            } else if (messageType === "SubscriptionMessage") {
                if (JsonRpc.observerQueue.has(response.id)) {
                    var saved = JsonRpc.observerQueue.get(response.id);
                    if (typeof saved.observer.onNextCallback === "function") {
                        // Mirror the Dev UI JsonRpc contract: deliver a { result } envelope, the
                        // same shape the promise path resolves with above, so a prod-safe Dev UI
                        // component that reads jsonRpcResponse.result works unchanged in Prod UI.
                        saved.observer.onNextCallback({ result: result.object });
                    }
                }
            } else if (messageType === "Void") {
                // no-op
            }
        };

        JsonRpc.webSocket.onclose = function () {
            if (JsonRpc.retryCount < JsonRpc.maxRetries) {
                JsonRpc.retryCount++;
                setTimeout(() => JsonRpc.connect(), 2000 * JsonRpc.retryCount);
            }
        };

        JsonRpc.webSocket.onerror = function () {
            // handled by onclose
        };
    }
}

// Build component-to-namespace map from pages data
JsonRpc._componentMap = new Map();
if (pages) {
    for (const ext of pages) {
        if (ext.pages) {
            for (const p of ext.pages) {
                if (p.componentLink) {
                    const tagName = p.componentLink.replace('.js', '');
                    JsonRpc._componentMap.set(tagName, ext.namespace);
                }
            }
        }
    }
}
