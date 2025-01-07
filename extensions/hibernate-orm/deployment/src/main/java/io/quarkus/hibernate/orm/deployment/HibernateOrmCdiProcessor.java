package io.quarkus.hibernate.orm.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.persistence.AttributeConverter;
import jakarta.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
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
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.hibernate.orm.runtime.RequestScopedStatelessSessionHolder;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;
import io.quarkus.hibernate.orm.runtime.cdi.QuarkusArcBeanContainer;
import io.quarkus.runtime.ShutdownEvent;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class HibernateOrmCdiProcessor {

    private static final int JPA_CONFIG_SHUTDOWN_PRIORITY = Interceptor.Priority.LIBRARY_AFTER + 100;
    private static final List<DotName> SESSION_FACTORY_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER_FACTORY,
            ClassNames.SESSION_FACTORY);
    private static final List<DotName> SESSION_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER, ClassNames.SESSION);
    private static final List<DotName> STATELESS_SESSION_EXPOSED_TYPES = List.of(ClassNames.STATELESS_SESSION);

    private static final Set<DotName> PERSISTENCE_UNIT_EXTENSION_VALID_TYPES = Set.of(
            ClassNames.TENANT_RESOLVER,
            ClassNames.TENANT_CONNECTION_RESOLVER,
            ClassNames.INTERCEPTOR,
            ClassNames.STATEMENT_INSPECTOR,
            ClassNames.FORMAT_MAPPER);

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
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(JPAConfig.class)
                .addType(JPAConfig.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .supplier(recorder.jpaConfigSupplier(hibernateOrmRuntimeConfig));

        syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateJpaConfigBeanObserver(
            HibernateOrmRecorder recorder,
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem> observerConfigurationRegistry) {
        observerConfigurationRegistry.produce(
                new ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem(observerRegistrationPhase.getContext()
                        .configure()
                        .beanClass(DotName.createSimple("io.quarkus.hibernate.orm.runtime.JPAConfig"))
                        .observedType(ShutdownEvent.class)
                        .priority(JPA_CONFIG_SHUTDOWN_PRIORITY)
                        .notify(mc -> {
                            // Essentially do the following:
                            // Arc.container().instance( JPAConfig.class ).get().shutdown();
                            ResultHandle arcContainer = mc.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
                            ResultHandle jpaConfigInstance = mc.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class,
                                            Class.class, Annotation[].class),
                                    arcContainer,
                                    mc.loadClassFromTCCL(JPAConfig.class),
                                    mc.newArray(Annotation.class, 0));
                            ResultHandle jpaConfig = mc.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                                    jpaConfigInstance);

                            mc.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(JPAConfig.class, "shutdown", void.class),
                                    jpaConfig);

                            mc.returnValue(null);
                        })));
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

            if (capabilities.isPresent(Capability.TRANSACTIONS)
                    && capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
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

            if (capabilities.isPresent(Capability.TRANSACTIONS)
                    && capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
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
    void registerBeans(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            Capabilities capabilities,
            CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaModelBuildItem jpaModel) {
        if (!HibernateOrmProcessor.hasEntities(jpaModel)) {
            return;
        }

        List<Class<?>> unremovableClasses = new ArrayList<>();
        unremovableClasses.add(QuarkusArcBeanContainer.class);

        if (capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
            // The following beans only make sense for Hibernate ORM, not for Hibernate Reactive

            if (capabilities.isPresent(Capability.TRANSACTIONS)) {
                unremovableClasses.add(TransactionManager.class);
                unremovableClasses.add(TransactionSessions.class);
            }
            unremovableClasses.add(RequestScopedSessionHolder.class);
            unremovableClasses.add(RequestScopedStatelessSessionHolder.class);
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(unremovableClasses.toArray(new Class<?>[unremovableClasses.size()]))
                .build());

        // Some user-injectable beans are retrieved programmatically and shouldn't be removed
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(AttributeConverter.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(jpaModel.getPotentialCdiBeanClassNames()));
    }

    @BuildStep
    void registerAnnotations(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @PersistenceUnit, @PersistenceUnitExtension, @JsonFormat and @XmlFormat classes
        // otherwise they won't be registered as qualifiers
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(ClassNames.QUARKUS_PERSISTENCE_UNIT.toString(),
                        ClassNames.PERSISTENCE_UNIT_EXTENSION.toString(),
                        ClassNames.JSON_FORMAT.toString(),
                        ClassNames.XML_FORMAT.toString())
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
