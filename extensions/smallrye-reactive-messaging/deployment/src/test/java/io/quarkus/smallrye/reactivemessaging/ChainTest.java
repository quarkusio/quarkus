package io.quarkus.smallrye.reactivemessaging;

import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.hotreload.SomeProcessor;
import io.quarkus.smallrye.reactivemessaging.hotreload.SomeSource;
import io.quarkus.test.QuarkusUnitTest;

public class ChainTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SomeSource.class, MySink.class, SomeProcessor.class));

    @Inject
    MySink sink;

    @Test
    public void testSourceToProcessorToSink() {
        await().until(() -> sink.items().size() > 5);
    }

    @ApplicationScoped
    static class MySink {
        private List<String> items = new CopyOnWriteArrayList<>();

        @Incoming("my-sink")
        public void sink(String l) {
            items.add(l);
        }

        public List<String> items() {
            return items;
        }
    }

}
