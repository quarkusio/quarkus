package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HashTest {

    @Test
    public void testDeterministicHashing() {
        // a simple test to verify that repeatedly creating hash from the same String is deterministic
        String someString = "test123FooBar";
        String hash1 = Hashes.sha1_base64(someString);
        String hash2 = Hashes.sha1_base64(someString);
        assertEquals(hash1, hash2);
    }
}
