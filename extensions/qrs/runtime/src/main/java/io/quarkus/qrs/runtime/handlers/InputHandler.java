package io.quarkus.qrs.runtime.handlers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

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
public class InputHandler implements RestHandler {

    final long maxBufferSize;
    private volatile Executor executor;
    private final Supplier<Executor> supplier;

    public InputHandler(long maxBufferSize, Supplier<Executor> supplier) {
        this.maxBufferSize = maxBufferSize;
        this.supplier = supplier;
    }

    @Override
    public void handle(QrsRequestContext context) throws Exception {
        if (context.getContext().request().method().equals(HttpMethod.GET) ||
                context.getContext().request().method().equals(HttpMethod.HEAD)) {
            return;
        }
        InputListener h = new InputListener(context);
        context.suspend();
        HttpServerRequest req = context.getContext().request();
        req.endHandler(h);
        req.handler(h::handleBuffer);
        req.resume();
    }

    class InputListener implements Handler<Void> {
        final QrsRequestContext context;
        int dataCount;
        final List<Buffer> data = new ArrayList<>();

        InputListener(QrsRequestContext context) {
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
