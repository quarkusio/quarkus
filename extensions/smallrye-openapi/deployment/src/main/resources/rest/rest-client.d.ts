export interface RestClientOptions {
    basePath?: string;
    token?: string;
    tokenProvider?: () => string | Promise<string>;
}

export interface RequestOptions {
    pathParams?: Record<string, string | number>;
    queryParams?: Record<string, string | number | boolean>;
    headerParams?: Record<string, string>;
    cookieParams?: Record<string, string>;
    body?: unknown;
    contentType?: string;
    accept?: string;
}

export class RestClient {
    static configure(options?: RestClientOptions): void;

    constructor(options?: RestClientOptions);

    basePath: string;
    token: string | null;

    request(method: string, pathTemplate: string, opts?: RequestOptions): Promise<unknown>;
}

export class RestError extends Error {
    status: number;
    statusText: string;
    body: string;

    constructor(status: number, statusText: string, body: string);
}
