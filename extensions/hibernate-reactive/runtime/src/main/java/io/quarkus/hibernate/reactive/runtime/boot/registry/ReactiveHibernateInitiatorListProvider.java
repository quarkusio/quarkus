package io.quarkus.hibernate.reactive.runtime.boot.registry;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.UnmodifiableBatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.reactive.id.impl.ReactiveIdentifierGeneratorFactoryInitiator;
import org.hibernate.reactive.provider.service.NoJtaPlatformInitiator;
import org.hibernate.reactive.provider.service.ReactiveMarkerServiceInitiator;
import org.hibernate.reactive.provider.service.ReactivePersisterClassResolverInitiator;
import org.hibernate.reactive.provider.service.ReactiveQueryTranslatorFactoryInitiator;
import org.hibernate.reactive.provider.service.ReactiveSessionFactoryBuilderInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

import io.quarkus.hibernate.orm.runtime.cdi.QuarkusManagedBeanRegistryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.BootstrapOnlyProxyFactoryFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusJndiServiceInitiator;
import io.quarkus.hibernate.orm.runtime.service.DialectFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.DisabledJMXInitiator;
import io.quarkus.hibernate.orm.runtime.service.InitialInitiatorListProvider;
import io.quarkus.hibernate.orm.runtime.service.QuarkusImportSqlCommandExtractorInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusMutableIdentifierGeneratorFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRegionFactoryInitiator;
import io.quarkus.hibernate.orm.runtime.service.StandardHibernateORMInitiatorListProvider;
import io.quarkus.hibernate.reactive.runtime.customized.QuarkusNoJdbcConnectionProviderInitiator;

/**
 * Defines the initial list of StandardServiceInitiator instances used to initialize the
 * ServiceRegistry of a new Hibernate Reactive instance.
 * This is similar to StandardHibernateORMInitiatorListProvider except it will enable the
 * specific customizations to make it Reactive.
 * 
 * @see StandardHibernateORMInitiatorListProvider
 */
public final class ReactiveHibernateInitiatorListProvider implements InitialInitiatorListProvider {

    @Override
    public List<StandardServiceInitiator> initialInitiatorList() {
        final ArrayList<StandardServiceInitiator> serviceInitiators = new ArrayList<StandardServiceInitiator>();

        //This one needs to be replaced after Metadata has been recorded:
        serviceInitiators.add(BootstrapOnlyProxyFactoryFactoryInitiator.INSTANCE);

        // Definitely exclusive to Hibernate Reactive, as it marks the registry as Reactive:
        serviceInitiators.add(ReactiveMarkerServiceInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(ReactiveSessionFactoryBuilderInitiator.INSTANCE);

        serviceInitiators.add(CfgXmlAccessServiceInitiator.INSTANCE);
        serviceInitiators.add(ConfigurationServiceInitiator.INSTANCE);
        serviceInitiators.add(PropertyAccessStrategyResolverInitiator.INSTANCE);

        serviceInitiators.add(QuarkusImportSqlCommandExtractorInitiator.INSTANCE);
        serviceInitiators.add(SchemaManagementToolInitiator.INSTANCE);

        serviceInitiators.add(JdbcEnvironmentInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(QuarkusJndiServiceInitiator.INSTANCE);

        // Custom one!
        serviceInitiators.add(DisabledJMXInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(ReactivePersisterClassResolverInitiator.INSTANCE);
        serviceInitiators.add(PersisterFactoryInitiator.INSTANCE);

        //Custom for Hibernate Reactive:
        serviceInitiators.add(QuarkusNoJdbcConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(MultiTenantConnectionProviderInitiator.INSTANCE);
        serviceInitiators.add(DialectResolverInitiator.INSTANCE);

        // Custom Quarkus implementation !
        serviceInitiators.add(DialectFactoryInitiator.INSTANCE);

        // Non-default implementation: optimised for lack of JMX management
        serviceInitiators.add(UnmodifiableBatchBuilderInitiator.INSTANCE);
        serviceInitiators.add(JdbcServicesInitiator.INSTANCE);
        serviceInitiators.add(RefCursorSupportInitiator.INSTANCE);

        // Custom for Hibernate Reactive:
        //serviceInitiators.add(QueryTranslatorFactoryInitiator.INSTANCE);
        serviceInitiators.add(ReactiveQueryTranslatorFactoryInitiator.INSTANCE);

        // Custom one! Also, this one has state so can't use the singleton.
        serviceInitiators.add(new QuarkusMutableIdentifierGeneratorFactoryInitiator());// MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

        // Custom for Hibernate Reactive:
        serviceInitiators.add(NoJtaPlatformInitiator.INSTANCE);

        serviceInitiators.add(SessionFactoryServiceRegistryFactoryInitiator.INSTANCE);

        serviceInitiators.add(QuarkusRegionFactoryInitiator.INSTANCE);

        serviceInitiators.add(TransactionCoordinatorBuilderInitiator.INSTANCE);

        // Replaces ManagedBeanRegistryInitiator.INSTANCE
        serviceInitiators.add(QuarkusManagedBeanRegistryInitiator.INSTANCE);

        serviceInitiators.add(EntityCopyObserverFactoryInitiator.INSTANCE);

        // Custom for Hibernate Reactive:
        serviceInitiators.add(ReactiveIdentifierGeneratorFactoryInitiator.INSTANCE);

        serviceInitiators.trimToSize();
        return serviceInitiators;
    }

}
