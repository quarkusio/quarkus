package io.quarkus.grpc.runtime.supports;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class CompressionInterceptor implements ServerInterceptor {

    private final String compression;

    public CompressionInterceptor(String compression) {
        if (compression == null) {
            throw new NullPointerException("Compression cannot be null");
        }
        this.compression = compression;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        call.setCompression(compression);
        return next.startCall(call, headers);
    }
}
