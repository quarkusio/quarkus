package io.quarkus.reactive.pg.client.runtime;

import java.util.Set;

public class PgPoolSupport {

    private final Set<String> pgPoolNames;

    public PgPoolSupport(Set<String> pgPoolNames) {
        this.pgPoolNames = Set.copyOf(pgPoolNames);
    }

    public Set<String> getPgPoolNames() {
        return pgPoolNames;
    }
}
