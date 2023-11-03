package io.quarkus.it.smallrye.graphql.metricresources;

import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import io.micrometer.core.instrument.Metrics;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Just a test endpoint
 */
@GraphQLApi
public class TestResource {

    public static final double SLEEP_TIME = 0.15;

    @Query
    public TestPojo foo() {
        return new TestPojo("bar");
    }

    @Query
    public TestPojo[] superMetricFoo() throws InterruptedException {
        Thread.sleep(sleepTimeInMilliseconds());
        return new TestPojo[] { foo(), foo(), foo() };
    }

    @Query
    public List<TestPojo> batchFoo(@Source List<TestPojo> testPojos) throws InterruptedException {
        Thread.sleep(sleepTimeInMilliseconds());
        return List.of(new TestPojo("bar1"), new TestPojo("bar2"), new TestPojo("bar3"));
    }

    @Query
    public Uni<List<TestPojo>> asyncBatchFoo(@Source List<TestPojo> testPojos) {
        return Uni.createFrom().item(() -> {
            try {
                Thread.sleep(sleepTimeInMilliseconds());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return List.of(new TestPojo("abar1"), new TestPojo("abar2"), new TestPojo("abar3"));
        });
    }

    // <placeholder>
    @Query
    public Uni<TestPojo[]> asyncSuperMetricFoo() throws InterruptedException {
        return Uni.createFrom().item(() -> {
            try {
                Thread.sleep(sleepTimeInMilliseconds());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return new TestPojo[] { new TestPojo("async1"), new TestPojo("async2"), new TestPojo("async3") };
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

    }

    @Mutation
    public void clearMetrics() {
        Metrics.globalRegistry.clear();
    }

    public TestRandom getRandomNumber(@Source TestPojo testPojo) throws InterruptedException {
        Thread.sleep(sleepTimeInMilliseconds());
        return new TestRandom(123);
    }

    public Uni<TestRandom> getRandomNumberAsync(@Source TestPojo testPojo) throws InterruptedException {
        return Uni.createFrom().item(() -> {
            try {
                Thread.sleep(sleepTimeInMilliseconds());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return new TestRandom(123);
        });
    }

    private long sleepTimeInMilliseconds() {
        return (long) (SLEEP_TIME * 1000);
    }
}
