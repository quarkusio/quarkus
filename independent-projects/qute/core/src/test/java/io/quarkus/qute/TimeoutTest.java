package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class TimeoutTest {

    @Test
    public void testTimeout() {
        Engine engine = Engine.builder().addDefaults().timeout(100).build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{foo}").data("foo", new CompletableFuture<>())
                        // Invalid timeout is ignored
                        .setAttribute("timeout", "bar")
                        .render())
                .withMessage("Template 1 [generatedId=1] rendering timeout [100ms] occured");

        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(
                        () -> engine.parse("{foo}").data("foo", new CompletableFuture<>()).createUni().await().indefinitely())
                .withMessage("Template 2 [generatedId=2] rendering timeout [100ms] occured");

        assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(
                        () -> engine.parse("{foo}").data("foo", new CompletableFuture<>()).renderAsync().toCompletableFuture()
                                .get());

        assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(
                        () -> engine.parse("{foo}").data("foo", new CompletableFuture<>()).consume(s -> {
                        }).toCompletableFuture()
                                .get());
    }

    @Test
    public void testTimeoutAttribute() {
        Engine engine = Engine.builder().addDefaults().build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{foo}").data("foo", new CompletableFuture<>()).setAttribute("timeout", 300)
                        .render())
                .withMessage("Template 1 [generatedId=1] rendering timeout [300ms] occured");
    }

    @Test
    public void testUniNoItemOrFailure() {
        Engine engine = Engine.builder().addDefaults().timeout(100).build();
        Template template = engine.parse("{foo.toLowerCase}");
        Uni<String> fooUni = Uni.createFrom().completionStage(new CompletableFuture<>());
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> template.data("foo", fooUni).render())
                .withMessage("Template 1 [generatedId=1] rendering timeout [100ms] occured");
    }

}
