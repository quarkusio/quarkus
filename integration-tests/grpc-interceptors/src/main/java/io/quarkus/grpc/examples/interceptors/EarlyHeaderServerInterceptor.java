package io.quarkus.grpc.examples.interceptors;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;

@GlobalInterceptor
@ApplicationScoped
public class EarlyHeaderServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> HEADER = Metadata.Key.of("xx-acme-header", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        ServerCallDiscardingHeaders<ReqT, RespT> wrappedServerCall = new ServerCallDiscardingHeaders<>(call);
        ServerCall.Listener<ReqT> serverCallListener = next.startCall(wrappedServerCall, headers);
        if (wrappedServerCall.isClosed()) {
            return new ServerCall.Listener<>() {
            };
        }

        Metadata metadata = new Metadata();
        metadata.put(HEADER, "whatever");
        call.sendHeaders(metadata);

        return serverCallListener;
    }

    private static class ServerCallDiscardingHeaders<ReqT, RespT>
            extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

        private boolean closed;

        protected ServerCallDiscardingHeaders(ServerCall<ReqT, RespT> delegate) {
            super(delegate);
        }

        @Override
        public void sendHeaders(Metadata headers) {
            // headers have been sent already
        }

        @Override
        public void sendMessage(RespT message) {
            super.sendMessage(message);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            closed = true;
            super.close(status, trailers);
        }

        boolean isClosed() {
            return closed;
        }
    }
}
