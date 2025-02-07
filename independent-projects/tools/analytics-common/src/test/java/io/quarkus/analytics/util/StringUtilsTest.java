package io.quarkus.analytics.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void hashSHA256() {
        assertEquals("GshzUynU9pmVBB/NRIxbbQKjlFEauyo619PRnhAI7zM=",
                StringUtils.hashSHA256("Something/12@"));
    }

    @Test
    void hashSHA256NA() {
        assertEquals("4veeW2AzC7pMKJliIxtropV9CxTn3rMRBBcAPHnepjU=",
                StringUtils.hashSHA256("N/A"));
    }

    @Test
    void hashSHA256Null() {
        assertEquals("4veeW2AzC7pMKJliIxtropV9CxTn3rMRBBcAPHnepjU=",
                StringUtils.hashSHA256(null));
    }
}
