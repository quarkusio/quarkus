package io.quarkus.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.ListeningAddresses;
import io.quarkus.test.common.LogPathProvider;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.Config;

public class TestResourceUtilTest {

    // Basic sense check, since most of the heavy lifting is done by TestResourceManager#getReloadGroupIdentifier
    @Test
    public void testReloadGroupIdentifierIsEqualForTestsWithNoResources() {
        String identifier1 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClass.class);
        String identifier2 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, AnotherProfileClass.class);
        assertEquals(identifier2, identifier1);
    }

    @Test
    public void testReloadGroupIdentifierIsEqualForTestsWithIdenticalResources() {
        String identifier1 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClassWithResources.class);
        String identifier2 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, AnotherProfileClassWithResources.class);
        assertEquals(identifier2, identifier1);
    }

    @Test
    public void testReloadGroupIdentifierIsEqualForTestsWithDifferentResources() {
        String identifier1 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClassWithResources.class);
        String identifier2 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClass.class);
        assertNotEquals(identifier2, identifier1);
    }

    @Test
    void resolveLogFilePathShouldGetLogPathWhenLauncherIsLogPathProvider() {
        Path path = Path.of("target", "quarkus-abcde.log");
        Path logPath = IntegrationTestUtil.resolveLogFilePath(new LogPathProviderLauncher(path));
        assertThat(logPath).isEqualTo(path);
    }

    @Test
    void resolveLogFileShouldNotGetPathWhenLauncherIsNotLogPathProvider() {
        Path expected = runtimeConfiguredLogPath();
        Path logPath = IntegrationTestUtil.resolveLogFilePath(new NonLogPathProviderLauncher());
        assertThat(logPath).isEqualTo(expected);
    }

    @Test
    void resolveLogFileShouldNotGetPathWhenLogFilePathIsNull() {
        Path expected = runtimeConfiguredLogPath();
        Path logPath = IntegrationTestUtil.resolveLogFilePath(new LogPathProviderLauncher(null));
        assertThat(logPath).isEqualTo(expected);
    }

    private static Path runtimeConfiguredLogPath() {
        LogRuntimeConfig logRuntimeConfig = Config.get().getConfigMapping(LogRuntimeConfig.class);
        return logRuntimeConfig.file().path().toPath();
    }

    // -------------------------------------------------------------------------
    // Stubs
    // -------------------------------------------------------------------------

    /** Minimal ArtifactLauncher stub that also implements LogPathProvider. */
    @SuppressWarnings("rawtypes")
    private static class LogPathProviderLauncher implements ArtifactLauncher, LogPathProvider {
        private final Path path;

        LogPathProviderLauncher(Path path) {
            this.path = path;
        }

        @Override
        public Path getLogPath() {
            return path;
        }

        @Override
        public void init(InitContext initContext) {
        }

        @Override
        public ListeningAddresses start() throws IOException {
            return ListeningAddresses.EMPTY;
        }

        @Override
        public ArtifactLauncher.LaunchResult runToCompletion(String[] args) {
            return null;
        }

        @Override
        public void includeAsSysProps(Map systemProps) {
        }

        @Override
        public void close() {
        }
    }

    /** Minimal ArtifactLauncher stub that does NOT implement LogPathProvider. */
    @SuppressWarnings("rawtypes")
    private static class NonLogPathProviderLauncher implements ArtifactLauncher {
        @Override
        public void init(InitContext initContext) {
        }

        @Override
        public ListeningAddresses start() throws IOException {
            return ListeningAddresses.EMPTY;
        }

        @Override
        public ArtifactLauncher.LaunchResult runToCompletion(String[] args) {
            return null;
        }

        @Override
        public void includeAsSysProps(Map systemProps) {
        }

        @Override
        public void close() {
        }
    }
}

class TestClass {

}

class ProfileClass implements QuarkusTestProfile {

    public ProfileClass() {
    }
}

class AnotherProfileClass implements QuarkusTestProfile {

    public AnotherProfileClass() {
    }
}

class ProfileClassWithResources implements QuarkusTestProfile {

    public ProfileClassWithResources() {
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.singletonList(
                new TestResourceEntry(
                        Dummy.class, Map.of()));
    }
}

class AnotherProfileClassWithResources implements QuarkusTestProfile {

    public AnotherProfileClassWithResources() {
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.singletonList(
                new TestResourceEntry(
                        Dummy.class, Map.of()));
    }
}

abstract class Dummy implements QuarkusTestResourceLifecycleManager {
}
