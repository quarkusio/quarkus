package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidEveryExpressionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .assertException(t -> {
                assertThat(t).cause().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Invalid every() expression");
            })
            .withApplicationRoot((jar) -> jar
                    .addClasses(InvalidEveryExpressionTest.InvalidBean.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class InvalidBean {

        @Scheduled(every = "call me every other day")
        void wrong() {
        }

    }

}
