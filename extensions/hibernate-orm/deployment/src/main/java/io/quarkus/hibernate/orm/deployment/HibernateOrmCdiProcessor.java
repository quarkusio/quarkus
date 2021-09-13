package io.quarkus.hibernate.orm.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class HibernateOrmCdiProcessor {

    private static final List<DotName> SESSION_FACTORY_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER_FACTORY,
            ClassNames.SESSION_FACTORY);
    private static final List<DotName> SESSION_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER, ClassNames.SESSION);

    @BuildStep
    AnnotationsTransformerBuildItem convertJpaResourceAnnotationsToQualifier(
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedBlockingPersistenceUnitType) {
        AnnotationsTransformer transformer = new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                // at some point we might want to support METHOD_PARAMETER too but for now getting annotations for them
                // is cumbersome so let's wait for Jandex improvements
                return kind == Kind.FIELD;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                FieldInfo field = transformationContext.getTarget().asField();

                DotName fieldTypeName = field.type().name();
                if (!SESSION_EXPOSED_TYPES.contains(fieldTypeName)
                        && !SESSION_FACTORY_EXPOSED_TYPES.contains(fieldTypeName)) {
                    return;
                }

                DotName jpaAnnotation;
                if (field.hasAnnotation(ClassNames.JPA_PERSISTENCE_UNIT)) {
                    jpaAnnotation = ClassNames.JPA_PERSISTENCE_UNIT;
                } else if (field.hasAnnotation(ClassNames.JPA_PERSISTENCE_CONTEXT)) {
                    jpaAnnotation = ClassNames.JPA_PERSISTENCE_CONTEXT;
                } else {
                    return;
                }

                AnnotationValue persistenceUnitNameAnnotationValue = field.annotation(jpaAnnotation).value("unitName");

                Transformation transformation = transformationContext.transform()
                        .add(DotNames.INJECT);
                if (persistenceUnitNameAnnotationValue == null || persistenceUnitNameAnnotationValue.asString().isEmpty()) {
                    transformation.add(DotNames.DEFAULT);
                } else if (persistenceUnitDescriptors.size() == 1
                        && !impliedBlockingPersistenceUnitType.shouldGenerateImpliedBlockingPersistenceUnit()
                        && persistenceUnitDescriptors.get(0).getPersistenceUnitName()
                                .equals(persistenceUnitNameAnnotationValue.asString())) {
                    // we are in the case where we have only one persistence unit defined in a persistence.xml
                    // in this case, we consider it the default too if the name matches
                    transformation.add(DotNames.DEFAULT);
                } else {
                    transformation.add(ClassNames.QUARKUS_PERSISTENCE_UNIT,
                            AnnotationValue.createStringValue("value", persistenceUnitNameAnnotationValue.asString()));
                }
                transformation.done();
            }
        };

        return new AnnotationsTransformerBuildItem(transformer);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void generateDataSourceBeans(HibernateOrmRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedBlockingPersistenceUnitType,
            List<JdbcDataSourceBuildItem> jdbcDataSources, // just make sure the datasources are initialized
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (persistenceUnitDescriptors.isEmpty()) {
            // No persistence units have been configured so bail out
            return;
        }

        // we have only one persistence unit defined in a persistence.xml: we make it the default even if it has a name
        if (persistenceUnitDescriptors.size() == 1
                && !impliedBlockingPersistenceUnitType.shouldGenerateImpliedBlockingPersistenceUnit()) {

            String persistenceUnitName = persistenceUnitDescriptors.get(0).getPersistenceUnitName();

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            true,
                            SessionFactory.class, SESSION_FACTORY_EXPOSED_TYPES,
                            recorder.sessionFactorySupplier(persistenceUnitName),
                            true));

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            true,
                            Session.class, SESSION_EXPOSED_TYPES,
                            recorder.sessionSupplier(persistenceUnitName),
                            false));

            return;
        }

        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : persistenceUnitDescriptors) {
            String persistenceUnitName = persistenceUnitDescriptor.getPersistenceUnitName();

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName),
                            SessionFactory.class, SESSION_FACTORY_EXPOSED_TYPES,
                            recorder.sessionFactorySupplier(persistenceUnitName),
                            true));

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName),
                            Session.class, SESSION_EXPOSED_TYPES,
                            recorder.sessionSupplier(persistenceUnitName),
                            false));
        }
    }

    @BuildStep
    void registerAnnotations(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @PersistenceUnit and @PersistenceUnitExtension classes
        // otherwise they won't be registered as qualifiers
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(ClassNames.QUARKUS_PERSISTENCE_UNIT.toString(),
                        ClassNames.PERSISTENCE_UNIT_EXTENSION.toString())
                .build());

        // Register the default scope for @PersistenceUnitExtension and make such beans unremovable by default
        // TODO make @PUExtension beans unremovable only if the corresponding PU actually exists and is enabled
        //   (I think there's a feature request for a configuration property to disable a PU at runtime?)
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ClassNames.PERSISTENCE_UNIT_EXTENSION, DotNames.APPLICATION_SCOPED,
                        false));
    }

    private static <T> SyntheticBeanBuildItem createSyntheticBean(String persistenceUnitName, boolean isDefaultPersistenceUnit,
            Class<T> type, List<DotName> allExposedTypes, Supplier<T> supplier, boolean defaultBean) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                // NOTE: this is using ApplicationScope and not Singleton, by design, in order to be mockable
                // See https://github.com/quarkusio/quarkus/issues/16437
                .scope(ApplicationScoped.class)
                .unremovable()
                .supplier(supplier);

        for (DotName exposedType : allExposedTypes) {
            configurator.addType(exposedType);
        }

        if (defaultBean) {
            configurator.defaultBean();
        }

        if (isDefaultPersistenceUnit) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", persistenceUnitName).done();
            configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", persistenceUnitName).done();
        }

        return configurator.done();
    }
}
