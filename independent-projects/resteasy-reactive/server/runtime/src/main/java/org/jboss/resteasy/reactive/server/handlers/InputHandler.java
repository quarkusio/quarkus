package org.jboss.resteasy.reactive.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.common.runtime.util.EmptyInputStream;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.VertxInputStream;

/**
 * Handler that reads data and sets up the input stream
 * 
 * By default this will attempt to buffer and use a fully in memory stream,
 * to allow the request to stay on the IO thread. If the request is too large
 * it will be delegated to an executor and a blocking stream used instead.
 * 
 * TODO: the stream implementation here could be a lot more efficent.
 * 
 */
public class InputHandler implements ServerRestHandler {

    final long maxBufferSize;
    private volatile Executor executor;
    private final Supplier<Executor> supplier;

    public InputHandler(long maxBufferSize, Supplier<Executor> supplier) {
        this.maxBufferSize = maxBufferSize;
        this.supplier = supplier;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext context) throws Exception {
        // in some cases, with sub-resource locators or via request filters, 
        // it's possible we've already read the entity
        if (context.getInputStream() != EmptyInputStream.INSTANCE) {
            // let's not set it twice
            return;
        }
        if (context.getContext().request().method().equals(HttpMethod.GET) ||
                context.getContext().request().method().equals(HttpMethod.HEAD)) {
            return;
        }
        InputListener h = new InputListener(context);
        context.suspend();
        HttpServerRequest req = context.getContext().request();
        if (!req.isEnded()) {
            req.endHandler(h);
            req.handler(h::handleBuffer);
            req.resume();
        } else {
            req.resume();
            h.handle(null);
        }
    }

    class InputListener implements Handler<Void> {
        final ResteasyReactiveRequestContext context;
        int dataCount;
        final List<Buffer> data = new ArrayList<>();

        InputListener(ResteasyReactiveRequestContext context) {
            this.context = context;
        }

        public void handleBuffer(Buffer event) {
            dataCount += event.length();
            data.add(event);
            if (dataCount > maxBufferSize) {
                context.getContext().request().pause();
                if (executor == null) {
                    executor = supplier.get();
                }
                //super inefficient
                //TODO: write a stream that just uses the existing vert.x buffers
                ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(dataCount);
                int count = 0;
                for (Buffer i : data) {
                    i.getBytes(0, i.length(), buf.array(), count);
                    count += i.length();
                }
                buf.writerIndex(count);
                //todo timeout
                VertxInputStream inputStream = new VertxInputStream(context.getContext(), 100000, buf);
                context.setInputStream(inputStream);
                context.resume(executor);
            }
        }

        @Override
        public void handle(Void event) {
            //super inefficient
            //TODO: write a stream that just uses the existing vert.x buffers
            byte[] ar = new byte[dataCount];
            int count = 0;
            for (Buffer i : data) {
                i.getBytes(count, i.length(), ar);
                count += i.length();
            }
            context.setInputStream(new ByteArrayInputStream(ar));
            context.resume();
        }
    }
}
