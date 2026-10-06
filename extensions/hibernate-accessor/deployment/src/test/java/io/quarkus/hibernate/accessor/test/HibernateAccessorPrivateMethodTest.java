package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorPrivateMethodTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(PrivateMethodEntity.class));

    @Test
    void readViaPrivateGetter() throws Exception {
        ValueReader<?> reader = readerMethod(PrivateMethodEntity.class, "getSecret");

        PrivateMethodEntity entity = new PrivateMethodEntity("hidden", 42);
        assertThat(reader.get(entity)).isEqualTo("hidden");
    }

    @Test
    void writeViaPrivateSetter() throws Exception {
        ValueReader<?> reader = readerMethod(PrivateMethodEntity.class, "getSecret");
        ValueWriter writer = writerMethod(PrivateMethodEntity.class, "setSecret", String.class);

        PrivateMethodEntity entity = new PrivateMethodEntity();
        writer.set(entity, "new-secret");
        assertThat(reader.get(entity)).isEqualTo("new-secret");
    }

}
