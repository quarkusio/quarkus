package io.quarkus.avro.runtime.jackson;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * By default, you cannot serialize Avro specific records to JSON, as they contain non-serializable members.
 * This serializer iterates over the declared fields (in the schema), and build an object only containing these fields.
 * It means that the Avro "metadata" won't be included in the JSON representation.
 */
public class SpecificRecordBaseSerializer extends StdSerializer<SpecificRecordBase> {

    public SpecificRecordBaseSerializer() {
        super(SpecificRecordBase.class);
    }

    @Override
    public void serialize(SpecificRecordBase record, JsonGenerator gen,
            SerializationContext context) {
        gen.writeStartObject();
        List<Schema.Field> fields = record.getSchema().getFields();
        for (Schema.Field field : fields) {
            Object value = record.get(field.pos());
            gen.writeName(field.name());
            if (value == null) {
                context.defaultSerializeNullValue(gen);
            } else {
                context.findValueSerializer(value.getClass()).serialize(value, gen, context);
            }
        }
        gen.writeEndObject();
    }
}
