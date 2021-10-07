package io.quarkus.grpc.runtime.devmode;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.runtime.ServerCalls;
import io.quarkus.grpc.runtime.StreamCollector;

@CollectStreams
@Priority(1)
@Interceptor
public class StreamCollectorInterceptor {

    private final StreamCollector streamCollector;

    public StreamCollectorInterceptor() {
        this.streamCollector = ServerCalls.getStreamCollector();
    }

    @SuppressWarnings("unchecked")
    @AroundInvoke
    Object collect(InvocationContext context) throws Exception {
        // Wraps the first StreamObserver parameter if available
        Object[] params = context.getParameters();
        int streamIndex = 0;
        StreamObserver<Object> stream = null;
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                continue;
            }
            if (StreamObserver.class.isAssignableFrom(param.getClass())) {
                stream = (StreamObserver<Object>) param;
                streamIndex = i;
                break;
            }
        }
        if (stream == null) {
            return context.proceed();
        }
        streamCollector.add(stream);
        Object[] newParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (i == streamIndex) {
                newParams[i] = new StreamObserverWrapper<>(stream);
            } else {
                newParams[i] = params[i];
            }
        }
        context.setParameters(newParams);
        return context.proceed();
    }

    private final class StreamObserverWrapper<T> implements StreamObserver<T> {

        private final StreamObserver<T> delegate;

        public StreamObserverWrapper(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
            streamCollector.remove(delegate);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
            streamCollector.remove(delegate);
        }

    }

}
