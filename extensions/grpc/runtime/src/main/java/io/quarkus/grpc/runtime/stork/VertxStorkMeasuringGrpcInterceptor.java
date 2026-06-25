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
 * Similar to {@link StorkMeasuringGrpcInterceptor}, but with different entry points,
 * since we use delayed {@link StorkGrpcChannel}.
 */
@ApplicationScoped
public class VertxStorkMeasuringGrpcInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
            Channel next) {
        boolean recordTime = method.getType() == MethodDescriptor.MethodType.UNARY;
        AtomicReference<ServiceInstance> serviceInstanceRef = new AtomicReference<>();
        Context storkContext = Context.current().withValues(
                STORK_SERVICE_INSTANCE, serviceInstanceRef,
                STORK_MEASURE_TIME, recordTime);
        Context previous = storkContext.attach();
        Runnable detachContext = () -> storkContext.detach(previous);
        try {
            return new VertxStorkMeasuringCall<>(next.newCall(method, callOptions), recordTime, serviceInstanceRef,
                    detachContext);
        } catch (RuntimeException | Error e) {
            detachContext.run();
            throw e;
        }
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    private static class VertxStorkMeasuringCall<ReqT, RespT> extends AbstractStorkMeasuringCall<ReqT, RespT> {
        private final Runnable detachContext;

        protected VertxStorkMeasuringCall(ClientCall<ReqT, RespT> delegate, boolean recordTime,
                AtomicReference<ServiceInstance> serviceInstanceRef, Runnable detachContext) {
            super(delegate, recordTime, serviceInstanceRef);
            this.detachContext = detachContext;
        }

        @Override
        public void start(final Listener<RespT> responseListener, final Metadata metadata) {
            super.start(new StorkMeasuringCallListener<>(responseListener, this, detachContext), metadata);
        }
    }
}
