package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidTimeZoneTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .assertException(t -> {
                assertThat(t).cause().isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid timeZone()");
            })
            .withApplicationRoot((jar) -> jar
                    .addClasses(InvalidBean.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class InvalidBean {

        @Scheduled(cron = "0/1 * * * * ?", timeZone = "fooZoneId!")
        void wrong() {
        }

    }

}
