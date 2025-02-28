package io.quarkus.vertx.http.runtime;

import io.quarkus.vertx.http.runtime.filters.AbstractResponseWrapper;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.http.impl.HttpServerRequestWrapper;

public class ResumingRequestWrapper extends HttpServerRequestWrapper {

    private final HttpServerResponse httpServerResponse;
    private boolean userSetState;

    public ResumingRequestWrapper(HttpServerRequest request, boolean mustResumeRequest) {
        super((HttpServerRequestInternal) request);

        // TODO: replace this when more than one response end handlers are allowed
        if (mustResumeRequest) {
            HttpServerResponse response = delegate.response();
            response.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void unused) {
                    if (!delegate.isEnded()) {
                        delegate.resume();
                    }
                }
            });
            this.httpServerResponse = new AbstractResponseWrapper(response) {
                @Override
                public HttpServerResponse endHandler(Handler<Void> handler) {
                    return super.endHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void unused) {
                            handler.handle(null);
                            if (!delegate.isEnded()) {
                                delegate.resume();
                            }
                        }
                    });
                }
            };
        } else {
            this.httpServerResponse = null;
        }
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        delegate.handler(handler);
        if (!userSetState) {
            delegate.resume();
        }
        return this;
    }

    @Override
    public HttpServerRequest pause() {
        userSetState = true;
        delegate.pause();
        return this;
    }

    @Override
    public HttpServerRequest resume() {
        userSetState = true;
        delegate.resume();
        return this;
    }

    @Override
    public HttpServerRequest fetch(long amount) {
        userSetState = true;
        delegate.fetch(amount);
        return this;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> handler) {
        delegate.endHandler(handler);
        if (!userSetState) {
            delegate.resume();
        }
        return this;
    }

    @Override
    public HttpServerRequest bodyHandler(Handler<Buffer> handler) {
        delegate.bodyHandler(handler);
        if (!userSetState) {
            delegate.resume();
        }
        return this;
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> handler) {
        delegate.uploadHandler(handler);
        if (!userSetState) {
            delegate.resume();
        }
        return this;
    }

    @Override
    public HttpServerResponse response() {
        if (httpServerResponse != null) {
            return httpServerResponse;
        } else {
            return super.response();
        }
    }
}
