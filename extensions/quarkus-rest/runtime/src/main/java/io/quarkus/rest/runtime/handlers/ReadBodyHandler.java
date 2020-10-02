package io.quarkus.rest.runtime.handlers;

import java.io.ByteArrayInputStream;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.util.EmptyInputStream;
import io.vertx.core.http.HttpServerRequest;

public class ReadBodyHandler implements RestHandler {

    private static final byte[] NO_BYTES = new byte[0];

    private boolean alsoSetInputStream;

    public ReadBodyHandler(boolean alsoSetInputStream) {
        this.alsoSetInputStream = alsoSetInputStream;
    }

    // FIXME: we should be able to use this, but I couldn't figure out how:
    // every time I try to forward to it, I end up with a 404
    //    BodyHandler bodyHandler = BodyHandler.create();

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        // in some cases, with sub-resource locators or via request filters, 
        // it's possible we've already read the entity
        if (requestContext.getInputStream() != EmptyInputStream.INSTANCE) {
            // let's not set it twice
            return;
        }
        HttpServerRequest vertxRequest = requestContext.getContext().request();
        if (vertxRequest.isEnded()) {
            if (alsoSetInputStream) {
                // do not use the EmptyInputStream.INSTANCE marker
                requestContext.setInputStream(new ByteArrayInputStream(NO_BYTES));
            }
        } else {
            requestContext.suspend();
            vertxRequest.setExpectMultipart(true);
            vertxRequest.bodyHandler(buf -> {
                // the TCK allows the body to be read as a form param and also as a body param
                // the spec is silent about this
                if (alsoSetInputStream) {
                    requestContext.setInputStream(new ByteArrayInputStream(buf.getBytes()));
                }
                requestContext.resume();
            });
        }
    }

}
