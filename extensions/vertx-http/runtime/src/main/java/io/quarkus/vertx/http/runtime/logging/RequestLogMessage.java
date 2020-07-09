package io.quarkus.vertx.http.runtime.logging;

import java.util.Collections;
import java.util.Map;

import io.vertx.core.http.HttpVersion;

public final class RequestLogMessage {

    private final Map<String, String> headers;

    private final String method;

    private final String path;

    private final HttpVersion version;

    public RequestLogMessage(Builder builder) {
        this.headers = Collections.unmodifiableMap(builder.headers);
        this.method = builder.method;
        this.path = builder.path;
        this.version = builder.version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public HttpVersion getVersion() {
        return version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Map<String, String> headers;

        private String method;

        private String path;

        private HttpVersion version;

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public RequestLogMessage build() {
            return new RequestLogMessage(this);
        }

        public Builder version(HttpVersion version) {
            this.version = version;
            return this;
        }
    }

}
