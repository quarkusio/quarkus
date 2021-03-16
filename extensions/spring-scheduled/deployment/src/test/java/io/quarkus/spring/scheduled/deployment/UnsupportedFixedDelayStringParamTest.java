package io.quarkus.spring.scheduled.deployment;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedFixedDelayStringParamTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalArgumentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(UnsupportedFixedDelayStringParamTest.InvalidBean.class));

    @Test
    public void test() {
        // This method should not be invoked
        Assertions.fail();
    }

    @ApplicationScoped
    static class InvalidBean {

        @Scheduled(fixedDelayString = "1000")
        void unsupportedParamFixedDelayString() {
        }
    }

}
