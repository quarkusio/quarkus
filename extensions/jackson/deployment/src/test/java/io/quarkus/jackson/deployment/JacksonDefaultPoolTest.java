package io.quarkus.jackson.deployment;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.util.JsonRecyclerPools;

public class JacksonDefaultPoolTest {

    @Test
    public void validateDefaultJacksonPool() {
        Assertions.assertThat(JsonRecyclerPools.defaultPool()).isInstanceOf(JsonRecyclerPools.LockFreePool.class);
    }
}
