package io.quarkus.vertx.http.runtime;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;

class ResumingRequestWrapper extends AbstractRequestWrapper {

    private boolean userSetState;

    ResumingRequestWrapper(HttpServerRequest request) {
        super(request);
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
}
