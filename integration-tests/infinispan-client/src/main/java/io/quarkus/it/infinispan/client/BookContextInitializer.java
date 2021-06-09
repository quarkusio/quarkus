package io.quarkus.it.infinispan.client;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.types.java.math.BigDecimalAdapter;

@AutoProtoSchemaBuilder(includeClasses = { Book.class, Type.class, Author.class,
        BigDecimalAdapter.class }, schemaPackageName = "book_sample")
interface BookContextInitializer extends SerializationContextInitializer {
}
