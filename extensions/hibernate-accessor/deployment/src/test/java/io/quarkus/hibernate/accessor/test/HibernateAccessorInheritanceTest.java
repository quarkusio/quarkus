package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorInheritanceTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(BaseEntity.class, SubEntity.class));

    @Test
    void readInheritedStringField() throws Exception {
        ValueReader<?> reader = reader(BaseEntity.class, "name");

        SubEntity entity = new SubEntity("inherited", 10, "extra");
        assertThat(reader.get(entity)).isEqualTo("inherited");
    }

    @Test
    void writeInheritedStringField() throws Exception {
        ValueReader<?> reader = reader(BaseEntity.class, "name");
        ValueWriter writer = writer(BaseEntity.class, "name");

        SubEntity entity = new SubEntity();
        writer.set(entity, "written-via-base");
        assertThat(reader.get(entity)).isEqualTo("written-via-base");
    }

    @Test
    void readInheritedIntField() throws Exception {
        ValueReader<?> reader = reader(BaseEntity.class, "value");

        SubEntity entity = new SubEntity("test", 42, "extra");
        assertThat(reader.get(entity)).isEqualTo(42);
    }

    @Test
    void writeInheritedIntField() throws Exception {
        ValueReader<?> reader = reader(BaseEntity.class, "value");
        ValueWriter writer = writer(BaseEntity.class, "value");

        SubEntity entity = new SubEntity();
        writer.set(entity, 99);
        assertThat(reader.get(entity)).isEqualTo(99);
    }

    @Test
    void readSubclassOwnField() throws Exception {
        ValueReader<?> reader = reader(SubEntity.class, "extra");

        SubEntity entity = new SubEntity("name", 0, "sub-value");
        assertThat(reader.get(entity)).isEqualTo("sub-value");
    }

    @Test
    void writeSubclassOwnField() throws Exception {
        ValueReader<?> reader = reader(SubEntity.class, "extra");
        ValueWriter writer = writer(SubEntity.class, "extra");

        SubEntity entity = new SubEntity();
        writer.set(entity, "written-sub");
        assertThat(reader.get(entity)).isEqualTo("written-sub");
    }
}
