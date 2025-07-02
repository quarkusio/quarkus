package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import static io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchConfigUtil.addConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchConfigHandler;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean.ArcBeanProvider;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean.HibernateSearchBeanUtil;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.management.HibernateSearchStandaloneManagementHandler;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.mapping.QuarkusHibernateSearchStandaloneMappingConfigurer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HibernateSearchStandaloneRecorder {
    private final HibernateSearchStandaloneBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<HibernateSearchStandaloneRuntimeConfig> runtimeConfig;

    public HibernateSearchStandaloneRecorder(
            final HibernateSearchStandaloneBuildTimeConfig buildTimeConfig,
            final RuntimeValue<HibernateSearchStandaloneRuntimeConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public void preBoot(HibernateSearchStandaloneElasticsearchMapperContext mapperContext,
            Set<String> rootAnnotationMappedClassNames) {
        Set<Class<?>> rootAnnotationMappedClasses = new LinkedHashSet<>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        for (String className : rootAnnotationMappedClassNames) {
            try {
                rootAnnotationMappedClasses.add(Class.forName(className, true, tccl));
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize mapped class " + className, e);
            }
        }
        Map<String, Object> bootProperties = new LinkedHashMap<>();
        new StaticInitListener(mapperContext, buildTimeConfig, rootAnnotationMappedClasses)
                .contributeBootProperties(bootProperties::put);
        StandalonePojoIntegrationBooter booter = StandalonePojoIntegrationBooter.builder()
                .properties(bootProperties)
                // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21
                .valueReadHandleFactory(ValueHandleFactory.usingJavaLangReflect())
                // Integrate CDI
                .property(StandalonePojoMapperSpiSettings.BEAN_PROVIDER, new ArcBeanProvider(Arc.container()))
                .build();
        booter.preBoot(bootProperties::put);
        HibernateSearchStandalonePreBootState.set(bootProperties);
    }

    public void checkNoExplicitActiveTrue() {
        if (runtimeConfig.getValue().active().orElse(false)) {
            String enabledPropertyKey = HibernateSearchStandaloneRuntimeConfig.extensionPropertyKey("enabled");
            String activePropertyKey = HibernateSearchStandaloneRuntimeConfig.mapperPropertyKey("active");
            throw new ConfigurationException(
                    "Hibernate Search Standalone activated explicitly,"
                            + " but the Hibernate Search Standalone extension was disabled at build time."
                            + " If you want Hibernate Search Standalone to be active, you must set '"
                            + enabledPropertyKey
                            + "' to 'true' at build time."
                            + " If you don't want Hibernate Search Standalone to be active, you must leave '"
                            + activePropertyKey
                            + "' unset or set it to 'false'.",
                    Set.of(enabledPropertyKey, activePropertyKey));
        }
    }

    public void clearPreBootState() {
        HibernateSearchStandalonePreBootState.pop();
    }

    public Function<SyntheticCreationalContext<SearchMapping>, SearchMapping> createSearchMappingFunction(
            HibernateSearchStandaloneElasticsearchMapperContext mapperContext) {
        return new Function<SyntheticCreationalContext<SearchMapping>, SearchMapping>() {
            @Override
            public SearchMapping apply(SyntheticCreationalContext<SearchMapping> context) {
                if (!runtimeConfig.getValue().active().orElse(true)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the SearchMapping: Hibernate Search Standalone was deactivated through configuration properties");
                }
                Map<String, Object> bootProperties = new LinkedHashMap<>(HibernateSearchStandalonePreBootState.pop());
                new RuntimeInitListener(mapperContext, runtimeConfig.getValue())
                        .contributeRuntimeProperties(bootProperties::put);
                StandalonePojoIntegrationBooter booter = StandalonePojoIntegrationBooter.builder()
                        .properties(bootProperties)
                        .build();
                return booter.boot();
            }
        };
    }

    public void bootEagerly() {
        if (!runtimeConfig.getValue().active().orElse(true)) {
            // Hibernate Search is deactivated: skip eager bootstrap.
            return;
        }
        Arc.container().instance(SearchMapping.class).get()
                // Just call some side-effect-free method to initialize the proxy
                .allIndexedEntities();
    }

    public Handler<RoutingContext> managementHandler() {
        return new HibernateSearchStandaloneManagementHandler();
    }

    private static final class StaticInitListener {
        private final HibernateSearchStandaloneElasticsearchMapperContext mapperContext;
        private final HibernateSearchStandaloneBuildTimeConfig buildTimeConfig;
        private final Set<Class<?>> rootAnnotationMappedClasses;

        private StaticInitListener(HibernateSearchStandaloneElasticsearchMapperContext mapperContext,
                HibernateSearchStandaloneBuildTimeConfig buildTimeConfig,
                Set<Class<?>> rootAnnotationMappedClasses) {
            this.mapperContext = mapperContext;
            this.buildTimeConfig = buildTimeConfig;
            this.rootAnnotationMappedClasses = rootAnnotationMappedClasses;
        }

        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector,
                    EngineSettings.BACKGROUND_FAILURE_HANDLER,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            buildTimeConfig == null ? Optional.empty()
                                    : buildTimeConfig.backgroundFailureHandler(),
                            FailureHandler.class, null, null));

            addConfig(propertyCollector,
                    StandalonePojoMapperSettings.MAPPING_CONFIGURER,
                    collectAllStandalonePojoMappingConfigurers());

            HibernateSearchBackendElasticsearchConfigHandler.contributeBackendBuildTimeProperties(
                    propertyCollector, mapperContext,
                    buildTimeConfig == null ? Collections.emptyMap() : buildTimeConfig.backends());
        }

        private List<BeanReference<StandalonePojoMappingConfigurer>> collectAllStandalonePojoMappingConfigurers() {
            List<BeanReference<StandalonePojoMappingConfigurer>> configurers = new ArrayList<>();
            // 1. We add the quarkus-specific configurer:
            configurers.add(BeanReference.ofInstance(
                    new QuarkusHibernateSearchStandaloneMappingConfigurer(buildTimeConfig.mapping().structure(),
                            rootAnnotationMappedClasses)));
            // 2. Then we check if any configurers were supplied by a user be it through a property or via an extension:
            Optional<List<BeanReference<StandalonePojoMappingConfigurer>>> beanReferences = HibernateSearchBeanUtil
                    .multiExtensionBeanReferencesFor(
                            buildTimeConfig.mapping().configurer(),
                            StandalonePojoMappingConfigurer.class, null, null);
            if (beanReferences.isPresent()) {
                configurers.addAll(beanReferences.get());
            }

            return configurers;
        }
    }

    private static final class RuntimeInitListener {
        private final HibernateSearchStandaloneElasticsearchMapperContext mapperContext;
        private final HibernateSearchStandaloneRuntimeConfig runtimeConfig;

        private RuntimeInitListener(HibernateSearchStandaloneElasticsearchMapperContext mapperContext,
                HibernateSearchStandaloneRuntimeConfig runtimeConfig) {
            this.mapperContext = mapperContext;
            this.runtimeConfig = runtimeConfig;
        }

        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (runtimeConfig != null) {
                addConfig(propertyCollector,
                        StandalonePojoMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
                        runtimeConfig.schemaManagement().strategy());
            }

            addConfig(propertyCollector,
                    StandalonePojoMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            runtimeConfig == null ? Optional.empty()
                                    : runtimeConfig.indexing().plan().synchronization().strategy(),
                            IndexingPlanSynchronizationStrategy.class, null, null));

            HibernateSearchBackendElasticsearchConfigHandler.contributeBackendRuntimeProperties(
                    propertyCollector, mapperContext,
                    runtimeConfig == null ? Collections.emptyMap() : runtimeConfig.backends());
        }
    }
}
