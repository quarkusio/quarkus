package io.quarkus.datasource.deployment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

public class DataSourceProcessorUtil {
    private static final Logger LOG = Logger.getLogger(DataSourceProcessorUtil.class);

    private DataSourceProcessorUtil() {
    }

    public static void collectImplicitDataSourceRequestsFromConfiguration(
            ProgrammingParadigm paradigm,
            DataSourcesBuildTimeConfig dsConfig,
            Set<String> keySet, String radicalWildcard, Predicate<String> enabled,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        LOG.debugf("Collecting implicit %s datasource requests from configuration '%s': keySet = %s",
                paradigm, radicalWildcard, keySet);
        for (String name : keySet) {
            if (!enabled.test(name)) {
                // Explicitly disabled
                continue;
            }

            // TODO remove when this gets fixed: https://github.com/smallrye/smallrye-config/pull/1534
            //   For now, since we can't trust keySet for the default datasource, we skip it
            //   unless db-kind is explicitly configured.
            if (DataSourceUtil.isDefault(name) && dsConfig.dataSources().get(name).dbKind().isEmpty()) {
                continue;
            }

            // TODO possible improvement: we could ignore configuration when the JDBC datasource can't be requested for JDBC
            //   (see DataSourceLookupBuildItem) but can be requested for Reactive, and vice-versa?
            //   See similar code in io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.collectPersistenceUnitReferencesFromConfiguration

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

        // TODO this condition is kept for backward compatibility but... seems odd?
        //   It was introduced in https://github.com/quarkusio/quarkus/commit/91a2d8f14775a91b93a013081694f29a1961ffa2
        //   and eventually looked like this:
        //   https://github.com/quarkusio/quarkus/blob/d18aee960572116ef6356ceac561dff5e650db16/extensions/agroal/deployment/src/main/java/io/quarkus/agroal/deployment/AgroalProcessor.java#L327-L329
        //   Note the part about enabling the default DS when there is no named datasource is already handled below;
        //   here we're mostly concerned by what happens when "dev-services.enabled" is set on the default datasource.
        //   Also note this does not depend on the value of "dev-services.enabled" (true or false),
        //   just on whether it's set or not -- that might simply have been a mistake?
        Optional<Boolean> defaultDataSourceDevServicesEnabled = config.dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).devservices()
                .enabled();
        if (defaultDataSourceDevServicesEnabled.isPresent()) {
            if (lookupBuildItem.getLookup().availableParadigms(DataSourceUtil.DEFAULT_DATASOURCE_NAME).contains(paradigm)) {
                dataSourceNamesWithReasons.computeIfAbsent(DataSourceUtil.DEFAULT_DATASOURCE_NAME, k -> new ArrayList<>())
                        .add(new Reason(String.format(Locale.ROOT,
                                "Configuration'%s' is set, and the default datasource can be configured",
                                DataSourceUtil.dataSourcePropertyKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                                        "devservices.enabled"))));
            }
        }
        // If no datasource was requested for the current paradigm at all,
        // and there can be a default datasource (for our current paradigm),
        // then we'll define that default datasource.
        // We intentionally check per-paradigm, because historically a BLOCKING request from
        // e.g. Hibernate ORM has not prevented the default REACTIVE datasource from being created.
        // TODO note the 'else': we disable this when "dev-services.enabled" is set (to true or false),
        //   for backward compatibility. But should we?
        else if (dataSourceNamesWithReasons.isEmpty()
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
