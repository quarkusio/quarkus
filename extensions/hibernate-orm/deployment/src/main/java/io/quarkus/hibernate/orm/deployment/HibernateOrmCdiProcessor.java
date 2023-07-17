package io.quarkus.hibernate.orm.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class HibernateOrmCdiProcessor {

    private static final List<DotName> SESSION_FACTORY_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER_FACTORY,
            ClassNames.SESSION_FACTORY);
    private static final List<DotName> SESSION_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER, ClassNames.SESSION);
    private static final List<DotName> STATELESS_SESSION_EXPOSED_TYPES = List.of(ClassNames.STATELESS_SESSION);

    private static final Set<DotName> PERSISTENCE_UNIT_EXTENSION_VALID_TYPES = Set.of(
            ClassNames.TENANT_RESOLVER,
            ClassNames.TENANT_CONNECTION_RESOLVER,
            ClassNames.INTERCEPTOR,
            ClassNames.STATEMENT_INSPECTOR);

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

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateJpaConfigBean(HibernateOrmRecorder recorder,
            Capabilities capabilities,
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(JPAConfig.class)
                .addType(JPAConfig.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .supplier(recorder.jpaConfigSupplier(hibernateOrmRuntimeConfig))
                .destroyer(JPAConfig.Destroyer.class);

        // Add a synthetic dependency from JPAConfig to any datasource/pool,
        // so that JPAConfig is destroyed before the datasource/pool.
        // The alternative would be adding an application destruction observer
        // (@Observes @BeforeDestroyed(ApplicationScoped.class)) to JPAConfig,
        // but that would force initialization of JPAConfig upon application shutdown,
        // which may cause cascading failures if the shutdown happened before JPAConfig was initialized.
        if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
            configurator.addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                    new Type[] { ClassType.create(DotName.createSimple("io.vertx.sqlclient.Pool")) }, null),
                    AnnotationInstance.builder(Any.class).build());
        } else {
            configurator.addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                    new Type[] { ClassType.create(DotName.createSimple(DataSources.class)) }, null),
                    AnnotationInstance.builder(Any.class).build());
        }

        syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    }

    // These beans must be initialized at runtime because their initialization
    // depends on runtime configuration (to activate/deactivate a persistence unit)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateDataSourceBeans(HibernateOrmRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedBlockingPersistenceUnitType,
            List<JdbcDataSourceBuildItem> jdbcDataSources, // just make sure the datasources are initialized
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (persistenceUnitDescriptors.isEmpty()) {
            // No persistence units have been configured so bail out
            return;
        }

        // we have only one persistence unit defined in a persistence.xml: we make it the default even if it has a name
        if (persistenceUnitDescriptors.size() == 1 && persistenceUnitDescriptors.get(0).isFromPersistenceXml()) {
            String persistenceUnitName = persistenceUnitDescriptors.get(0).getPersistenceUnitName();

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            true, true,
                            SessionFactory.class, SESSION_FACTORY_EXPOSED_TYPES, true)
                            .createWith(recorder.sessionFactorySupplier(persistenceUnitName))
                            .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)))
                            .done());

            if (capabilities.isPresent(Capability.TRANSACTIONS)) {
                // Do register a Session/EntityManager bean only if JTA is available
                // Note that the Hibernate Reactive extension excludes JTA intentionally
                syntheticBeanBuildItemBuildProducer
                        .produce(createSyntheticBean(persistenceUnitName,
                                true, true,
                                Session.class, SESSION_EXPOSED_TYPES, false)
                                .createWith(recorder.sessionSupplier(persistenceUnitName))
                                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                                .done());

                // same for StatelessSession
                syntheticBeanBuildItemBuildProducer
                        .produce(createSyntheticBean(persistenceUnitName,
                                true, true,
                                StatelessSession.class, STATELESS_SESSION_EXPOSED_TYPES, false)
                                .createWith(recorder.statelessSessionSupplier(persistenceUnitName))
                                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                                .done());
            }
            return;
        }

        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : persistenceUnitDescriptors) {
            String persistenceUnitName = persistenceUnitDescriptor.getPersistenceUnitName();
            // Hibernate Reactive does not use the same name for its default persistence unit,
            // but we still want to use the @Default qualifier for that PU.
            // We will need to fix this at some point, see https://github.com/quarkusio/quarkus/issues/21110
            String persistenceUnitConfigName = persistenceUnitDescriptor.getConfigurationName();
            boolean isDefaultPU = PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitConfigName);
            boolean isNamedPU = !isDefaultPU;

            syntheticBeanBuildItemBuildProducer
                    .produce(createSyntheticBean(persistenceUnitName,
                            isDefaultPU, isNamedPU,
                            SessionFactory.class, SESSION_FACTORY_EXPOSED_TYPES, true)
                            .createWith(recorder.sessionFactorySupplier(persistenceUnitName))
                            .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)))
                            .done());

            if (capabilities.isPresent(Capability.TRANSACTIONS)) {
                // Do register a Session/EntityManager bean only if JTA is available
                // Note that the Hibernate Reactive extension excludes JTA intentionally
                syntheticBeanBuildItemBuildProducer
                        .produce(createSyntheticBean(persistenceUnitName,
                                isDefaultPU, isNamedPU,
                                Session.class, SESSION_EXPOSED_TYPES, false)
                                .createWith(recorder.sessionSupplier(persistenceUnitName))
                                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                                .done());

                // same for StatelessSession
                syntheticBeanBuildItemBuildProducer
                        .produce(createSyntheticBean(persistenceUnitName,
                                true, true,
                                StatelessSession.class, STATELESS_SESSION_EXPOSED_TYPES, false)
                                .createWith(recorder.statelessSessionSupplier(persistenceUnitName))
                                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                                .done());
            }
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
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ClassNames.PERSISTENCE_UNIT_EXTENSION, DotNames.APPLICATION_SCOPED,
                        false));
    }

    @BuildStep
    void validatePersistenceUnitExtensions(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        List<Throwable> throwables = validationPhase.getContext()
                .beans().withQualifier(ClassNames.PERSISTENCE_UNIT_EXTENSION)
                .filter(beanInfo -> beanInfo.getTypes().stream().map(Type::name)
                        .noneMatch(PERSISTENCE_UNIT_EXTENSION_VALID_TYPES::contains))
                .stream().map(beanInfo -> new IllegalStateException(String.format(Locale.ROOT,
                        "A @%s bean must implement one or more of the following types: %s. Invalid bean: %s",
                        DotNames.simpleName(ClassNames.PERSISTENCE_UNIT_EXTENSION),
                        PERSISTENCE_UNIT_EXTENSION_VALID_TYPES,
                        beanInfo)))
                .collect(Collectors.toList());
        if (!throwables.isEmpty()) {
            errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(throwables));
        }
    }

    private static <T> ExtendedBeanConfigurator createSyntheticBean(String persistenceUnitName,
            boolean isDefaultPersistenceUnit, boolean isNamedPersistenceUnit,
            Class<T> type, List<DotName> allExposedTypes, boolean defaultBean) {
        ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                // NOTE: this is using ApplicationScope and not Singleton, by design, in order to be mockable
                // See https://github.com/quarkusio/quarkus/issues/16437
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit();

        for (DotName exposedType : allExposedTypes) {
            configurator.addType(exposedType);
        }

        if (defaultBean) {
            configurator.defaultBean();
        }

        if (isDefaultPersistenceUnit) {
            configurator.addQualifier(Default.class);
        }
        if (isNamedPersistenceUnit) {
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", persistenceUnitName).done();
            configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", persistenceUnitName).done();
        }

        return configurator;
    }
}
