package io.quarkus.hibernate.reactive.deployment.component;

import java.util.List;

import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionBuildItem;
import io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionSupport;
import io.quarkus.hibernate.orm.deployment.spi.component.PersistenceUnitLookupBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.component.PersistenceUnitRequestBuildItem;
import io.quarkus.hibernate.reactive.deployment.HibernateReactiveEnabled;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Handles the lifecycle of {@link ProgrammingParadigm#REACTIVE reactive} persistence units:
 * collecting requests, producing definitions, and declaring the resulting datasource needs.
 * <p>
 * A <b>request</b> ({@link PersistenceUnitRequestBuildItem}) declares that a persistence unit
 * is needed, carrying a name, a {@link ProgrammingParadigm}, and a {@link Reason} explaining why.
 * A <b>definition</b> ({@link PersistenceUnitDefinitionBuildItem}) is produced by checking
 * each request against the {@link PersistenceUnitLookupBuildItem lookup}: if the lookup deems
 * the persistence unit unavailable, a {@code ConfigurationException} is thrown with the full
 * reason chain; otherwise a definition is produced for downstream consumption.
 *
 * @see io.quarkus.hibernate.orm.deployment.component.PersistenceUnitLookupProcessor
 * @see io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionBlockingProcessor
 */
@BuildSteps(onlyIf = HibernateReactiveEnabled.class)
class PersistenceUnitDefinitionReactiveProcessor {

    @BuildStep
    void collectImplicitReactivePersistenceUnitRequests(Capabilities capabilities, HibernateOrmConfig config,
            JpaModelPerPersistenceUnitBuildItem jpaModelPerPersistenceUnit,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            BuildProducer<PersistenceUnitRequestBuildItem> puRequests) {
        PersistenceUnitDefinitionSupport.collectPersistenceUnitRequestsFromConfiguration(ProgrammingParadigm.REACTIVE,
                capabilities, config,
                jpaModelPerPersistenceUnit, lookupBuildItem, puRequests);

        // We don't derive requests from injection points of persistence unit related beans,
        // because those could just be referencing custom beans,
        // as we suggest in https://quarkus.io/guides/hibernate-orm#persistence-unit-active
        // TODO https://github.com/quarkusio/quarkus/issues/55217
        //  Find a way to collect injection points for a given PU that have no matching user-defined producer
    }

    @BuildStep
    void defineReactivePersistenceUnits(
            HibernateOrmConfig hibernateOrmConfig,
            PersistenceUnitLookupBuildItem lookupBuildItem,
            List<PersistenceUnitRequestBuildItem> puRequests,
            BuildProducer<PersistenceUnitDefinitionBuildItem> persistenceUnitDefinitions) {
        PersistenceUnitDefinitionSupport.definePersistenceUnits(ProgrammingParadigm.REACTIVE, hibernateOrmConfig,
                lookupBuildItem,
                puRequests, List.of(), List.of(), persistenceUnitDefinitions);
    }

    @BuildStep
    public void collectDatasourceReferencesFromPersistenceUnits(
            List<PersistenceUnitDefinitionBuildItem> puDefinitions,
            BuildProducer<DataSourceRequestBuildItem> datasourceReferences) {
        for (PersistenceUnitDefinitionBuildItem puDefinition : puDefinitions) {
            if (!ProgrammingParadigm.REACTIVE.equals(puDefinition.getParadigm())
                    || puDefinition.getDataSourceName().isEmpty()) {
                continue;
            }
            Reason reason = new Reason(
                    "Hibernate Reactive persistence unit '" + puDefinition.getPersistenceUnitName() + "'",
                    puDefinition.getReasons());
            datasourceReferences.produce(new DataSourceRequestBuildItem(puDefinition.getDataSourceName().get(),
                    ProgrammingParadigm.REACTIVE, reason));
        }
    }

}
