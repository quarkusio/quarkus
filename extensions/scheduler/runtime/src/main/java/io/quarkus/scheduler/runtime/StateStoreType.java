package io.quarkus.scheduler.runtime;

public enum StateStoreType {
    IN_MEMORY("org.quartz.simpl.RAMJobStore", "RAMJobStore"),
    /**
     * Not supported in Native Image because of a missing Object serialization.
     * See https://github.com/oracle/graal/issues/460
     */
    JDBC("org.quartz.impl.jdbcjobstore.JobStoreTX", "JobStoreTX");

    public String name;
    public String clazz;

    StateStoreType(String clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }
}
