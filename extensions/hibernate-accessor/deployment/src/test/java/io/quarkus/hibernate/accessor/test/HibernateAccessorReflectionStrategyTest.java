package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorReflectionStrategyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(SimpleEntity.class))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "reflection");

    @Test
    void generatedFactoryClassDoesNotExist() {
        try {
            Thread.currentThread().getContextClassLoader()
                    .loadClass("io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory");
            assertThat(true).as("Generated factory should not exist in reflection mode").isFalse();
        } catch (ClassNotFoundException expected) {
            // bytecode generation was correctly skipped
        }
    }

    @Test
    void reflectionFactoryCanReadAndWriteFields() throws Exception {
        AccessorFactory factory = AccessorFactory.reflection();

        SimpleEntity entity = new SimpleEntity();
        ValueWriter writer = factory.valueWriter(SimpleEntity.class.getDeclaredField("name"));
        writer.set(entity, "hello");

        ValueReader<?> reader = factory.valueReader(SimpleEntity.class.getDeclaredField("name"));
        assertThat(reader.get(entity)).isEqualTo("hello");
    }
}
