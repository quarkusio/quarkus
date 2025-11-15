package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Additional Jpa model class that we need to index
 *
 * @author Stéphane Épardaud
 */
public final class AdditionalJpaModelBuildItem extends MultiBuildItem {

    private final String className;
    private final Set<String> persistenceUnits;

    /**
     * @deprecated Use {@link AdditionalJpaModelBuildItem#AdditionalJpaModelBuildItem(String, Set)} instead,
     *             which should fit the use case of JBeret better.
     */
    @Deprecated(since = "3.28", forRemoval = true)
    public AdditionalJpaModelBuildItem(String className) {
        Objects.requireNonNull(className);
        this.className = className;
        this.persistenceUnits = null;
    }

    /**
     * @param className The name of the additional class.
     * @param persistenceUnits The name of persistence units to which this class should be added
     *        even if the application does not request it explicitly (e.g. using `quarkus.hibernate-orm.packages`).
     *        Note the class can still be added to a persistence unit at static init through other means --
     *        for example Hibernate Envers and Hibernate Search use {@link org.hibernate.boot.spi.AdditionalMappingContributor}.
     *        In such case the set of persistence units can be empty, and the build item will only ensure build-time processing
     *        (Jandex indexing, bytecode enhancement, proxy generation, reflection enablement, ...) happens as expected.
     */
    public AdditionalJpaModelBuildItem(String className, Set<String> persistenceUnits) {
        Objects.requireNonNull(className);
        Objects.requireNonNull(persistenceUnits);
        this.className = className;
        this.persistenceUnits = persistenceUnits;
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getPersistenceUnits() {
        return persistenceUnits;
    }
}
