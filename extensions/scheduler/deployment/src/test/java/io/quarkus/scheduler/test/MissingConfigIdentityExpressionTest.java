package io.quarkus.scheduler.test;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class MissingConfigIdentityExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(NoSuchElementException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(MissingConfigIdentityExpressionTest.InvalidBean.class));

    @Test
    public void test() {
    }

    static class InvalidBean {

        @Scheduled(every = "1s", identity = "{my.identity}")
        void wrong() {
        }

    }

}
