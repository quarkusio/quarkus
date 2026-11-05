package io.quarkus.deployment.dev.remotedev;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class RemoteDevPackageClientTest {

    @Test
    void clientSurfaceDoesNotExposeGradleTypes() {
        assertNoGradleType(RemoteDevPackageClient.class);
        assertNoGradleType(RemoteDevPackageClientConfig.class);
        assertNoGradleType(RemoteDevPackageClientFactory.class);
        assertNoGradleType(RemoteDevPackageClientResult.class);
        assertNoGradleType(RemoteDevPackageChange.class);
        assertNoGradleType(RemoteDevPackageDiff.class);
    }

    @Test
    void redactsUrlUserInfo() {
        var config = new RemoteDevPackageClientConfig(
                java.net.URI.create("https://user:secret@example.com/app"),
                java.util.Optional.of("password"));

        assertThat(config.redactedRemoteUrl()).isEqualTo("https://<redacted>@example.com/app");
    }

    private static void assertNoGradleType(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName()).doesNotContain("org.gradle");
            for (Class<?> parameterType : method.getParameterTypes()) {
                assertThat(parameterType.getName()).doesNotContain("org.gradle");
            }
        }
    }
}
