package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import static io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchConfigUtil.addConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.literal.NamedLiteral;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchPreIntegrationService;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchConfigHandler;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.bean.HibernateSearchBeanUtil;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.management.HibernateSearchManagementHandler;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.mapping.QuarkusHibernateOrmSearchMappingConfigurer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HibernateSearchElasticsearchRecorder {
    private final HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<HibernateSearchElasticsearchRuntimeConfig> runtimeConfig;

    public HibernateSearchElasticsearchRecorder(
            final HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig,
            final RuntimeValue<HibernateSearchElasticsearchRuntimeConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(
            HibernateSearchOrmElasticsearchMapperContext mapperContext,
            Set<String> rootAnnotationMappedClassNames,
            List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners) {
        Set<Class<?>> rootAnnotationMappedClasses = new LinkedHashSet<>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        for (String className : rootAnnotationMappedClassNames) {
            try {
                rootAnnotationMappedClasses.add(Class.forName(className, true, tccl));
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize mapped class " + className, e);
            }
        }
        return new HibernateSearchIntegrationStaticInitListener(mapperContext,
                buildTimeConfig.persistenceUnits().get(mapperContext.persistenceUnitName),
                rootAnnotationMappedClasses,
                integrationStaticInitListeners);
    }

    public HibernateOrmIntegrationStaticInitListener createStaticInitInactiveListener() {
        return new HibernateSearchIntegrationStaticInitInactiveListener();
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(
            HibernateSearchOrmElasticsearchMapperContext mapperContext,
            List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners) {
        HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puConfig = runtimeConfig.getValue()
                .persistenceUnits()
                .get(mapperContext.persistenceUnitName);
        return new HibernateSearchIntegrationRuntimeInitListener(mapperContext, puConfig,
                integrationRuntimeInitListeners);
    }

    public void checkNoExplicitActiveTrue() {
        for (var entry : runtimeConfig.getValue().persistenceUnits().entrySet()) {
            var config = entry.getValue();
            if (config.active().orElse(false)) {
                var puName = entry.getKey();
                String enabledPropertyKey = HibernateSearchElasticsearchRuntimeConfig.extensionPropertyKey("enabled");
                String activePropertyKey = HibernateSearchElasticsearchRuntimeConfig.mapperPropertyKey(puName, "active");
                throw new ConfigurationException(
                        "Hibernate Search activated explicitly for persistence unit '" + puName
                                + "', but the Hibernate Search extension was disabled at build time."
                                + " If you want Hibernate Search to be active for this persistence unit, you must set '"
                                + enabledPropertyKey
                                + "' to 'true' at build time."
                                + " If you don't want Hibernate Search to be active for this persistence unit, you must leave '"
                                + activePropertyKey
                                + "' unset or set it to 'false'.",
                        Set.of(enabledPropertyKey, activePropertyKey));
            }
        }
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitInactiveListener() {
        return new HibernateSearchIntegrationRuntimeInitInactiveListener();
    }

    public Supplier<SearchMapping> searchMappingSupplier(String persistenceUnitName, boolean isDefaultPersistenceUnit) {
        return new Supplier<SearchMapping>() {
            @Override
            public SearchMapping get() {
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puRuntimeConfig = runtimeConfig
                        .getValue()
                        .persistenceUnits().get(persistenceUnitName);
                if (puRuntimeConfig != null && !puRuntimeConfig.active().orElse(true)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the SearchMapping for persistence unit " + persistenceUnitName
                                    + ": Hibernate Search was deactivated through configuration properties");
                }
                SessionFactory sessionFactory;
                if (isDefaultPersistenceUnit) {
                    sessionFactory = Arc.container().instance(SessionFactory.class).get();
                } else {
                    sessionFactory = Arc.container().instance(
                            SessionFactory.class, NamedLiteral.of(persistenceUnitName)).get();
                }
                return Search.mapping(sessionFactory);
            }
        };
    }

    public Supplier<SearchSession> searchSessionSupplier(String persistenceUnitName, boolean isDefaultPersistenceUnit) {
        return new Supplier<SearchSession>() {
            @Override
            public SearchSession get() {
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puRuntimeConfig = runtimeConfig.getValue()
                        .persistenceUnits().get(persistenceUnitName);
                if (puRuntimeConfig != null && !puRuntimeConfig.active().orElse(true)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the SearchSession for persistence unit " + persistenceUnitName
                                    + ": Hibernate Search was deactivated through configuration properties");
                }
                Session session;
                if (isDefaultPersistenceUnit) {
                    session = Arc.container().instance(Session.class).get();
                } else {
                    session = Arc.container().instance(
                            Session.class, NamedLiteral.of(persistenceUnitName)).get();
                }
                return Search.session(session);
            }
        };
    }

    public Handler<RoutingContext> managementHandler() {
        return new HibernateSearchManagementHandler();
    }

    private static final class HibernateSearchIntegrationStaticInitInactiveListener
            implements HibernateOrmIntegrationStaticInitListener {
        private HibernateSearchIntegrationStaticInitInactiveListener() {
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            propertyCollector.accept(HibernateOrmMapperSettings.ENABLED, false);
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }
    }

    private static final class HibernateSearchIntegrationStaticInitListener
            implements HibernateOrmIntegrationStaticInitListener {

        private final HibernateSearchOrmElasticsearchMapperContext mapperContext;
        private final String persistenceUnitName;
        private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;
        private final Set<Class<?>> rootAnnotationMappedClasses;
        private final List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners;

        private HibernateSearchIntegrationStaticInitListener(HibernateSearchOrmElasticsearchMapperContext mapperContext,
                HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig,
                Set<Class<?>> rootAnnotationMappedClasses,
                List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners) {
            this.mapperContext = mapperContext;
            this.persistenceUnitName = mapperContext.persistenceUnitName;
            this.buildTimeConfig = buildTimeConfig;
            this.rootAnnotationMappedClasses = rootAnnotationMappedClasses;
            this.integrationStaticInitListeners = integrationStaticInitListeners;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector,
                    EngineSettings.BACKGROUND_FAILURE_HANDLER,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            buildTimeConfig == null ? Optional.empty() : buildTimeConfig.backgroundFailureHandler(),
                            FailureHandler.class, persistenceUnitName, null, null));

            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.MAPPING_CONFIGURER,
                    collectAllHibernateOrmSearchMappingConfigurers());

            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.COORDINATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            buildTimeConfig == null ? Optional.empty() : buildTimeConfig.coordination().strategy(),
                            CoordinationStrategy.class, persistenceUnitName, null, null));

            HibernateSearchBackendElasticsearchConfigHandler.contributeBackendBuildTimeProperties(
                    propertyCollector, mapperContext,
                    buildTimeConfig == null ? Collections.emptyMap() : buildTimeConfig.backends());

            for (HibernateOrmIntegrationStaticInitListener listener : integrationStaticInitListeners) {
                listener.contributeBootProperties(propertyCollector);
            }
        }

        private List<BeanReference<HibernateOrmSearchMappingConfigurer>> collectAllHibernateOrmSearchMappingConfigurers() {
            List<BeanReference<HibernateOrmSearchMappingConfigurer>> configurers = new ArrayList<>();
            // 1. We add the quarkus-specific configurer:
            configurers
                    .add(BeanReference.ofInstance(new QuarkusHibernateOrmSearchMappingConfigurer(rootAnnotationMappedClasses)));
            // 2. Then we check if any configurers were supplied by a user be it through a property or via an extension:
            Optional<List<BeanReference<HibernateOrmSearchMappingConfigurer>>> beanReferences = HibernateSearchBeanUtil
                    .multiExtensionBeanReferencesFor(
                            buildTimeConfig.mapping().configurer(),
                            HibernateOrmSearchMappingConfigurer.class,
                            persistenceUnitName, null, null);
            if (beanReferences.isPresent()) {
                configurers.addAll(beanReferences.get());
            }

            return configurers;
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
            HibernateOrmIntegrationBooter booter = HibernateOrmIntegrationBooter.builder(metadata, bootstrapContext)
                    // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21
                    .valueReadHandleFactory(ValueHandleFactory.usingJavaLangReflect())
                    .build();
            booter.preBoot(propertyCollector);

            for (HibernateOrmIntegrationStaticInitListener listener : integrationStaticInitListeners) {
                listener.onMetadataInitialized(metadata, bootstrapContext, propertyCollector);
            }
        }
    }

    private static final class HibernateSearchIntegrationRuntimeInitInactiveListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private HibernateSearchIntegrationRuntimeInitInactiveListener() {
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            // Not strictly necessary since this should be set during static init,
            // but let's be on the safe side.
            propertyCollector.accept(HibernateOrmMapperSettings.ENABLED, false);
        }

        @Override
        public List<StandardServiceInitiator<?>> contributeServiceInitiators() {
            return List.of(
                    // The service must be initiated even if Hibernate Search is not supposed to start,
                    // because it's also responsible for determining that Hibernate Search should not start.
                    new HibernateSearchPreIntegrationService.Initiator());
        }
    }

    private static final class HibernateSearchIntegrationRuntimeInitListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private final HibernateSearchOrmElasticsearchMapperContext mapperContext;
        private final String persistenceUnitName;
        private final HibernateSearchElasticsearchRuntimeConfigPersistenceUnit runtimeConfig;
        private final List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners;

        private HibernateSearchIntegrationRuntimeInitListener(HibernateSearchOrmElasticsearchMapperContext mapperContext,
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit runtimeConfig,
                List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners) {
            this.mapperContext = mapperContext;
            this.persistenceUnitName = mapperContext.persistenceUnitName;
            this.runtimeConfig = runtimeConfig;
            this.integrationRuntimeInitListeners = integrationRuntimeInitListeners;
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (runtimeConfig != null) {
                if (!runtimeConfig.active().orElse(true)) {
                    addConfig(propertyCollector, HibernateOrmMapperSettings.ENABLED, false);
                    // Do not process other properties: Hibernate Search is disabled anyway.
                    return;
                }

                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
                        runtimeConfig.schemaManagement().strategy());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK,
                        runtimeConfig.automaticIndexing().enableDirtyCheck());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.QUERY_LOADING_CACHE_LOOKUP_STRATEGY,
                        runtimeConfig.queryLoading().cacheLookup().strategy());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE,
                        runtimeConfig.queryLoading().fetchSize());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS,
                        runtimeConfig.multiTenancy().tenantIds());
            }

            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            runtimeConfig == null ? Optional.empty()
                                    : runtimeConfig.indexing().plan().synchronization().strategy(),
                            IndexingPlanSynchronizationStrategy.class, persistenceUnitName, null, null));
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            runtimeConfig == null ? Optional.empty()
                                    : runtimeConfig.automaticIndexing().synchronization().strategy(),
                            AutomaticIndexingSynchronizationStrategy.class, persistenceUnitName, null, null));

            HibernateSearchBackendElasticsearchConfigHandler.contributeBackendRuntimeProperties(
                    propertyCollector, mapperContext,
                    runtimeConfig == null ? Collections.emptyMap() : runtimeConfig.backends());

            for (HibernateOrmIntegrationRuntimeInitListener integrationRuntimeInitListener : integrationRuntimeInitListeners) {
                integrationRuntimeInitListener.contributeRuntimeProperties(propertyCollector);
            }
        }

        @Override
        public List<StandardServiceInitiator<?>> contributeServiceInitiators() {
            return List.of(
                    // One of the purposes of this service is to provide configuration to Hibernate Search,
                    // so it absolutely must be updated with the runtime configuration.
                    // The service must be initiated even if Hibernate Search is not supposed to start,
                    // because it's also responsible for determining that Hibernate Search should not start.
                    new HibernateSearchPreIntegrationService.Initiator());
        }
    }

    private static void mergeInto(Map<String, Set<String>> target, Map<String, Set<String>> source) {
        for (Entry<String, Set<String>> entry : source.entrySet()) {
            mergeInto(target, entry.getKey(), entry.getValue());
        }
    }

    private static void mergeInto(Map<String, Set<String>> target, String key, Set<String> values) {
        target.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                .addAll(values);
    }
}
