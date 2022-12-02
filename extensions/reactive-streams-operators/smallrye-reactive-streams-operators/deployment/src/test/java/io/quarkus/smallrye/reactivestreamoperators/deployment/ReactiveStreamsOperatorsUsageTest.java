package io.quarkus.smallrye.reactivestreamoperators.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ReactiveStreamsOperatorsUsageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanUsingReactiveStreamsOperators.class));

    @Inject
    BeanUsingReactiveStreamsOperators bean;

    @Test
    public void test() {
        List<Integer> list = bean.verify().toCompletableFuture().join();
        assertTrue(list.size() == 2 && list.get(0) == 2 && list.get(1) == 3);
    }

    @ApplicationScoped
    static class BeanUsingReactiveStreamsOperators {

        public CompletionStage<List<Integer>> verify() {
            return ReactiveStreams.of(1, 2, 3)
                    .map(i -> i + 1) // 2, 3, 4
                    .flatMapRsPublisher(x -> ReactiveStreams.of(x, x).buildRs()) // 2,2,3,3,4,4
                    .distinct() // 2, 3, 4
                    .limit(2) // 2, 3
                    .toList()
                    .run();
        }
    }

}
