package io.quarkus.quartz.runtime;

public enum StoreType {
    RAM("org.quartz.simpl.RAMJobStore", "RAMJobStore"),
    JDBC_TX("org.quartz.impl.jdbcjobstore.JobStoreTX", "JobStoreTX"),
    JDBC_CMT("org.quartz.impl.jdbcjobstore.JobStoreCMT", "JobStoreCMT");

    public String clazz;
    public String simpleName;

    StoreType(String clazz, String simpleName) {
        this.clazz = clazz;
        this.simpleName = simpleName;
    }

    public boolean isDbStore() {
        return RAM != this;
    }

    public boolean isNonManagedTxJobStore() {
        return this.clazz.equals("org.quartz.impl.jdbcjobstore.JobStoreCMT");
    }
}
