package org.jboss.resteasy.reactive.client.impl.multipart;

import java.io.File;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;

/**
 * based on {@link io.vertx.ext.web.client.impl.MultipartFormUpload}
 */
public class QuarkusMultipartFormUpload implements ReadStream<Buffer>, Runnable {

    private static final UnpooledByteBufAllocator ALLOC = new UnpooledByteBufAllocator(false);

    private DefaultFullHttpRequest request;
    private PausableHttpPostRequestEncoder encoder;
    private Handler<Throwable> exceptionHandler;
    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private boolean ended;
    private final InboundBuffer<Object> pending;
    private final Context context;

    public QuarkusMultipartFormUpload(Context context,
            QuarkusMultipartForm parts,
            boolean multipart,
            int maxChunkSize,
            PausableHttpPostRequestEncoder.EncoderMode encoderMode) throws Exception {
        this.context = context;
        this.pending = new InboundBuffer<>(context)
                .handler(this::handleChunk)
                .drainHandler(v -> run())
                .pause();
        this.request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                io.netty.handler.codec.http.HttpMethod.POST,
                "/");
        Charset charset = parts.getCharset() != null ? parts.getCharset() : HttpConstants.DEFAULT_CHARSET;
        DefaultHttpDataFactory httpDataFactory = new DefaultHttpDataFactory(-1, charset) {
            @Override
            public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType,
                    String contentTransferEncoding, Charset _charset, long size) {
                if (_charset == null) {
                    _charset = charset;
                }
                return super.createFileUpload(request, name, filename, contentType, contentTransferEncoding, _charset,
                        size);
            }
        };
        this.encoder = new PausableHttpPostRequestEncoder(httpDataFactory, request, multipart, maxChunkSize, charset,
                encoderMode);
        for (QuarkusMultipartFormDataPart formDataPart : parts) {
            if (formDataPart.isAttribute()) {
                encoder.addBodyAttribute(formDataPart.name(), formDataPart.value());
            } else if (formDataPart.isObject()) {
                MemoryFileUpload data = new MemoryFileUpload(formDataPart.name(),
                        formDataPart.filename() != null ? formDataPart.filename() : "",
                        formDataPart.mediaType(),
                        formDataPart.isText() ? null : "binary",
                        null,
                        formDataPart.content().length());
                data.setContent(formDataPart.content().getByteBuf());
                encoder.addBodyHttpData(data);
            } else if (formDataPart.multiByteContent() != null) {
                String contentTransferEncoding = null;
                String contentType = formDataPart.mediaType();
                if (contentType == null) {
                    if (formDataPart.isText()) {
                        contentType = "text/plain";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }
                if (!formDataPart.isText()) {
                    contentTransferEncoding = "binary";
                }

                encoder.addBodyHttpData(new MultiByteHttpData(
                        formDataPart.name(),
                        formDataPart.filename(),
                        contentType,
                        contentTransferEncoding,
                        Charset.defaultCharset(),
                        formDataPart.multiByteContent(),
                        this::handleError,
                        context,
                        this));
            } else {
                String pathname = formDataPart.pathname();
                if (pathname != null) {
                    encoder.addBodyFileUpload(formDataPart.name(),
                            formDataPart.filename(), new File(formDataPart.pathname()),
                            formDataPart.mediaType(), formDataPart.isText());
                } else {
                    String contentType = formDataPart.mediaType();
                    if (formDataPart.mediaType() == null) {
                        if (formDataPart.isText()) {
                            contentType = "text/plain";
                        } else {
                            contentType = "application/octet-stream";
                        }
                    }
                    String transferEncoding = formDataPart.isText() ? null : "binary";
                    MemoryFileUpload fileUpload = new MemoryFileUpload(
                            formDataPart.name(),
                            formDataPart.filename(),
                            contentType, transferEncoding, null, formDataPart.content().length());
                    fileUpload.setContent(formDataPart.content().getByteBuf());
                    encoder.addBodyHttpData(fileUpload);
                }
            }
        }
        encoder.finalizeRequest();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void handleChunk(Object item) {
        Handler handler;
        synchronized (QuarkusMultipartFormUpload.this) {
            if (item instanceof Buffer) {
                handler = dataHandler;
            } else if (item instanceof Throwable) {
                handler = exceptionHandler;
            } else if (item == InboundBuffer.END_SENTINEL) {
                handler = endHandler;
                item = null;
            } else {
                return;
            }
        }
        handler.handle(item);
    }

    private void clearEncoder() {
        if (encoder == null) {
            return;
        }
        encoder.cleanFiles();
        encoder = null;
    }

    @Override
    public void run() {
        if (Vertx.currentContext() != context) {
            throw new IllegalArgumentException("Wrong Vert.x context used for multipart upload. Expected: " + context +
                    ", actual: " + Vertx.currentContext());
        }
        while (!ended) {
            if (encoder.isChunked()) {
                try {
                    HttpContent chunk = encoder.readChunk(ALLOC);
                    if (chunk == PausableHttpPostRequestEncoder.WAIT_MARKER) {
                        return; // resumption will be scheduled by encoder
                    } else if (chunk == LastHttpContent.EMPTY_LAST_CONTENT || encoder.isEndOfInput()) {
                        ended = true;
                        request = null;
                        clearEncoder();
                        pending.write(InboundBuffer.END_SENTINEL);
                    } else {
                        ByteBuf content = chunk.content();
                        Buffer buff = Buffer.buffer(content);
                        boolean writable = pending.write(buff);
                        if (!writable) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    handleError(e);
                    break;
                }
            } else {
                ByteBuf content = request.content();
                Buffer buffer = Buffer.buffer(content);
                request = null;
                clearEncoder();
                pending.write(buffer);
                ended = true;
                pending.write(InboundBuffer.END_SENTINEL);
            }
        }
    }

    public boolean isChunked() {
        return encoder.isChunked();
    }

    private void handleError(Throwable e) {
        ended = true;
        request = null;
        clearEncoder();
        pending.write(e);
    }

    public MultiMap headers() {
        return new HeadersAdaptor(request.headers());
    }

    @Override
    public synchronized QuarkusMultipartFormUpload exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    @Override
    public synchronized QuarkusMultipartFormUpload handler(Handler<Buffer> handler) {
        dataHandler = handler;
        return this;
    }

    @Override
    public synchronized QuarkusMultipartFormUpload pause() {
        pending.pause();
        return this;
    }

    @Override
    public ReadStream<Buffer> fetch(long amount) {
        pending.fetch(amount);
        return this;
    }

    @Override
    @Deprecated
    public synchronized QuarkusMultipartFormUpload resume() {
        pending.resume();
        return this;
    }

    @Override
    public synchronized QuarkusMultipartFormUpload endHandler(Handler<Void> handler) {
        endHandler = handler;
        return this;
    }

}
