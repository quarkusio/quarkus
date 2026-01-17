package io.quarkus.hibernate.reactive.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class TransactionalMultiNotSupportedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MultiBean.class)
                    .addClasses(TransactionalInterceptorRequired.class))
            .assertException(t -> {
                assertThat(t)
                        .isInstanceOf(DeploymentException.class)
                        .hasMessageContaining("@Transactional methods cannot return Multi")
                        .hasMessageContaining("must return Uni instead");
            });

    @Test
    public void testTransactionalWithMulti() {
        // Should not be reached - the build should fail
        fail("Build should have failed");
    }

    @ApplicationScoped
    public static class MultiBean {

        @Transactional
        public Multi<String> multiMethod() {
            return Multi.createFrom().items("item1", "item2");
        }
    }
}
