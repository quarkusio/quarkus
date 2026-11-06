package io.quarkus.redis.it.lettuce.withoutreactor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
class LettuceWithoutReactorTest {

    @Test
    @Launch(value = {}, exitCode = 0)
    void commands(LaunchResult result) {
        assertThat(result.getOutput()).contains("Lettuce works without Reactor");
    }
}
