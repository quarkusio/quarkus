package io.quarkus.smallrye.reactivemessaging;

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

import io.quarkus.smallrye.reactivemessaging.blocking.beans.IncomingUsingSmallRyeBlocking;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class SmallRyeBlockingSubscriberTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProduceIn.class, IncomingUsingSmallRyeBlocking.class)
                    .addAsResource(
                            new File("src/test/resources/config/worker-config.properties"),
                            "application.properties"));

    @Inject
    IncomingUsingSmallRyeBlocking incoming;

    @Test
    public void testIncomingUsingRunOnWorkerThread() {
        await().until(() -> incoming.list().size() == 6);
        assertThat(incoming.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = incoming.threads().stream().distinct()
                .collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("executor-thread-")).isTrue();
        }
    }

    @ApplicationScoped
    public static class ProduceIn {
        @Outgoing("in")
        public Publisher<String> produce() {
            return Multi.createFrom().items("a", "b", "c", "d", "e", "f");
        }
    }

}