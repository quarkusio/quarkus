package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.NativeImageFutureDefault.RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS;
import static io.quarkus.deployment.pkg.steps.NativeImageFutureDefault.RUN_TIME_INITIALIZE_SECURITY_PROVIDERS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.deployment.pkg.TestNativeConfig;

class NativeImageFutureDefaultTest {

    @ParameterizedTest
    @ValueSource(strings = { "all", "run-time-initialize-jdk",
            "run-time-initialize-security-providers,run-time-initialize-file-system-providers" })
    void runtimeInitFSandSecurityProvidersWithFutureDefaultsAll(String param) {
        List<String> futureDefaultsValue = List.of("--future-defaults=" + param);
        TestNativeConfig testNativeConfig = new TestNativeConfig(futureDefaultsValue, null);
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
        testNativeConfig = new TestNativeConfig(null, futureDefaultsValue);
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
        testNativeConfig = new TestNativeConfig(futureDefaultsValue, futureDefaultsValue);
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "run-time-initialize-file-system-providers", "run-time-initialize-file-system-providers,other",
            "other,run-time-initialize-file-system-providers" })
    void runtimeInitFileSystemProviders(String param) {
        TestNativeConfig testNativeConfig = new TestNativeConfig(
                List.of("--future-defaults=" + param), null);
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
    }

    @Test
    void runtimeInitSecurityProviders() {
        TestNativeConfig testNativeConfig = new TestNativeConfig(
                List.of("--future-defaults=run-time-initialize-security-providers"), null);
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "complete-reflection-types", "none", "arbitrary-text" })
    void runtimeInitOfFSandSecurityProvidersWithOtherFutureDefaults(String param) {
        List<String> futureDefaultsValue = List.of("--future-defaults=" + param);
        TestNativeConfig testNativeConfig = new TestNativeConfig(futureDefaultsValue, null);
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
        testNativeConfig = new TestNativeConfig(null, futureDefaultsValue);
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
        testNativeConfig = new TestNativeConfig(futureDefaultsValue, futureDefaultsValue);
        assertThat(RUN_TIME_INITIALIZE_SECURITY_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
        assertThat(RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS.isEnabled(testNativeConfig)).isFalse();
    }
}
