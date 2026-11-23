package io.quarkus.kafka.client.runtime.dev.ui.model.response;

public class KafkaAclEntry {
    private String operation;
    private String principal;
    private String perm;
    private String host;
    private String resourceType;
    private String resourceName;
    private String patternType;

    public KafkaAclEntry() {
    }

    public KafkaAclEntry(String operation, String principal, String perm, String host,
            String resourceType, String resourceName, String patternType) {
        this.operation = operation;
        this.principal = principal;
        this.perm = perm;
        this.host = host;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.patternType = patternType;
    }

    public String getHost() {
        return host;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getPatternType() {
        return patternType;
    }

    public String getOperation() {
        return operation;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getPerm() {
        return perm;
    }
}
