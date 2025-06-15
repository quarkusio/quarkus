package io.quarkus.amazon.lambda.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.handler.codec.DecoderException;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.impl.RoutingContextInternal;

/**
 * Copy of Vertx BodyHandlerImpl. Had to do this because I want to get raw bytes of everything and if it was a form or
 * multipart it would not set the body buffer.
 */
public class MockBodyHandler implements BodyHandler {

    private static final Logger LOG = LoggerFactory.getLogger(io.vertx.ext.web.handler.impl.BodyHandlerImpl.class);

    private long bodyLimit = DEFAULT_BODY_LIMIT;
    private String uploadsDir;
    private boolean mergeFormAttributes = DEFAULT_MERGE_FORM_ATTRIBUTES;
    private boolean isPreallocateBodyBuffer = DEFAULT_PREALLOCATE_BODY_BUFFER;
    private static final int DEFAULT_INITIAL_BODY_BUFFER_SIZE = 1024; // bytes

    public MockBodyHandler() {
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        if (request.headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true)) {
            context.next();
            return;
        }
        // we need to keep state since we can be called again on reroute
        if (!((RoutingContextInternal) context).seenHandler(RoutingContextInternal.BODY_HANDLER)) {
            long contentLength = isPreallocateBodyBuffer ? parseContentLengthHeader(request) : -1;
            BHandler handler = new BHandler(context, contentLength);
            request.handler(handler);
            request.endHandler(v -> handler.end());
            ((RoutingContextInternal) context).visitHandler(RoutingContextInternal.BODY_HANDLER);
        } else {
            // on reroute we need to re-merge the form params if that was desired
            if (mergeFormAttributes && request.isExpectMultipart()) {
                request.params().addAll(request.formAttributes());
            }

            context.next();
        }
    }

    @Override
    public BodyHandler setHandleFileUploads(boolean handleFileUploads) {
        throw new IllegalStateException("Not Allowed");
    }

    @Override
    public BodyHandler setBodyLimit(long bodyLimit) {
        this.bodyLimit = bodyLimit;
        return this;
    }

    @Override
    public BodyHandler setUploadsDirectory(String uploadsDirectory) {
        this.uploadsDir = uploadsDirectory;
        return this;
    }

    @Override
    public BodyHandler setMergeFormAttributes(boolean mergeFormAttributes) {
        this.mergeFormAttributes = mergeFormAttributes;
        return this;
    }

    @Override
    public BodyHandler setDeleteUploadedFilesOnEnd(boolean deleteUploadedFilesOnEnd) {
        return this;
    }

    @Override
    public BodyHandler setPreallocateBodyBuffer(boolean isPreallocateBodyBuffer) {
        this.isPreallocateBodyBuffer = isPreallocateBodyBuffer;
        return this;
    }

    private long parseContentLengthHeader(HttpServerRequest request) {
        String contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLength == null || contentLength.isEmpty()) {
            return -1;
        }
        try {
            long parsedContentLength = Long.parseLong(contentLength);
            return parsedContentLength < 0 ? -1 : parsedContentLength;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private class BHandler implements Handler<Buffer> {
        private static final int MAX_PREALLOCATED_BODY_BUFFER_BYTES = 65535;

        final RoutingContext context;
        final long contentLength;
        Buffer body;
        boolean failed;
        AtomicInteger uploadCount = new AtomicInteger();
        AtomicBoolean cleanup = new AtomicBoolean(false);
        boolean ended;
        long uploadSize = 0L;

        public BHandler(RoutingContext context, long contentLength) {
            this.context = context;
            this.contentLength = contentLength;
            // the request clearly states that there should
            // be a body, so we respect the client and ensure
            // that the body will not be null
            if (contentLength != -1) {
                initBodyBuffer();
            }

            context.request().exceptionHandler(t -> {
                if (t instanceof DecoderException) {
                    // bad request
                    context.fail(400, t.getCause());
                } else {
                    context.fail(t);
                }
            });
        }

        private void initBodyBuffer() {
            int initialBodyBufferSize;
            if (contentLength < 0) {
                initialBodyBufferSize = DEFAULT_INITIAL_BODY_BUFFER_SIZE;
            } else if (contentLength > MAX_PREALLOCATED_BODY_BUFFER_BYTES) {
                initialBodyBufferSize = MAX_PREALLOCATED_BODY_BUFFER_BYTES;
            } else {
                initialBodyBufferSize = (int) contentLength;
            }

            if (bodyLimit != -1) {
                initialBodyBufferSize = (int) Math.min(initialBodyBufferSize, bodyLimit);
            }

            this.body = Buffer.buffer(initialBodyBufferSize);
        }

        @Override
        public void handle(Buffer buff) {
            if (failed) {
                return;
            }
            uploadSize += buff.length();
            if (bodyLimit != -1 && uploadSize > bodyLimit) {
                failed = true;
                context.fail(413);
            } else {
                if (body == null) {
                    initBodyBuffer();
                }
                body.appendBuffer(buff);
            }
        }

        void end() {
            // this marks the end of body parsing, calling doEnd should
            // only be possible from this moment onwards
            ended = true;

            // only if parsing is done and count is 0 then all files have been processed
            if (uploadCount.get() == 0) {
                doEnd();
            }
        }

        void doEnd() {

            if (failed) {
                return;
            }

            HttpServerRequest req = context.request();
            if (mergeFormAttributes && req.isExpectMultipart()) {
                req.params().addAll(req.formAttributes());
            }
            context.setBody(body);
            // release body as it may take lots of memory
            body = null;

            context.next();
        }
    }

}
