package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorDifferentHostTest extends HibernateAccessorBaseTest {

    /*
     * Make sure that nested classes with same name but different hosts do not clash.
     */
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    HostOneEntity.class,
                    HostOneEntity.Nested.class,
                    HostTwoEntity.class,
                    HostTwoEntity.Nested.class));

    @Test
    void readHostOneNestedValue() throws Exception {
        ValueReader<?> reader = reader(HostOneEntity.Nested.class, "value");

        HostOneEntity.Nested entity = new HostOneEntity.Nested("host-one", 1);
        assertThat(reader.get(entity)).isEqualTo("host-one");
    }

    @Test
    void writeHostOneNestedValue() throws Exception {
        ValueReader<?> reader = reader(HostOneEntity.Nested.class, "value");
        ValueWriter writer = writer(HostOneEntity.Nested.class, "value");

        HostOneEntity.Nested entity = new HostOneEntity.Nested();
        writer.set(entity, "written-one");
        assertThat(reader.get(entity)).isEqualTo("written-one");
    }

    @Test
    void readHostTwoNestedValue() throws Exception {
        ValueReader<?> reader = reader(HostTwoEntity.Nested.class, "value");

        HostTwoEntity.Nested entity = new HostTwoEntity.Nested("host-two", 2);
        assertThat(reader.get(entity)).isEqualTo("host-two");
    }

    @Test
    void writeHostTwoNestedValue() throws Exception {
        ValueReader<?> reader = reader(HostTwoEntity.Nested.class, "value");
        ValueWriter writer = writer(HostTwoEntity.Nested.class, "value");

        HostTwoEntity.Nested entity = new HostTwoEntity.Nested();
        writer.set(entity, "written-two");
        assertThat(reader.get(entity)).isEqualTo("written-two");
    }

    @Test
    void readHostOneNestedNumber() throws Exception {
        ValueReader<?> reader = reader(HostOneEntity.Nested.class, "number");

        HostOneEntity.Nested entity = new HostOneEntity.Nested("test", 42);
        assertThat(reader.get(entity)).isEqualTo(42);
    }

    @Test
    void readHostTwoNestedNumber() throws Exception {
        ValueReader<?> reader = reader(HostTwoEntity.Nested.class, "number");

        HostTwoEntity.Nested entity = new HostTwoEntity.Nested("test", 99);
        assertThat(reader.get(entity)).isEqualTo(99);
    }
}
