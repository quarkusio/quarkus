package io.quarkus.vertx.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VertxLoggingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
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
            LOGGER.info("Info with exception", new NoStackTraceThrowable("boom"));
            LOGGER.info("Info with parameters {0} {1}", "Quarkus", 1);
            LOGGER.info("Info with exception and param {0}", new NoStackTraceThrowable("boom"), "quarkus");
            LOGGER.info("Info with exception as last param {0}", "quarkus", new NoStackTraceThrowable("boom"));
            LOGGER.info("Info with null as failure", (Throwable) null);
            LOGGER.info("Info with null as parameter {0}", (String) null);

            // Null message -> NULL
            LOGGER.info(null);
        }

        public void trace() {
            LOGGER.trace("Should not be displayed");
        }

    }
}
