package io.quarkus.infinispan.client.runtime;

import java.util.Collection;
import java.util.List;

public final class InfinispanClientUtil {

    public static final String DEFAULT_INFINISPAN_DEV_SERVICE_NAME = "infinispan";
    public static final String DEFAULT_INFINISPAN_CLIENT_NAME = "<default>";
    public static final String INFINISPAN_CLIENT_CONFIG_ROOT_NAME = "infinispan-client";

    public static boolean isDefault(String infinispanClientName) {
        return DEFAULT_INFINISPAN_CLIENT_NAME.equals(infinispanClientName);
    }

    public static boolean hasDefault(Collection<String> infinispanClientNames) {
        return infinispanClientNames.contains(DEFAULT_INFINISPAN_CLIENT_NAME);
    }

    public static List<String> infinispanClientPropertyKeys(String infinispanClientName, String radical) {
        if (infinispanClientName == null || InfinispanClientUtil.isDefault(infinispanClientName)) {
            return List.of("quarkus.infinispan-client." + radical);
        } else {
            // Two possible syntaxes: with or without quotes
            return List.of(
                    "quarkus.infinispan-client.\"" + infinispanClientName + "\"." + radical,
                    "quarkus.infinispan-client." + infinispanClientName + "." + radical);
        }
    }

    private InfinispanClientUtil() {
    }
}
