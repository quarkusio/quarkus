package io.quarkus.grpc.runtime.stork;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.smallrye.stork.api.ServiceInstance;

@ApplicationScoped
public class StorkMeasuringGrpcInterceptor implements ClientInterceptor, Prioritized {

    public static final Context.Key<AtomicReference<ServiceInstance>> STORK_SERVICE_INSTANCE = Context
            .key("stork.service-instance");
    public static final Context.Key<Boolean> STORK_MEASURE_TIME = Context.key("stork.measure-time");

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
            Channel next) {
        return new StorkMeasuringCall<>(next.newCall(method, callOptions), method.getType());
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    private static class StorkMeasuringCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
        ServiceInstance serviceInstance;
        final boolean recordTime;

        protected StorkMeasuringCall(ClientCall<ReqT, RespT> delegate,
                MethodDescriptor.MethodType type) {
            super(delegate);
            this.recordTime = type == MethodDescriptor.MethodType.UNARY;
        }

        @Override
        public void start(final ClientCall.Listener<RespT> responseListener, final Metadata metadata) {
            Context context = Context.current().withValues(STORK_SERVICE_INSTANCE, new AtomicReference<>(),
                    STORK_MEASURE_TIME, recordTime);
            Context oldContext = context.attach();
            try {
                super.start(new StorkMeasuringCallListener<>(responseListener, this), metadata);
                serviceInstance = STORK_SERVICE_INSTANCE.get().get();
            } finally {
                context.detach(oldContext);
            }
        }

        void recordReply() {
            if (serviceInstance != null && recordTime) {
                serviceInstance.recordReply();
            }
        }

        void recordEnd(Throwable error) {
            if (serviceInstance != null) {
                serviceInstance.recordEnd(error);
            }
        }
    }

    private static class StorkMeasuringCallListener<RespT>
            extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
        final StorkMeasuringCall<?, ?> collector;

        public StorkMeasuringCallListener(ClientCall.Listener<RespT> responseListener, StorkMeasuringCall<?, ?> collector) {
            super(responseListener);
            this.collector = collector;
        }

        @Override
        public void onMessage(RespT message) {
            collector.recordReply();
            super.onMessage(message);
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            Exception error = null;
            if (!status.isOk()) {
                error = status.asException(trailers);
            }
            collector.recordEnd(error);
            super.onClose(status, trailers);
        }
    }
}
