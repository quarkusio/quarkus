package io.quarkus.datasource.deployment;

import java.util.List;
import java.util.function.Function;

import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestHandlerBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.util.Reason;

class DataSourceProcessor {
    public static final String TEST = "test";

    @BuildStep
    DataSourceLookupBuildItem defineLookup(List<DataSourceRequestHandlerBuildItem> handlers) {
        boolean blockingFound = false;
        boolean reactiveFound = false;
        Function<String, List<Reason>> blockingUnavailableFunction = ignored -> List
                .of(new Reason("Agroal extension is absent"));
        Function<String, List<Reason>> reactiveUnavailableFunction = ignored -> List
                .of(new Reason("Reactive Datasource extension is absent"));
        for (DataSourceRequestHandlerBuildItem handler : handlers) {
            switch (handler.getParadigm()) {
                case BLOCKING -> {
                    if (blockingFound) {
                        throw new IllegalStateException("Multiple blocking datasource request handlers " + handlers);
                    }
                    blockingFound = true;
                    blockingUnavailableFunction = handler.getUnavailableFunction();
                }
                case REACTIVE -> {
                    if (reactiveFound) {
                        throw new IllegalStateException("Multiple blocking datasource request handlers " + handlers);
                    }
                    reactiveFound = true;
                    reactiveUnavailableFunction = handler.getUnavailableFunction();
                }
            }
        }

        return new DataSourceLookupBuildItem(
                ComponentLookup.of(blockingUnavailableFunction, reactiveUnavailableFunction));
    }

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
}
