package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorMixedTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MixedEntity.class));

    @Test
    void readFieldAccessor() throws Exception {
        ValueReader<?> reader = reader(MixedEntity.class, "name");

        MixedEntity entity = new MixedEntity("field-value", "method-value");
        assertThat(reader.get(entity)).isEqualTo("field-value");
    }

    @Test
    void writeFieldAccessor() throws Exception {
        ValueReader<?> reader = reader(MixedEntity.class, "name");
        ValueWriter writer = writer(MixedEntity.class, "name");

        MixedEntity entity = new MixedEntity();
        writer.set(entity, "updated");
        assertThat(reader.get(entity)).isEqualTo("updated");
    }

    @Test
    void readMethodAccessor() throws Exception {
        ValueReader<?> reader = readerMethod(MixedEntity.class, "getDescription");

        MixedEntity entity = new MixedEntity("name", "description");
        assertThat(reader.get(entity)).isEqualTo("description");
    }

    @Test
    void writeMethodAccessor() throws Exception {
        ValueWriter writer = writerMethod(MixedEntity.class, "setDescription", String.class);

        MixedEntity entity = new MixedEntity();
        writer.set(entity, "new-description");
        assertThat(entity.getDescription()).isEqualTo("new-description");
    }
}
