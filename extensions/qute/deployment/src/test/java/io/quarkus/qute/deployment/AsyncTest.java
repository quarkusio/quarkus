package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class AsyncTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Foo.class)
                    .addAsResource(new StringAsset("{foo.val} is not {foo.val.setScale(2,roundingMode)}"),
                            "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testAsyncRendering() {
        CompletionStage<String> async = foo.data("roundingMode", RoundingMode.HALF_UP)
                .data("foo", new Foo(new BigDecimal("123.4563"))).renderAsync();
        assertEquals("123.4563 is not 123.46", async.toCompletableFuture().join());
    }

    @Test
    public void testAsyncRenderingAsUni() {
        Uni<String> uni = Uni.createFrom().completionStage(() -> foo.data("roundingMode", RoundingMode.HALF_UP)
                .data("foo", new Foo(new BigDecimal("123.4563"))).renderAsync());
        assertEquals("123.4563 is not 123.46", uni.await().indefinitely());
    }

    @TemplateData
    @TemplateData(target = BigDecimal.class)
    public static class Foo {

        public final BigDecimal val;

        public Foo(BigDecimal val) {
            this.val = val;
        }

    }
}
