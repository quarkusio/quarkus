package org.jboss.resteasy.reactive.server.vertx;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.ws.rs.core.HttpHeaders;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class ResteasyReactiveOutputStreamOnSameContext extends OutputStream {
    private final Context context;
    private final HttpServerResponse response;
    private final VertxResteasyReactiveRequestContext resteasyContext;

    public ResteasyReactiveOutputStreamOnSameContext(VertxResteasyReactiveRequestContext resteasyContext,
            Context ctxt) {
        this.context = ctxt;
        this.resteasyContext = resteasyContext;
        this.response = resteasyContext.context.response();
    }

    private void runOnContext(Runnable runnable) {
        if (Vertx.currentContext() == context) {
            runnable.run();
        } else {
            context.runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void v) {
                    runnable.run();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        runOnContext(new Runnable() {
            @Override
            public void run() {
                prepareResponse();

                if (len < 1) {
                    if (!response.ended()) {
                        response.end();
                    }
                } else {
                    Buffer buffer = Buffer.buffer(len).appendBytes(b, off, len);
                    response.write(buffer);
                }
            }
        });
    }

    private void prepareResponse() {
        if (!response.headWritten()) {
            var contentLengthSet = ResteasyReactiveOutputStream.contentLengthSet(
                    resteasyContext.request,
                    resteasyContext.getResponse());
            if (contentLengthSet == ResteasyReactiveOutputStream.ContentLengthSetResult.NOT_SET) {
                response.setChunked(true);
            } else if (contentLengthSet == ResteasyReactiveOutputStream.ContentLengthSetResult.IN_JAX_RS_HEADER) {
                // we need to make sure the content-length header is copied to Vert.x headers
                // otherwise we could run into a race condition: see https://github.com/quarkusio/quarkus/issues/26599
                Object contentLength = resteasyContext.getResponse().get().getHeaders()
                        .getFirst(HttpHeaders.CONTENT_LENGTH);
                resteasyContext.serverResponse().setResponseHeader(HttpHeaderNames.CONTENT_LENGTH,
                        contentLength.toString());
            }
        }
    }

    @Override
    public void close() throws IOException {
        runOnContext(new Runnable() {
            @Override
            public void run() {
                if (!response.ended()) {
                    response.end();
                }
            }
        });
    }
}
