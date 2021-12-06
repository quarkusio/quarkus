package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.ArtifactLauncher;
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
                    ConfigUtil.waitTimeValue(config),
                    config.getOptionalValue("quarkus.test.native-image-profile", String.class).orElse(null),
                    ConfigUtil.argLineValue(config),
                    context.devServicesLaunchResult(),
                    context.buildOutputDirectory().resolve(pathStr)));
            return launcher;
        } else {
            throw new IllegalStateException("The path of the native binary could not be determined");
        }
    }

    static class DefaultJarInitContext extends DefaultInitContextBase implements JarArtifactLauncher.JarInitContext {

        private final Path jarPath;

        DefaultJarInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile, List<String> argLine,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult, Path jarPath) {
            super(httpPort, httpsPort, waitTime, testProfile, argLine, devServicesLaunchResult);
            this.jarPath = jarPath;
        }

        @Override
        public Path jarPath() {
            return jarPath;
        }
    }

}
