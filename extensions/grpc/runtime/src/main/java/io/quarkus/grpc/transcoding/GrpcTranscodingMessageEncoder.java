package io.quarkus.grpc.transcoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.grpc.MethodDescriptor;
import io.vertx.core.buffer.Buffer;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageEncoder;

public class GrpcTranscodingMessageEncoder<T> implements GrpcMessageEncoder<T> {

    private final MethodDescriptor.Marshaller<T> marshaller;

    public GrpcTranscodingMessageEncoder(MethodDescriptor.Marshaller<T> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public GrpcMessage encode(T msg) {
        return new GrpcMessage() {
            private Buffer encoded;

            @Override
            public String encoding() {
                return "identity";
            }

            @Override
            public Buffer payload() {
                if (encoded == null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        marshaller.stream(msg).transferTo(baos);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] bytes = baos.toByteArray();
                    encoded = Buffer.buffer(bytes);
                }
                return encoded;
            }
        };
    }
}
