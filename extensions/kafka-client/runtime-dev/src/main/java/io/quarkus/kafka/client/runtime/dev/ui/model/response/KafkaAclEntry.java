package io.quarkus.kafka.client.runtime.dev.ui.model.response;

public class KafkaAclEntry {
    private String operation;
    private String principal;
    private String perm;
    private String pattern;

    public KafkaAclEntry() {
    }

    public KafkaAclEntry(String operation, String principal, String perm, String pattern) {
        this.operation = operation;
        this.principal = principal;
        this.perm = perm;
        this.pattern = pattern;
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

    public String getPattern() {
        return pattern;
    }
}
