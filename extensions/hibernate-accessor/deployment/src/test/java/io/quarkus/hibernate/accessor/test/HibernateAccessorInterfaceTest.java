package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorInterfaceTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    AccessorInterface.class,
                    InterfaceImplEntity.class));

    @Test
    void readViaInterfaceGetter() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Method getter = AccessorInterface.class.getDeclaredMethod("getLabel");
        ValueReader<?> reader = factory.valueReader(getter);

        InterfaceImplEntity entity = new InterfaceImplEntity("iface-value");
        assertThat(reader.get(entity)).isEqualTo("iface-value");
    }

}
