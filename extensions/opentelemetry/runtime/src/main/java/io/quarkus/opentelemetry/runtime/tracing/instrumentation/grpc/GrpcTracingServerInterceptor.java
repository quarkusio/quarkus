package io.quarkus.opentelemetry.runtime.tracing.instrumentation.grpc;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;
import static io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.GrpcHttpInstrumenterVertxTracer.GRPC_HTTP_CLIENT_ADDRESS;
import static io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.GrpcHttpInstrumenterVertxTracer.GRPC_HTTP_PROTOCOL_VERSION;
import static io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.GrpcHttpInstrumenterVertxTracer.GRPC_HTTP_URL_SCHEME;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

@Singleton
@GlobalInterceptor
public class GrpcTracingServerInterceptor implements ServerInterceptor {
    private final Instrumenter<GrpcRequest, Status> instrumenter;

    public GrpcTracingServerInterceptor(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {
        InstrumenterBuilder<GrpcRequest, Status> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new GrpcSpanNameExtractor());

        builder.setEnabled(!runtimeConfig.sdkDisabled());

        GrpcServerNetworkAttributesGetter getter = new GrpcServerNetworkAttributesGetter();

        builder.addAttributesExtractor(RpcServerAttributesExtractor.create(GrpcAttributesGetter.INSTANCE))
                .addAttributesExtractor(ServerAttributesExtractor.create(getter))
                .addAttributesExtractor(NetworkAttributesExtractor.create(getter))
                .addAttributesExtractor(new GrpcStatusCodeExtractor())
                .setSpanStatusExtractor(new GrpcSpanStatusExtractor());

        this.instrumenter = builder.buildInstrumenter(SpanKindExtractor.alwaysServer());
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            final ServerCall<ReqT, RespT> call, final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {

        GrpcRequest grpcRequest = GrpcRequest.server(call.getMethodDescriptor(), headers, call.getAttributes(),
                call.getAuthority());
        Context parentContext = Context.current();
        boolean shouldStart = instrumenter.shouldStart(parentContext, grpcRequest);
        if (shouldStart) {
            Context spanContext = instrumenter.start(parentContext, grpcRequest);
            Scope scope = spanContext.makeCurrent();
            addHttpAttributes(spanContext);
            TracingServerCall<ReqT, RespT> tracingServerCall = new TracingServerCall<>(call, spanContext, scope, grpcRequest);
            return new TracingServerCallListener<>(next.startCall(tracingServerCall, headers), spanContext, scope, grpcRequest);
        }

        return next.startCall(call, headers);
    }

    private static void addHttpAttributes(Context spanContext) {
        io.vertx.core.Context vertxContext = Vertx.currentContext();
        if (vertxContext == null || !VertxContext.isDuplicatedContext(vertxContext)) {
            return;
        }
        ConcurrentHashMap<String, Object> data = VertxContext.localContextData(vertxContext);
        Span span = Span.fromContext(spanContext);
        String scheme = (String) data.get(GRPC_HTTP_URL_SCHEME);
        if (scheme != null) {
            span.setAttribute(UrlAttributes.URL_SCHEME, scheme);
        }
        String clientAddress = (String) data.get(GRPC_HTTP_CLIENT_ADDRESS);
        if (clientAddress != null) {
            span.setAttribute(ClientAttributes.CLIENT_ADDRESS, clientAddress);
        }
        String protocolVersion = (String) data.get(GRPC_HTTP_PROTOCOL_VERSION);
        if (protocolVersion != null) {
            span.setAttribute(NetworkAttributes.NETWORK_PROTOCOL_VERSION, protocolVersion);
        }
    }

    static class GrpcServerNetworkAttributesGetter implements NetworkAttributesGetter<GrpcRequest, Status>,
            ServerAttributesGetter<GrpcRequest> {

        @Override
        public String getServerAddress(GrpcRequest grpcRequest) {
            return grpcRequest.getLogicalHost();
        }

        @Override
        public Integer getServerPort(GrpcRequest grpcRequest) {
            return grpcRequest.getLogicalPort();
        }

        @Override
        public InetSocketAddress getNetworkLocalInetSocketAddress(
                GrpcRequest grpcRequest, Status status) {
            // TODO: later version introduces TRANSPORT_ATTR_LOCAL_ADDR, might be a good idea to use it
            return null;
        }

        @Override
        public InetSocketAddress getNetworkPeerInetSocketAddress(
                GrpcRequest request, Status status) {
            SocketAddress address = request.getPeerSocketAddress();
            if (address instanceof InetSocketAddress) {
                return (InetSocketAddress) address;
            }
            return null;
        }
    }

    private class TracingServerCallListener<ReqT> extends SimpleForwardingServerCallListener<ReqT> {
        private final Context spanContext;
        private final Scope scope;
        private final GrpcRequest grpcRequest;

        protected TracingServerCallListener(
                final ServerCall.Listener<ReqT> delegate,
                final Context spanContext,
                final Scope scope,
                final GrpcRequest grpcRequest) {

            super(delegate);
            this.scope = scope;
            this.spanContext = spanContext;
            this.grpcRequest = grpcRequest;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (Exception e) {
                try (scope) {
                    instrumenter.end(spanContext, grpcRequest, null, e);
                }
                throw e;
            }
        }

        @Override
        public void onCancel() {
            try {
                super.onCancel();
            } catch (Exception e) {
                try (scope) {
                    instrumenter.end(spanContext, grpcRequest, null, e);
                }
                throw e;
            }
            try (scope) {
                instrumenter.end(spanContext, grpcRequest, Status.CANCELLED, null);
            }
        }

        @Override
        public void onComplete() {
            try {
                super.onComplete();
            } catch (Exception e) {
                try (scope) {
                    instrumenter.end(spanContext, grpcRequest, null, e);
                }
                throw e;
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (Exception e) {
                try (scope) {
                    instrumenter.end(spanContext, grpcRequest, null, e);
                }
                throw e;
            }
        }
    }

    private class TracingServerCall<ReqT, RespT> extends SimpleForwardingServerCall<ReqT, RespT> {
        private final Context spanContext;
        private final Scope scope;
        private final GrpcRequest grpcRequest;

        public TracingServerCall(
                final ServerCall<ReqT, RespT> delegate,
                final Context spanContext,
                final Scope scope,
                final GrpcRequest grpcRequest) {

            super(delegate);
            this.spanContext = spanContext;
            this.scope = scope;
            this.grpcRequest = grpcRequest;
        }

        @Override
        public void close(final Status status, final Metadata trailers) {
            try {
                super.close(status, trailers);
            } catch (Exception e) {
                try (scope) {
                    instrumenter.end(spanContext, grpcRequest, null, e);
                }
                throw e;
            }
            try (scope) {
                instrumenter.end(spanContext, grpcRequest, status, status.getCause());
            }
        }
    }
}
