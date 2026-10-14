package io.quarkus.hibernate.orm.deployment.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.datasource.deployment.spi.component.DataSourceLookupBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.component.PersistenceUnitLookupBuildItem;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Produces a {@link PersistenceUnitLookupBuildItem persistence unit lookup}
 * by implementing a {@link ComponentLookup} with checks such as
 * whether the relevant paradigm is enabled in configuration
 * or whether the backing datasource can be created.
 *
 * @see PersistenceUnitDefinitionBlockingProcessor
 * @see io.quarkus.hibernate.reactive.deployment.component.PersistenceUnitDefinitionReactiveProcessor
 */
class PersistenceUnitLookupProcessor {

    private static final Logger LOG = Logger.getLogger(PersistenceUnitLookupProcessor.class);

    @BuildStep
    PersistenceUnitLookupBuildItem defineLookup(HibernateOrmConfig config,
            Capabilities capabilities,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits,
            HibernateOrmClientLookupBuildItem clientLookupBuildItem,
            DataSourceLookupBuildItem dataSourceLookupBuildItem) {
        var dataSourceLookup = dataSourceLookupBuildItem.getLookup();
        var clientLookup = clientLookupBuildItem.getLookup();
        var blockingEnabled = config.blocking();
        if (!blockingEnabled) {
            LOG.infof("Hibernate ORM was disabled explicitly by quarkus.hibernate-orm.blocking=false."
                    + " This property is deprecated: use 'quarkus.hibernate-orm.jdbc.enabled=false' instead"
                    + " (or the per-persistence-unit equivalent).");
        }
        var hibernateReactivePresent = capabilities.isPresent(Capability.HIBERNATE_REACTIVE);

        return new PersistenceUnitLookupBuildItem(new ComponentLookup() {
            @Override
            public List<Reason> unavailableReasons(String name, ProgrammingParadigm paradigm) {
                var unavailableReasons = new ArrayList<Reason>();
                switch (paradigm) {
                    case BLOCKING -> unavailableReasons.addAll(
                            checkBlockingDisabled(name, config, blockingEnabled));
                    case REACTIVE -> unavailableReasons.addAll(
                            checkReactiveDisabled(name, config, hibernateReactivePresent));
                }
                unavailableReasons.addAll(
                        checkBackendUnavailable(name, paradigm, dataSourceLookup, config,
                                additionalPersistenceUnits, clientLookup));
                return unavailableReasons;
            }
        });
    }

    private static List<Reason> checkBlockingDisabled(String name, HibernateOrmConfig config, boolean blockingEnabled) {
        var reasons = new ArrayList<Reason>();
        if (!blockingEnabled) {
            reasons.add(new Reason(String.format(java.util.Locale.ROOT,
                    "Hibernate ORM was disabled explicitly by setting '%s' to 'false'",
                    HibernateOrmRuntimeConfig.puPropertyKey(name, "blocking"))));
        }
        if (!config.persistenceUnits().get(name).jdbc().enabled().orElse(true)) {
            reasons.add(new Reason(String.format(java.util.Locale.ROOT,
                    "Hibernate ORM was disabled explicitly by setting '%s' to 'false'",
                    HibernateOrmRuntimeConfig.puPropertyKey(name, "jdbc.enabled"))));
        }
        return reasons;
    }

    private static List<Reason> checkReactiveDisabled(String name, HibernateOrmConfig config,
            boolean hibernateReactivePresent) {
        var reasons = new ArrayList<Reason>();
        if (!hibernateReactivePresent) {
            reasons.add(new Reason("Hibernate Reactive extension is absent"));
        }
        if (!config.persistenceUnits().get(name).reactive().enabled().orElse(true)) {
            reasons.add(new Reason(String.format(java.util.Locale.ROOT,
                    "Hibernate Reactive was disabled explicitly by setting '%s' to 'false'",
                    HibernateOrmRuntimeConfig.puPropertyKey(name, "reactive.enabled"))));
        }
        return reasons;
    }

    private static List<Reason> checkBackendUnavailable(String name, ProgrammingParadigm paradigm,
            ComponentLookup dataSourceLookup, HibernateOrmConfig config,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits,
            ComponentLookup clientLookup) {
        Optional<AdditionalPersistenceUnitBuildItem> additionalPu = additionalPersistenceUnits.stream()
                .filter(item -> item.getPersistenceUnitName().equals(name))
                .findFirst();
        PersistenceUnitDefinitionBuildItem.AdditionalConfig additionalConfig = additionalPu
                .map(item -> new PersistenceUnitDefinitionBuildItem.AdditionalConfig(
                        item.getDataSourceName(), item.getClientName(),
                        item.getExplicitDialect(), item.getProperties()))
                .orElse(null);

        var backend = PersistenceUnitDefinitionSupport.resolveBackend(
                config, name, additionalConfig, dataSourceLookup, clientLookup);

        // Ambiguous: both datasource and client available. Available if either works for this paradigm.
        if (backend.dataSourceName().isPresent() && backend.clientName().isPresent()) {
            if (dataSourceLookup.unavailableReasons(backend.dataSourceName().get(), paradigm).isEmpty()) {
                return List.of();
            }
            List<Reason> clientReasons = clientLookup.unavailableReasons(backend.clientName().get(), paradigm);
            if (clientReasons.isEmpty()) {
                return List.of();
            }
            return clientReasons;
        }

        if (backend.clientName().isPresent()) {
            return clientLookup.unavailableReasons(backend.clientName().get(), paradigm);
        }

        if (backend.dataSourceName().isPresent()) {
            List<Reason> dsReasons = dataSourceLookup.unavailableReasons(backend.dataSourceName().get(), paradigm);
            if (!dsReasons.isEmpty()) {
                return List.of(new Reason(
                        String.format(java.util.Locale.ROOT, "%s datasource '%s' cannot be created",
                                switch (paradigm) {
                                    case BLOCKING -> "JDBC";
                                    case REACTIVE -> "Reactive";
                                },
                                backend.dataSourceName().get()),
                        dsReasons));
            }
            return List.of();
        }

        return checkMissingDataSource(name, paradigm, config);
    }

    private static List<Reason> checkMissingDataSource(String name, ProgrammingParadigm paradigm,
            HibernateOrmConfig config) {
        MultiTenancyStrategy multiTenancyStrategy = HibernateProcessorUtil.getMultiTenancyStrategy(
                config.persistenceUnits().get(name).multitenant());
        boolean reactive = ProgrammingParadigm.REACTIVE.equals(paradigm);
        if (reactive || multiTenancyStrategy != MultiTenancyStrategy.DATABASE) {
            String dsConfigProperty = HibernateOrmRuntimeConfig.puPropertyKey(name, "datasource");
            return List.of(new Reason(String.format(java.util.Locale.ROOT,
                    "Datasource must be defined for persistence unit '%s'. "
                            + "Set the datasource via the '%s' property. "
                            + (reactive ? ""
                                    : "Alternatively, for dynamic datasource selection, set '%s=database'. ")
                            + "Refer to https://quarkus.io/guides/datasource "
                            + (reactive ? "" : "or https://quarkus.io/guides/hibernate-orm#database-approach ")
                            + "for guidance.",
                    name, dsConfigProperty, dsConfigProperty)));
        }
        return List.of();
    }
}
