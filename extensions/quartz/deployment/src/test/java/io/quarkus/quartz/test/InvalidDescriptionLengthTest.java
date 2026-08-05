package io.quarkus.quartz.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidDescriptionLengthTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .assertException(t -> {
                assertThat(t).cause().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(
                                "description() must not exceed " + Scheduled.DESCRIPTION_MAX_LENGTH + " characters");
            })
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Test
    public void test() {
    }

    static class Jobs {

        @Scheduled(every = "1s", description = "A" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        void ping() {
        }
    }

}
