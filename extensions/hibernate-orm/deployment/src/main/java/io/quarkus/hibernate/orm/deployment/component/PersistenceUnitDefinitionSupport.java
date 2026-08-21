package io.quarkus.hibernate.orm.deployment.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.component.PersistenceUnitLookupBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.component.PersistenceUnitRequestBuildItem;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Shared logic for persistence unit definition processors (blocking and reactive):
 * collecting persistence unit requests from configuration and defining persistence units.
 *
 * @see io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport
 * @see io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil
 */
public final class PersistenceUnitDefinitionSupport {
    private static final Logger LOG = Logger.getLogger(PersistenceUnitDefinitionSupport.class);

    private PersistenceUnitDefinitionSupport() {
    }

    public static void collectPersistenceUnitRequestsFromConfiguration(ProgrammingParadigm paradigm,
            Capabilities capabilities, HibernateOrmConfig config,
            JpaModelPerPersistenceUnitBuildItem jpaModelPerPersistenceUnit,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            BuildProducer<PersistenceUnitRequestBuildItem> puRequests) {
        LOG.debugf("Collecting implicit %s persistence unit requests from configuration: keySet = %s",
                paradigm, config.persistenceUnits().keySet());
        for (String name : config.persistenceUnits().keySet()) {
            if (isExplicitlyDisabled(paradigm, name, config)) {
                continue;
            }

            if (shouldLeaveRequestToOtherParadigm(paradigm, capabilities, config, lookupBuildItem, name, "configuration")) {
                continue;
            }

            puRequests.produce(new PersistenceUnitRequestBuildItem(name, paradigm,
                    String.format(Locale.ROOT, "Configuration '%s'", HibernateOrmRuntimeConfig.puPropertyKey(name, "*"))));
        }

        for (var entry : jpaModelPerPersistenceUnit.getModelPerPersistenceUnit().entrySet()) {
            String name = entry.getKey();
            if (entry.getValue().entityClassNames().isEmpty()) {
                // Non-entity model classes alone (e.g. PanacheEntityBase) should not trigger PU creation.
                continue;
            }
            if (isExplicitlyDisabled(paradigm, name, config)) {
                continue;
            }

            if (shouldLeaveRequestToOtherParadigm(paradigm, capabilities, config, lookupBuildItem, name, "model")) {
                continue;
            }

            puRequests.produce(new PersistenceUnitRequestBuildItem(name, paradigm,
                    "JPA model including classes/packages " + entry.getValue().allModelClassAndPackageNames()));
        }
    }

    private static boolean shouldLeaveRequestToOtherParadigm(ProgrammingParadigm paradigm,
            Capabilities capabilities, HibernateOrmConfig config, PersistenceUnitLookupBuildItem lookupBuildItem,
            String puName, String requestSource) {
        Set<ProgrammingParadigm> available = lookupBuildItem.getLookup().availableParadigms(puName);
        if (!available.contains(paradigm)) {
            if (!available.isEmpty()) {
                // The extension handling the other paradigm (Hibernate ORM vs. Hibernate Reactive)
                // will create a request -- on our side we'll assume this configuration can be ignored.
                // Note other requests for this PU and paradigm can be created
                // (and potentially lead to failures down the line),
                // it's just that we won't request it *only* because it's configured.
                LOG.debugf("Persistence unit '%s' can only be %s; assuming %s is meant for variant %s",
                        puName, available, requestSource, paradigm.other());
                return true;
            } else if (ProgrammingParadigm.BLOCKING.equals(paradigm) && capabilities.isPresent(Capability.HIBERNATE_REACTIVE)
                    && !isExplicitlyDisabled(ProgrammingParadigm.REACTIVE, puName, config)) {
                // If neither extension (Hibernate ORM vs. Hibernate Reactive) can create the request,
                // and Reactive is present, we'll only have Hibernate Reactive create it, as it's the most likely culprit
                // and it will deliver the most useful error.
                // Ideally we'd have both fail, but unfortunately the Quarkus build stops on the first error.
                LOG.debugf(
                        "Persistence unit '%s' can be neither %s nor %s; assuming %s is meant for variant %s",
                        puName, ProgrammingParadigm.BLOCKING, ProgrammingParadigm.REACTIVE,
                        available, requestSource, paradigm.other());
                return true;
            }
            // Else: we'll produce the request here, so that an error is produced.
        }
        return false;
    }

    public static void definePersistenceUnits(ProgrammingParadigm paradigm,
            HibernateOrmConfig config,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            List<PersistenceUnitRequestBuildItem> puRequests,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits,
            BuildProducer<PersistenceUnitDefinitionBuildItem> persistenceUnitDefinitions) {
        if (!persistenceXmlDescriptors.isEmpty()) {
            // When using persistence.xml, this entire infrastructure gets bypassed.
            // See also checks that prevent using persistence.xml and Quarkus config at the same time
            // in HibernateOrmProcessor.
            return;
        }

        // Collect all relevant persistence unit names that are referenced, with their reasons
        Map<String, List<Reason>> puNamesWithReasons = new LinkedHashMap<>();
        Set<String> puNamesBlockingOrReactive = new HashSet<>();
        for (PersistenceUnitRequestBuildItem puReq : puRequests) {
            puNamesBlockingOrReactive.add(puReq.getName());
            if (puReq.getParadigm() == paradigm) {
                puNamesWithReasons.computeIfAbsent(puReq.getName(), k -> new ArrayList<>())
                        .add(puReq.getReason());
            }
        }

        // If there is no requested PU (across blocking/reactive, which implies there is no configuration)
        // and there can be a default PU (for our current paradigm), then we'll define that default PU.
        if (puNamesBlockingOrReactive.isEmpty()
                && lookupBuildItem.getLookup().availableParadigms(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
                        .contains(paradigm)) {
            puNamesBlockingOrReactive.add(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
            puNamesWithReasons.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    List.of(new Reason(
                            "No other persistence unit exists, and the default persistence unit can be configured")));
        }

        // Note:
        // * We do this after deciding whether there should be a default PU on purpose:
        //   we don't want that decision to be affected by AdditionalPersistenceUnitBuildItem.
        // * We are guaranteed at this point that no application-configured PU conflicts:
        //   see checks that prevent using AdditionalPersistenceUnitBuildItem and Quarkus config for the same PU
        //   in HibernateOrmProcessor.
        Map<String, PersistenceUnitDefinitionBuildItem.AdditionalConfig> additionalConfigs = new HashMap<>();
        for (AdditionalPersistenceUnitBuildItem item : additionalPersistenceUnits) {
            String puName = item.getPersistenceUnitName();
            puNamesBlockingOrReactive.add(puName);
            puNamesWithReasons.computeIfAbsent(puName, k -> new ArrayList<>())
                    .add(item.getReason());
            var previous = additionalConfigs.put(puName,
                    new PersistenceUnitDefinitionBuildItem.AdditionalConfig(
                            item.getDataSourceName(),
                            item.getExplicitDialect(), item.getProperties()));
            if (previous != null) {
                throw new IllegalStateException("Multiple " + AdditionalPersistenceUnitBuildItem.class.getSimpleName()
                        + " for persistence unit '" + puName + "'");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debugf("Defining %s persistence units; reasons:\n%s", paradigm,
                    puNamesWithReasons.entrySet().stream()
                            .map(e -> e.getKey() + ": " + Reason.format(e.getValue()))
                            .collect(Collectors.joining("\n")));
        }
        for (var entry : puNamesWithReasons.entrySet()) {
            String puName = entry.getKey();

            List<Reason> unavailableReasons = lookupBuildItem.getLookup().unavailableReasons(puName, paradigm);
            if (!unavailableReasons.isEmpty()) {
                throw new ConfigurationException(String.format(Locale.ROOT,
                        """
                                Hibernate %s persistence unit '%s' cannot be created for the following reason(s):
                                %s
                                Refer to https://quarkus.io/guides/datasource for guidance.
                                This persistence unit is being created because of:
                                %s
                                """,
                        switch (paradigm) {
                            case BLOCKING -> "ORM";
                            case REACTIVE -> "Reactive";
                        },
                        puName,
                        Reason.format(unavailableReasons),
                        Reason.format(entry.getValue())));
            }

            PersistenceUnitDefinitionBuildItem.AdditionalConfig additionalConfig = additionalConfigs.get(puName);
            Optional<String> dataSourceName = additionalConfig != null
                    ? additionalConfig.dataSourceName().or(() -> HibernateProcessorUtil.getDataSourceName(config, puName))
                    : HibernateProcessorUtil.getDataSourceName(config, puName);
            persistenceUnitDefinitions.produce(new PersistenceUnitDefinitionBuildItem(puName, paradigm,
                    entry.getValue(),
                    config.persistenceUnits().get(puName),
                    dataSourceName,
                    Optional.ofNullable(additionalConfigs.get(puName))));
        }
    }

    private static boolean isExplicitlyDisabled(ProgrammingParadigm paradigm, String puName, HibernateOrmConfig config) {
        HibernateOrmConfigPersistenceUnit puConfig = config.persistenceUnits().get(puName);
        return switch (paradigm) {
            case BLOCKING -> !config.blocking()
                    || (puConfig != null && !puConfig.jdbc().enabled().orElse(true));
            case REACTIVE -> puConfig != null && !puConfig.reactive().enabled().orElse(true);
        };
    }

}
