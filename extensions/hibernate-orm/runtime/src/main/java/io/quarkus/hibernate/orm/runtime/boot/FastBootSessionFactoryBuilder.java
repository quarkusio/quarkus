package io.quarkus.hibernate.orm.runtime.boot;

import java.util.function.Supplier;

import jakarta.persistence.criteria.Nulls;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.format.FormatMapper;

/**
 * The default {@link SessionFactoryBuilder} that {@link FastBootEntityManagerFactoryBuilder} passes to the
 * {@link org.hibernate.boot.spi.SessionFactoryBuilderFactory} services, which can return a custom builder,
 * e.g., to build a custom {@link SessionFactory} implementation.
 * <p>
 * Unlike Hibernate ORM's {@link org.hibernate.boot.internal.SessionFactoryBuilderImpl},
 * this builder doesn't register any default {@link SessionFactoryObserver},
 * since {@link FastBootEntityManagerFactoryBuilder} registers the Quarkus ones.
 */
final class FastBootSessionFactoryBuilder implements SessionFactoryBuilderImplementor {

    private final MetadataImplementor metadata;
    private final SessionFactoryOptionsBuilder optionsBuilder;
    private final BootstrapContext bootstrapContext;

    FastBootSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryOptionsBuilder optionsBuilder,
            BootstrapContext bootstrapContext) {
        this.metadata = metadata;
        this.optionsBuilder = optionsBuilder;
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    public SessionFactoryBuilder applyBeanManager(Object beanManager) {
        optionsBuilder.applyBeanManager(beanManager);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyValidatorFactory(Object validatorFactory) {
        optionsBuilder.applyValidatorFactory(validatorFactory);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyName(String sessionFactoryName) {
        optionsBuilder.applySessionFactoryName(sessionFactoryName);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName) {
        optionsBuilder.enableSessionFactoryNameAsJndiName(isJndiName);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyAutoClosing(boolean enabled) {
        optionsBuilder.enableSessionAutoClosing(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyAutoFlushing(boolean enabled) {
        optionsBuilder.enableSessionAutoFlushing(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled) {
        optionsBuilder.enableJtaTrackingByThread(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions) {
        optionsBuilder.enablePreferUserTransaction(preferUserTransactions);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyStatisticsSupport(boolean enabled) {
        optionsBuilder.enableStatisticsSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers) {
        optionsBuilder.addSessionFactoryObservers(observers);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyInterceptor(Interceptor interceptor) {
        optionsBuilder.applyInterceptor(interceptor);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
        optionsBuilder.applyStatelessInterceptor(statelessInterceptorClass);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyStatelessInterceptor(Supplier<? extends Interceptor> statelessInterceptorSupplier) {
        optionsBuilder.applyStatelessInterceptorSupplier(statelessInterceptorSupplier);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyStatementObserver(StatementObserver statementObserver) {
        optionsBuilder.applyStatementObserver(statementObserver);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector) {
        optionsBuilder.applyStatementInspector(statementInspector);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
        optionsBuilder.applyCustomEntityDirtinessStrategy(strategy);
        return this;
    }

    @Override
    public SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers) {
        optionsBuilder.addEntityNameResolvers(entityNameResolvers);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
        optionsBuilder.applyEntityNotFoundDelegate(entityNotFoundDelegate);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled) {
        optionsBuilder.enableIdentifierRollbackSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyNullabilityChecking(boolean enabled) {
        optionsBuilder.enableNullabilityChecking(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled) {
        optionsBuilder.allowLazyInitializationOutsideTransaction(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyDefaultBatchFetchSize(int size) {
        optionsBuilder.applyDefaultBatchFetchSize(size);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyMaximumFetchDepth(int depth) {
        optionsBuilder.applyMaximumFetchDepth(depth);
        return this;
    }

    @Override
    public SessionFactoryBuilder applySubselectFetchEnabled(boolean enabled) {
        optionsBuilder.applySubselectFetchEnabled(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyDefaultNullPrecedence(Nulls nullPrecedence) {
        optionsBuilder.applyDefaultNullPrecedence(nullPrecedence);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyOrderingOfInserts(boolean enabled) {
        optionsBuilder.enableOrderingOfInserts(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled) {
        optionsBuilder.enableOrderingOfUpdates(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyMultiTenancy(boolean enabled) {
        optionsBuilder.applyMultiTenancy(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver) {
        optionsBuilder.applyCurrentTenantIdentifierResolver(resolver);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyTenantSchemaMapper(TenantSchemaMapper<?> mapper) {
        optionsBuilder.applyTenantSchemaMapper(mapper);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyTenantCredentialsMapper(TenantCredentialsMapper<?> mapper) {
        optionsBuilder.applyTenantCredentialsMapper(mapper);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled) {
        optionsBuilder.enableNamedQueryCheckingOnStartup(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled) {
        optionsBuilder.enableSecondLevelCacheSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyQueryCacheSupport(boolean enabled) {
        optionsBuilder.enableQueryCacheSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyQueryCacheLayout(CacheLayout cacheLayout) {
        optionsBuilder.applyQueryCacheLayout(cacheLayout);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyTimestampsCacheFactory(TimestampsCacheFactory factory) {
        optionsBuilder.applyTimestampsCacheFactory(factory);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyCacheRegionPrefix(String prefix) {
        optionsBuilder.applyCacheRegionPrefix(prefix);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled) {
        optionsBuilder.enableMinimalPuts(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled) {
        optionsBuilder.enabledStructuredCacheEntries(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled) {
        optionsBuilder.allowDirectReferenceCacheEntries(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
        optionsBuilder.enableAutoEvictCollectionCaches(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyJdbcBatchSize(int size) {
        optionsBuilder.applyJdbcBatchSize(size);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled) {
        optionsBuilder.enableScrollableResultSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled) {
        optionsBuilder.enableGeneratedKeysSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyJdbcFetchSize(int size) {
        optionsBuilder.applyJdbcFetchSize(size);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
        optionsBuilder.applyConnectionHandlingMode(connectionHandlingMode);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
        optionsBuilder.applyConnectionProviderDisablesAutoCommit(providerDisablesAutoCommit);
        return this;
    }

    @Override
    public SessionFactoryBuilder applySqlComments(boolean enabled) {
        optionsBuilder.enableCommentsSupport(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applySqlFunction(String registrationName, SqmFunctionDescriptor functionDescriptor) {
        optionsBuilder.applySqlFunction(registrationName, functionDescriptor);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyCollectionsInDefaultFetchGroup(boolean enabled) {
        optionsBuilder.enableCollectionInDefaultFetchGroup(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyTemporalTableStrategy(TemporalTableStrategy strategy) {
        optionsBuilder.applyTemporalTableStrategy(strategy);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyAuditStrategy(AuditStrategy strategy) {
        optionsBuilder.applyAuditStrategy(strategy);
        return this;
    }

    @Override
    public SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow) {
        optionsBuilder.allowOutOfTransactionUpdateOperations(allow);
        return this;
    }

    @Override
    public SessionFactoryBuilder enableJpaQueryCompliance(boolean enabled) {
        optionsBuilder.enableJpaQueryCompliance(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder enableJpaOrderByMappingCompliance(boolean enabled) {
        optionsBuilder.enableJpaOrderByMappingCompliance(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder enableJpaTransactionCompliance(boolean enabled) {
        optionsBuilder.enableJpaTransactionCompliance(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder enableJpaClosedCompliance(boolean enabled) {
        optionsBuilder.enableJpaClosedCompliance(enabled);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyJsonFormatMapper(FormatMapper jsonFormatMapper) {
        optionsBuilder.applyJsonFormatMapper(jsonFormatMapper);
        return this;
    }

    @Override
    public SessionFactoryBuilder applyXmlFormatMapper(FormatMapper xmlFormatMapper) {
        optionsBuilder.applyXmlFormatMapper(xmlFormatMapper);
        return this;
    }

    @Override
    public void disableJtaTransactionAccess() {
        optionsBuilder.disableJtaTransactionAccess();
    }

    @Override
    public SessionFactoryOptions buildSessionFactoryOptions() {
        return optionsBuilder.buildOptions();
    }

    @Override
    public SessionFactory build() {
        return new SessionFactoryImpl(metadata, buildSessionFactoryOptions(), bootstrapContext);
    }
}
