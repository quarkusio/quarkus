package io.quarkus.it.jpa.postgresql;

import java.util.HashMap;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.Priorities;
import io.smallrye.config.common.MapBackedConfigSource;

// To test https://github.com/quarkusio/quarkus/issues/16123
@Priority(Priorities.APPLICATION + 100)
public class OverrideJdbcUrlBuildTimeConfigSource extends MapBackedConfigSource {
    public OverrideJdbcUrlBuildTimeConfigSource() {
        super(OverrideJdbcUrlBuildTimeConfigSource.class.getName(), new HashMap<>(), 1000);
    }

    @Override
    public String getValue(final String propertyName) {
        if (!propertyName.equals("quarkus.datasource.jdbc.url")) {
            return super.getValue(propertyName);
        }

        boolean someotherprofile = ConfigUtils.isProfileActive("someotherprofile");
        // This config source should only kick in when the custom profile is active, or dev services get disabled for all tests
        if (!someotherprofile) {
            return super.getValue(propertyName);
        }

        boolean isBuildTime = false;
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            if (configSource.getName().equals("PropertiesConfigSource[source=Build system]")) {
                isBuildTime = true;
                break;
            }
        }

        //  Originally, the JDBC Extension queried the JDBC URL value at build time to either start or skip the DevService. In cases where the URL was an expansion, this would fail if the expansion is not available at build time. In the original issue, the JDBC URL was set as an expansion in Vault, only available at runtime.
        // To simulate the original issue, the source sets quarkus.datasource.jdbc.url at build time, and sets it to an 'impossible' expansion.
        if (isBuildTime) {
            return "${arbitrary.unavailable.value}";
        }

        // To keep things working, we can then either return nothing at runtime so that we default to the normal application config, or return valid config at runtime.
        return super.getValue(propertyName);
    }
}
