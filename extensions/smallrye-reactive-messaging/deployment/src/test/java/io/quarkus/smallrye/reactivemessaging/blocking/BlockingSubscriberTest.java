package io.quarkus.smallrye.reactivemessaging.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Publisher;

import io.quarkus.smallrye.reactivemessaging.blocking.beans.IncomingCustomTwoBlockingBean;
import io.quarkus.smallrye.reactivemessaging.blocking.beans.IncomingCustomUnorderedBlockingBean;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.Broadcast;

public class BlockingSubscriberTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProduceIn.class, IncomingCustomUnorderedBlockingBean.class, IncomingCustomTwoBlockingBean.class)
                    .addAsResource(
                            new File("src/test/resources/config/worker-config.properties"),
                            "application.properties"));

    @Inject
    IncomingCustomUnorderedBlockingBean incomingCustomUnorderedBlockingBean;
    @Inject
    IncomingCustomTwoBlockingBean incomingCustomTwoBlockingBean;

    @Test
    public void testIncomingBlockingCustomPoolUnordered() {
        await().until(() -> incomingCustomUnorderedBlockingBean.list().size() == 6);
        assertThat(incomingCustomUnorderedBlockingBean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = incomingCustomUnorderedBlockingBean.threads().stream().distinct()
                .collect(Collectors.toList());
        assertThat(threadNames.size()).isLessThanOrEqualTo(2);
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("my-pool-")).isTrue();
        }
        for (String name : threadNames) {
            assertThat(name.startsWith("executor-thread-")).isFalse();
        }
    }

    @Test
    public void testIncomingBlockingCustomPoolTwo() {
        await().until(() -> incomingCustomTwoBlockingBean.list().size() == 6);
        assertThat(incomingCustomTwoBlockingBean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = incomingCustomTwoBlockingBean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.size()).isLessThanOrEqualTo(5);
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("another-pool-")).isTrue();
        }
        for (String name : threadNames) {
            assertThat(name.startsWith("executor-thread-")).isFalse();
        }
    }

    @ApplicationScoped
    public static class ProduceIn {
        @Outgoing("in")
        @Broadcast(2)
        public Publisher<String> produce() {
            return Multi.createFrom().items("a", "b", "c", "d", "e", "f");
        }
    }

}
