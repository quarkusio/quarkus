package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.smallrye.mutiny.Multi;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class CreateMultiTest {

    @Test
    public void test() throws InterruptedException {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers().build();
        Template template = engine.parse("{#each}{it}{/}");
        List<String> data = Arrays.asList("foo", "foo", "alpha");
        Multi<String> multi = template.instance().data(data).createMulti();

        assertMulti(multi, "foofooalpha");
        assertMulti(multi.transform().byDroppingDuplicates(), "fooalpha");
        assertMulti(multi.transform().byTakingFirstItems(1), "foo");
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
