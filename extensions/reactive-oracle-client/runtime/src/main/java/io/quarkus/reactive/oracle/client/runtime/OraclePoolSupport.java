package io.quarkus.reactive.oracle.client.runtime;

import java.util.Set;

public class OraclePoolSupport {

    private final Set<String> oraclePoolNames;

    public OraclePoolSupport(Set<String> oraclePoolNames) {
        this.oraclePoolNames = Set.copyOf(oraclePoolNames);
    }

    public Set<String> getOraclePoolNames() {
        return oraclePoolNames;
    }
}