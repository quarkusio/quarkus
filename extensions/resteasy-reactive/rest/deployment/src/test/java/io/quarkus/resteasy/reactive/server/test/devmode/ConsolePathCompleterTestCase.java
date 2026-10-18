package io.quarkus.resteasy.reactive.server.test.devmode;

import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveDevModeProcessor.PathCompleter.complete;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveDevModeProcessor;

public class ConsolePathCompleterTestCase {

    @Test
    public void testPathCompletion() {
        ResteasyReactiveDevModeProcessor.CompletionResult result;

        result = complete(Set.of("/fruit", "/fruit/{foo}", "/fruit/{foo}/bar"), "/fruit/1/");
        Assertions.assertEquals(Set.of("/fruit/1/bar"), result.getResults());
        Assertions.assertTrue(result.isAppendSpace());

        result = complete(Set.of("/fruit"), "/");
        Assertions.assertTrue(result.isAppendSpace());
        Assertions.assertEquals(Set.of("/fruit"), result.getResults());

        result = complete(Set.of("/fruit", "/fruit/{foo}"), "/");
        Assertions.assertFalse(result.isAppendSpace());
        Assertions.assertEquals(Set.of("/fruit", "/fruit/"), result.getResults());

        result = complete(Set.of("/fruit", "/fruit/{foo}"), "/fruit");
        Assertions.assertFalse(result.isAppendSpace());
        Assertions.assertEquals(Set.of("/fruit/{foo}"), result.getResults());

        result = complete(Set.of("/fruit", "/fruit/{foo}"), "/fruit/1");
        Assertions.assertEquals(Set.of("/fruit/{foo}"), result.getResults());
        Assertions.assertFalse(result.isAppendSpace());
    }
}
