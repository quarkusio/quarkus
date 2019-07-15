package io.quarkus.axon.deployment.querytest;

public class RequestTestItemQuery {
    private Long lookupId;

    public RequestTestItemQuery() {
    }

    public RequestTestItemQuery(Long lookupId) {
        this.lookupId = lookupId;
    }

    public void setLookupId(Long lookupId) {
        this.lookupId = lookupId;
    }

    public Long getLookupId() {
        return lookupId;
    }
}
