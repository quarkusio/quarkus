package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Represents a request for a persistence unit to be created.
 * <p>
 * This build item is produced by extensions that need a specific persistence unit to exist,
 * even if that persistence unit is not explicitly configured.
 */
public final class PersistenceUnitRequestBuildItem extends MultiBuildItem {

    private final String name;
    private final ProgrammingParadigm paradigm;
    private final Reason reason;

    public PersistenceUnitRequestBuildItem(String name, ProgrammingParadigm paradigm, String reason) {
        this(name, paradigm, new Reason(reason));
    }

    public PersistenceUnitRequestBuildItem(String name, ProgrammingParadigm paradigm, Reason reason) {
        this.name = name;
        this.paradigm = paradigm;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public Reason getReason() {
        return reason;
    }
}
