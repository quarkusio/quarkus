package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.vertx.core.http.HttpServerRequest;

public class ReadBodyHandler implements RestHandler {

    // FIXME: we should be able to use this, but I couldn't figure out how:
    // every time I try to forward to it, I end up with a 404
    //    BodyHandler bodyHandler = BodyHandler.create();

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        requestContext.suspend();
        HttpServerRequest vertxRequest = requestContext.getContext().request();
        vertxRequest.setExpectMultipart(true);
        vertxRequest.bodyHandler(buf -> {
            requestContext.resume();
        });
    }

}
