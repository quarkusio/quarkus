package io.quarkus.hibernate.search.orm.elasticsearch;

import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;

import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;

public class HibernateSearchElasticsearchCdiProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void generateSearchBeans(HibernateSearchElasticsearchRecorder recorder,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem persistenceUnit : configuredPersistenceUnits) {
            String persistenceUnitName = persistenceUnit.getPersistenceUnitName();

            boolean isDefaultPersistenceUnit = PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName);
            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            isDefaultPersistenceUnit,
                            SearchMapping.class,
                            recorder.searchMappingSupplier(persistenceUnitName, isDefaultPersistenceUnit)));

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName),
                            SearchSession.class,
                            recorder.searchSessionSupplier(persistenceUnitName, isDefaultPersistenceUnit)));
        }
    }

    private static <T> SyntheticBeanBuildItem createSyntheticBean(String persistenceUnitName, boolean isDefaultPersistenceUnit,
            Class<T> type, Supplier<T> supplier) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .scope(Singleton.class)
                .unremovable()
                .supplier(supplier);

        if (isDefaultPersistenceUnit) {
            configurator.addQualifier(Default.class);
        }

        configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", persistenceUnitName).done();
        configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", persistenceUnitName).done();

        return configurator.done();
    }
}
