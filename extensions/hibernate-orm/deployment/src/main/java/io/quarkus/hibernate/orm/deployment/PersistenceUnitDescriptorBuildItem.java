package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
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

    private final ParsedPersistenceXmlDescriptor descriptor;

    // The default PU in Hibernate Reactive is named "default-reactive" instead of "<default>",
    // but everything related to configuration (e.g. getAllPersistenceUnitConfigsAsMap() still
    // use the name "<default>", so we need to convert between those.
    private final String configurationName;
    private final RecordedConfig config;
    private final String multiTenancySchemaDataSource;
    private final List<RecordableXmlMapping> xmlMappings;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;
    private final Optional<FormatMapperKind> jsonMapper;
    private final Optional<FormatMapperKind> xmlMapper;

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor, String configurationName,
            RecordedConfig config,
            String multiTenancySchemaDataSource,
            List<RecordableXmlMapping> xmlMappings,
            boolean isReactive, boolean fromPersistenceXml, Capabilities capabilities) {
        this.descriptor = descriptor;
        this.configurationName = configurationName;
        this.config = config;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
        this.xmlMappings = xmlMappings;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
        this.jsonMapper = json(capabilities);
        this.xmlMapper = xml(capabilities);
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
        return configurationName;
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

    public QuarkusPersistenceUnitDefinition asOutputPersistenceUnitDefinition(
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        return new QuarkusPersistenceUnitDefinition(descriptor, configurationName, config,
                xmlMappings, isReactive, fromPersistenceXml, jsonMapper, xmlMapper, integrationStaticDescriptors);
    }

    private Optional<FormatMapperKind> json(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.JACKSON)) {
            return Optional.of(FormatMapperKind.JACKSON);
        }
        if (capabilities.isPresent(Capability.JSONB)) {
            return Optional.of(FormatMapperKind.JSONB);
        }
        return Optional.empty();
    }

    private Optional<FormatMapperKind> xml(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.JAXB)) {
            return Optional.of(FormatMapperKind.JAXB);
        }
        return Optional.empty();
    }
}
