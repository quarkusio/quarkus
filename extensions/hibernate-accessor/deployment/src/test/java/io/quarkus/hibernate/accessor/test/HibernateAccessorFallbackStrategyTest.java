package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorFallbackStrategyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(SimpleEntity.class, UnregisteredEntity.class))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "reflection-free-with-fallback");

    @Test
    void generatedFactoryHasCreateMethodWithFallback() throws Exception {
        Class<?> factoryClass = Thread.currentThread().getContextClassLoader()
                .loadClass("io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory");
        assertThat(factoryClass.getMethod("create", AccessorFactory.class)).isNotNull();
    }

    @Test
    void registeredEntityFieldAccessWorks() throws Exception {
        AccessorFactory factory = loadFallbackFactory();

        SimpleEntity entity = new SimpleEntity();
        ValueWriter writer = factory.valueWriter(SimpleEntity.class.getDeclaredField("name"));
        writer.set(entity, "test");

        ValueReader<?> reader = factory.valueReader(SimpleEntity.class.getDeclaredField("name"));
        assertThat(reader.get(entity)).isEqualTo("test");
    }

    @Test
    void unregisteredEntityFallsBackToReflection() throws Exception {
        AccessorFactory factory = loadFallbackFactory();

        UnregisteredEntity entity = new UnregisteredEntity();
        ValueWriter writer = factory
                .valueWriter(UnregisteredEntity.class.getDeclaredField("value"));
        writer.set(entity, "fallback-value");

        ValueReader<?> reader = factory
                .valueReader(UnregisteredEntity.class.getDeclaredField("value"));
        assertThat(reader.get(entity)).isEqualTo("fallback-value");
    }

    private AccessorFactory loadFallbackFactory() throws Exception {
        Class<?> factoryClass = Thread.currentThread().getContextClassLoader()
                .loadClass("io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory");
        return (AccessorFactory) factoryClass
                .getMethod("create", AccessorFactory.class)
                .invoke(null, AccessorFactory.reflection());
    }
}
