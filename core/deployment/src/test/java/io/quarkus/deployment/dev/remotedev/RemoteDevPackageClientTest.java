package io.quarkus.deployment.dev.remotedev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RemoteDevPackageClientTest {

    @Test
    void clientSurfaceDoesNotExposeGradleTypes() {
        assertNoGradleType(RemoteDevPackageClient.class);
        assertNoGradleType(RemoteDevPackageClientConfig.class);
        assertNoGradleType(RemoteDevPackageClientFactory.class);
        assertNoGradleType(RemoteDevPackageClientResult.class);
        assertNoGradleType(RemoteDevPackageClientOutcome.class);
        assertNoGradleType(RemoteDevPackageReconnectListener.class);
        assertNoGradleType(RemoteDevPackageChange.class);
        assertNoGradleType(RemoteDevPackageDiff.class);
    }

    @Test
    void validatesClientResultShapesAndCopiesRequestedPaths() {
        Set<String> requested = new LinkedHashSet<>(Set.of("app/application.jar"));
        RemoteDevPackageClientResult connected = RemoteDevPackageClientResult.connected(requested);
        requested.clear();

        assertThat(connected.outcome()).isEqualTo(RemoteDevPackageClientOutcome.CONNECTED);
        assertThat(connected.requested()).isEqualTo(1);
        assertThat(connected.requestedPaths()).containsExactly("app/application.jar");
        assertThat(RemoteDevPackageClientResult.sent(2, 1).outcome())
                .isEqualTo(RemoteDevPackageClientOutcome.SENT);
        assertThat(RemoteDevPackageClientResult.reconnectRequired().outcome())
                .isEqualTo(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED);

        assertThatThrownBy(() -> new RemoteDevPackageClientResult(null, 0, 0, 0, Set.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoteDevPackageClientResult(
                RemoteDevPackageClientOutcome.CONNECTED, 0, 0, 0, Set.of("app/application.jar")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteDevPackageClientResult(
                RemoteDevPackageClientOutcome.CONNECTED, 0, 1, 0, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteDevPackageClientResult(
                RemoteDevPackageClientOutcome.SENT, 1, 0, 0, Set.of("app/application.jar")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteDevPackageClientResult(
                RemoteDevPackageClientOutcome.RECONNECT_REQUIRED, 0, 1, 0, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RemoteDevPackageClientResult.sent(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
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
