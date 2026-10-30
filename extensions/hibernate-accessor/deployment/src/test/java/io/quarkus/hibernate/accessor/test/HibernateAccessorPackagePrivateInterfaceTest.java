package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorPackagePrivateInterfaceTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    PackagePrivateAccessorInterface.class,
                    PackagePrivateInterfaceImplEntity.class));

    @Test
    void readViaPackagePrivateInterfaceGetter() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Method getter = PackagePrivateAccessorInterface.class.getDeclaredMethod("getLabel");
        ValueReader<?> reader = factory.valueReader(getter);

        PackagePrivateInterfaceImplEntity entity = new PackagePrivateInterfaceImplEntity("iface-value");
        assertThat(reader.get(entity)).isEqualTo("iface-value");
    }
}
