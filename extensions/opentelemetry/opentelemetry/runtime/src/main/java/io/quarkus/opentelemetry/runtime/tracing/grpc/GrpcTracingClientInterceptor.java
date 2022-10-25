package io.quarkus.opentelemetry.runtime.tracing.grpc;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;

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
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcClientAttributesExtractor;
import io.quarkus.grpc.GlobalInterceptor;

@Singleton
@GlobalInterceptor
public class GrpcTracingClientInterceptor implements ClientInterceptor {
    private final OpenTelemetry openTelemetry;
    private final Instrumenter<GrpcRequest, Status> instrumenter;

    public GrpcTracingClientInterceptor(final OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;

        InstrumenterBuilder<GrpcRequest, Status> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new GrpcSpanNameExtractor());

        builder.addAttributesExtractor(RpcClientAttributesExtractor.create(GrpcAttributesGetter.INSTANCE))
                .addAttributesExtractor(new GrpcStatusCodeExtractor())
                .setSpanStatusExtractor(new GrpcSpanStatusExtractor());

        this.instrumenter = builder.newClientInstrumenter(GrpcTextMapSetter.INSTANCE);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> method, final CallOptions callOptions, final Channel next) {

        GrpcRequest grpcRequest = GrpcRequest.client(method);
        Context parentContext = Context.current();
        boolean shouldStart = instrumenter.shouldStart(parentContext, grpcRequest);
        if (shouldStart) {
            Context spanContext = instrumenter.start(parentContext, grpcRequest);
            try (Scope ignored = spanContext.makeCurrent()) {
                ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
                return new TracingClientCall<>(clientCall, spanContext, grpcRequest);
            }
        }

        return next.newCall(method, callOptions);
    }

    private enum GrpcTextMapSetter implements TextMapSetter<GrpcRequest> {
        INSTANCE;

        @Override
        public void set(final GrpcRequest carrier, final String key, final String value) {
            if (carrier != null && carrier.getMetadata() != null) {
                carrier.getMetadata().put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);
            }
        }
    }

    private class TracingClientCallListener<ReqT> extends SimpleForwardingClientCallListener<ReqT> {
        private final Context spanContext;
        private final GrpcRequest grpcRequest;

        public TracingClientCallListener(final ClientCall.Listener<ReqT> delegate, final Context spanContext,
                final GrpcRequest grpcRequest) {
            super(delegate);
            this.spanContext = spanContext;
            this.grpcRequest = grpcRequest;
        }

        @Override
        public void onClose(final Status status, final Metadata trailers) {
            instrumenter.end(spanContext, grpcRequest, status, status.getCause());
            super.onClose(status, trailers);
        }
    }

    private class TracingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
        private final Context spanContext;
        private final GrpcRequest grpcRequest;

        protected TracingClientCall(final ClientCall<ReqT, RespT> delegate, final Context spanContext,
                final GrpcRequest grpcRequest) {
            super(delegate);
            this.spanContext = spanContext;
            this.grpcRequest = grpcRequest;
        }

        @Override
        public void start(final Listener<RespT> responseListener, final Metadata headers) {
            GrpcRequest clientRequest = GrpcRequest.client(grpcRequest.getMethodDescriptor(), headers);
            openTelemetry.getPropagators().getTextMapPropagator().inject(spanContext, clientRequest,
                    GrpcTextMapSetter.INSTANCE);
            super.start(new TracingClientCallListener<>(responseListener, spanContext, clientRequest), headers);
        }
    }
}
