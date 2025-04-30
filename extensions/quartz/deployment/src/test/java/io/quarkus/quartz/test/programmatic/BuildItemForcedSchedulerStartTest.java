package io.quarkus.quartz.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.deployment.ForceStartSchedulerBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class BuildItemForcedSchedulerStartTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ForceStartSchedulerBuildItem());
                    }
                }).produces(ForceStartSchedulerBuildItem.class).build();
            }
        };
    }

    @Inject
    Scheduler scheduler;

    static final CountDownLatch SYNC_LATCH = new CountDownLatch(1);

    @Test
    public void testScheduler() throws InterruptedException {
        assertTrue(scheduler.isRunning());

        scheduler.newJob("foo")
                .setInterval("1s")
                .setTask(ec -> SYNC_LATCH.countDown())
                .schedule();

        assertTrue(SYNC_LATCH.await(5, TimeUnit.SECONDS));
    }
}
