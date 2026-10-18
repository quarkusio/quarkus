export interface GraphQLClientOptions {
    endpoint?: string;
    token?: string;
    tokenProvider?: () => string | Promise<string>;
}

export class GraphQLClient {
    static configure(options?: GraphQLClientOptions): void;
    constructor(options?: GraphQLClientOptions);
    endpoint: string;
    token: string | null;
    query<T = unknown>(queryString: string, variables?: Record<string, unknown>): Promise<T>;
    mutate<T = unknown>(queryString: string, variables?: Record<string, unknown>): Promise<T>;
    subscribe(queryString: string, variables?: Record<string, unknown>): Subscription;
}

export class GraphQLError extends Error {
    errors: Array<{ message: string; [key: string]: unknown }>;
    data: unknown;
}

export class Subscription {
    onData(callback: (data: unknown) => void): Subscription;
    onError(callback: (error: unknown) => void): Subscription;
    onComplete(callback: () => void): Subscription;
    cancel(): Promise<void>;
}
