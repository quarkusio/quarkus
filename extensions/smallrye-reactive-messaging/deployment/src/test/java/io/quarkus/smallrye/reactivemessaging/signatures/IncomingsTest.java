package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Publisher;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class IncomingsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProducerOnA.class, ProducerOnB.class, MyBeanUsingMultipleIncomings.class));

    @Inject
    MyBeanUsingMultipleIncomings bean;

    @Test
    public void testIncomingsWithTwoSources() {
        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).containsSubsequence("a", "b", "c");
        assertThat(bean.list()).containsSubsequence("d", "e", "f");
    }

    @ApplicationScoped
    public static class ProducerOnA {

        @Outgoing("a")
        public Publisher<String> produce() {
            return Multi.createFrom().items("a", "b", "c");
        }

    }

    @ApplicationScoped
    public static class ProducerOnB {

        @Outgoing("b")
        public Publisher<String> produce() {
            return Multi.createFrom().items("d", "e", "f");
        }

    }

    @ApplicationScoped
    public static class MyBeanUsingMultipleIncomings {

        private final List<String> list = new CopyOnWriteArrayList<>();

        @Incoming("a")
        @Incoming("b")
        public void consume(String s) {
            list.add(s);
        }

        public List<String> list() {
            return list;
        }

    }
}
