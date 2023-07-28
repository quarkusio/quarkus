package io.quarkus.analytics.config;

import java.util.List;

import io.quarkus.devtools.messagewriter.MessageWriter;

public class GroupIdFilter {
    private static final List<String> DENIED_GROUPS = List.of(
            "io.quarkus",
            "io.quarkiverse",
            "org.acme",
            "org.test",
            "g1",
            "g2",
            "org.apache.camel.quarkus",
            "io.debezium",
            "org.drools",
            "org.optaplanner",
            "org.amqphub.quarkus",
            "com.hazelcast",
            "com.datastax.oss.quarkus");

    public static boolean isAuthorizedGroupId(String groupId, MessageWriter log) {
        if (groupId == null || groupId.isEmpty()) {
            log.warn("[Quarkus build analytics] Artifact with empty or null group ID will not send analytics.");
            return false;
        }
        boolean result = DENIED_GROUPS.stream()
                .noneMatch(groupId::startsWith);
        if (!result) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Artifact with group ID: " + groupId +
                        " will not send analytics because it's on the default deny list.");
            }
        }
        return result;
    }
}
