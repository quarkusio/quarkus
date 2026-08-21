package io.quarkus.datasource.deployment;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceDefinitionBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

class DataSourceProcessor {
    public static final String TEST = "test";

    @BuildStep
    DataSourceDbKindResolverBuildItem resolveDbKinds(
            DataSourcesBuildTimeConfig config,
            List<DefaultDataSourceDbKindBuildItem> defaultDbKinds,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (defaultDbKinds.isEmpty()) {
            return new DataSourceDbKindResolverBuildItem(new DbKindResolverImpl.NoDefault(config));
        }

        List<String> defaultDbKindStrings = defaultDbKinds.stream().map(DefaultDataSourceDbKindBuildItem::getDbKind).distinct()
                .toList();
        if (defaultDbKindStrings.size() == 1) {
            return new DataSourceDbKindResolverBuildItem(
                    new DbKindResolverImpl.SingleDefault(config, defaultDbKindStrings.get(0)));
        }

        //if we have one and only one test scoped driver we assume it is the default
        //it is common to use a different DB such as H2 in tests
        DefaultDataSourceDbKindBuildItem testScopedDriver = null;
        for (DefaultDataSourceDbKindBuildItem i : defaultDbKinds) {
            if (i.getScope(curateOutcomeBuildItem).equals(TEST)) {
                if (testScopedDriver == null) {
                    testScopedDriver = i;
                } else {
                    // Two test-scoped drivers
                    testScopedDriver = null;
                    break;
                }
            }
        }
        if (testScopedDriver != null) {
            return new DataSourceDbKindResolverBuildItem(
                    new DbKindResolverImpl.SingleDefault(config, testScopedDriver.getDbKind()));
        }

        return new DataSourceDbKindResolverBuildItem(new DbKindResolverImpl.MultipleDefaults(config, defaultDbKindStrings));
    }

    @BuildStep
    void aggregateDefinedDataSources(List<DataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<DataSourceDefinedBuildItem> definedDataSources) {
        Map<String, Set<ProgrammingParadigm>> paradigmsByName = new LinkedHashMap<>();
        Map<String, String> dbKindByName = new LinkedHashMap<>();
        for (DataSourceDefinitionBuildItem item : dataSourceDefinitions) {
            dbKindByName.put(item.getName(), item.getDbKind());
            paradigmsByName.computeIfAbsent(item.getName(), k -> EnumSet.noneOf(ProgrammingParadigm.class))
                    .add(item.getParadigm());
        }
        for (var entry : paradigmsByName.entrySet()) {
            definedDataSources.produce(new DataSourceDefinedBuildItem(
                    entry.getKey(), dbKindByName.get(entry.getKey()), entry.getValue()));
        }
    }
}
