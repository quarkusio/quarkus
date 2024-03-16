package io.quarkus.it.infinispan.client;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.types.java.math.BigDecimalAdapter;

@ProtoSchema(includeClasses = { Book.class, Type.class, Author.class,
        BigDecimalAdapter.class }, schemaPackageName = "book_sample")
interface BookStoreSchema extends GeneratedSchema {
}
