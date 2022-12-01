package io.quarkus.smallrye.reactivemessaging.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.blocking.beans.BeanReturningMessagesUsingSmallRyeBlocking;
import io.quarkus.smallrye.reactivemessaging.blocking.beans.BeanReturningPayloadsUsingSmallRyeBlocking;
import io.quarkus.smallrye.reactivemessaging.blocking.beans.InfiniteSubscriber;
import io.quarkus.test.QuarkusUnitTest;

public class SmallRyeBlockingPublisherTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanReturningPayloadsUsingSmallRyeBlocking.class,
                            BeanReturningMessagesUsingSmallRyeBlocking.class, InfiniteSubscriber.class));

    @Inject
    BeanReturningPayloadsUsingSmallRyeBlocking beanReturningPayloads;
    @Inject
    BeanReturningMessagesUsingSmallRyeBlocking beanReturningMessages;
    @Inject
    InfiniteSubscriber subscriber;

    @Test
    public void testBlockingWhenProducingPayload() {
        await().untilAsserted(() -> assertThat(subscriber.payloads()).containsExactly(1, 2, 3, 4));
        List<String> threadNames = beanReturningPayloads.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("executor-thread-")).isTrue();
        }
    }

    @Test
    public void testBlockingWhenProducingMessages() {
        await().untilAsserted(() -> assertThat(subscriber.messages()).containsExactly(1, 2, 3, 4));
        List<String> threadNames = beanReturningMessages.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("executor-thread-")).isTrue();
        }
    }
}
