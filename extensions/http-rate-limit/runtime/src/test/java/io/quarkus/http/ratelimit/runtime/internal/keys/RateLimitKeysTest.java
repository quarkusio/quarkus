package io.quarkus.http.ratelimit.runtime.internal.keys;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitKeysTest {

    @Test
    void keyOfAtMost256CharactersIsUnchanged() {
        String key = "k".repeat(256);
        assertThat(RateLimitKeys.cap(key)).isSameAs(key);
        assertThat(RateLimitKeys.cap("")).isEmpty();
    }

    @Test
    void longerKeyIsHashed() {
        // SHA-256 of 257 'k' characters
        String capped = RateLimitKeys.cap("k".repeat(257));
        assertThat(capped).matches("sha256:[0-9a-f]{64}");
    }

    @Test
    void hashIsOfTheUtf8Bytes() {
        // 300 characters, 600 UTF-8 bytes: python3 -c "import hashlib; print(hashlib.sha256(('é' * 300).encode()).hexdigest())"
        assertThat(RateLimitKeys.cap("é".repeat(300)))
                .isEqualTo("sha256:7250b66610f8b7dbd6f5e5426d2143bcba6d826cedb4bea8a358695da78db023");
    }

    @Test
    void hashIsDeterministicAndDistinct() {
        String a = "a".repeat(10_000);
        String b = "a".repeat(9_999) + "b";
        assertThat(RateLimitKeys.cap(a)).isEqualTo(RateLimitKeys.cap(new String(a)));
        assertThat(RateLimitKeys.cap(a)).isNotEqualTo(RateLimitKeys.cap(b));
    }

    /**
     * A short key that looks like a hashed one is hashed too, so that a client that controls a key, through a custom
     * resolver, cannot take the bucket of a long key by sending its hash.
     */
    @Test
    void keyThatLooksHashedIsHashedAgain() {
        String hashed = RateLimitKeys.cap("k".repeat(257));
        assertThat(RateLimitKeys.cap(hashed)).matches("sha256:[0-9a-f]{64}").isNotEqualTo(hashed);
    }

    /**
     * Braces would make part of the key a Redis Cluster hash tag, which pins every key that shares it to one shard.
     */
    @Test
    void keyWithBracesIsHashed() {
        assertThat(RateLimitKeys.cap("path:/a{b}")).matches("sha256:[0-9a-f]{64}");
        assertThat(RateLimitKeys.cap("path:/a}")).matches("sha256:[0-9a-f]{64}");
        assertThat(RateLimitKeys.cap("path:/a{")).matches("sha256:[0-9a-f]{64}");
    }
}
