package io.quarkus.spring.scheduled.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidCronExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InvalidBean.class));

    @Test
    public void test() throws InterruptedException {
        fail();
    }

    @ApplicationScoped
    static class InvalidBean {

        @Scheduled(cron = "0 0 0")
        void wrong() {
        }

    }

}
