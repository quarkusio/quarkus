package io.quarkus.smallrye.reactivemessaging.blocking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.blocking.beans.BeanReturningMessages;
import io.quarkus.smallrye.reactivemessaging.blocking.beans.BeanReturningPayloads;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.ChannelRegistry;

public class BlockingPublisherTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanReturningPayloads.class, BeanReturningMessages.class));

    @Inject
    BeanReturningPayloads beanReturningPayloads;
    @Inject
    BeanReturningMessages beanReturningMessages;
    @Inject
    ChannelRegistry channelRegistry;

    @Test
    public void testBlockingWhenProducingPayload() {
        List<PublisherBuilder<? extends Message<?>>> producer = channelRegistry.getPublishers("infinite-producer-payload");
        assertThat(producer).isNotEmpty();
        List<Integer> list = producer.get(0).map(Message::getPayload)
                .limit(5)
                .map(i -> (Integer) i)
                .toList().run().toCompletableFuture().join();
        assertThat(list).containsExactly(1, 2, 3, 4, 5);

        List<String> threadNames = beanReturningPayloads.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @Test
    public void testBlockingWhenProducingMessages() {
        List<PublisherBuilder<? extends Message<?>>> producer = channelRegistry.getPublishers("infinite-producer-msg");
        assertThat(producer).isNotEmpty();
        List<Integer> list = producer.get(0).map(Message::getPayload)
                .limit(5)
                .map(i -> (Integer) i)
                .toList().run().toCompletableFuture().join();
        assertThat(list).containsExactly(1, 2, 3, 4, 5);

        List<String> threadNames = beanReturningMessages.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }
}
