package com.github.mkouba.qute.mutiny;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.smallrye.mutiny.Multi;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

public class SimplePublisherTest {

    @Test
    public void test() throws InterruptedException {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers().build();
        Template template = engine.parse("{#each}{it}{/}");
        List<String> data = Arrays.asList("foo", "foo", "alpha");
        Publisher<String> publisher = template.instance().data(data).publisher();

        assertPublisher(() -> Multi.createFrom().publisher(publisher), "foofooalpha");
        assertPublisher(() -> Multi.createFrom().publisher(publisher)
                .transform().byDroppingDuplicates(), "fooalpha");
        assertPublisher(() -> Multi.createFrom().publisher(publisher)
                .transform().byTakingFirstItems(1), "foo");
    }

    private void assertPublisher(Supplier<Publisher<String>> test, String expected) throws InterruptedException {
        StringBuilder builder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        Multi.createFrom().publisher(test.get())
                .subscribe().with(
                        builder::append,
                        latch::countDown);

        if (latch.await(2, TimeUnit.SECONDS)) {
            assertEquals(expected, builder.toString());
        } else {
            fail();
        }
    }

}