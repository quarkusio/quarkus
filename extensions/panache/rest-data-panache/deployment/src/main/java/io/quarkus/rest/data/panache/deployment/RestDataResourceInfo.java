package io.quarkus.rest.data.panache.deployment;

public final class RestDataResourceInfo {

    private final String type;

    private final RestDataEntityInfo entityInfo;

    private final DataAccessImplementor dataAccessImplementor;

    public RestDataResourceInfo(String type, RestDataEntityInfo entityInfo, DataAccessImplementor dataAccessImplementor) {
        this.type = type;
        this.entityInfo = entityInfo;
        this.dataAccessImplementor = dataAccessImplementor;
    }

    public String getType() {
        return type;
    }

    public RestDataEntityInfo getEntityInfo() {
        return entityInfo;
    }

    public DataAccessImplementor getDataAccessImplementor() {
        return dataAccessImplementor;
    }
}
