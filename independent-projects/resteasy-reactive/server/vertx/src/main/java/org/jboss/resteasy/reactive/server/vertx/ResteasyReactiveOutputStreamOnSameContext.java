package org.jboss.resteasy.reactive.server.vertx;

import java.io.IOException;
import java.io.OutputStream;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class ResteasyReactiveOutputStreamOnSameContext extends OutputStream {
    private final Context context;
    private final HttpServerResponse response;

    public ResteasyReactiveOutputStreamOnSameContext(VertxResteasyReactiveRequestContext vertxResteasyReactiveRequestContext,
            Context ctxt) {
        this.context = ctxt;
        this.response = vertxResteasyReactiveRequestContext.context.response();
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
