package io.quarkus.analytics.config;

import java.util.List;

import io.quarkus.devtools.messagewriter.MessageWriter;

public class ExtensionsFilter {
    private static final List<String> AUTHORIZED_GROUPS = List.of(
            "io.quarkus",
            "io.quarkiverse",
            "org.apache.camel.quarkus",
            "io.debezium",
            "org.drools",
            "org.optaplanner",
            "org.amqphub.quarkus",
            "com.hazelcast",
            "com.datastax.oss.quarkus");

    public static boolean onlyPublic(String groupId, MessageWriter log) {
        if (groupId == null) {
            log.warn(
                    "[Quarkus build analytics] Extension with null or empty group ID will not be included in the build analytics.");
            return false;
        }
        boolean result = AUTHORIZED_GROUPS.stream()
                .anyMatch(groupId::startsWith);
        if (!result) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Extension with group ID: " + groupId +
                        " will not be included in the build analytics because it's not part of the Quarkus platform extensions.");
            }
        }
        return result;
    }
}
