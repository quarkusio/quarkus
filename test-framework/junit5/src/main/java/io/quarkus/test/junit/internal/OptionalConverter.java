package io.quarkus.test.junit.internal;

import java.util.Optional;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class OptionalConverter implements Converter {
    private final Mapper mapper;

    public OptionalConverter(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        final Optional<?> optional = (Optional<?>) source;

        if (optional.isPresent()) {
            Object item = optional.get();
            String name = mapper.serializedClass(item.getClass());

            writer.startNode(name);
            context.convertAnother(item);
            writer.endNode();
        } else {
            final String name = mapper.serializedClass(null);
            writer.startNode(name);
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        final Class<?> type = HierarchicalStreams.readClassType(reader, mapper);
        Object item = context.convertAnother(null, type);
        reader.moveUp();

        return Optional.ofNullable(item);
    }

    @Override
    public boolean canConvert(Class type) {
        return type == Optional.class;
    }
}
