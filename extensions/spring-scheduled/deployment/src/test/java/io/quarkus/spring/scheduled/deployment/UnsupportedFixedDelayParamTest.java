package io.quarkus.spring.scheduled.deployment;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedFixedDelayParamTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnsupportedFixedDelayParamTest.InvalidBean.class));

    @Test
    public void test() {
        // This method should not be invoked
        Assertions.fail();
    }

    @ApplicationScoped
    static class InvalidBean {

        @Scheduled(fixedDelay = 1000)
        void unsupportedParamFixedDelay() {
        }
    }

}
