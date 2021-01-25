package io.quarkus.it.infinispan.client;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Book.class, Type.class, Author.class,
        BigDecimalAdapter.class }, schemaPackageName = "book_sample")
interface BookContextInitializer extends SerializationContextInitializer {
}
