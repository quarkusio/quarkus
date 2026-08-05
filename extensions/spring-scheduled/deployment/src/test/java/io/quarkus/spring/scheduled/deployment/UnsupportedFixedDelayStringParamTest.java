package io.quarkus.spring.scheduled.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedFixedDelayStringParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar
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
