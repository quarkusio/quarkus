package io.quarkus.vertx.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.VertxException;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class VertxLoggingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanThatLog.class));

    @Inject
    BeanThatLog bean;

    @Test
    public void test() {
        bean.info();

        bean.trace();
    }

    @ApplicationScoped
    static class BeanThatLog {

        private final static Logger LOGGER = LoggerFactory.getLogger("verbose-bean");

        public void info() {
            LOGGER.info("Info");
            LOGGER.info("Info with exception", new VertxException("boom"));
            LOGGER.info("Info");
            LOGGER.info("Info with null as failure", (Throwable) null);

            // Null message -> NULL
            LOGGER.info(null);
        }

        public void trace() {
            LOGGER.trace("Should not be displayed");
        }

    }
}
