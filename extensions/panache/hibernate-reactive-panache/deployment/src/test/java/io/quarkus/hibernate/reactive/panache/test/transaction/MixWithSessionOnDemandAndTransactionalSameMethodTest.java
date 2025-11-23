package io.quarkus.hibernate.reactive.panache.test.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;

public class MixWithSessionOnDemandAndTransactionalSameMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addDefaultPackage())
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(
                            "Cannot mix @Transactional and @WithSession"));

    @Test
    @RunOnVertxContext
    public void avoidMixingTransactionalAndWithSessionTest() {
        fail(); // this will never be called, extension will fail seeing the method below
    }

    @Transactional
    @WithSession
    public Uni<?> avoidMixingTransactionalAndWithSession() {
        throw new UnsupportedOperationException("this shouldn't be called");
    }
}