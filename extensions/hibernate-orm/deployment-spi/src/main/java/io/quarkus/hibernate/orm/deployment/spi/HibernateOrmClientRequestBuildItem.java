package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Represents a request for an external client to be set up for use by a Hibernate ORM persistence unit.
 * <p>
 * Extensions can produce this build item to indicate they need a specific client.
 * The client extension should then ensure the client exists or produce
 * a helpful error message if it cannot be created.
 * <p>
 * Acknowledged requests will yield a {@link HibernateOrmClientDefinedBuildItem} later on.
 *
 * @see HibernateOrmClientLookupBuildItem
 * @see HibernateOrmClientDefinedBuildItem
 */
public final class HibernateOrmClientRequestBuildItem extends MultiBuildItem {

    private final String name;
    private final ProgrammingParadigm paradigm;
    private final Reason reason;

    public HibernateOrmClientRequestBuildItem(String name, ProgrammingParadigm paradigm, String reason) {
        this(name, paradigm, new Reason(reason));
    }

    public HibernateOrmClientRequestBuildItem(String name, ProgrammingParadigm paradigm, Reason reason) {
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
