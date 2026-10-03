package io.quarkus.hibernate.orm.deployment.spi.client;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Declares that an external client has been defined and is available
 * for Hibernate ORM persistence units.
 * <p>
 * Produced by client extensions (e.g. quarkus-mongodb-hibernate) for each
 * client they make available. Carries the dialect class and properties
 * needed to configure a persistence unit for the client.
 *
 * @see HibernateOrmClientHandlerBuildItem
 * @see HibernateOrmClientRequestBuildItem
 */
public final class HibernateOrmClientDefinedBuildItem extends MultiBuildItem {

    private final String name;
    private final Set<ProgrammingParadigm> supportedParadigms;
    private final String dialectClass;
    private final Map<String, String> properties;
    private final boolean devServicesEnabled;

    /**
     * @param name the client name, as used in {@code quarkus.hibernate-orm.client};
     *        {@code "<default>"} for the default (unnamed) client
     * @param supportedParadigms the programming paradigms this client supports
     * @param dialectClass the fully qualified name of the Hibernate dialect class
     * @param properties additional Hibernate properties required by this client
     */
    public HibernateOrmClientDefinedBuildItem(String name, Set<ProgrammingParadigm> supportedParadigms,
            String dialectClass, Map<String, String> properties) {
        this(name, supportedParadigms, dialectClass, properties, false);
    }

    /**
     * @param name the client name, as used in {@code quarkus.hibernate-orm.client};
     *        {@code "<default>"} for the default (unnamed) client
     * @param supportedParadigms the programming paradigms this client supports
     * @param dialectClass the fully qualified name of the Hibernate dialect class
     * @param properties additional Hibernate properties required by this client
     * @param devServicesEnabled whether the client is backed by dev services;
     *        when {@code true}, the schema management strategy will default to {@code drop-and-create}
     */
    public HibernateOrmClientDefinedBuildItem(String name, Set<ProgrammingParadigm> supportedParadigms,
            String dialectClass, Map<String, String> properties,
            boolean devServicesEnabled) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(supportedParadigms, "supportedParadigms must not be null");
        Objects.requireNonNull(dialectClass, "dialectClass must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.name = name;
        this.supportedParadigms = Set.copyOf(supportedParadigms);
        this.dialectClass = dialectClass;
        this.properties = properties;
        this.devServicesEnabled = devServicesEnabled;
    }

    public String getName() {
        return name;
    }

    public Set<ProgrammingParadigm> getSupportedParadigms() {
        return supportedParadigms;
    }

    public String getDialectClass() {
        return dialectClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isDevServicesEnabled() {
        return devServicesEnabled;
    }
}
