package io.quarkus.hibernate.orm.runtime.boot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private final QuarkusPersistenceUnitDescriptor persistenceUnitDescriptor;
    private final RecordedConfig config;
    private final List<RecordableXmlMapping> xmlMappings;
    private final boolean fromPersistenceXml;
    private final boolean isHibernateValidatorPresent;
    private final Optional<FormatMapperKind> jsonMapperCreator;
    private final Optional<FormatMapperKind> xmlMapperCreator;
    private final List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors;

    @RecordableConstructor
    public QuarkusPersistenceUnitDefinition(QuarkusPersistenceUnitDescriptor persistenceUnitDescriptor,
            RecordedConfig config,
            List<RecordableXmlMapping> xmlMappings,
            boolean fromPersistenceXml,
            boolean hibernateValidatorPresent,
            Optional<FormatMapperKind> jsonMapperCreator,
            Optional<FormatMapperKind> xmlMapperCreator,
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        Objects.requireNonNull(persistenceUnitDescriptor);
        Objects.requireNonNull(config);
        this.persistenceUnitDescriptor = persistenceUnitDescriptor;
        this.config = config;
        this.xmlMappings = xmlMappings;
        this.fromPersistenceXml = fromPersistenceXml;
        this.isHibernateValidatorPresent = hibernateValidatorPresent;
        this.jsonMapperCreator = jsonMapperCreator;
        this.xmlMapperCreator = xmlMapperCreator;
        this.integrationStaticDescriptors = integrationStaticDescriptors;
    }

    public QuarkusPersistenceUnitDescriptor getPersistenceUnitDescriptor() {
        return persistenceUnitDescriptor;
    }

    public String getName() {
        return persistenceUnitDescriptor.getName();
    }

    public RecordedConfig getConfig() {
        return config;
    }

    public List<RecordableXmlMapping> getXmlMappings() {
        return xmlMappings;
    }

    //TODO assert that we match the right type of ORM!
    public boolean isReactive() {
        return persistenceUnitDescriptor.isReactive();
    }

    public boolean isFromPersistenceXml() {
        return fromPersistenceXml;
    }

    public boolean isHibernateValidatorPresent() {
        return isHibernateValidatorPresent;
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
