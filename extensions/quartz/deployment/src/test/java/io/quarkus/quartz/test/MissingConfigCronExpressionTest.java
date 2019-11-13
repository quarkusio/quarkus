package io.quarkus.quartz.test;

import java.util.NoSuchElementException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class MissingConfigCronExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(NoSuchElementException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MissingConfigCronExpressionTest.InvalidBean.class));

    @Test
    public void test() {
    }

    static class InvalidBean {

        @Scheduled(cron = "{my.cron}")
        void wrong() {
        }

    }

}
