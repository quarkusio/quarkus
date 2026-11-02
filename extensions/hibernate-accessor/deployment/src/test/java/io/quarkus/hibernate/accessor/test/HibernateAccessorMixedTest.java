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
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated")
            .withApplicationRoot(root -> root.addClasses(MixedEntity.class));

    @Test
    void hostLookupsReturnMemberIndexes() throws Exception {
        for (String role : new String[] { "fieldReader", "methodReader", "fieldWriter", "methodWriter", "instantiator" }) {
            var lookup = MixedEntity.class.getMethod("$$__hibernateAccessor_" + role + "Index", String.class);
            assertThat(lookup.getReturnType()).isEqualTo(int.class);
            assertThat(lookup.invoke(null, "missing")).isEqualTo(-1);
        }
        assertThat((int) MixedEntity.class.getMethod("$$__hibernateAccessor_fieldReaderIndex", String.class)
                .invoke(null, "name")).isGreaterThanOrEqualTo(0);
        assertThat((int) MixedEntity.class.getMethod("$$__hibernateAccessor_methodReaderIndex", String.class)
                .invoke(null, "getDescription()Ljava/lang/String;")).isGreaterThanOrEqualTo(0);
        assertThat((int) MixedEntity.class.getMethod("$$__hibernateAccessor_fieldWriterIndex", String.class)
                .invoke(null, "name")).isGreaterThanOrEqualTo(0);
        assertThat((int) MixedEntity.class.getMethod("$$__hibernateAccessor_methodWriterIndex", String.class)
                .invoke(null, "setDescription(Ljava/lang/String;)V")).isGreaterThanOrEqualTo(0);
    }

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
