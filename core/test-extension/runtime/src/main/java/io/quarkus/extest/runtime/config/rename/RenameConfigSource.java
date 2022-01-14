package io.quarkus.extest.runtime.config.rename;

import static java.util.Collections.emptySet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * This simulates a build time only source to test the recording of configuration values. It is still discovered at
 * runtime, but it doesn't return any configuration.
 */
@StaticInitSafe
public class RenameConfigSource extends MapBackedConfigSource {
    // Because getPropertyNames() is called during SmallRyeConfig init
    private int propertyNamesCallCount = 0;

    private static final Map<String, String> FALLBACK_PROPERTIES = Map.of(
            "quarkus.rename.prop", "1234",
            "quarkus.rename.only-in-new", "only-in-new",
            "quarkus.rename-old.only-in-old", "only-in-old",
            "quarkus.rename.in-both", "new",
            "quarkus.rename-old.in-both", "old",
            "quarkus.rename-old.with-default", "old-default");

    public RenameConfigSource() {
        super(RenameConfigSource.class.getName(), new HashMap<>());
    }

    @Override
    public String getValue(final String propertyName) {
        if (propertyName.startsWith("quarkus.rename") && isBuildTime()) {
            return FALLBACK_PROPERTIES.get(propertyName);
        }
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        if (propertyNamesCallCount > 0) {
            return isBuildTime() ? FALLBACK_PROPERTIES.keySet() : emptySet();
        } else {
            propertyNamesCallCount++;
            return emptySet();
        }
    }

    private static boolean isBuildTime() {
        // We can only call this when the SmallRyeConfig is already initialized, or else we may get into a loop
        Config config = ConfigProvider.getConfig();
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getClass().getSimpleName().equals("BuildTimeEnvConfigSource")) {
                return true;
            }
        }
        return false;
    }
}
