package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;

/**
 * Not to be confused with PersistenceXmlDescriptorBuildItem, which holds
 * items of the same type.
 * This build item represents a later phase, and might include the implicit
 * configuration definitions that are automatically defined by Quarkus.
 */
public final class PersistenceUnitDescriptorBuildItem extends MultiBuildItem {

    private final QuarkusPersistenceUnitDescriptor descriptor;

    private final RecordedConfig config;
    private final String multiTenancySchemaDataSource;
    private final List<RecordableXmlMapping> xmlMappings;
    private final boolean fromPersistenceXml;
    private final boolean isHibernateValidatorPresent;
    private final Optional<FormatMapperKind> jsonMapper;
    private final Optional<FormatMapperKind> xmlMapper;

    public PersistenceUnitDescriptorBuildItem(QuarkusPersistenceUnitDescriptor descriptor,
            RecordedConfig config,
            String multiTenancySchemaDataSource,
            List<RecordableXmlMapping> xmlMappings,
            boolean fromPersistenceXml,
            boolean isHibernateValidatorPresent, Optional<FormatMapperKind> jsonMapper, Optional<FormatMapperKind> xmlMapper) {
        this.descriptor = descriptor;
        this.config = config;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
        this.xmlMappings = xmlMappings;
        this.fromPersistenceXml = fromPersistenceXml;
        this.isHibernateValidatorPresent = isHibernateValidatorPresent;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    public Collection<String> getManagedClassNames() {
        return descriptor.getManagedClassNames();
    }

    public String getExplicitSqlImportScriptResourceName() {
        return descriptor.getProperties().getProperty("jakarta.persistence.sql-load-script-source");
    }

    public String getPersistenceUnitName() {
        return descriptor.getName();
    }

    public String getConfigurationName() {
        return descriptor.getConfigurationName();
    }

    public RecordedConfig getConfig() {
        return config;
    }

    public String getMultiTenancySchemaDataSource() {
        return multiTenancySchemaDataSource;
    }

    public boolean hasXmlMappings() {
        return !xmlMappings.isEmpty();
    }

    public boolean isFromPersistenceXml() {
        return fromPersistenceXml;
    }

    public boolean isReactive() {
        return descriptor.isReactive();
    }

    public boolean isHibernateValidatorPresent() {
        return isHibernateValidatorPresent;
    }

    public QuarkusPersistenceUnitDefinition asOutputPersistenceUnitDefinition(
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        return new QuarkusPersistenceUnitDefinition(descriptor, config,
                xmlMappings, fromPersistenceXml, isHibernateValidatorPresent, jsonMapper, xmlMapper,
                integrationStaticDescriptors);
    }
}
