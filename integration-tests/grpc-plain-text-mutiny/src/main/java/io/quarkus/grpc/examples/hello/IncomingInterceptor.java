package io.quarkus.grpc.examples.hello;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

@ApplicationScoped
public class IncomingInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> EXTRA_HEADER = Metadata.Key.of("my-extra-header",
            Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> INTERFACE_HEADER = Metadata.Key.of("my-interface-header",
            Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> EXTRA_BLOCKING_HEADER = Metadata.Key.of("my-blocking-header",
            Metadata.ASCII_STRING_MARSHALLER);

    private final Map<String, String> headerValues = new HashMap<>();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {

        for (Metadata.Key<String> key : asList(EXTRA_HEADER, INTERFACE_HEADER, EXTRA_BLOCKING_HEADER)) {
            String header = metadata.get(key);
            if (header != null) {
                headerValues.put(key.name(), header);
            }
        }

        return serverCallHandler.startCall(serverCall, metadata);
    }

    public void clear() {
        headerValues.clear();
    }

    public Map<String, String> getCollectedHeaders() {
        return headerValues;
    }
}
