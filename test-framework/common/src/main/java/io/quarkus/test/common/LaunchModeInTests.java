package io.quarkus.test.common;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class LaunchModeInTests {
    public static void set(LaunchMode launchMode) {
        LaunchMode current = ProfileManager.getLaunchMode();
        if (current == launchMode) {
            return;
        }

        // if current launch mode changes when executing tests, configuration needs to be rebuilt
        // (only happens if there's at least one `QuarkusDevModeTest` and at least one other test
        // to be executed in the same JVM)
        //
        // this is important at least when `@TestHTTPResource` is used, because the URL is different
        // between dev and test mode

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        if (resolver instanceof SmallRyeConfigProviderResolver) {
            final Map<ClassLoader, Config> configs;
            try {
                Field field = SmallRyeConfigProviderResolver.class.getDeclaredField("configsForClassLoader");
                field.setAccessible(true);
                configs = (Map<ClassLoader, Config>) field.get(resolver);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            synchronized (configs) {
                Config tcclConfig = configs.get(tccl);
                if (tcclConfig != null) {
                    resolver.releaseConfig(tcclConfig);
                }
            }
        }

        ProfileManager.setLaunchMode(launchMode);
    }
}
