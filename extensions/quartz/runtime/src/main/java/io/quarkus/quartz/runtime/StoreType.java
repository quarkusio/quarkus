package io.quarkus.quartz.runtime;

import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.simpl.RAMJobStore;

public enum StoreType {
    RAM(RAMJobStore.class.getName(), RAMJobStore.class.getSimpleName()),
    DB(JobStoreTX.class.getName(), JobStoreTX.class.getSimpleName());

    public String name;
    public String clazz;

    StoreType(String clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }
}
