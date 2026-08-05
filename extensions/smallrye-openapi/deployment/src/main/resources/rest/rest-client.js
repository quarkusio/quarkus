export class RestClient {
    static _config = {};

    static configure(options = {}) {
        RestClient._config = { ...RestClient._config, ...options };
    }

    _basePath;
    _token = null;
    _tokenProvider = null;

    constructor(options = {}) {
        const merged = { ...RestClient._config, ...options };
        this._basePath = merged.basePath || '';
        this._token = merged.token || null;
        this._tokenProvider = merged.tokenProvider || null;
    }

    get basePath() {
        return this._basePath;
    }

    set basePath(value) {
        this._basePath = value;
    }

    get token() {
        return this._token;
    }

    set token(value) {
        this._token = value;
    }

    async request(method, pathTemplate, opts = {}) {
        const { pathParams, queryParams, headerParams, cookieParams, body, contentType, accept } = opts;

        let resolvedPath = pathTemplate;
        if (pathParams) {
            resolvedPath = pathTemplate.replace(/\{([^}]+)\}/g, (_, key) =>
                encodeURIComponent(pathParams[key]));
        }

        let url = this._basePath + resolvedPath;

        if (queryParams) {
            const search = new URLSearchParams();
            for (const [key, value] of Object.entries(queryParams)) {
                if (value !== undefined && value !== null) {
                    search.append(key, value);
                }
            }
            const qs = search.toString();
            if (qs) {
                url += '?' + qs;
            }
        }

        const headers = {};
        headers['Accept'] = accept || 'application/json';

        if (body !== null && body !== undefined) {
            headers['Content-Type'] = contentType || 'application/json';
        }

        if (headerParams) {
            for (const [key, value] of Object.entries(headerParams)) {
                if (value !== undefined && value !== null) {
                    headers[key] = value;
                }
            }
        }

        if (cookieParams) {
            for (const [key, value] of Object.entries(cookieParams)) {
                if (value !== undefined && value !== null) {
                    document.cookie = `${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
                }
            }
        }

        const token = await this._resolveToken();
        if (token) {
            headers['Authorization'] = token;
        }

        const options = { method, headers };
        if (body !== null && body !== undefined) {
            options.body = typeof body === 'string' ? body : JSON.stringify(body);
        }

        const resp = await fetch(url, options);
        if (!resp.ok) {
            throw new RestError(resp.status, resp.statusText, await resp.text().catch(() => ''));
        }
        const text = await resp.text();
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    }

    _resolveToken() {
        const provider = this._tokenProvider || RestClient._config.tokenProvider;
        if (provider) {
            return provider();
        }
        return this._token || RestClient._config.token || null;
    }
}

export class RestError extends Error {
    constructor(status, statusText, body) {
        super(`${status} ${statusText}`);
        this.name = 'RestError';
        this.status = status;
        this.statusText = statusText;
        this.body = body;
    }
}
