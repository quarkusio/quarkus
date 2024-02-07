package io.quarkus.hal;

import java.util.Map;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

import org.eclipse.yasson.internal.ProcessingContext;
import org.eclipse.yasson.internal.model.ClassModel;
import org.eclipse.yasson.internal.model.PropertyModel;

// Using the raw type here as eclipse yasson doesn't like custom serializers for
// generic root types, see https://github.com/eclipse-ee4j/yasson/issues/639
public class HalEntityWrapperJsonbSerializer implements JsonbSerializer<HalEntityWrapper> {

    @Override
    public void serialize(HalEntityWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        ProcessingContext processingContext = (ProcessingContext) context;
        Object entity = wrapper.getEntity();

        if (!processingContext.addProcessedObject(entity)) {
            throw new RuntimeException("Cyclic dependency when marshaling an object");
        }

        try {
            generator.writeStartObject();
            ClassModel classModel = processingContext.getMappingContext().getOrCreateClassModel(entity.getClass());

            for (PropertyModel property : classModel.getSortedProperties()) {
                if (property.isReadable()) {
                    writeValue(property.getWriteName(), property.getValue(entity), generator, context);
                }
            }

            writeLinks(wrapper.getLinks(), generator, context);
            generator.writeEnd();
        } finally {
            processingContext.removeProcessedObject(entity);
        }
    }

    private void writeValue(String name, Object value, JsonGenerator generator, SerializationContext context) {
        if (value == null) {
            generator.writeNull(name);
        } else {
            context.serialize(name, value, generator);
        }
    }

    private void writeLinks(Map<String, HalLink> links, JsonGenerator generator, SerializationContext context) {
        context.serialize("_links", links, generator);
    }
}
