package io.quarkus.hibernate.accessor.deployment;

import org.hibernate.accessor.asm.aot.AsmAotGenerator;
import org.hibernate.accessor.asm.aot.GenerationNames;

import io.quarkus.hibernate.accessor.runtime.AccessorImplFactory;

final class HibernateAccessorGenerator {

    public static final String GENERATED_INSTANTIATOR_IMPL = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorInstantiatorImpl";

    public static final String GENERATED_WRITER_IMPL = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorValueWriterImpl";
    public static final String GENERATED_READER_IMPL = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorValueReaderImpl";
    public static final String QUARKUS_HIBERNATE_ACCESSOR_FACTORY = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory";

    // Both the generator and its names are immutable, and the generator is used once per transformed
    // class, so a single shared instance is enough.
    private static final AsmAotGenerator GENERATOR = new AsmAotGenerator(new GenerationNames(
            QUARKUS_HIBERNATE_ACCESSOR_FACTORY,
            GENERATED_READER_IMPL,
            GENERATED_WRITER_IMPL,
            GENERATED_INSTANTIATOR_IMPL,
            AccessorImplFactory.class.getName()));

    private HibernateAccessorGenerator() {
    }

    static AsmAotGenerator generator() {
        return GENERATOR;
    }

}
