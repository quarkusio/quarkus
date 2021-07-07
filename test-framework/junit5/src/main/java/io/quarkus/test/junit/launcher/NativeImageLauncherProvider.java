package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_JAR_WAIT_TIME_SECONDS;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.time.Duration;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.DefaultNativeImageLauncher;
import io.quarkus.test.common.LauncherUtil;
import io.quarkus.test.common.NativeImageLauncher;

public class NativeImageLauncherProvider implements ArtifactLauncherProvider {
    @Override
    public boolean supportsArtifactType(String type) {
        return "native".equals(type);
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

            Config config = LauncherUtil.installAndGetSomeConfig();
            launcher.init(new NativeImageLauncherProvider.DefaultNativeImageInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    Duration.ofSeconds(config.getValue("quarkus.test.jar-wait-time", OptionalLong.class)
                            .orElse(DEFAULT_JAR_WAIT_TIME_SECONDS)),
                    config.getOptionalValue("quarkus.test.native-image-profile", String.class).orElse(null),
                    System.getProperty("native.image.path"),
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

        public DefaultNativeImageInitContext(int httpPort, int httpsPort, Duration waitTime, String testProfile,
                String nativeImagePath, Class<?> testClass) {
            super(httpPort, httpsPort, waitTime, testProfile);
            this.nativeImagePath = nativeImagePath;
            this.testClass = testClass;
        }

        @Override
        public String nativeImagePath() {
            return nativeImagePath;
        }

        @Override
        public Class<?> testClass() {
            return testClass;
        }
    }
}
