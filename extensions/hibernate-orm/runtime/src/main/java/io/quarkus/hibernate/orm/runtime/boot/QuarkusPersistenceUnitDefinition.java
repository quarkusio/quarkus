package io.quarkus.hibernate.orm.runtime.boot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * This represents the fully specified configuration of a Persistence Unit,
 * in a format which is compatible with the bytecode recorder.
 */
public final class QuarkusPersistenceUnitDefinition {

    private final RuntimePersistenceUnitDescriptor actualHibernateDescriptor;
    private final RecordedConfig config;
    private final List<RecordableXmlMapping> xmlMappings;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;
    private final Optional<FormatMapperKind> jsonMapperCreator;
    private final Optional<FormatMapperKind> xmlMapperCreator;
    private final List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors;

    public QuarkusPersistenceUnitDefinition(PersistenceUnitDescriptor persistenceUnitDescriptor,
            String configurationName, RecordedConfig config,
            List<RecordableXmlMapping> xmlMappings,
            boolean isReactive, boolean fromPersistenceXml,
            Optional<FormatMapperKind> jsonMapperCreator,
            Optional<FormatMapperKind> xmlMapperCreator,
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        Objects.requireNonNull(persistenceUnitDescriptor);
        Objects.requireNonNull(config);
        this.actualHibernateDescriptor = RuntimePersistenceUnitDescriptor.validateAndReadFrom(persistenceUnitDescriptor,
                configurationName);
        this.config = config;
        this.xmlMappings = xmlMappings;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
        this.jsonMapperCreator = jsonMapperCreator;
        this.xmlMapperCreator = xmlMapperCreator;
        this.integrationStaticDescriptors = integrationStaticDescriptors;
    }

    @RecordableConstructor
    public QuarkusPersistenceUnitDefinition(RuntimePersistenceUnitDescriptor actualHibernateDescriptor,
            RecordedConfig config,
            List<RecordableXmlMapping> xmlMappings,
            boolean reactive,
            boolean fromPersistenceXml,
            Optional<FormatMapperKind> jsonMapperCreator,
            Optional<FormatMapperKind> xmlMapperCreator,
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        Objects.requireNonNull(actualHibernateDescriptor);
        Objects.requireNonNull(config);
        this.actualHibernateDescriptor = actualHibernateDescriptor;
        this.config = config;
        this.xmlMappings = xmlMappings;
        this.isReactive = reactive;
        this.fromPersistenceXml = fromPersistenceXml;
        this.jsonMapperCreator = jsonMapperCreator;
        this.xmlMapperCreator = xmlMapperCreator;
        this.integrationStaticDescriptors = integrationStaticDescriptors;
    }

    public RuntimePersistenceUnitDescriptor getActualHibernateDescriptor() {
        return actualHibernateDescriptor;
    }

    public String getName() {
        return actualHibernateDescriptor.getName();
    }

    public RecordedConfig getConfig() {
        return config;
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

    public Optional<FormatMapperKind> getJsonMapperCreator() {
        return jsonMapperCreator;
    }

    public Optional<FormatMapperKind> getXmlMapperCreator() {
        return xmlMapperCreator;
    }

    public List<HibernateOrmIntegrationStaticDescriptor> getIntegrationStaticDescriptors() {
        return integrationStaticDescriptors;
    }

}
