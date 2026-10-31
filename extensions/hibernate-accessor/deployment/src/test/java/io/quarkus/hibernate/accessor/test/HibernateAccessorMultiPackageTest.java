package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.accessor.test.other.OtherPackageEntity;
import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorMultiPackageTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(SimpleEntity.class, OtherPackageEntity.class));

    @Test
    void readFieldFromSecondPackage() throws Exception {
        ValueReader<?> reader = reader(OtherPackageEntity.class, "label");

        OtherPackageEntity entity = new OtherPackageEntity("second-pkg", 100L);
        assertThat(reader.get(entity)).isEqualTo("second-pkg");
    }

    @Test
    void writeFieldFromSecondPackage() throws Exception {
        ValueReader<?> reader = reader(OtherPackageEntity.class, "label");
        ValueWriter writer = writer(OtherPackageEntity.class, "label");

        OtherPackageEntity entity = new OtherPackageEntity();
        writer.set(entity, "updated-second");
        assertThat(reader.get(entity)).isEqualTo("updated-second");
    }

    @Test
    void readPrimitiveFromSecondPackage() throws Exception {
        ValueReader<?> reader = reader(OtherPackageEntity.class, "count");

        OtherPackageEntity entity = new OtherPackageEntity("test", 999L);
        assertThat(reader.get(entity)).isEqualTo(999L);
    }
}
