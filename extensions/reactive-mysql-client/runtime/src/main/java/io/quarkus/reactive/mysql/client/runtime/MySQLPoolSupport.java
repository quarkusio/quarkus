package io.quarkus.reactive.mysql.client.runtime;

import java.util.Set;

public class MySQLPoolSupport {

    private final Set<String> mySQLPoolNames;

    public MySQLPoolSupport(Set<String> mySQLPoolNames) {
        this.mySQLPoolNames = Set.copyOf(mySQLPoolNames);
    }

    public Set<String> getMySQLPoolNames() {
        return mySQLPoolNames;
    }
}
