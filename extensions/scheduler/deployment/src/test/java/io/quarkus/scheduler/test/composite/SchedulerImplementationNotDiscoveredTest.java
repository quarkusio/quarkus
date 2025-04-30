package io.quarkus.scheduler.test.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class SchedulerImplementationNotDiscoveredTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Jobs.class))
            .assertException(t -> {
                assertThat(t).cause().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(
                                "The required scheduler implementation was not discovered in application: QUARTZ");
            });

    @Test
    public void test() {
        fail();
    }

    static class Jobs {

        @Scheduled(every = "1s", executeWith = Scheduled.QUARTZ)
        void quartz() {
        }

    }
}
