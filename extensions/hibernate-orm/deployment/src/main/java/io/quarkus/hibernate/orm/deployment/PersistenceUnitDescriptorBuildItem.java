package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;
import java.util.List;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;

/**
 * Not to be confused with PersistenceXmlDescriptorBuildItem, which holds
 * items of the same type.
 * This build item represents a later phase, and might include the implicit
 * configuration definitions that are automatically defined by Quarkus.
 */
public final class PersistenceUnitDescriptorBuildItem extends MultiBuildItem {

    private final ParsedPersistenceXmlDescriptor descriptor;
    private final String dataSource;
    private final MultiTenancyStrategy multiTenancyStrategy;
    private final String multiTenancySchemaDataSource;
    private final List<RecordableXmlMapping> xmlMappings;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor,
            List<RecordableXmlMapping> xmlMappings, boolean isReactive, boolean fromPersistenceXml) {
        this(descriptor, DataSourceUtil.DEFAULT_DATASOURCE_NAME, MultiTenancyStrategy.NONE, null,
                xmlMappings, isReactive, fromPersistenceXml);
    }

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor, String dataSource,
            MultiTenancyStrategy multiTenancyStrategy, String multiTenancySchemaDataSource,
            List<RecordableXmlMapping> xmlMappings,
            boolean isReactive, boolean fromPersistenceXml) {
        this.descriptor = descriptor;
        this.dataSource = dataSource;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
        this.xmlMappings = xmlMappings;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
    }

    public Collection<String> getManagedClassNames() {
        return descriptor.getManagedClassNames();
    }

    public String getExplicitSqlImportScriptResourceName() {
        return descriptor.getProperties().getProperty("javax.persistence.sql-load-script-source");
    }

    public String getPersistenceUnitName() {
        return descriptor.getName();
    }

    public String getDataSource() {
        return dataSource;
    }

    public MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy;
    }

    public String getMultiTenancySchemaDataSource() {
        return multiTenancySchemaDataSource;
    }

    public boolean hasXmlMappings() {
        return !xmlMappings.isEmpty();
    }

    public QuarkusPersistenceUnitDefinition asOutputPersistenceUnitDefinition(
            List<HibernateOrmIntegrationStaticDescriptor> integrationStaticDescriptors) {
        return new QuarkusPersistenceUnitDefinition(descriptor, dataSource, multiTenancyStrategy, xmlMappings,
                isReactive, fromPersistenceXml, integrationStaticDescriptors);
    }
}
