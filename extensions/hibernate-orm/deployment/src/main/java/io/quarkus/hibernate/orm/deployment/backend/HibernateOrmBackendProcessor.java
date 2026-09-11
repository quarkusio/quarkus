package io.quarkus.hibernate.orm.deployment.backend;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.hibernate.orm.deployment.component.HibernateOrmClientLookupBuildItem;
import io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.client.HibernateOrmClientHandlerBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.client.HibernateOrmClientRequestBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Assembles the client lookup from handler contributions,
 * and produces datasource/client requests from persistence unit definitions.
 *
 * @see io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionBlockingProcessor
 * @see io.quarkus.hibernate.reactive.deployment.component.PersistenceUnitDefinitionReactiveProcessor
 */
class HibernateOrmBackendProcessor {

    @BuildStep
    HibernateOrmClientLookupBuildItem assembleClientLookup(
            List<HibernateOrmClientHandlerBuildItem> handlers) {
        ComponentLookup aggregated = (name, paradigm) -> {
            List<Reason> allReasons = new ArrayList<>();
            for (var handler : handlers) {
                List<Reason> reasons = handler.getLookup().unavailableReasons(name, paradigm);
                if (reasons.isEmpty()) {
                    return List.of();
                }
                allReasons.addAll(reasons);
            }
            if (allReasons.isEmpty()) {
                return List.of(new Reason(
                        "No client extension is available."
                                + " Add an extension that provides external clients"
                                + " (e.g. quarkus-mongodb-hibernate)."));
            }
            return allReasons;
        };
        return new HibernateOrmClientLookupBuildItem(aggregated);
    }

    @BuildStep
    void produceDataSourceRequests(
            List<PersistenceUnitDefinitionBuildItem> puDefinitions,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        for (PersistenceUnitDefinitionBuildItem puDefinition : puDefinitions) {
            if (puDefinition.getDataSourceName().isEmpty()) {
                continue;
            }
            Reason reason = new Reason(
                    String.format(java.util.Locale.ROOT,
                            "Hibernate %s persistence unit '%s'",
                            switch (puDefinition.getParadigm()) {
                                case BLOCKING -> "ORM";
                                case REACTIVE -> "Reactive";
                            },
                            puDefinition.getPersistenceUnitName()),
                    puDefinition.getReasons());
            dataSourceRequests.produce(new DataSourceRequestBuildItem(puDefinition.getDataSourceName().get(),
                    puDefinition.getParadigm(), reason));
        }
    }

    @BuildStep
    void produceClientRequests(
            List<PersistenceUnitDefinitionBuildItem> puDefinitions,
            BuildProducer<HibernateOrmClientRequestBuildItem> clientRequests) {
        for (PersistenceUnitDefinitionBuildItem puDefinition : puDefinitions) {
            if (!ProgrammingParadigm.BLOCKING.equals(puDefinition.getParadigm())
                    || puDefinition.getClientName().isEmpty()) {
                continue;
            }
            Reason reason = new Reason(
                    "Hibernate ORM persistence unit '" + puDefinition.getPersistenceUnitName() + "'",
                    puDefinition.getReasons());
            clientRequests.produce(new HibernateOrmClientRequestBuildItem(
                    puDefinition.getClientName().get(),
                    ProgrammingParadigm.BLOCKING, reason));
        }
    }
}
