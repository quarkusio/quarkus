package io.quarkus.hibernate.orm.runtime.boot;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * This represents the fully specified configuration of a Persistence Unit,
 * in a format which is compatible with the bytecode recorder.
 */
public final class QuarkusPersistenceUnitDefinition {

    private final RuntimePersistenceUnitDescriptor actualHibernateDescriptor;
    private final Optional<String> dataSource;
    private final MultiTenancyStrategy multitenancyStrategy;
    private final List<RecordableXmlMapping> xmlMappings;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;
    private final List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors;
    private final Map<String, String> quarkusConfigUnsupportedProperties;

    public QuarkusPersistenceUnitDefinition(PersistenceUnitDescriptor persistenceUnitDescriptor,
            String configurationName, Optional<String> dataSource,
            MultiTenancyStrategy multitenancyStrategy, List<RecordableXmlMapping> xmlMappings,
            Map<String, String> quarkusConfigUnsupportedProperties,
            boolean isReactive, boolean fromPersistenceXml,
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        Objects.requireNonNull(persistenceUnitDescriptor);
        Objects.requireNonNull(multitenancyStrategy);
        this.actualHibernateDescriptor = RuntimePersistenceUnitDescriptor.validateAndReadFrom(persistenceUnitDescriptor,
                configurationName);
        this.dataSource = dataSource;
        this.multitenancyStrategy = multitenancyStrategy;
        this.xmlMappings = xmlMappings;
        this.quarkusConfigUnsupportedProperties = quarkusConfigUnsupportedProperties;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
        this.integrationStaticDescriptors = integrationStaticDescriptors;
    }

    @RecordableConstructor
    public QuarkusPersistenceUnitDefinition(RuntimePersistenceUnitDescriptor actualHibernateDescriptor,
            Optional<String> dataSource,
            MultiTenancyStrategy multitenancyStrategy,
            List<RecordableXmlMapping> xmlMappings,
            Map<String, String> quarkusConfigUnsupportedProperties,
            boolean reactive,
            boolean fromPersistenceXml,
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        Objects.requireNonNull(actualHibernateDescriptor);
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(multitenancyStrategy);
        this.actualHibernateDescriptor = actualHibernateDescriptor;
        this.dataSource = dataSource;
        this.multitenancyStrategy = multitenancyStrategy;
        this.xmlMappings = xmlMappings;
        this.quarkusConfigUnsupportedProperties = quarkusConfigUnsupportedProperties;
        this.isReactive = reactive;
        this.fromPersistenceXml = fromPersistenceXml;
        this.integrationStaticDescriptors = integrationStaticDescriptors;
    }

    public RuntimePersistenceUnitDescriptor getActualHibernateDescriptor() {
        return actualHibernateDescriptor;
    }

    public String getName() {
        return actualHibernateDescriptor.getName();
    }

    public Optional<String> getDataSource() {
        return dataSource;
    }

    public MultiTenancyStrategy getMultitenancyStrategy() {
        return multitenancyStrategy;
    }

    public List<RecordableXmlMapping> getXmlMappings() {
        return xmlMappings;
    }

    //TODO assert that we match the right type of ORM!
    public boolean isReactive() {
        return isReactive;
    }

    public boolean isFromPersistenceXml() {
        return fromPersistenceXml;
    }

    public List<HibernateOrmIntegrationStaticDescriptor> getIntegrationStaticDescriptors() {
        return integrationStaticDescriptors;
    }

    public Map<String, String> getQuarkusConfigUnsupportedProperties() {
        return quarkusConfigUnsupportedProperties;
    }

}
