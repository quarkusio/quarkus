package io.quarkus.reactive.mssql.client.runtime;

import java.util.Set;

public class MSSQLPoolSupport {

    private final Set<String> msSQLPoolNames;

    public MSSQLPoolSupport(Set<String> msSQLPoolNames) {
        this.msSQLPoolNames = Set.copyOf(msSQLPoolNames);
    }

    public Set<String> getMSSQLPoolNames() {
        return msSQLPoolNames;
    }
}