package io.quarkus.grpc.transcoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.grpc.MethodDescriptor;
import io.vertx.core.buffer.Buffer;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcMessageEncoder;

/*
 * A message encoder that uses a {@link MethodDescriptor.Marshaller} to encode the message payload.
 *
 * @param <T> The type of the message payload.
 * @see io.vertx.grpc.common.impl.GrpcMessageEncoderImpl for the original implementation
 */
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
