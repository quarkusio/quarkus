package io.quarkus.vertx.mdc.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the thread local MDC used before the Vert.x delegate is set, for example during early
 * startup. SLF4J allows {@code MDC.put(key, null)}; a {@code null} value must remove the key.
 */
class LateBoundMDCProviderTest {

    private static final String KEY = "nullValueTestKey";

    private LateBoundMDCProvider provider;

    @BeforeEach
    void setUp() {
        LateBoundMDCProvider.setMDCProviderDelegate(null);
        provider = new LateBoundMDCProvider();
    }

    @AfterEach
    void tearDown() {
        provider.clear();
    }

    @Test
    void putObjectNullRemovesExistingKey() {
        provider.putObject(KEY, "value");

        assertThat(provider.putObject(KEY, null)).isEqualTo("value");
        assertThat(provider.copyObject()).doesNotContainKey(KEY);
    }

    @Test
    void putNullRemovesExistingKey() {
        provider.put(KEY, "value");

        assertThat(provider.put(KEY, null)).isEqualTo("value");
        assertThat(provider.copy()).doesNotContainKey(KEY);
    }

    @Test
    void putNullForAbsentKey() {
        assertThat(provider.put(KEY, null)).isNull();
        assertThat(provider.isEmpty()).as("MDC is empty").isTrue();
    }

    @Test
    void putNonNullValueIsStillStored() {
        assertThat(provider.put(KEY, "value")).isNull();
        assertThat(provider.get(KEY)).isEqualTo("value");
    }

    @Test
    void nullKeyIsStillRejected() {
        assertThatNullPointerException().isThrownBy(() -> provider.put(null, "value"));
        assertThatNullPointerException().isThrownBy(() -> provider.put(null, null));
    }
}
