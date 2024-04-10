package io.quarkus.grpc.transcoding;

import java.util.HashMap;
import java.util.Map;

import io.grpc.MethodDescriptor;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.impl.GrpcMethodCall;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

/**
 * @see <a href="https://github.com/googleapis/googleapis/blob/master/google/api/http.proto">for the HTTP mapping rules</a>
 */
public class GrpcTranscodingServer implements GrpcServer {

    private final Vertx vertx;
    private Handler<GrpcServerRequest<Buffer, Buffer>> requestHandler;
    private final Map<String, String> methodMapping = new HashMap<>();
    private final Map<String, GrpcTranscodingServer.MethodCallHandler<?, ?>> methodCallHandlers = new HashMap<>();
    private final Map<String, GrpcTranscodingMetadata<?, ?>> metadataHandlers = new HashMap<>();

    public GrpcTranscodingServer(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(HttpServerRequest httpRequest) {
        String requestPath = httpRequest.path();

        for (Map.Entry<String, String> entry : methodMapping.entrySet()) {
            String pathTemplate = entry.getKey();
            String mappedMethod = entry.getValue();
            if (GrpcTranscodingHttpUtils.isPathMatch(requestPath, pathTemplate)) {
                GrpcTranscodingMetadata<?, ?> metadata = metadataHandlers.get(mappedMethod);
                if (metadata != null) {
                    if (metadata.getHttpMethodName().equals(httpRequest.method().name())) {
                        handleWithMappedMethod(httpRequest, pathTemplate, mappedMethod);
                        return;
                    }
                }
            }
        }

        httpRequest.response().setStatusCode(404).end();
    }

    private void handleWithMappedMethod(HttpServerRequest httpRequest, String pathTemplate, String mappedMethod) {
        GrpcMethodCall methodCall = new GrpcMethodCall("/" + mappedMethod);
        String fmn = methodCall.fullMethodName();
        MethodCallHandler<?, ?> method = methodCallHandlers.get(fmn);

        if (method != null) {
            handle(pathTemplate, method, httpRequest, methodCall);
        } else {
            httpRequest.response().setStatusCode(500).end();
        }
    }

    private <Req, Resp> void handle(String pathTemplate, MethodCallHandler<Req, Resp> method, HttpServerRequest httpRequest,
            GrpcMethodCall methodCall) {
        Map<String, String> pathParams = GrpcTranscodingHttpUtils.extractPathParams(pathTemplate, httpRequest.path());
        Map<String, String> queryParameters = new HashMap<>(httpRequest.params().entries().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll));

        GrpcTranscodingRequest<Req, Resp> grpcRequest = new GrpcTranscodingRequest<>(httpRequest, method.messageDecoder,
                method.messageEncoder, methodCall, pathParams, queryParameters);
        grpcRequest.init();
        method.handle(grpcRequest);
    }

    public GrpcServer callHandler(Handler<GrpcServerRequest<Buffer, Buffer>> handler) {
        this.requestHandler = handler;
        return this;
    }

    @Override
    public <Req, Resp> GrpcServer callHandler(MethodDescriptor<Req, Resp> methodDesc,
            Handler<GrpcServerRequest<Req, Resp>> handler) {
        if (handler != null) {
            MethodDescriptor.Marshaller<Req> reqMarshaller = findRequestMarshaller(methodDesc.getFullMethodName());
            MethodDescriptor.Marshaller<Resp> respMarshaller = findResponseMarshaller(methodDesc.getFullMethodName());

            methodCallHandlers.put(methodDesc.getFullMethodName(),
                    new GrpcTranscodingServer.MethodCallHandler<>(methodDesc,
                            GrpcMessageDecoder.unmarshaller(reqMarshaller),
                            GrpcMessageEncoder.marshaller(respMarshaller), handler));
        } else {
            methodCallHandlers.remove(methodDesc.getFullMethodName());
        }
        return this;
    }

    public void addMethodMapping(String path, String fullMethodName) {
        methodMapping.put(path, fullMethodName);
    }

    public void addMetadataHandler(String fullMethodName, GrpcTranscodingMetadata<?, ?> metadata) {
        metadataHandlers.put(fullMethodName, metadata);
    }

    @SuppressWarnings("unchecked")
    public <T> MethodDescriptor.Marshaller<T> findRequestMarshaller(String fullMethodName) {
        GrpcTranscodingMetadata<?, ?> metadata = metadataHandlers.get(fullMethodName);
        return (MethodDescriptor.Marshaller<T>) metadata.getRequestMarshaller();
    }

    @SuppressWarnings("unchecked")
    public <T> MethodDescriptor.Marshaller<T> findResponseMarshaller(String fullMethodName) {
        GrpcTranscodingMetadata<?, ?> metadata = metadataHandlers.get(fullMethodName);
        return (MethodDescriptor.Marshaller<T>) metadata.getResponseMarshaller();
    }

    private static class MethodCallHandler<Req, Resp> implements Handler<GrpcServerRequest<Req, Resp>> {

        final MethodDescriptor<Req, Resp> def;
        final GrpcMessageDecoder<Req> messageDecoder;
        final GrpcMessageEncoder<Resp> messageEncoder;
        final Handler<GrpcServerRequest<Req, Resp>> handler;

        MethodCallHandler(MethodDescriptor<Req, Resp> def, GrpcMessageDecoder<Req> messageDecoder,
                GrpcMessageEncoder<Resp> messageEncoder, Handler<GrpcServerRequest<Req, Resp>> handler) {
            this.def = def;
            this.messageDecoder = messageDecoder;
            this.messageEncoder = messageEncoder;
            this.handler = handler;
        }

        @Override
        public void handle(GrpcServerRequest<Req, Resp> grpcRequest) {
            handler.handle(grpcRequest);
        }
    }
}
