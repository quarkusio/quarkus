package io.quarkus.grpc.transcoding;

import static io.vertx.grpc.common.GrpcError.mapHttp2ErrorCode;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collector;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.future.PromiseInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import io.vertx.grpc.common.CodecException;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcReadStream;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.grpc.server.GrpcServerResponse;

/**
 * A gRPC transcoding request that maps HTTP requests to gRPC methods.
 *
 * @param <Req> The type of the request message.
 * @param <Resp> The type of the response message.
 * @see io.vertx.grpc.server.impl.GrpcServerRequestImpl for the original implementation
 */
public class GrpcTranscodingRequest<Req, Resp> implements GrpcReadStream<Req>, Handler<Buffer>, GrpcServerRequest<Req, Resp> {

    static final GrpcMessage END_SENTINEL = new GrpcMessage() {
        @Override
        public String encoding() {
            return null;
        }

        @Override
        public Buffer payload() {
            return null;
        }
    };

    private final HttpServerRequest httpRequest;
    private final GrpcServerResponse<Req, Resp> response;
    private GrpcMethodCall methodCall;
    protected final ContextInternal context;
    private final ReadStream<Buffer> stream;
    private final InboundBuffer<GrpcMessage> queue;
    private Buffer buffer;
    private Handler<GrpcError> errorHandler;
    private Handler<Throwable> exceptionHandler;
    private Handler<GrpcMessage> messageHandler;
    private Handler<Void> endHandler;
    private GrpcMessage last;
    private final GrpcMessageDecoder<Req> messageDecoder;
    private final Promise<Void> end;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;

    public GrpcTranscodingRequest(HttpServerRequest httpRequest,
            GrpcMessageDecoder<Req> messageDecoder,
            GrpcMessageEncoder<Resp> messageEncoder,
            GrpcMethodCall methodCall,
            Map<String, String> pathParams,
            Map<String, String> queryParams) {
        this.httpRequest = httpRequest;
        this.response = new GrpcTranscodingResponse<>(this, httpRequest.response(), messageEncoder);
        this.methodCall = methodCall;
        this.pathParams = pathParams;
        this.queryParams = queryParams;

        this.context = (ContextInternal) ((HttpServerRequestInternal) httpRequest).context();
        this.stream = httpRequest;
        this.queue = new InboundBuffer<>(context);
        this.messageDecoder = messageDecoder;
        this.end = context.promise();
    }

    public void init() {
        stream.handler(this);
        stream.endHandler(v -> queue.write(END_SENTINEL));
        stream.exceptionHandler(err -> {
            if (err instanceof StreamResetException) {
                handleReset(((StreamResetException) err).getCode());
            } else {
                handleException(err);
            }
        });
        queue.drainHandler(v -> stream.resume());
        queue.handler(msg -> {
            if (msg == END_SENTINEL) {
                if (httpRequest.bytesRead() == 0) {
                    handleMessage(mergeParametersIntoMessage(msg));
                }

                handleEnd();
            } else {
                handleMessage(mergeParametersIntoMessage(msg));
            }
        });
    }

    private GrpcMessage mergeParametersIntoMessage(GrpcMessage msg) {
        try {
            Map<String, Object> allParams = GrpcTranscodingMessageWriter.mergeParameters(
                    pathParams,
                    queryParams,
                    msg.payload());

            byte[] jsonPayload = Json.encode(allParams).getBytes();
            return GrpcMessage.message(msg.encoding(), Buffer.buffer(jsonPayload));
        } catch (DecodeException e) {
            // Invalid JSON payload
            httpRequest.response().setStatusCode(422).end();
            return null;
        }
    }

    protected Req decodeMessage(GrpcMessage msg) throws CodecException {
        return messageDecoder.decode(msg);
    }

    @Override
    public void handle(Buffer chunk) {
        if (buffer == null) {
            buffer = chunk;
        } else {
            buffer.appendBuffer(chunk);
        }

        Buffer payload = buffer.slice();
        GrpcMessage message = GrpcMessage.message("identity", payload);
        boolean pause = !queue.write(message);

        if (pause) {
            stream.pause();
        }

        buffer = null;
    }

    protected void handleReset(long code) {
        Handler<GrpcError> handler = errorHandler;
        if (handler != null) {
            GrpcError error = mapHttp2ErrorCode(code);
            if (error != null) {
                handler.handle(error);
            }
        }
    }

    protected void handleException(Throwable err) {
        end.tryFail(err);
        Handler<Throwable> handler = exceptionHandler;
        if (handler != null) {
            handler.handle(err);
        }
    }

    protected void handleEnd() {
        end.tryComplete();
        Handler<Void> handler = endHandler;
        if (handler != null) {
            handler.handle(null);
        }
    }

    protected void handleMessage(GrpcMessage msg) {
        last = msg;
        Handler<GrpcMessage> handler = messageHandler;
        if (handler != null) {
            handler.handle(msg);
        }
    }

    @Override
    public ServiceName serviceName() {
        return methodCall.serviceName();
    }

    @Override
    public String methodName() {
        return methodCall.methodName();
    }

    @Override
    public String fullMethodName() {
        return methodCall.fullMethodName();
    }

    @Override
    public GrpcServerResponse<Req, Resp> response() {
        return response;
    }

    @Override
    public MultiMap headers() {
        return httpRequest.headers();
    }

    @Override
    public String encoding() {
        return httpRequest.getHeader("grpc-encoding");
    }

    @Override
    public GrpcServerRequest<Req, Resp> messageHandler(@Nullable Handler<GrpcMessage> handler) {
        messageHandler = handler;
        return this;
    }

    @Override
    public GrpcServerRequest<Req, Resp> errorHandler(@Nullable Handler<GrpcError> handler) {
        errorHandler = handler;
        return this;
    }

    @Override
    public GrpcServerRequest<Req, Resp> exceptionHandler(@Nullable Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    @Override
    public GrpcServerRequest<Req, Resp> handler(@Nullable Handler<Req> handler) {
        if (handler != null) {
            return messageHandler(msg -> {
                Req decoded;
                try {
                    decoded = decodeMessage(msg);
                } catch (CodecException e) {
                    response.cancel();
                    return;
                }
                handler.handle(decoded);
            });
        } else {
            return messageHandler(null);
        }
    }

    @Override
    public GrpcServerRequest<Req, Resp> pause() {
        queue.pause();
        return this;
    }

    @Override
    public GrpcServerRequest<Req, Resp> resume() {
        queue.resume();
        return this;
    }

    @Override
    public GrpcServerRequest<Req, Resp> fetch(long amount) {
        queue.fetch(amount);
        return this;
    }

    @Override
    public GrpcServerRequest<Req, Resp> endHandler(@Nullable Handler<Void> handler) {
        this.endHandler = handler;
        return this;
    }

    @Override
    public Future<Req> last() {
        return end().map(v -> decodeMessage(last));
    }

    @Override
    public Future<Void> end() {
        return end.future();
    }

    @Override
    public <R, C> Future<R> collecting(Collector<Req, C, R> collector) {
        PromiseInternal<R> promise = context.promise();
        C cumulation = collector.supplier().get();
        BiConsumer<C, Req> accumulator = collector.accumulator();
        handler(elt -> accumulator.accept(cumulation, elt));
        endHandler(v -> {
            R result = collector.finisher().apply(cumulation);
            promise.tryComplete(result);
        });
        exceptionHandler(promise::tryFail);
        return promise.future();
    }

    @Override
    public HttpConnection connection() {
        return httpRequest.connection();
    }
}
