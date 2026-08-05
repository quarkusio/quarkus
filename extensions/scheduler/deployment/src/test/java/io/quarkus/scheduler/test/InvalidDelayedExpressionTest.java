package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidDelayedExpressionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .assertException(t -> {
                assertThat(t).cause().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Invalid delayed() expression");
            })
            .withApplicationRoot((jar) -> jar
                    .addClasses(InvalidBean.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class InvalidBean {

        @Scheduled(every = "${my.every.expr:off}", delayed = "for 10 seconds")
        void wrong() {
        }

    }

}
