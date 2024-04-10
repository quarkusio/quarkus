package io.quarkus.grpc.transcoding;

import java.io.ByteArrayInputStream;

import io.grpc.MethodDescriptor;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageDecoder;

public class GrpcTranscodingMessageDecoder<T> implements GrpcMessageDecoder<T> {

    private final MethodDescriptor.Marshaller<T> marshaller;

    public GrpcTranscodingMessageDecoder(MethodDescriptor.Marshaller<T> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public T decode(GrpcMessage msg) {
        return marshaller.parse(new ByteArrayInputStream(msg.payload().getBytes()));
    }
}
