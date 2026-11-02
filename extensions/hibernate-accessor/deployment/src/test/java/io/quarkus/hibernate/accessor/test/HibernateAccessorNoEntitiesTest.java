package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.AccessorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorNoEntitiesTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated")
            .withEmptyApplication();

    @Test
    void extensionBootsWithNoAnnotatedClasses() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        assertThat(factory).isNotNull();
    }
}
