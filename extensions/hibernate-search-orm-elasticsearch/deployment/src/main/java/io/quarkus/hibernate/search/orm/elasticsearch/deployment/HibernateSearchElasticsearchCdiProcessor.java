package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;

@BuildSteps(onlyIf = HibernateSearchEnabled.class)
public class HibernateSearchElasticsearchCdiProcessor {

    @Record(ExecutionTime.RUNTIME_INIT)
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
                // NOTE: this is using ApplicationScoped and not Singleton, by design, in order to be mockable
                // See https://github.com/quarkusio/quarkus/issues/16437
                .scope(ApplicationScoped.class)
                .unremovable()
                .supplier(supplier)
                .setRuntimeInit();

        if (isDefaultPersistenceUnit) {
            configurator.addQualifier(Default.class);
        }

        configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", persistenceUnitName).done();
        configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", persistenceUnitName).done();

        return configurator.done();
    }

    @BuildStep
    void registerAnnotations(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @SearchExtension class
        // otherwise it won't be registered as qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(ClassNames.SEARCH_EXTENSION.toString())
                .build());

        // Register the default scope for @SearchExtension and make such beans unremovable by default
        // TODO make @SearchExtension beans unremovable only if the corresponding PU actually exists and is enabled
        //   (I think there's a feature request for a configuration property to disable a PU at runtime?)
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ClassNames.SEARCH_EXTENSION, DotNames.APPLICATION_SCOPED,
                        false));
    }
}
