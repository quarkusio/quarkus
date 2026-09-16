package io.quarkus.hibernate.orm.deployment.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            DataSourceLookupBuildItem dataSourceLookupBuildItem) {
        var dataSourceLookup = dataSourceLookupBuildItem.getLookup();
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
                        checkDataSourceUnavailable(name, paradigm, dataSourceLookup, config, additionalPersistenceUnits));
                return unavailableReasons;
            }
        });
    }

    private static List<Reason> checkBlockingDisabled(String name, HibernateOrmConfig config, boolean blockingEnabled) {
        var reasons = new ArrayList<Reason>();
        if (!blockingEnabled) {
            reasons.add(new Reason(String.format(Locale.ROOT,
                    "Hibernate ORM was disabled explicitly by setting '%s' to 'false'",
                    HibernateOrmRuntimeConfig.puPropertyKey(name, "blocking"))));
        }
        if (!config.persistenceUnits().get(name).jdbc().enabled().orElse(true)) {
            reasons.add(new Reason(String.format(Locale.ROOT,
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
            reasons.add(new Reason(String.format(Locale.ROOT,
                    "Hibernate Reactive was disabled explicitly by setting '%s' to 'false'",
                    HibernateOrmRuntimeConfig.puPropertyKey(name, "reactive.enabled"))));
        }
        return reasons;
    }

    private static List<Reason> checkDataSourceUnavailable(String name, ProgrammingParadigm paradigm,
            ComponentLookup dataSourceLookup, HibernateOrmConfig config,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits) {
        var reasons = new ArrayList<Reason>();
        Optional<String> dataSourceName = additionalPersistenceUnits.stream()
                .filter(item -> item.getPersistenceUnitName().equals(name))
                .findFirst()
                .flatMap(AdditionalPersistenceUnitBuildItem::getDataSourceName)
                .or(() -> HibernateProcessorUtil.getDataSourceName(config, name));
        if (dataSourceName.isPresent()) {
            List<Reason> dataSourceUnavailableReason = dataSourceLookup.unavailableReasons(dataSourceName.get(),
                    paradigm);
            if (!dataSourceUnavailableReason.isEmpty()) {
                reasons.add(new Reason(
                        String.format(Locale.ROOT, "%s datasource '%s' cannot be created",
                                switch (paradigm) {
                                    case BLOCKING -> "JDBC";
                                    case REACTIVE -> "Reactive";
                                },
                                dataSourceName.get()),
                        dataSourceUnavailableReason));
            }
        } else {
            reasons.addAll(checkMissingDataSource(name, paradigm, config));
        }
        return reasons;
    }

    private static List<Reason> checkMissingDataSource(String name, ProgrammingParadigm paradigm,
            HibernateOrmConfig config) {
        MultiTenancyStrategy multiTenancyStrategy = HibernateProcessorUtil.getMultiTenancyStrategy(
                config.persistenceUnits().get(name).multitenant());
        // Reactive does not support multitenancy so we always require a datasource (explicit or implied)
        // See https://github.com/quarkusio/quarkus/issues/15959
        boolean reactive = ProgrammingParadigm.REACTIVE.equals(paradigm);
        if (reactive || multiTenancyStrategy != MultiTenancyStrategy.DATABASE) {
            String dsConfigProperty = HibernateOrmRuntimeConfig.puPropertyKey(name, "datasource");
            return List.of(new Reason(String.format(Locale.ROOT,
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
