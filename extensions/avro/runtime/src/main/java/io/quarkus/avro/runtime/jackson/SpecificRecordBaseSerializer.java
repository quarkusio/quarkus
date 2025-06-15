package io.quarkus.avro.runtime.jackson;

import java.io.IOException;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * By default, you cannot serialize Avro specific records to JSON, as they contain non-serializable members. This
 * serializer iterates over the declared fields (in the schema), and build an object only containing these fields. It
 * means that the Avro "metadata" won't be included in the JSON representation.
 */
public class SpecificRecordBaseSerializer extends StdSerializer<SpecificRecordBase> {

    public SpecificRecordBaseSerializer() {
        super(SpecificRecordBase.class);
    }

    @Override
    public void serialize(SpecificRecordBase record, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        List<Schema.Field> fields = record.getSchema().getFields();
        for (Schema.Field field : fields) {
            provider.defaultSerializeField(field.name(), record.get(field.pos()), gen);
        }
        gen.writeEndObject();
    }
}
