package io.quarkus.quartz.test;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class NoExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InvalidBean.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class InvalidBean {

        @Scheduled
        void wrong() {
        }

    }

}
