package io.quarkus.grpc.runtime.stork;

import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_MEASURE_TIME;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_SERVICE_INSTANCE;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.smallrye.stork.api.ServiceInstance;

/**
 * Similar to {@link StorkMeasuringGrpcInterceptor}, but with different entry points, since we use delayed
 * {@link StorkGrpcChannel}.
 */
@ApplicationScoped
public class VertxStorkMeasuringGrpcInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        boolean recordTime = method.getType() == MethodDescriptor.MethodType.UNARY;
        Context context = Context.current().withValues(STORK_SERVICE_INSTANCE, new AtomicReference<>(),
                STORK_MEASURE_TIME, recordTime);
        Context oldContext = context.attach();
        try {
            return new VertxStorkMeasuringCall<>(next.newCall(method, callOptions), recordTime);
        } finally {
            context.detach(oldContext);
        }
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    private static class VertxStorkMeasuringCall<ReqT, RespT> extends AbstractStorkMeasuringCall<ReqT, RespT> {
        ServiceInstance serviceInstance;

        protected VertxStorkMeasuringCall(ClientCall<ReqT, RespT> delegate, boolean recordTime) {
            super(delegate, recordTime);
        }

        @Override
        protected ServiceInstance serviceInstance() {
            return serviceInstance;
        }

        @Override
        public void start(final Listener<RespT> responseListener, final Metadata metadata) {
            AtomicReference<ServiceInstance> ref = STORK_SERVICE_INSTANCE.get();
            if (ref != null) {
                serviceInstance = ref.get();
            }
            super.start(new StorkMeasuringCallListener<>(responseListener, this), metadata);
        }
    }
}
