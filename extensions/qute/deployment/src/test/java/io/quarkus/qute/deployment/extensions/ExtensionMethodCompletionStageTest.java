package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.deployment.Foo;
import io.quarkus.test.QuarkusUnitTest;

public class ExtensionMethodCompletionStageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Foo.class, Extensions.class, FooParentService.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateExtensions() throws InterruptedException, ExecutionException {
        CompletableFuture<String> result = engine.parse("{foo.parent.name}").data("foo", new Foo("alpha", 10l)).renderAsync()
                .toCompletableFuture();
        assertFalse(result.isDone());
        FooParentService.parent.complete(new Foo("bravo", 1l));
        assertEquals("bravo", result.toCompletableFuture().get());
    }

    @Singleton
    public static class FooParentService {

        static final CompletableFuture<Foo> parent = new CompletableFuture<Foo>();

        CompletableFuture<Foo> getParent(Foo foo) {
            return parent;
        }

    }

    public static class Extensions {

        @TemplateExtension(matchName = "parent")
        static CompletableFuture<Foo> fooParentAsync(Foo foo) {
            return CDI.current().select(FooParentService.class).get().getParent(foo);
        }

    }

}
