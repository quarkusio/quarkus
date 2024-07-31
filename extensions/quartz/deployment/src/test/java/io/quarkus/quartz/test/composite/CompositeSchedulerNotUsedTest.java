package io.quarkus.quartz.test.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class CompositeSchedulerNotUsedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Jobs.class))
            .assertException(t -> {
                assertThat(t).cause().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(
                                "The required scheduler implementation is not available because the composite scheduler is not used: SIMPLE");
            });

    @Test
    public void test() {
        fail();
    }

    static class Jobs {

        @Scheduled(every = "1s", executeWith = Scheduled.SIMPLE)
        void quartz() {
        }

    }
}
