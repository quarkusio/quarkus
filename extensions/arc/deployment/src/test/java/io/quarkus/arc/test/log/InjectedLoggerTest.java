package io.quarkus.arc.test.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.log.LoggerName;
import io.quarkus.test.QuarkusUnitTest;

public class InjectedLoggerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class));

    @Inject
    SimpleBean simpleBean;

    @Inject
    AnotherSimpleBean anotherSimpleBean;

    @Test
    public void testInjectedLogger() {
        assertEquals(SimpleBean.class.getName(), simpleBean.getLog().getName());
        assertEquals("shared", simpleBean.getSharedLog().getName());
        assertEquals(AnotherSimpleBean.class.getName(), anotherSimpleBean.getLog().getName());
        assertEquals(simpleBean.getSharedLog(), anotherSimpleBean.getSharedLog());
    }

    @ApplicationScoped
    static class SimpleBean {

        @Inject
        Logger log;

        @LoggerName("shared")
        Logger sharedLog;

        public Logger getLog() {
            log.info("Someone is here!");
            return log;
        }

        public Logger getSharedLog() {
            return sharedLog;
        }

    }

    @Dependent
    static class AnotherSimpleBean {

        private final Logger log;

        @LoggerName("shared")
        Logger sharedLog;

        public AnotherSimpleBean(Logger log) {
            this.log = log;
        }

        public Logger getLog() {
            return log;
        }

        public Logger getSharedLog() {
            sharedLog.info("Yet another someone is here!");
            return sharedLog;
        }

    }

}
