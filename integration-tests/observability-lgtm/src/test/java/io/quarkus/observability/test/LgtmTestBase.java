package io.quarkus.observability.test;

import org.junit.jupiter.api.Test;

public abstract class LgtmTestBase extends LgtmTestHelper {

    @Test
    public void testTracing() {
        poke("/api");
    }

}
