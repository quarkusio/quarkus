package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;

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
    private final boolean isReactive;

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor, boolean isReactive) {
        this.descriptor = descriptor;
        this.dataSource = DataSourceUtil.DEFAULT_DATASOURCE_NAME;
        this.multiTenancyStrategy = MultiTenancyStrategy.NONE;
        this.multiTenancySchemaDataSource = null;
        this.isReactive = isReactive;
    }

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor,
            MultiTenancyStrategy multiTenancyStrategy,
            String multiTenancySchemaDataSource,
            boolean isReactive) {
        this.descriptor = descriptor;
        this.dataSource = DataSourceUtil.DEFAULT_DATASOURCE_NAME;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
        this.isReactive = isReactive;
    }

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor, String dataSource,
            boolean isReactive) {
        this.descriptor = descriptor;
        this.dataSource = dataSource;
        this.multiTenancyStrategy = MultiTenancyStrategy.NONE;
        this.multiTenancySchemaDataSource = null;
        this.isReactive = isReactive;
    }

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor, String dataSource,
            MultiTenancyStrategy multiTenancyStrategy, String multiTenancySchemaDataSource, boolean isReactive) {
        this.descriptor = descriptor;
        this.dataSource = dataSource;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
        this.isReactive = isReactive;
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

    public QuarkusPersistenceUnitDefinition asOutputPersistenceUnitDefinition() {
        return new QuarkusPersistenceUnitDefinition(descriptor, dataSource, multiTenancyStrategy, isReactive);
    }
}
