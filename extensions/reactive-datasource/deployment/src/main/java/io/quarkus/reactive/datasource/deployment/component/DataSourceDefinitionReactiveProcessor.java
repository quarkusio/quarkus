package io.quarkus.reactive.datasource.deployment.component;

import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.REACTIVE_DATASOURCE_QUALIFIER;
import static io.quarkus.reactive.datasource.deployment.ReactiveDataSourceBuildUtil.REACTIVE_INJECTABLE_TYPES;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryInjectionPointsBuildItem;
import io.quarkus.arc.deployment.InjectionPointScanningUtil;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.component.DataSourceProcessorSupport;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbVersionBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceDefinitionBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.reactive.datasource.deployment.ReactiveDataSourceDefinitionBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Handles the lifecycle of {@link ProgrammingParadigm#REACTIVE reactive} datasources:
 * declaring the reactive datasource {@link DataSourceLookupBuildItem lookup},
 * collecting requests from configuration, and producing definitions.
 * <p>
 * A <b>request</b> ({@link DataSourceRequestBuildItem}) declares that a datasource is needed,
 * carrying a name, a {@link ProgrammingParadigm}, and a {@link Reason} explaining why.
 * A <b>definition</b> ({@link DataSourceDefinitionBuildItem}) is produced by checking each
 * request against the lookup: if the lookup deems the datasource unavailable, a
 * {@code ConfigurationException} is thrown with the full reason chain; otherwise a definition
 * is produced for downstream consumption (reactive pool creation, dev services, etc.).
 *
 * @see io.quarkus.datasource.deployment.component.DataSourceLookupProcessor
 * @see io.quarkus.agroal.deployment.component.DataSourceDefinitionBlockingProcessor
 */
class DataSourceDefinitionReactiveProcessor {

    private static final Logger log = Logger.getLogger(DataSourceDefinitionReactiveProcessor.class);

    @BuildStep
    DataSourceRequestHandlerBuildItem defineDataSourceRequestHandler(
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            DataSourceDbKindResolverBuildItem dbKindResolverBuildItem) {
        var dbKindResolver = dbKindResolverBuildItem.get();
        return new DataSourceRequestHandlerBuildItem(ProgrammingParadigm.REACTIVE,
                (dataSourceName, paradigm) -> {
                    if (paradigm != ProgrammingParadigm.REACTIVE) {
                        return List.of();
                    }
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
                        unavailableReasons.add(dbKindResolver.unavailableReason(dataSourceName, paradigm));
                    }
                    return unavailableReasons;
                });
    }

    @BuildStep
    void collectImplicitReactiveDataSourceRequests(
            DataSourcesBuildTimeConfig config,
            DataSourcesReactiveBuildTimeConfig reactiveConfig,
            DataSourceLookupBuildItem lookupBuildItem,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Predicate<String> enabled = name -> reactiveConfig.dataSources().get(name).reactive().enabled();
        DataSourceProcessorSupport.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.REACTIVE, config.dataSources().keySet(), "*", enabled,
                dataSourceRequests);
        DataSourceProcessorSupport.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.REACTIVE, reactiveConfig.dataSources().keySet(), "reactive.*", enabled,
                dataSourceRequests);
    }

    @BuildStep
    void collectReactiveDataSourceRequestsFromInjection(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            BeanDiscoveryInjectionPointsBuildItem injectionPointIndex,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        InjectionPointScanningUtil.collectUnsatisfiedInjectionPoints(
                beanDiscovery, injectionPointIndex,
                REACTIVE_INJECTABLE_TYPES,
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
            List<DefaultDataSourceDbVersionBuildItem> defaultDbVersions,
            BuildProducer<ReactiveDataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<DataSourceDefinitionBuildItem> definedDataSources,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        Set<String> defined = DataSourceProcessorSupport.defineDataSources(
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
                    // Should not throw since DataSourceProcessorSupport.defineDataSources skips datasources with no db-kind.
                    .orElseThrow();

            definedDataSources.produce(new DataSourceDefinitionBuildItem(dataSourceName, dbKind, ProgrammingParadigm.REACTIVE));

            dataSourceDefinitions.produce(new ReactiveDataSourceDefinitionBuildItem(dataSourceName,
                    config.dataSources().get(dataSourceName),
                    reactiveConfig.dataSources().get(dataSourceName).reactive(),
                    dbKind,
                    config.dataSources().get(dataSourceName).dbVersion()
                            .or(() -> DefaultDataSourceDbVersionBuildItem.resolveDefaultDbVersion(dbKind,
                                    defaultDbVersions, config.dbVersionDefaults()))));
        }
    }
}
