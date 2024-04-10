package io.quarkus.cache.infinispan.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.schema.Schema;
import org.infinispan.protostream.schema.Type;

/**
 * Produces the schema marshaller and protoschema to marshall {@link io.quarkus.cache.CompositeCacheKey}
 */
@ApplicationScoped
public class CompositeKeyMarshallerBean {

    @Produces
    public Schema compositeKeySchema() {
        return new Schema.Builder("io.quarkus.cache.infinispan.internal.cache.proto")
                .packageName(CompositeCacheKeyMarshaller.PACKAGE)
                .addImport("org/infinispan/protostream/message-wrapping.proto")
                .addMessage(CompositeCacheKeyMarshaller.NAME)
                .addRepeatedField(Type.create("org.infinispan.protostream.WrappedMessage"), CompositeCacheKeyMarshaller.KEYS, 1)
                .build();
    }

    @Produces
    public MessageMarshaller compositeKeyMarshaller() {
        return new CompositeCacheKeyMarshaller();
    }
}
