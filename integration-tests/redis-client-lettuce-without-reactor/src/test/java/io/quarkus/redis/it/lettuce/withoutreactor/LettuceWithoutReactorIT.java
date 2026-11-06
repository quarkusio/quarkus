package io.quarkus.redis.it.lettuce.withoutreactor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
class LettuceWithoutReactorIT extends LettuceWithoutReactorTest {

    // Native only: in JVM mode the reactive API fails with a NoClassDefFoundError, not with the substituted factory
    @Test
    @Launch(value = "reactive", exitCode = 0)
    void reactiveApiIsUnavailable(LaunchResult result) {
        assertThat(result.getOutput()).contains("The Lettuce reactive API requires Project Reactor");
    }
}
