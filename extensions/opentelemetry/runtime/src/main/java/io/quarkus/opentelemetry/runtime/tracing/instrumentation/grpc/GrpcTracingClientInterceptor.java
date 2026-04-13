package io.quarkus.opentelemetry.runtime.tracing.instrumentation.grpc;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import jakarta.inject.Singleton;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.vertx.core.Vertx;

@Singleton
@GlobalInterceptor
public class GrpcTracingClientInterceptor implements ClientInterceptor {
    private final Instrumenter<GrpcRequest, Status> instrumenter;

    public GrpcTracingClientInterceptor(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {
        InstrumenterBuilder<GrpcRequest, Status> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new GrpcSpanNameExtractor());

        builder.setEnabled(!runtimeConfig.sdkDisabled());

        builder.addAttributesExtractor(RpcClientAttributesExtractor.create(GrpcAttributesGetter.INSTANCE))
                .addAttributesExtractor(new GrpcStatusCodeExtractor())
                .setSpanStatusExtractor(new GrpcSpanStatusExtractor());

        this.instrumenter = builder.buildInstrumenter(SpanKindExtractor.alwaysClient());
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> method, final CallOptions callOptions, final Channel next) {

        GrpcRequest grpcRequest = GrpcRequest.client(method, callOptions.getAuthority());
        Context parentContext = Context.current();
        boolean shouldStart = instrumenter.shouldStart(parentContext, grpcRequest);
        if (shouldStart) {
            Context spanContext = instrumenter.start(parentContext, grpcRequest);
            Scope scope = spanContext.makeCurrent();
            QuarkusContextStorage.setContextOverride(Vertx.currentContext(), spanContext);
            ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
            return new TracingClientCall<>(clientCall, spanContext, scope, grpcRequest);
        }

        return next.newCall(method, callOptions);
    }

    private class TracingClientCallListener<ReqT> extends SimpleForwardingClientCallListener<ReqT> {
        private final Context spanContext;
        private final Scope scope;
        private final GrpcRequest grpcRequest;

        public TracingClientCallListener(final ClientCall.Listener<ReqT> delegate, final Context spanContext,
                final Scope scope, final GrpcRequest grpcRequest) {
            super(delegate);
            this.spanContext = spanContext;
            this.scope = scope;
            this.grpcRequest = grpcRequest;
        }

        @Override
        public void onClose(final Status status, final Metadata trailers) {
            try (scope) {
                QuarkusContextStorage.clearContextOverride(Vertx.currentContext());
                instrumenter.end(spanContext, grpcRequest, status, status.getCause());
            }
            super.onClose(status, trailers);
        }
    }

    private class TracingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
        private final Context spanContext;
        private final Scope scope;
        private final GrpcRequest grpcRequest;

        protected TracingClientCall(final ClientCall<ReqT, RespT> delegate, final Context spanContext,
                final Scope scope, final GrpcRequest grpcRequest) {
            super(delegate);
            this.spanContext = spanContext;
            this.scope = scope;
            this.grpcRequest = grpcRequest;
        }

        @Override
        public void start(final Listener<RespT> responseListener, final Metadata headers) {
            GrpcRequest clientRequest = GrpcRequest.client(grpcRequest.getMethodDescriptor(), headers);
            super.start(new TracingClientCallListener<>(responseListener, spanContext, scope, clientRequest), headers);
        }
    }
}
