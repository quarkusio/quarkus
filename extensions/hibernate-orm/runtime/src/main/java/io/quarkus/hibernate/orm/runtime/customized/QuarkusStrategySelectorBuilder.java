package io.quarkus.hibernate.orm.runtime.customized;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.internal.DefaultDialectSelector;
import org.hibernate.boot.registry.selector.internal.DefaultJtaPlatformSelector;
import org.hibernate.boot.registry.selector.internal.StrategySelectorImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.FormatMapper;
import org.hibernate.type.JacksonJsonFormatMapper;
import org.hibernate.type.JacksonXmlFormatMapper;
import org.hibernate.type.JaxbXmlFormatMapper;
import org.hibernate.type.JsonBJsonFormatMapper;

/**
 * Similar to {@link org.hibernate.boot.registry.selector.internal.StrategySelectorBuilder} but
 * omits registering the components we don't support, and uses a new pattern of registration
 * meant to avoid class initializations.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class QuarkusStrategySelectorBuilder {

    /**
     * Builds the selector.
     *
     * @param classLoaderService The class loading service used to (attempt to) resolve any un-registered
     *        strategy implementations.
     *
     * @return The selector.
     */
    public static StrategySelector buildSelector(ClassLoaderService classLoaderService) {
        final StrategySelectorImpl strategySelector = new StrategySelectorImpl(classLoaderService);

        // build the baseline...
        strategySelector.registerStrategyLazily(
                Dialect.class,
                new DefaultDialectSelector());
        strategySelector.registerStrategyLazily(JtaPlatform.class, new DefaultJtaPlatformSelector());
        addTransactionCoordinatorBuilders(strategySelector);
        addSqmMultiTableMutationStrategies(strategySelector);
        addImplicitNamingStrategies(strategySelector);
        addCacheKeysFactories(strategySelector);
        addJsonFormatMappers(strategySelector);
        addXmlFormatMappers(strategySelector);

        // Required to support well known extensions e.g. Envers
        // TODO: should we introduce a new integrator SPI to limit these to extensions supported by Quarkus?
        for (StrategyRegistrationProvider provider : classLoaderService.loadJavaServices(StrategyRegistrationProvider.class)) {
            for (StrategyRegistration<?> discoveredStrategyRegistration : provider.getStrategyRegistrations()) {
                applyFromStrategyRegistration(strategySelector, discoveredStrategyRegistration);
            }
        }

        return strategySelector;
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyFromStrategyRegistration(
            StrategySelectorImpl strategySelector,
            StrategyRegistration<T> strategyRegistration) {
        for (String name : strategyRegistration.getSelectorNames()) {
            strategySelector.registerStrategyImplementor(
                    strategyRegistration.getStrategyRole(),
                    name,
                    strategyRegistration.getStrategyImplementation());
        }
    }

    private static void addTransactionCoordinatorBuilders(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                TransactionCoordinatorBuilder.class,
                JdbcResourceLocalTransactionCoordinatorBuilderImpl.SHORT_NAME,
                JdbcResourceLocalTransactionCoordinatorBuilderImpl.class);
        strategySelector.registerStrategyImplementor(
                TransactionCoordinatorBuilder.class,
                JtaTransactionCoordinatorBuilderImpl.SHORT_NAME,
                JtaTransactionCoordinatorBuilderImpl.class);
    }

    private static void addSqmMultiTableMutationStrategies(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                SqmMultiTableMutationStrategy.class,
                CteMutationStrategy.SHORT_NAME,
                CteMutationStrategy.class);
        strategySelector.registerStrategyImplementor(
                SqmMultiTableMutationStrategy.class,
                GlobalTemporaryTableMutationStrategy.SHORT_NAME,
                GlobalTemporaryTableMutationStrategy.class);
        strategySelector.registerStrategyImplementor(
                SqmMultiTableMutationStrategy.class,
                LocalTemporaryTableMutationStrategy.SHORT_NAME,
                LocalTemporaryTableMutationStrategy.class);
        strategySelector.registerStrategyImplementor(
                SqmMultiTableMutationStrategy.class,
                PersistentTableMutationStrategy.SHORT_NAME,
                PersistentTableMutationStrategy.class);
    }

    private static void addImplicitNamingStrategies(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                ImplicitNamingStrategy.class,
                "default",
                ImplicitNamingStrategyJpaCompliantImpl.class);
        strategySelector.registerStrategyImplementor(
                ImplicitNamingStrategy.class,
                "jpa",
                ImplicitNamingStrategyJpaCompliantImpl.class);
        strategySelector.registerStrategyImplementor(
                ImplicitNamingStrategy.class,
                "component-path",
                ImplicitNamingStrategyComponentPathImpl.class);
        strategySelector.registerStrategyImplementor(
                ImplicitDatabaseObjectNamingStrategy.class,
                StandardNamingStrategy.STRATEGY_NAME,
                StandardNamingStrategy.class);
        strategySelector.registerStrategyImplementor(
                ImplicitDatabaseObjectNamingStrategy.class,
                SingleNamingStrategy.STRATEGY_NAME,
                SingleNamingStrategy.class);
        strategySelector.registerStrategyImplementor(
                ImplicitDatabaseObjectNamingStrategy.class,
                LegacyNamingStrategy.STRATEGY_NAME,
                LegacyNamingStrategy.class);
    }

    private static void addCacheKeysFactories(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                CacheKeysFactory.class,
                DefaultCacheKeysFactory.SHORT_NAME,
                DefaultCacheKeysFactory.class);
        strategySelector.registerStrategyImplementor(
                CacheKeysFactory.class,
                SimpleCacheKeysFactory.SHORT_NAME,
                SimpleCacheKeysFactory.class);
    }

    private static void addJsonFormatMappers(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                FormatMapper.class,
                JacksonJsonFormatMapper.SHORT_NAME,
                JacksonJsonFormatMapper.class);
        strategySelector.registerStrategyImplementor(
                FormatMapper.class,
                JsonBJsonFormatMapper.SHORT_NAME,
                JsonBJsonFormatMapper.class);
    }

    private static void addXmlFormatMappers(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                FormatMapper.class,
                JacksonXmlFormatMapper.SHORT_NAME,
                JacksonXmlFormatMapper.class);
        strategySelector.registerStrategyImplementor(
                FormatMapper.class,
                JaxbXmlFormatMapper.SHORT_NAME,
                JaxbXmlFormatMapper.class);
    }

}
