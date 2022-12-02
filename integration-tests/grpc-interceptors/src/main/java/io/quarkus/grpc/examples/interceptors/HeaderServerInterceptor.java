package io.quarkus.grpc.examples.interceptors;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.annotations.VisibleForTesting;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;

/**
 * A interceptor to handle server header.
 */
@ApplicationScoped
@GlobalInterceptor
public class HeaderServerInterceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(HeaderServerInterceptor.class.getName());

    @VisibleForTesting
    static final Metadata.Key<String> CUSTOM_HEADER_KEY = Metadata.Key.of("custom_server_header_key",
            Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <I, O> ServerCall.Listener<I> interceptCall(
            ServerCall<I, O> call,
            final Metadata requestHeaders,
            ServerCallHandler<I, O> next) {
        logger.info("header received from client:" + requestHeaders);
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                responseHeaders.put(CUSTOM_HEADER_KEY, "customRespondValue");
                super.sendHeaders(responseHeaders);
            }
        }, requestHeaders);
    }
}
