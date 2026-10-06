package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorAllPrimitivesTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(PrimitiveEntity.class));

    @Test
    void booleanField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "booleanValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "booleanValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo(false);

        writer.set(entity, true);
        assertThat(reader.get(entity)).isEqualTo(true);
    }

    @Test
    void byteField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "byteValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "byteValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo((byte) 0);

        writer.set(entity, (byte) 42);
        assertThat(reader.get(entity)).isEqualTo((byte) 42);
    }

    @Test
    void charField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "charValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "charValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo('\0');

        writer.set(entity, 'A');
        assertThat(reader.get(entity)).isEqualTo('A');
    }

    @Test
    void shortField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "shortValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "shortValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo((short) 0);

        writer.set(entity, (short) 1234);
        assertThat(reader.get(entity)).isEqualTo((short) 1234);
    }

    @Test
    void intField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "intValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "intValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo(0);

        writer.set(entity, Integer.MAX_VALUE);
        assertThat(reader.get(entity)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void longField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "longValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "longValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo(0L);

        writer.set(entity, Long.MAX_VALUE);
        assertThat(reader.get(entity)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void floatField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "floatValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "floatValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo(0.0f);

        writer.set(entity, 3.14f);
        assertThat(reader.get(entity)).isEqualTo(3.14f);
    }

    @Test
    void doubleField() throws Exception {
        ValueReader<?> reader = reader(PrimitiveEntity.class, "doubleValue");
        ValueWriter writer = writer(PrimitiveEntity.class, "doubleValue");

        PrimitiveEntity entity = new PrimitiveEntity();
        assertThat(reader.get(entity)).isEqualTo(0.0);

        writer.set(entity, 2.718281828);
        assertThat(reader.get(entity)).isEqualTo(2.718281828);
    }

}
