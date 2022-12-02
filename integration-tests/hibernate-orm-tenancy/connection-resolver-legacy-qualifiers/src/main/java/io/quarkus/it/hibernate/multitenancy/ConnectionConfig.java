package io.quarkus.it.hibernate.multitenancy;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "connection")
public interface ConnectionConfig {

    String urlPrefix();

    Map<String, PuConfig> pu();

    interface PuConfig {

        String username();

        String password();

        @WithDefault("10")
        int maxPoolSizePerTenant();

    }

}
