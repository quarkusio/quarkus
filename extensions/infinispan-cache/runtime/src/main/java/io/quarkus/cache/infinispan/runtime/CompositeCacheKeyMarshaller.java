package io.quarkus.cache.infinispan.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.WrappedMessage;

import io.quarkus.cache.CompositeCacheKey;

/**
 * {@link CompositeCacheKey } protostream marshaller class
 */
public class CompositeCacheKeyMarshaller implements MessageMarshaller<CompositeCacheKey> {
    public static final String PACKAGE = "io.quarkus.cache.infinispan.internal";
    public static final String NAME = "CompositeCacheKey";
    public static final String FULL_NAME = PACKAGE + "." + NAME;
    public static final String KEYS = "keys";

    @Override
    public CompositeCacheKey readFrom(ProtoStreamReader reader) throws IOException {
        Object[] compositeKeys = reader.readCollection(KEYS, new ArrayList<>(), WrappedMessage.class).stream()
                .map(we -> we.getValue()).collect(Collectors.toList()).toArray();
        return new CompositeCacheKey(compositeKeys);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, CompositeCacheKey compositeCacheKey) throws IOException {
        List<WrappedMessage> wrappedMessages = Arrays.stream(compositeCacheKey.getKeyElements())
                .map(e -> new WrappedMessage(e))
                .collect(Collectors.toList());
        writer.writeCollection(KEYS, wrappedMessages, WrappedMessage.class);
    }

    @Override
    public Class<? extends CompositeCacheKey> getJavaClass() {
        return CompositeCacheKey.class;
    }

    @Override
    public String getTypeName() {
        return FULL_NAME;
    }
}
