package io.quarkus.reactive.datasource.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;

class ReactiveDataSourceProcessor {
    private static final Logger log = Logger.getLogger(ReactiveDataSourceProcessor.class);

    @BuildStep
    void addQualifierAsBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // add the @ReactiveDataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ReactiveDataSource.class).build());
    }

    @BuildStep
    void build(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDbKinds,
            BuildProducer<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {
        if (dataSourcesBuildTimeConfig.driver().isPresent() || dataSourcesBuildTimeConfig.url().isPresent()) {
            throw new ConfigurationException(
                    "quarkus.datasource.url and quarkus.datasource.driver have been deprecated in Quarkus 1.3 and removed in 1.9. "
                            + "Please use the new datasource configuration as explained in https://quarkus.io/guides/datasource.");
        }

        List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs = getAggregatedConfigBuildItems(
                dataSourcesBuildTimeConfig,
                dataSourcesReactiveBuildTimeConfig, curateOutcomeBuildItem,
                defaultDbKinds);

        if (aggregatedDataSourceBuildTimeConfigs.isEmpty()) {
            log.warn("The Datasource Reactive dependency is present but no Reactive datasources have been defined.");
            return;
        }

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            aggregatedConfig.produce(aggregatedDataSourceBuildTimeConfig);
        }

    }

    private List<AggregatedDataSourceBuildTimeConfigBuildItem> getAggregatedConfigBuildItems(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DefaultDataSourceDbKindBuildItem> defaultDbKinds) {
        List<AggregatedDataSourceBuildTimeConfigBuildItem> dataSources = new ArrayList<>();

        for (Map.Entry<String, DataSourceBuildTimeConfig> entry : dataSourcesBuildTimeConfig.dataSources().entrySet()) {
            DataSourceReactiveBuildTimeConfig reactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                    .dataSources().get(entry.getKey()).reactive();
            if (!reactiveBuildTimeConfig.enabled()) {
                continue;
            }

            boolean enableImplicitResolution = DataSourceUtil.isDefault(entry.getKey())
                    ? entry.getValue().devservices().enabled().orElse(!dataSourcesBuildTimeConfig.hasNamedDataSources())
                    : true;

            Optional<String> effectiveDbKind = DefaultDataSourceDbKindBuildItem
                    .resolve(entry.getValue().dbKind(), defaultDbKinds,
                            enableImplicitResolution,
                            curateOutcomeBuildItem);

            if (effectiveDbKind.isEmpty()) {
                continue;
            }

            dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(entry.getKey(),
                    entry.getValue(),
                    reactiveBuildTimeConfig,
                    effectiveDbKind.get()));
        }

        return dataSources;
    }

    @BuildStep
    void produceReactiveDataSourceBuildItem(
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedBuildTimeConfigBuildItems,
            BuildProducer<io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem> dataSource) {
        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedBuildTimeConfigBuildItem : aggregatedBuildTimeConfigBuildItems) {
            dataSource.produce(new io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem(
                    aggregatedBuildTimeConfigBuildItem.getName(),
                    aggregatedBuildTimeConfigBuildItem.getDbKind(),
                    aggregatedBuildTimeConfigBuildItem.isDefault(),
                    aggregatedBuildTimeConfigBuildItem.getDataSourceConfig().dbVersion()));
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
