package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isNativeBinary;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultNativeImageLauncher;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.smallrye.config.SmallRyeConfig;

public class NativeImageLauncherProvider implements ArtifactLauncherProvider {
    @Override
    public boolean supportsArtifactType(String type, String testProfile) {
        return isNativeBinary(type);
    }

    @Override
    public NativeImageLauncher create(CreateContext context) {
        String pathStr = context.quarkusArtifactProperties().getProperty("path");
        if ((pathStr != null) && !pathStr.isEmpty()) {
            NativeImageLauncher launcher;
            ServiceLoader<NativeImageLauncher> loader = ServiceLoader.load(NativeImageLauncher.class);
            Iterator<NativeImageLauncher> iterator = loader.iterator();
            if (iterator.hasNext()) {
                launcher = iterator.next();
            } else {
                launcher = new DefaultNativeImageLauncher();
            }

            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            TestConfig testConfig = config.getConfigMapping(TestConfig.class);
            launcher.init(new NativeImageLauncherProvider.DefaultNativeImageInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    testConfig.waitTime(),
                    testConfig.integrationTestProfile(),
                    TestConfigUtil.argLineValues(testConfig.argLine().orElse("")),
                    testConfig.env(),
                    context.devServicesLaunchResult(),
                    System.getProperty("native.image.path"),
                    config.getOptionalValue("quarkus.package.output-directory", String.class).orElse(null),
                    context.testClass()));
            return launcher;
        } else {
            throw new IllegalStateException("The path of the native binary could not be determined");
        }
    }

    public static class DefaultNativeImageInitContext extends DefaultInitContextBase
            implements NativeImageLauncher.NativeImageInitContext {

        private final String nativeImagePath;
        private final Class<?> testClass;
        private final String configuredOutputDirectory;

        public DefaultNativeImageInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile,
                List<String> argLine, Map<String, String> env,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult,
                String nativeImagePath, String configuredOutputDirectory, Class<?> testClass) {
            super(httpPort, httpsPort, waitTime, testProfile, argLine, env, devServicesLaunchResult);
            this.nativeImagePath = nativeImagePath;
            this.configuredOutputDirectory = configuredOutputDirectory;
            this.testClass = testClass;
        }

        @Override
        public String nativeImagePath() {
            return nativeImagePath;
        }

        @Override
        public String getConfiguredOutputDirectory() {
            return configuredOutputDirectory;
        }

        @Override
        public Class<?> testClass() {
            return testClass;
        }
    }
}
