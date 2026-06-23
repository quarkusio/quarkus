package io.quarkus.reactive.datasource.deployment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryInjectionPointsBuildItem;
import io.quarkus.arc.deployment.InjectionPointScanningUtil;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.DataSourceProcessorUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.spi.ReactiveDataSourceInjectableTypeBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

class ReactiveDataSourceProcessor {
    private static final Logger log = Logger.getLogger(ReactiveDataSourceProcessor.class);

    @BuildStep
    DataSourceRequestHandlerBuildItem defineDataSourceRequestHandler(
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            DataSourceDbKindResolverBuildItem dbKindResolverBuildItem) {
        var dbKindResolver = dbKindResolverBuildItem.get();
        return new DataSourceRequestHandlerBuildItem(ProgrammingParadigm.REACTIVE, dataSourceName -> {
            var unavailableReasons = new ArrayList<Reason>();
            if (!reactiveConfig.dataSources().get(dataSourceName).reactive().enabled()) {
                unavailableReasons.add(new Reason(String.format(Locale.ROOT, """
                        Reactive datasource '%s' was disabled explicitly by setting '%s' to 'false'. \
                        Refer to https://quarkus.io/guides/datasource for guidance.
                        """,
                        dataSourceName,
                        DataSourceUtil.dataSourcePropertyKey(dataSourceName, "reactive"))));
            }
            if (dbKindResolver.getOptional(dataSourceName).isEmpty()) {
                unavailableReasons.add(dbKindResolver.unavailableReason(dataSourceName, ProgrammingParadigm.REACTIVE));
            }
            return unavailableReasons;
        });
    }

    @BuildStep
    void collectImplicitReactiveDataSourceRequests(
            DataSourcesBuildTimeConfig config,
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Predicate<String> enabled = name -> reactiveConfig.dataSources().get(name).reactive().enabled();
        DataSourceProcessorUtil.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.REACTIVE, config, config.dataSources().keySet(), enabled,
                "*", dataSourceRequests);
        DataSourceProcessorUtil.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.REACTIVE, config, reactiveConfig.dataSources().keySet(), enabled,
                "reactive.*", dataSourceRequests);

    }

    private static final DotName REACTIVE_DATASOURCE_QUALIFIER = DotName.createSimple(ReactiveDataSource.class);

    @BuildStep
    void collectReactiveDataSourceRequestsFromInjection(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            BeanDiscoveryInjectionPointsBuildItem injectionPointIndex,
            List<ReactiveDataSourceInjectableTypeBuildItem> injectableTypes,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Set<DotName> reactiveTypes = new HashSet<>();
        // Always include the generic Pool type
        reactiveTypes.add(ReactiveDataSourceDotNames.VERTX_POOL);
        for (ReactiveDataSourceInjectableTypeBuildItem item : injectableTypes) {
            reactiveTypes.add(item.getTypeName());
        }

        InjectionPointScanningUtil.collectUnsatisfiedInjectionPoints(
                beanDiscovery, injectionPointIndex,
                reactiveTypes,
                List.of(REACTIVE_DATASOURCE_QUALIFIER, DotNames.NAMED),
                DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                qualifier -> {
                    AnnotationValue value = qualifier.value();
                    return (value != null && !value.asString().isEmpty()) ? value.asString()
                            : DataSourceUtil.DEFAULT_DATASOURCE_NAME;
                },
                (name, reason) -> dataSourceRequests
                        .produce(new DataSourceRequestBuildItem(name, ProgrammingParadigm.REACTIVE, reason)));
    }

    @BuildStep
    public void defineReactiveDataSources(
            DataSourcesBuildTimeConfig config,
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            DataSourceDbKindResolverBuildItem dbKindResolutionBuildItem,
            DataSourceLookupBuildItem lookupBuildItem,
            List<DataSourceRequestBuildItem> dataSourceReferences,
            BuildProducer<DataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<DataSourceDefinedBuildItem> definedDataSources,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        Set<String> defined = DataSourceProcessorUtil.defineDataSources(
                ProgrammingParadigm.REACTIVE, config,
                lookupBuildItem,
                dataSourceReferences,
                validationErrors);

        if (defined.isEmpty()) {
            log.warn("The Datasource Reactive dependency is present but no Reactive datasources have been defined.");
            return;
        }

        for (String dataSourceName : defined) {
            String dbKind = dbKindResolutionBuildItem.get().getOptional(dataSourceName)
                    // Should not throw since DataSourceProcessorUtil.defineDataSources skips datasources with no db-kind.
                    .orElseThrow();

            definedDataSources.produce(new DataSourceDefinedBuildItem(dataSourceName, ProgrammingParadigm.REACTIVE, dbKind));

            dataSourceDefinitions.produce(new DataSourceDefinitionBuildItem(dataSourceName,
                    config.dataSources().get(dataSourceName),
                    reactiveConfig.dataSources().get(dataSourceName).reactive(),
                    dbKind));
        }
    }

    @BuildStep
    void addQualifierAsBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // add the @ReactiveDataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ReactiveDataSource.class).build());
    }

    @BuildStep
    void produceReactiveDataSourceBuildItem(
            List<DataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem> dataSource) {
        if (dataSourceDefinitions.isEmpty()) {
            return;
        }

        for (DataSourceDefinitionBuildItem dsDefinition : dataSourceDefinitions) {
            dataSource.produce(new io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem(
                    dsDefinition.getName(),
                    dsDefinition.getDbKind(),
                    dsDefinition.isDefault(),
                    dsDefinition.getDataSourceConfig().dbVersion()));
        }
    }

    @BuildStep
    void convertSPIReactiveDataSourceToDeprecatedOne(
            List<io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem> dataSource,
            BuildProducer<ReactiveDataSourceBuildItem> reactiveDataSource) {

        for (io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem newDataSourceBuildItem : dataSource) {
            reactiveDataSource.produce(new ReactiveDataSourceBuildItem(
                    newDataSourceBuildItem.getName(),
                    newDataSourceBuildItem.getDbKind(),
                    newDataSourceBuildItem.isDefault(),
                    newDataSourceBuildItem.getVersion()));
        }
    }
}
