package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;
import io.quarkus.test.QuarkusExtensionTest;

class HibernateAccessorInvalidGetterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(InvalidGetterEntity.class))
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-void")
                    .hasMessageContaining("invalidGetter")
                    .hasMessageContaining(InvalidGetterEntity.class.getName()));

    @Test
    void shouldNotBeInvoked() {
        fail("Build should have failed");
    }

    public static class InvalidGetterEntity {

        @ReflectionFreeMethodAccessor
        public void invalidGetter() {
        }
    }
}
