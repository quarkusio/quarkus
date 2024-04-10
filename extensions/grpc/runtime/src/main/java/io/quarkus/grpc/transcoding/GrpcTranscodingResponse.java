package io.quarkus.grpc.transcoding;

import java.util.Map;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.impl.Utils;
import io.vertx.grpc.server.GrpcServerResponse;

public class GrpcTranscodingResponse<Req, Resp> implements GrpcServerResponse<Req, Resp> {

    private final GrpcTranscodingRequest<Req, Resp> request;
    private final HttpServerResponse httpResponse;
    private final GrpcMessageEncoder<Resp> encoder;
    private String encoding;
    private GrpcStatus status = GrpcStatus.OK;
    private String statusMessage;
    private boolean headersSent;
    private boolean trailersSent;
    private boolean cancelled;
    private MultiMap headers, trailers;

    public GrpcTranscodingResponse(GrpcTranscodingRequest<Req, Resp> request, HttpServerResponse httpResponse,
            GrpcMessageEncoder<Resp> encoder) {
        this.request = request;
        this.httpResponse = httpResponse;
        this.encoder = encoder;
    }

    public GrpcServerResponse<Req, Resp> status(GrpcStatus status) {
        Objects.requireNonNull(status);
        this.status = status;
        return this;
    }

    @Override
    public GrpcServerResponse<Req, Resp> statusMessage(String msg) {
        this.statusMessage = msg;
        return this;
    }

    public GrpcServerResponse<Req, Resp> encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    @Override
    public MultiMap headers() {
        if (headersSent) {
            throw new IllegalStateException("Headers already sent");
        }
        if (headers == null) {
            headers = MultiMap.caseInsensitiveMultiMap();
        }
        return headers;
    }

    @Override
    public MultiMap trailers() {
        if (trailersSent) {
            throw new IllegalStateException("Trailers already sent");
        }
        if (trailers == null) {
            trailers = MultiMap.caseInsensitiveMultiMap();
        }
        return trailers;
    }

    @Override
    public GrpcTranscodingResponse<Req, Resp> exceptionHandler(Handler<Throwable> handler) {
        httpResponse.exceptionHandler(handler);
        return this;
    }

    @Override
    public Future<Void> write(Resp message) {
        return writeMessage(encoder.encode(message));
    }

    @Override
    public Future<Void> end(Resp message) {
        return endMessage(encoder.encode(message));
    }

    @Override
    public Future<Void> writeMessage(GrpcMessage data) {
        return writeMessage(data, false);
    }

    @Override
    public Future<Void> endMessage(GrpcMessage message) {
        return writeMessage(message, true);
    }

    public Future<Void> end() {
        return writeMessage(null, true);
    }

    @Override
    public GrpcServerResponse<Req, Resp> setWriteQueueMaxSize(int maxSize) {
        httpResponse.setWriteQueueMaxSize(maxSize);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return httpResponse.writeQueueFull();
    }

    @Override
    public GrpcServerResponse<Req, Resp> drainHandler(Handler<Void> handler) {
        httpResponse.drainHandler(handler);
        return this;
    }

    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        Future<Void> fut = request.end();
        boolean requestEnded;
        if (fut.failed()) {
            return;
        } else {
            requestEnded = fut.succeeded();
        }
        if (!requestEnded || !trailersSent) {
            httpResponse.reset(GrpcError.CANCELLED.http2ResetCode);
        }
    }

    private Future<Void> writeMessage(GrpcMessage message, boolean end) {
        if (cancelled) {
            throw new IllegalStateException("The stream has been cancelled");
        }
        if (trailersSent) {
            throw new IllegalStateException("The stream has been closed");
        }

        if (message == null && !end) {
            throw new IllegalStateException();
        }

        boolean trailersOnly = status != GrpcStatus.OK && !headersSent && end;

        MultiMap responseHeaders = httpResponse.headers();
        if (!headersSent) {
            headersSent = true;
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> header : headers) {
                    responseHeaders.add(header.getKey(), header.getValue());
                }
            }

            responseHeaders.set("content-type", "application/json");
        }

        if (end) {
            if (!trailersSent) {
                trailersSent = true;
            }
            MultiMap responseTrailers;
            if (trailersOnly) {
                responseTrailers = httpResponse.headers();
            } else {
                responseTrailers = httpResponse.trailers();
            }

            if (trailers != null && trailers.size() > 0) {
                for (Map.Entry<String, String> trailer : trailers) {
                    responseTrailers.add(trailer.getKey(), trailer.getValue());
                }
            }
            if (!responseHeaders.contains("grpc-status")) {
                responseTrailers.set("grpc-status", status.toString());
            }
            if (status != GrpcStatus.OK) {
                String msg = statusMessage;
                if (msg != null && !responseHeaders.contains("grpc-status-message")) {
                    responseTrailers.set("grpc-message", Utils.utf8PercentEncode(msg));
                }
            } else {
                responseTrailers.remove("grpc-message");
            }
            if (message != null) {
                Buffer encoded = encode(message);
                if (encoded == null) {
                    throw new IllegalStateException("The message is null");
                }

                responseHeaders.set("content-length", String.valueOf(encoded.length()));
                return httpResponse.end(encoded);
            } else {
                return httpResponse.end();
            }
        } else {
            Buffer encoded = encode(message);
            if (encoded == null) {
                throw new IllegalStateException("The message is null");
            }

            responseHeaders.set("content-length", String.valueOf(encoded.length()));
            return httpResponse.write(encoded);
        }
    }

    private Buffer encode(GrpcMessage message) {
        if (message == null) {
            return null;
        }

        ByteBuf bbuf = message.payload().getByteBuf();
        CompositeByteBuf composite = Unpooled.compositeBuffer();
        composite.addComponent(true, bbuf);
        return Buffer.buffer(composite);
    }

    @Override
    public void write(Resp data, Handler<AsyncResult<Void>> handler) {
        write(data).onComplete(handler);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        end().onComplete(handler);
    }
}
