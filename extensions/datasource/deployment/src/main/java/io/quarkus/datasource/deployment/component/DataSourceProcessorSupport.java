package io.quarkus.datasource.deployment.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.component.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Shared logic for JDBC and reactive datasource deployment processors:
 * collecting implicit datasource requests from configuration
 * and defining datasources from requests.
 */
public final class DataSourceProcessorSupport {
    private static final Logger LOG = Logger.getLogger(DataSourceProcessorSupport.class);

    private DataSourceProcessorSupport() {
    }

    public static void collectImplicitDataSourceRequestsFromConfiguration(
            ProgrammingParadigm paradigm,
            Set<String> keySet, String radicalWildcard, Predicate<String> enabled,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        LOG.debugf("Collecting implicit %s datasource requests from configuration '%s': keySet = %s",
                paradigm, radicalWildcard, keySet);
        for (String name : keySet) {
            if (!enabled.test(name)) {
                // Explicitly disabled
                continue;
            }

            // TODO possible improvement: we could ignore configuration when the JDBC datasource can't be requested for JDBC
            //   (see DataSourceLookupBuildItem) but can be requested for Reactive, and vice-versa?
            //   See similar code in io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionSupport.collectPersistenceUnitRequestsFromConfiguration

            dataSourceRequests.produce(new DataSourceRequestBuildItem(name, paradigm,
                    String.format(Locale.ROOT, "Configuration '%s'",
                            DataSourceUtil.dataSourcePropertyKey(name, radicalWildcard))));
        }
    }

    public static Set<String> defineDataSources(ProgrammingParadigm paradigm,
            DataSourcesBuildTimeConfig config,
            DataSourceLookupBuildItem lookupBuildItem,
            List<DataSourceRequestBuildItem> dataSourceReferences,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        if (config.driver().isPresent() || config.url().isPresent()) {
            throw new ConfigurationException(
                    "quarkus.datasource.url and quarkus.datasource.driver have been deprecated in Quarkus 1.3 and removed in 1.9. "
                            + "Please use the new datasource configuration as explained in https://quarkus.io/guides/datasource.");
        }

        Set<String> defined = new LinkedHashSet<>();

        // Collect all relevant datasource names that are requested for the current paradigm, with their reasons
        Map<String, List<Reason>> dataSourceNamesWithReasons = new LinkedHashMap<>();
        for (DataSourceRequestBuildItem dsReq : dataSourceReferences) {
            if (paradigm.equals(dsReq.getParadigm())) {
                dataSourceNamesWithReasons.computeIfAbsent(dsReq.getName(), k -> new ArrayList<>())
                        .add(dsReq.getReason());
            }
        }

        // If no datasource was requested for the current paradigm at all,
        // and there can be a default datasource (for our current paradigm),
        // then we'll define that default datasource.
        // We intentionally check per-paradigm, because historically a BLOCKING request from
        // e.g. Hibernate ORM has not prevented the default REACTIVE datasource from being created.
        if (dataSourceNamesWithReasons.isEmpty()
                && lookupBuildItem.getLookup().availableParadigms(DataSourceUtil.DEFAULT_DATASOURCE_NAME).contains(paradigm)) {
            dataSourceNamesWithReasons.put(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                    List.of(new Reason("No other " + paradigm + " datasource exists,"
                            + " and the default datasource can be configured")));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debugf("Defining %s datasources; reasons:\n%s", paradigm,
                    dataSourceNamesWithReasons.entrySet().stream()
                            .map(e -> e.getKey() + ": " + Reason.format(e.getValue()))
                            .collect(Collectors.joining("\n")));
        }
        for (var entry : dataSourceNamesWithReasons.entrySet()) {
            String dataSourceName = entry.getKey();

            List<Reason> unavailableReasons = lookupBuildItem.getLookup().unavailableReasons(dataSourceName, paradigm);
            if (!unavailableReasons.isEmpty()) {
                validationErrors
                        .produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(new ConfigurationException(String.format(
                                """
                                        %s datasource '%s' cannot be created for the following reason(s):
                                        %s
                                        Refer to https://quarkus.io/guides/datasource for guidance.
                                        This datasource is being created because of:
                                        %s
                                        """,
                                switch (paradigm) {
                                    case BLOCKING -> "JDBC";
                                    case REACTIVE -> "Reactive";
                                },
                                dataSourceName,
                                Reason.format(unavailableReasons),
                                Reason.format(entry.getValue())))));
                continue;
            }

            defined.add(dataSourceName);
        }

        return defined;
    }
}
