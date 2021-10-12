package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class MutinyTest {

    @Test
    public void testCreateMulti() throws InterruptedException {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#each}{it}{/}");
        List<String> data = Arrays.asList("foo", "foo", "alpha");
        Multi<String> multi = template.data(data).createMulti();

        assertMulti(multi, "foofooalpha");
        assertMulti(multi.select().distinct(), "fooalpha");
        assertMulti(multi.select().first(), "foo");
    }

    @Test
    public void testCreateUni() throws InterruptedException {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#each}{it}{/}");
        List<Object> data = new ArrayList<>(Arrays.asList("foo", "foo", "alpha"));
        Uni<String> uni = template.data(data).createUni();

        BlockingQueue<Object> synchronizer = new LinkedBlockingQueue<>();

        uni.subscribe().with(synchronizer::add);
        assertEquals("foofooalpha", synchronizer.poll(2, TimeUnit.SECONDS));

        data.remove(0);
        uni.subscribe().with(synchronizer::add);
        assertEquals("fooalpha", synchronizer.poll(2, TimeUnit.SECONDS));

        data.add(new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("foo");
            }

        });
        uni.subscribe().with(synchronizer::add, synchronizer::add);
        assertEquals(IllegalStateException.class, synchronizer.poll(2, TimeUnit.SECONDS).getClass());
    }

    @Test
    public void testUniResolution() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Template template = engine.parse("{foo.toLowerCase}::{#each items}{it_count}.{it}{#if it_hasNext},{/if}{/each}");
        Uni<Object> fooUni = Uni.createFrom().item("FOO");
        Uni<List<String>> itemsUni = Uni.createFrom().item(() -> Arrays.asList("foo", "bar", "baz"));
        assertEquals("foo::1.foo,2.bar,3.baz", template.data("foo", fooUni, "items", itemsUni).render());
    }

    private void assertMulti(Multi<String> multi, String expected) throws InterruptedException {
        StringBuilder builder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        multi
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
