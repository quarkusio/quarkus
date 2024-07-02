package io.quarkus.grpc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.MethodDescriptor;
import io.grpc.Status;

public class GrpcTranscodingMarshaller<T extends Message> implements MethodDescriptor.PrototypeMarshaller<T> {

    private final static Logger log = Logger.getLogger(GrpcTranscodingMarshaller.class);

    private final T defaultInstance;

    public GrpcTranscodingMarshaller(T defaultInstance) {
        this.defaultInstance = checkNotNull(defaultInstance, "defaultInstance cannot be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getMessageClass() {
        return (Class<T>) defaultInstance.getClass();
    }

    @Override
    public T getMessagePrototype() {
        return defaultInstance;
    }

    @Override
    public InputStream stream(T value) {
        try {
            String response = JsonFormat.printer().omittingInsignificantWhitespace().print(value);
            return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        } catch (InvalidProtocolBufferException e) {
            throw Status.INTERNAL.withDescription("Unable to convert message to JSON").withCause(e).asRuntimeException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T parse(InputStream stream) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Message.Builder builder = defaultInstance.newBuilderForType();
            JsonFormat.parser().merge(reader, builder);
            return (T) builder.build();
        } catch (InvalidProtocolBufferException e) {
            log.error("Unable to parse JSON to message", e);
            throw Status.INTERNAL.withDescription("Unable to parse JSON to message").withCause(e).asRuntimeException();
        } catch (IOException e) {
            log.error("An I/O error occurred while parsing the stream", e);
            throw Status.INTERNAL.withDescription("An I/O error occurred while parsing the stream").withCause(e)
                    .asRuntimeException();
        }
    }
}
