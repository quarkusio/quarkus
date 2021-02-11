package io.quarkus.rest.data.panache.runtime.hal;

import java.util.Map;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

import org.eclipse.yasson.internal.Marshaller;
import org.eclipse.yasson.internal.model.ClassModel;
import org.eclipse.yasson.internal.model.PropertyModel;

public class HalEntityWrapperJsonbSerializer implements JsonbSerializer<HalEntityWrapper> {

    private final HalLinksProvider linksExtractor;

    public HalEntityWrapperJsonbSerializer() {
        this.linksExtractor = new RestEasyHalLinksProvider();
    }

    HalEntityWrapperJsonbSerializer(HalLinksProvider linksExtractor) {
        this.linksExtractor = linksExtractor;
    }

    @Override
    public void serialize(HalEntityWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        Marshaller marshaller = (Marshaller) context;
        Object entity = wrapper.getEntity();

        if (!marshaller.addProcessedObject(entity)) {
            throw new RuntimeException("Cyclic dependency when marshaling an object");
        }

        try {
            generator.writeStartObject();
            ClassModel classModel = marshaller.getMappingContext().getOrCreateClassModel(entity.getClass());

            for (PropertyModel property : classModel.getSortedProperties()) {
                if (property.isReadable()) {
                    writeValue(property.getWriteName(), property.getValue(entity), generator, context);
                }
            }

            writeLinks(entity, generator, context);
            generator.writeEnd();
        } finally {
            marshaller.removeProcessedObject(entity);
        }
    }

    private void writeValue(String name, Object value, JsonGenerator generator, SerializationContext context) {
        if (value == null) {
            generator.writeNull(name);
        } else {
            context.serialize(name, value, generator);
        }
    }

    private void writeLinks(Object entity, JsonGenerator generator, SerializationContext context) {
        Map<String, HalLink> links = linksExtractor.getLinks(entity);
        context.serialize("_links", links, generator);
    }
}
