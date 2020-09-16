package io.quarkus.hibernate.orm.deployment;

import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
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

    private static final DotName PERSISTENCE_UNIT_QUALIFIER = DotName.createSimple(PersistenceUnit.class.getName());

    private static final DotName ENTITY_MANAGER_FACTORY = DotName.createSimple(EntityManagerFactory.class.getName());
    private static final DotName JPA_PERSISTENCE_UNIT = DotName.createSimple(javax.persistence.PersistenceUnit.class.getName());

    private static final DotName ENTITY_MANAGER = DotName.createSimple(EntityManager.class.getName());
    private static final DotName JPA_PERSISTENCE_CONTEXT = DotName
            .createSimple(javax.persistence.PersistenceContext.class.getName());

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

                if (!ENTITY_MANAGER.equals(field.type().name()) && !ENTITY_MANAGER_FACTORY.equals(field.type().name())) {
                    return;
                }

                DotName jpaAnnotation;
                if (field.hasAnnotation(JPA_PERSISTENCE_UNIT)) {
                    jpaAnnotation = JPA_PERSISTENCE_UNIT;
                } else if (field.hasAnnotation(JPA_PERSISTENCE_CONTEXT)) {
                    jpaAnnotation = JPA_PERSISTENCE_CONTEXT;
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
                    transformation.add(PERSISTENCE_UNIT_QUALIFIER,
                            AnnotationValue.createStringValue("value", persistenceUnitNameAnnotationValue.asString()));
                }
                transformation.done();
            }
        };

        return new AnnotationsTransformerBuildItem(transformer);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
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

        // add the @PersistenceUnit class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(PersistenceUnit.class).build());

        // we have only one persistence unit defined in a persistence.xml: we make it the default even if it has a name
        if (persistenceUnitDescriptors.size() == 1
                && !impliedBlockingPersistenceUnitType.shouldGenerateImpliedBlockingPersistenceUnit()) {

            String persistenceUnitName = persistenceUnitDescriptors.get(0).getPersistenceUnitName();

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            true,
                            EntityManagerFactory.class,
                            recorder.entityManagerFactorySupplier(persistenceUnitName),
                            true));

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            true,
                            EntityManager.class,
                            recorder.entityManagerSupplier(persistenceUnitName),
                            false));

            return;
        }

        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : persistenceUnitDescriptors) {
            String persistenceUnitName = persistenceUnitDescriptor.getPersistenceUnitName();

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName),
                            EntityManagerFactory.class,
                            recorder.entityManagerFactorySupplier(persistenceUnitName),
                            true));

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName),
                            EntityManager.class,
                            recorder.entityManagerSupplier(persistenceUnitName),
                            false));
        }
    }

    private static <T> SyntheticBeanBuildItem createSyntheticBean(String persistenceUnitName, boolean isDefaultPersistenceUnit,
            Class<T> type, Supplier<T> supplier, boolean defaultBean) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .scope(Singleton.class)
                .setRuntimeInit()
                .unremovable()
                .supplier(supplier);

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
