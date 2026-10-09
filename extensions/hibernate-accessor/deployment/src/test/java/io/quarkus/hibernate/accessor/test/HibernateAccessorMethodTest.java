package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorMethodTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MethodEntity.class));

    @Test
    void readViaGetter() throws Exception {
        ValueReader<?> reader = readerMethod(MethodEntity.class, "getValue");

        MethodEntity entity = new MethodEntity("hello", 10);
        assertThat(reader.get(entity)).isEqualTo("hello");
    }

    @Test
    void writeViaSetter() throws Exception {
        Method setter = MethodEntity.class.getDeclaredMethod("setValue", String.class);
        ValueWriter writer = writerMethod(MethodEntity.class, "setValue", String.class);

        MethodEntity entity = new MethodEntity();
        writer.set(entity, "written");
        assertThat(entity.getValue()).isEqualTo("written");
    }

    @Test
    void writeViaOverloadedSetters() throws Exception {
        MethodEntity entity = new MethodEntity();
        writerMethod(MethodEntity.class, "setValue", String.class).set(entity, "written");
        writerMethod(MethodEntity.class, "setValue", Integer.class).set(entity, 42);
        assertThat(entity.getValue()).isEqualTo("written");
        assertThat(entity.getCount()).isEqualTo(42);
    }

    @Test
    void readPrimitiveViaGetter() throws Exception {
        ValueReader<?> reader = readerMethod(MethodEntity.class, "getCount");

        MethodEntity entity = new MethodEntity("test", 42);
        assertThat(reader.get(entity)).isEqualTo(42);
    }

    @Test
    void writePrimitiveViaSetter() throws Exception {
        ValueWriter writer = writerMethod(MethodEntity.class, "setCount", int.class);

        MethodEntity entity = new MethodEntity();
        writer.set(entity, 99);
        assertThat(entity.getCount()).isEqualTo(99);
    }

    @Test
    void writeNullViaSetter() throws Exception {
        ValueWriter writer = writerMethod(MethodEntity.class, "setValue", String.class);
        ValueReader<?> reader = readerMethod(MethodEntity.class, "getValue");

        MethodEntity entity = new MethodEntity("initial", 0);
        writer.set(entity, null);
        assertThat(reader.get(entity)).isNull();
    }

}
