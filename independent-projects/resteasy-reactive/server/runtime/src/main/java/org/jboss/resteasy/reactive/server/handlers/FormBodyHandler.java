package org.jboss.resteasy.reactive.server.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class FormBodyHandler implements ServerRestHandler {

    private static final byte[] NO_BYTES = new byte[0];

    private final boolean alsoSetInputStream;

    public FormBodyHandler(boolean alsoSetInputStream) {
        this.alsoSetInputStream = alsoSetInputStream;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // in some cases, with sub-resource locators or via request filters, 
        // it's possible we've already read the entity
        if (requestContext.hasInputStream()) {
            // let's not set it twice
            return;
        }
        ServerHttpRequest serverHttpRequest = requestContext.serverRequest();
        if (serverHttpRequest.isRequestEnded()) {
            if (alsoSetInputStream) {
                // do not use the EmptyInputStream.INSTANCE marker
                requestContext.setInputStream(new ByteArrayInputStream(NO_BYTES));
            }
        } else {
            serverHttpRequest.setExpectMultipart(true);
            requestContext.suspend();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serverHttpRequest.setReadListener(new ServerHttpRequest.ReadCallback() {
                @Override
                public void done() {
                    requestContext.setInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
                    requestContext.resume();
                }

                @Override
                public void data(ByteBuffer data) {
                    // the TCK allows the body to be read as a form param and also as a body param
                    // the spec is silent about this
                    // TODO: this is really really horrible and hacky and needs to be fixed.
                    byte[] buf = new byte[data.remaining()];
                    data.get(buf);
                    try {
                        outputStream.write(buf);
                    } catch (IOException ignored) {
                        //impossible
                    }
                }
            });
        }
    }

}
