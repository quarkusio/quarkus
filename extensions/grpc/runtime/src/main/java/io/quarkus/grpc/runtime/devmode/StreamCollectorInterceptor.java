package io.quarkus.grpc.runtime.devmode;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.stubs.ServerCalls;
import io.quarkus.grpc.stubs.StreamCollector;

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
                newParams[i] = wrap(stream);
            } else {
                newParams[i] = params[i];
            }
        }
        context.setParameters(newParams);
        return context.proceed();
    }

    private StreamObserver<Object> wrap(StreamObserver<Object> stream) {
        if (stream instanceof ServerCallStreamObserver) {
            return new ServerCallStreamObserverWrapper<>((ServerCallStreamObserver<Object>) stream);
        }
        return new StreamObserverWrapper<>(stream);
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

    private final class ServerCallStreamObserverWrapper<T> extends ServerCallStreamObserver<T> {

        private final ServerCallStreamObserver<T> delegate;

        public ServerCallStreamObserverWrapper(ServerCallStreamObserver<T> delegate) {
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

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public void setOnCancelHandler(Runnable runnable) {
            delegate.setOnCancelHandler(runnable);
        }

        @Override
        public void setCompression(String s) {
            delegate.setCompression(s);
        }

        @Override
        public void disableAutoRequest() {
            delegate.disableAutoRequest();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setOnReadyHandler(Runnable runnable) {
            delegate.setOnReadyHandler(runnable);
        }

        @Override
        public void request(int i) {
            delegate.request(i);
        }

        @Override
        public void setMessageCompression(boolean b) {
            delegate.setMessageCompression(b);
        }

        @Override
        public void setOnCloseHandler(Runnable onCloseHandler) {
            delegate.setOnCloseHandler(onCloseHandler);
        }

        @Override
        public void disableAutoInboundFlowControl() {
            delegate.disableAutoInboundFlowControl();
        }
    }

}
