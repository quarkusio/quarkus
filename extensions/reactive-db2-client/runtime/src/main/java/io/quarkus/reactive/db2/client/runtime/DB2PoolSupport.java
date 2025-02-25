package io.quarkus.reactive.db2.client.runtime;

import java.util.Set;

public class DB2PoolSupport {

    private final Set<String> db2PoolNames;

    public DB2PoolSupport(Set<String> db2PoolNames) {
        this.db2PoolNames = Set.copyOf(db2PoolNames);
    }

    public Set<String> getDB2PoolNames() {
        return db2PoolNames;
    }
}