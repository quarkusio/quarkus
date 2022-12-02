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
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

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
        strategySelector.registerStrategyLazily(Dialect.class, new DefaultDialectSelector());
        strategySelector.registerStrategyLazily(JtaPlatform.class, new DefaultJtaPlatformSelector());
        addTransactionCoordinatorBuilders(strategySelector);
        addMultiTableBulkIdStrategies(strategySelector);
        addImplicitNamingStrategies(strategySelector);
        addCacheKeysFactories(strategySelector);

        // Required to support well known extensions e.g. Envers
        // TODO: should we introduce a new integrator SPI to limit these to extensions supported by Quarkus?
        for (StrategyRegistrationProvider provider : classLoaderService.loadJavaServices(StrategyRegistrationProvider.class)) {
            for (StrategyRegistration discoveredStrategyRegistration : provider.getStrategyRegistrations()) {
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

    private static void addMultiTableBulkIdStrategies(StrategySelectorImpl strategySelector) {
        strategySelector.registerStrategyImplementor(
                MultiTableBulkIdStrategy.class,
                PersistentTableBulkIdStrategy.SHORT_NAME,
                PersistentTableBulkIdStrategy.class);
        strategySelector.registerStrategyImplementor(
                MultiTableBulkIdStrategy.class,
                GlobalTemporaryTableBulkIdStrategy.SHORT_NAME,
                GlobalTemporaryTableBulkIdStrategy.class);
        strategySelector.registerStrategyImplementor(
                MultiTableBulkIdStrategy.class,
                LocalTemporaryTableBulkIdStrategy.SHORT_NAME,
                LocalTemporaryTableBulkIdStrategy.class);
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

}
