package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultJarLauncher;
import io.quarkus.test.common.JarArtifactLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.smallrye.config.SmallRyeConfig;

public class JarLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type, String testProfile) {
        return isJar(type);
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

            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            TestConfig testConfig = config.getConfigMapping(TestConfig.class);
            launcher.init(new DefaultJarInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    testConfig.waitTime(),
                    testConfig.integrationTestProfile(),
                    TestConfigUtil.argLineValues(testConfig.argLine().orElse("")),
                    testConfig.env(),
                    context.devServicesLaunchResult(),
                    context.buildOutputDirectory().resolve(pathStr)));
            return launcher;
        } else {
            throw new IllegalStateException("The path of the native binary could not be determined");
        }
    }

    static class DefaultJarInitContext extends DefaultInitContextBase implements JarArtifactLauncher.JarInitContext {

        private final Path jarPath;

        DefaultJarInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile,
                List<String> argLine, Map<String, String> env,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult, Path jarPath) {
            super(httpPort, httpsPort, waitTime, testProfile, argLine, env, devServicesLaunchResult);
            this.jarPath = jarPath;
        }

        @Override
        public Path jarPath() {
            return jarPath;
        }
    }

}
