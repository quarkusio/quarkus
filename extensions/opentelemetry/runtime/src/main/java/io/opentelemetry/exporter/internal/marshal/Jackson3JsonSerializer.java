package io.opentelemetry.exporter.internal.marshal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;

/**
 * Jackson 3 replacement for OTel's {@code JsonSerializer} which uses Jackson 2.
 * <p>
 * This class lives in the same package as {@link Serializer} to access its package-private
 * constructor. At build time, Quarkus rewrites {@link Marshaler} to instantiate this class
 * instead of the original {@code JsonSerializer}.
 * <p>
 * TODO: remove once OTel exporters use Jackson 3 or don't use Jackson at all, see
 * <a href="https://github.com/open-telemetry/opentelemetry-java/issues/8533">this issue</a>
 */
@SuppressWarnings("unsused") // this is used in Marshaler because of a Quarkus build time transformation
public final class Jackson3JsonSerializer extends Serializer {

    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    private final JsonGenerator generator;

    public Jackson3JsonSerializer(OutputStream output) throws IOException {
        this(JSON_FACTORY.createGenerator(output));
    }

    public Jackson3JsonSerializer(JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    protected void writeTraceId(ProtoFieldInfo field, String traceId) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(traceId);
    }

    @Override
    protected void writeSpanId(ProtoFieldInfo field, String spanId) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(spanId);
    }

    @Override
    public void writeBool(ProtoFieldInfo field, boolean value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeBoolean(value);
    }

    @Override
    protected void writeEnum(ProtoFieldInfo field, ProtoEnumInfo enumValue) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeNumber(enumValue.getEnumNumber());
    }

    @Override
    protected void writeUint32(ProtoFieldInfo field, int value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeNumber(value);
    }

    @Override
    protected void writeSInt32(ProtoFieldInfo field, int value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeNumber(value);
    }

    @Override
    protected void writeint32(ProtoFieldInfo field, int value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeNumber(value);
    }

    @Override
    public void writeInt64(ProtoFieldInfo field, long value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(Long.toString(value));
    }

    @Override
    protected void writeFixed64(ProtoFieldInfo field, long value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(Long.toString(value));
    }

    @Override
    protected void writeFixed64Value(long value) throws IOException {
        generator.writeString(Long.toString(value));
    }

    @Override
    protected void writeUInt64Value(long value) throws IOException {
        generator.writeString(Long.toString(value));
    }

    @Override
    public void writeUInt64(ProtoFieldInfo field, long value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(Long.toString(value));
    }

    @Override
    protected void writeFixed32(ProtoFieldInfo field, int value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeNumber(value);
    }

    @Override
    public void writeDouble(ProtoFieldInfo field, double value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeNumber(value);
    }

    @Override
    protected void writeDoubleValue(double value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeString(ProtoFieldInfo field, byte[] utf8Bytes) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(new String(utf8Bytes, StandardCharsets.UTF_8));
    }

    @Override
    public void writeString(
            ProtoFieldInfo field, String string, int utf8Length, MarshalerContext context)
            throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeString(string);
    }

    @Override
    public void writeRepeatedString(ProtoFieldInfo field, byte[][] utf8Bytes) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
        for (byte[] value : utf8Bytes) {
            generator.writeString(new String(value, StandardCharsets.UTF_8));
        }
        generator.writeEndArray();
    }

    @Override
    public void writeBytes(ProtoFieldInfo field, byte[] value) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeBinary(value);
    }

    @Override
    public void writeByteBuffer(ProtoFieldInfo field, ByteBuffer value) throws IOException {
        byte[] data = new byte[value.capacity()];
        ((ByteBuffer) value.duplicate().clear()).get(data);
        generator.writeName(field.getJsonName());
        generator.writeBinary(data);
    }

    @Override
    protected void writeStartMessage(ProtoFieldInfo field, int protoMessageSize) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartObject();
    }

    @Override
    protected void writeEndMessage() throws IOException {
        generator.writeEndObject();
    }

    @Override
    protected void writeStartRepeatedPrimitive(
            ProtoFieldInfo field, int protoSizePerElement, int numElements) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
    }

    @Override
    protected void writeEndRepeatedPrimitive() throws IOException {
        generator.writeEndArray();
    }

    @Override
    protected void writeStartRepeatedVarint(ProtoFieldInfo field, int payloadSize)
            throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
    }

    @Override
    protected void writeEndRepeatedVarint() throws IOException {
        generator.writeEndArray();
    }

    @Override
    public void serializeRepeatedMessage(ProtoFieldInfo field, Marshaler[] repeatedMessage)
            throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
        for (Marshaler marshaler : repeatedMessage) {
            writeMessageValue(marshaler);
        }
        generator.writeEndArray();
    }

    @Override
    public void serializeRepeatedMessage(
            ProtoFieldInfo field, List<? extends Marshaler> repeatedMessage) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
        for (Marshaler marshaler : repeatedMessage) {
            writeMessageValue(marshaler);
        }
        generator.writeEndArray();
    }

    @Override
    public <T> void serializeRepeatedMessageWithContext(
            ProtoFieldInfo field,
            List<? extends T> messages,
            StatelessMarshaler<T> marshaler,
            MarshalerContext context)
            throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
        for (int i = 0; i < messages.size(); i++) {
            T message = messages.get(i);
            generator.writeStartObject();
            marshaler.writeTo(this, message, context);
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    @Override
    public void writeStartRepeated(ProtoFieldInfo field) throws IOException {
        generator.writeName(field.getJsonName());
        generator.writeStartArray();
    }

    @Override
    public void writeEndRepeated() throws IOException {
        generator.writeEndArray();
    }

    @Override
    public void writeStartRepeatedElement(ProtoFieldInfo field, int protoMessageSize)
            throws IOException {
        generator.writeStartObject();
    }

    @Override
    public void writeEndRepeatedElement() throws IOException {
        generator.writeEndObject();
    }

    // Not a field.
    public void writeMessageValue(Marshaler message) throws IOException {
        generator.writeStartObject();
        message.writeTo(this);
        generator.writeEndObject();
    }

    @Override
    public void writeSerializedMessage(byte[] protoSerialized, String jsonSerialized)
            throws IOException {
        generator.writeRaw(jsonSerialized);
    }

    @Override
    public void close() throws IOException {
        generator.close();
    }
}
