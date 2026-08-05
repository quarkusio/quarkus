export class GraphQLClient {
    static _config = {};

    static configure(options = {}) {
        GraphQLClient._config = { ...GraphQLClient._config, ...options };
    }

    _endpoint;
    _token = null;
    _tokenProvider = null;

    constructor(options = {}) {
        const merged = { ...GraphQLClient._config, ...options };
        this._endpoint = merged.endpoint || '/graphql';
        this._token = merged.token || null;
        this._tokenProvider = merged.tokenProvider || null;
    }

    get endpoint() {
        return this._endpoint;
    }

    set endpoint(value) {
        this._endpoint = value;
    }

    get token() {
        return this._token;
    }

    set token(value) {
        this._token = value;
    }

    async query(queryString, variables) {
        return this._fetch(queryString, variables);
    }

    async mutate(queryString, variables) {
        return this._fetch(queryString, variables);
    }

    subscribe(queryString, variables) {
        const token = this._resolveTokenSync();
        return new Subscription(this._wsUrl(), queryString, variables, token);
    }

    async _fetch(queryString, variables) {
        const body = { query: queryString };
        if (variables && Object.keys(variables).length > 0) {
            body.variables = variables;
        }
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/graphql-response+json, application/json',
        };

        const token = await this._resolveToken();
        if (token) {
            headers['Authorization'] = token;
        }

        const resp = await fetch(this._endpoint, {
            method: 'POST',
            headers,
            body: JSON.stringify(body),
        });
        const json = await resp.json();
        if (json.errors && json.errors.length > 0) {
            throw new GraphQLError(json.errors, json.data);
        }
        return json.data;
    }

    _wsUrl() {
        const loc = typeof location !== 'undefined' ? location : {};
        const proto = (loc.protocol === 'https:') ? 'wss:' : 'ws:';
        return `${proto}//${loc.host}${this._endpoint}`;
    }

    _resolveToken() {
        const provider = this._tokenProvider || GraphQLClient._config.tokenProvider;
        if (provider) {
            return provider();
        }
        return this._token || GraphQLClient._config.token || null;
    }

    _resolveTokenSync() {
        const provider = this._tokenProvider || GraphQLClient._config.tokenProvider;
        if (provider) {
            const result = provider();
            if (result && typeof result.then === 'function') {
                return null;
            }
            return result;
        }
        return this._token || GraphQLClient._config.token || null;
    }
}

export class GraphQLError extends Error {
    constructor(errors, data) {
        super(errors.map(e => e.message).join('; '));
        this.name = 'GraphQLError';
        this.errors = errors;
        this.data = data;
    }
}

export class Subscription {
    _ws = null;
    _url;
    _query;
    _variables;
    _token;
    _id = '1';
    _dataCallback = null;
    _errorCallback = null;
    _completeCallback = null;

    constructor(url, query, variables, token) {
        this._url = url;
        this._query = query;
        this._variables = variables;
        this._token = token;
        this._connect();
    }

    onData(callback) {
        this._dataCallback = callback;
        return this;
    }

    onError(callback) {
        this._errorCallback = callback;
        return this;
    }

    onComplete(callback) {
        this._completeCallback = callback;
        return this;
    }

    async cancel() {
        if (this._ws && this._ws.readyState === WebSocket.OPEN) {
            this._ws.send(JSON.stringify({ id: this._id, type: 'complete' }));
            this._ws.close();
        }
        this._ws = null;
    }

    _connect() {
        const protocols = ['graphql-transport-ws'];
        if (this._token) {
            const encoded = encodeURIComponent(
                'quarkus-http-upgrade#Authorization#' + this._token
            );
            protocols.push('bearer-token-carrier', encoded);
        }
        this._ws = new WebSocket(this._url, protocols);

        this._ws.onopen = () => {
            this._ws.send(JSON.stringify({ type: 'connection_init' }));
        };

        this._ws.onmessage = (event) => {
            const msg = JSON.parse(event.data);
            switch (msg.type) {
                case 'connection_ack':
                    this._ws.send(JSON.stringify({
                        id: this._id,
                        type: 'subscribe',
                        payload: { query: this._query, variables: this._variables },
                    }));
                    break;
                case 'next':
                    if (this._dataCallback && msg.payload) {
                        const data = msg.payload.data;
                        this._dataCallback(data);
                    }
                    break;
                case 'error':
                    if (this._errorCallback) {
                        this._errorCallback(msg.payload);
                    }
                    break;
                case 'complete':
                    if (this._completeCallback) {
                        this._completeCallback();
                    }
                    this._ws.close();
                    break;
            }
        };

        this._ws.onerror = (err) => {
            if (this._errorCallback) {
                this._errorCallback(err);
            }
        };
    }
}
