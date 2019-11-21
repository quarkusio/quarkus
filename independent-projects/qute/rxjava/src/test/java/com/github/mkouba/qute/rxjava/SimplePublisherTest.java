package com.github.mkouba.qute.rxjava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

public class SimplePublisherTest {

    @Test
    public void test() throws InterruptedException {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers().build();
        Template template = engine.parse("{#each}{it}{/}");
        List<String> data = Arrays.asList("foo", "foo", "alpha");
        Publisher<String> publisher = template.instance().data(data).publisher();

        assertPublisher((sb, l) -> ReactiveStreams.fromPublisher(publisher).forEach(sb::append).run()
                .whenComplete((r, t) -> l.countDown()), "foofooalpha");

        assertPublisher((sb, l) -> ReactiveStreams.fromPublisher(publisher).distinct().forEach(sb::append).run()
                .whenComplete((r, t) -> l.countDown()), "fooalpha");
    }

    private void assertPublisher(BiConsumer<StringBuilder, CountDownLatch> test, String expected) throws InterruptedException {
        StringBuilder builder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        test.accept(builder, latch);
        if (latch.await(2, TimeUnit.SECONDS)) {
            assertEquals(expected, builder.toString());
        } else {
            fail();
        }
    }

}
