package io.quarkus.extest.runtime.config;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Override a build time property in runtime.
 */
@StaticInitSafe
public class OverrideBuildTimeConfigSource extends MapBackedConfigSource {
    public static AtomicInteger counter = new AtomicInteger(0);

    public OverrideBuildTimeConfigSource() {
        super(OverrideBuildTimeConfigSource.class.getName(), new HashMap<>(), 1000);
        counter.incrementAndGet();
    }

    @Override
    public String getValue(final String propertyName) {
        if (!propertyName.endsWith("quarkus.btrt.all-values.long-primitive")) {
            return super.getValue(propertyName);
        }

        boolean isBuildTime = false;
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            if (configSource.getClass().getSimpleName().equals("BuildTimeEnvConfigSource")) {
                isBuildTime = true;
                break;
            }
        }

        if (isBuildTime) {
            return super.getValue(propertyName);
        }

        // Override the value if runtime for quarkus.btrt.all-values.long-primitive.
        return "0";
    }
}
