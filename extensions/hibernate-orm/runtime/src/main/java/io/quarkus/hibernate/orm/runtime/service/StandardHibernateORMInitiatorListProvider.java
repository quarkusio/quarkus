package io.quarkus.hibernate.orm.runtime.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.internal.SqlStatementLoggerInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.loader.ast.internal.BatchLoaderFactoryInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.query.sqm.mutation.internal.SqmMultiTableMutationStrategyProviderInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyInitiator;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesMappingProducerProviderInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

import io.quarkus.hibernate.orm.runtime.cdi.QuarkusManagedBeanRegistryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.BootstrapOnlyProxyFactoryFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJndiServiceInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJtaPlatformInitiator;

/**
 * Here we define the list of standard Service Initiators to be used by
 * Hibernate ORM when running on Quarkus.
 * WARNING: this is a customized list: we started from a copy of ORM's standard
 * list, then changes have evolved.
 * Also: Hibernate Reactive uses a different list, and there's an additional definition of
 * services in PreconfiguredServiceRegistryBuilder.
 */
public final class StandardHibernateORMInitiatorListProvider implements InitialInitiatorListProvider {

    @Override
    public List<StandardServiceInitiator<?>> initialInitiatorList() {

        // Note to maintainers: always remember to check for consistency needs with both:
        // io.quarkus.hibernate.orm.runtime.boot.registry.PreconfiguredServiceRegistryBuilder#buildQuarkusServiceInitiatorList(RecordedState)
        // and ReactiveHibernateInitiatorListProvider

        final ArrayList<StandardServiceInitiator<?>> serviceInitiators = new ArrayList<>();

        //This one needs to be replaced after Metadata has been recorded:
        serviceInitiators.add(BootstrapOnlyProxyFactoryFactoryInitiator.INSTANCE);

        serviceInitiators.add(CfgXmlAccessServiceInitiator.INSTANCE);
        serviceInitiators.add(ConfigurationServiceInitiator.INSTANCE);
        serviceInitiators.add(PropertyAccessStrategyResolverInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusImportSqlCommandExtractorInitiator.INSTANCE);

        serviceInitiators.add(SchemaManagementToolInitiator.INSTANCE);

        serviceInitiators.add(JdbcEnvironmentInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusJndiServiceInitiator.INSTANCE);

        serviceInitiators.add(PersisterClassResolverInitiator.INSTANCE);
        serviceInitiators.add(PersisterFactoryInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusStaticInitConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(MultiTenantConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(DialectResolverInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusStaticInitDialectFactoryInitiator.INSTANCE);

        serviceInitiators.add(BatchBuilderInitiator.INSTANCE);
        serviceInitiators.add(JdbcServicesInitiator.INSTANCE);
        serviceInitiators.add(RefCursorSupportInitiator.INSTANCE);

        serviceInitiators.add(QuarkusJtaPlatformInitiator.INSTANCE);

        serviceInitiators.add(SessionFactoryServiceRegistryFactoryInitiator.INSTANCE);

        serviceInitiators.add(QuarkusRegionFactoryInitiator.INSTANCE);

        serviceInitiators.add(TransactionCoordinatorBuilderInitiator.INSTANCE);

        // Replaces ManagedBeanRegistryInitiator.INSTANCE
        serviceInitiators.add(QuarkusManagedBeanRegistryInitiator.INSTANCE);

        serviceInitiators.add(EntityCopyObserverFactoryInitiator.INSTANCE);

        // Default implementation
        serviceInitiators.add(JdbcValuesMappingProducerProviderInitiator.INSTANCE);

        // Default implementation
        serviceInitiators.add(SqmMultiTableMutationStrategyProviderInitiator.INSTANCE);

        // Default implementation
        serviceInitiators.add(ParameterMarkerStrategyInitiator.INSTANCE);

        // Default implementation
        serviceInitiators.add(BatchLoaderFactoryInitiator.INSTANCE);

        // Default implementation
        serviceInitiators.add(SqlStatementLoggerInitiator.INSTANCE);

        serviceInitiators.trimToSize();

        return serviceInitiators;
    }

}
