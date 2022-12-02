package io.quarkus.test.junit.internal;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * A custom Map.Entry converter that always uses AbstractMap.SimpleEntry for unmarshalling.
 * This is probably not semantically correct 100% of the time, but it's likely fine
 * for all the cases where we are using marshalling / unmarshalling.
 *
 * The reason for doing this is to avoid XStream causing illegal access issues
 * for internal JDK types
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CustomMapEntryConverter extends MapConverter {

    private final Set<String> SUPPORTED_CLASS_NAMES = Set
            .of(Map.entry(Integer.MAX_VALUE, Integer.MAX_VALUE).getClass().getName());

    public CustomMapEntryConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return (type != null) && SUPPORTED_CLASS_NAMES.contains(type.getName());
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        var entryName = mapper().serializedClass(Map.Entry.class);
        var entry = (Map.Entry) source;
        writer.startNode(entryName);
        writeCompleteItem(entry.getKey(), context, writer);
        writeCompleteItem(entry.getValue(), context, writer);
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        var key = readCompleteItem(reader, context, null);
        var value = readCompleteItem(reader, context, null);
        reader.moveUp();
        return new AbstractMap.SimpleEntry(key, value);
    }
}
