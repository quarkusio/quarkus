package io.quarkus.grpc.runtime.stork;

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

@ApplicationScoped
public class StorkMeasuringGrpcInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new StorkMeasuringCall<>(next.newCall(method, callOptions), method.getType());
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    private static class StorkMeasuringCall<ReqT, RespT> extends AbstractStorkMeasuringCall<ReqT, RespT> {
        ServiceInstance serviceInstance;

        protected StorkMeasuringCall(ClientCall<ReqT, RespT> delegate, MethodDescriptor.MethodType type) {
            super(delegate, type == MethodDescriptor.MethodType.UNARY);
        }

        @Override
        protected ServiceInstance serviceInstance() {
            return serviceInstance;
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
    }
}
