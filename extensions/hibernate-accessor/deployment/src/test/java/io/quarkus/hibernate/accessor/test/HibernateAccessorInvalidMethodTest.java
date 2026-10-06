package io.quarkus.hibernate.accessor.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorInvalidMethodTest {

    /*
     * Setter should have exactly one parameter
     */
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(InvalidMethodEntity.class))
            .setExpectedException(UnsupportedOperationException.class);

    @Test
    void shouldNotBeInvoked() {
        fail("Build should have failed");
    }
}
