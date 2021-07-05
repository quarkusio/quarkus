package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_JAR_WAIT_TIME_SECONDS;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.DefaultJarLauncher;
import io.quarkus.test.common.JarArtifactLauncher;
import io.quarkus.test.common.LauncherUtil;

public class JarLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type) {
        return "jar".equals(type);
    }

    @Override
    public JarArtifactLauncher create(CreateContext context) {
        String pathStr = context.quarkusArtifactProperties().getProperty("path");
        if ((pathStr != null) && !pathStr.isEmpty()) {
            JarArtifactLauncher launcher;
            ServiceLoader<JarArtifactLauncher> loader = ServiceLoader.load(JarArtifactLauncher.class);
            Iterator<JarArtifactLauncher> iterator = loader.iterator();
            if (iterator.hasNext()) {
                launcher = iterator.next();
            } else {
                launcher = new DefaultJarLauncher();
            }

            Config config = LauncherUtil.installAndGetSomeConfig();
            launcher.init(new DefaultJarInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    Duration.ofSeconds(config.getValue("quarkus.test.jar-wait-time", OptionalLong.class)
                            .orElse(DEFAULT_JAR_WAIT_TIME_SECONDS)),
                    config.getOptionalValue("quarkus.test.native-image-profile", String.class).orElse(null),
                    config.getOptionalValue("quarkus.test.argLine", String.class).orElse(null),
                    context.buildOutputDirectory().resolve(pathStr)));
            return launcher;
        } else {
            throw new IllegalStateException("The path of the native binary could not be determined");
        }
    }

    static class DefaultJarInitContext extends DefaultInitContextBase implements JarArtifactLauncher.JarInitContext {

        private final String argLine;
        private final Path jarPath;

        DefaultJarInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile, String argLine,
                Path jarPath) {
            super(httpPort, httpsPort, waitTime, testProfile);
            this.argLine = argLine;
            this.jarPath = jarPath;
        }

        @Override
        public String argLine() {
            return argLine;
        }

        @Override
        public Path jarPath() {
            return jarPath;
        }
    }

}
