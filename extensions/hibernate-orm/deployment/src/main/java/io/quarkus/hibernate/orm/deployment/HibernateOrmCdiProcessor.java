package io.quarkus.hibernate.orm.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;
import jakarta.persistence.AttributeConverter;
import jakarta.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.relational.SchemaManager;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.hibernate.orm.runtime.RequestScopedStatelessSessionHolder;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;
import io.quarkus.hibernate.orm.runtime.cdi.QuarkusArcBeanContainer;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class HibernateOrmCdiProcessor {

    private static final List<DotName> SESSION_FACTORY_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER_FACTORY,
            ClassNames.SESSION_FACTORY);
    private static final List<DotName> SESSION_EXPOSED_TYPES = Arrays.asList(ClassNames.ENTITY_MANAGER, ClassNames.SESSION);
    private static final List<DotName> STATELESS_SESSION_EXPOSED_TYPES = List.of(ClassNames.STATELESS_SESSION);
    private static final List<DotName> CRITERIA_BUILDER_EXPOSED_TYPES = List.of(ClassNames.CRITERIA_BUILDER,
            ClassNames.HIBERNATE_CRITERIA_BUILDER);
    private static final List<DotName> METAMODEL_EXPOSED_TYPES = List.of(ClassNames.METAMODEL);
    private static final List<DotName> SCHEMA_MANAGER_EXPOSED_TYPES = List.of(ClassNames.SCHEMA_MANAGER,
            ClassNames.HIBERNATE_SCHEMA_MANAGER);
    private static final List<DotName> CACHE_EXPOSED_TYPES = List.of(ClassNames.CACHE, ClassNames.HIBERNATE_CACHE);
    private static final List<DotName> PERSISTENCE_UNIT_UTIL_EXPOSED_TYPES = List.of(ClassNames.PERSISTENCE_UNIT_UTIL);

    private static final Set<DotName> PERSISTENCE_UNIT_EXTENSION_VALID_TYPES = Set.of(
            ClassNames.TENANT_RESOLVER,
            ClassNames.TENANT_CONNECTION_RESOLVER,
            ClassNames.INTERCEPTOR,
            ClassNames.STATEMENT_INSPECTOR,
            ClassNames.FORMAT_MAPPER,
            ClassNames.FUNCTION_CONTRIBUTOR);

    @BuildStep
    AnnotationsTransformerBuildItem convertJpaResourceAnnotationsToQualifier(
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedBlockingPersistenceUnitType) {
        AnnotationTransformation transformer = new AnnotationsTransformer() {

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
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(JPAConfig.class)
                .addType(JPAConfig.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .supplier(recorder.jpaConfigSupplier());

        syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    }

    // These beans must be initialized at runtime because their initialization
    // depends on runtime configuration (to activate/deactivate a persistence unit)
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(JdbcDataSourceBuildItem.class) // just make sure the datasources are initialized
    @BuildStep
    void generateHibernateBeans(HibernateOrmRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (persistenceUnitDescriptors.isEmpty()) {
            // No persistence units have been configured so bail out
            return;
        }

        // we have only one persistence unit defined in a persistence.xml: we make it the default even if it has a name
        // NOTE: In this case we know we're not using Hibernate Reactive, because it doesn't support persistence.xml.
        if (persistenceUnitDescriptors.size() == 1 && persistenceUnitDescriptors.get(0).isFromPersistenceXml()) {
            var persistenceUnitDescriptor = persistenceUnitDescriptors.get(0);
            PersistenceUnitReference puRef = new PersistenceUnitReference(
                    persistenceUnitDescriptor.getPersistenceUnitName(),
                    true, recorder.checkActiveSupplier(
                            persistenceUnitDescriptor.getPersistenceUnitName(),
                            persistenceUnitDescriptor.getConfig().getDataSource(),
                            persistenceUnitDescriptor.getConfig().getEntityClassNames(),
                            true));

            produceSessionFactoryBean(syntheticBeanBuildItemBuildProducer, recorder, puRef);

            produceSessionBeans(syntheticBeanBuildItemBuildProducer, recorder, puRef);

            produceFactoryDependentBeans(syntheticBeanBuildItemBuildProducer, recorder, puRef);

            return;
        }

        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : persistenceUnitDescriptors) {
            // We want to register (blocking) SessionFactory/Session/EntityManager beans
            // for blocking persistence units only.
            // The Hibernate Reactive extension handles CDI beans that are specific to Hibernate Reactive.
            if (persistenceUnitDescriptor.isReactive()) {
                continue;
            }

            String persistenceUnitName = persistenceUnitDescriptor.getPersistenceUnitName();
            PersistenceUnitReference puRef = new PersistenceUnitReference(
                    persistenceUnitName,
                    false,
                    recorder.checkActiveSupplier(
                            persistenceUnitDescriptor.getPersistenceUnitName(),
                            persistenceUnitDescriptor.getConfig().getDataSource(),
                            persistenceUnitDescriptor.getConfig().getEntityClassNames(),
                            persistenceUnitDescriptor.isFromPersistenceXml()));

            produceSessionFactoryBean(syntheticBeanBuildItemBuildProducer, recorder, puRef);

            produceSessionBeans(syntheticBeanBuildItemBuildProducer, recorder, puRef);

            produceFactoryDependentBeans(syntheticBeanBuildItemBuildProducer, recorder, puRef);
        }
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<AutoAddScopeBuildItem> autoAddScope,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            Capabilities capabilities,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaModelBuildItem jpaModel) {
        if (descriptors.isEmpty()) {
            return;
        }

        List<Class<?>> unremovableClasses = new ArrayList<>();
        unremovableClasses.add(QuarkusArcBeanContainer.class);

        // The following beans only make sense for Hibernate ORM, not for Hibernate Reactive
        // If transactions are missing, then the application is likely using Hibernate Reactive directly
        // and is definitely not using Hibernate ORM.
        if (capabilities.isPresent(Capability.TRANSACTIONS)) {
            unremovableClasses.add(TransactionManager.class);
            unremovableClasses.add(TransactionSessions.class);
            unremovableClasses.add(RequestScopedSessionHolder.class);
            unremovableClasses.add(RequestScopedStatelessSessionHolder.class);
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(unremovableClasses.toArray(new Class<?>[unremovableClasses.size()]))
                .build());

        // For AttributeConverters and EntityListeners (which are all listed in getPotentialCdiBeanClassNames),
        // we want to achieve the behavior described in the javadoc of QuarkusArcBeanContainer.
        // In particular:
        // 1. They may be retrieved dynamically by Hibernate ORM, so if they are CDI beans, they should not be removed.
        // NOTE: We don't use .unremovable on AutoAddScopeBuildItem, because that would only make beans unremovable
        //       when we automatically add a scope.
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(jpaModel.getPotentialCdiBeanClassNames()));
        // TODO: Remove, this is there for backwards compatibility.
        //  It should only have an effect in edge cases where an attribute converter was imported from
        //  a library not indexed in Jandex, but it's doubtful the attribute converter would work in that case.
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(AttributeConverter.class));
        // 2. Per spec, they may be handled as CDI beans even if they don't have a user-declared scope,
        //    so we need them to default to @Dependent-scoped beans.
        //    See https://github.com/quarkusio/quarkus/issues/50470
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .match((clazz, annotations, index) -> jpaModel.getPotentialCdiBeanClassNames().contains(clazz.name()))
                .defaultScope(BuiltinScope.DEPENDENT)
                // ... but if they don't use CDI, we can safely default to instantiating in Hibernate ORM through reflection.
                .requiresContainerServices()
                .build());
    }

    @BuildStep
    void transformBeans(JpaModelBuildItem jpaModel, JpaModelIndexBuildItem indexBuildItem,
            BeanDiscoveryFinishedBuildItem beans,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        if (persistenceUnitDescriptors.isEmpty()) {
            return;
        }

        // the idea here is to remove the 'private' modifier from all methods that are annotated with JPA Listener methods
        // and don't belong to entities
        CompositeIndex index = indexBuildItem.getIndex();
        for (DotName dotName : jpaModel.getPotentialCdiBeanClassNames()) {
            if (jpaModel.getManagedClassNames().contains(dotName.toString())) {
                continue;
            }
            ClassInfo classInfo = index.getClassByName(dotName);
            List<BeanInfo> matchingBeans = beans.getBeans().stream().filter(bi -> bi.getBeanClass().equals(dotName)).toList();
            if (matchingBeans.size() == 1) {
                ScopeInfo beanScope = matchingBeans.get(0).getScope();
                for (DotName jpaListenerDotName : ClassNames.JPA_LISTENER_ANNOTATIONS) {
                    for (AnnotationInstance annotationInstance : classInfo.annotations(jpaListenerDotName)) {
                        AnnotationTarget target = annotationInstance.target();
                        if (target.kind() != AnnotationTarget.Kind.METHOD) {
                            continue;
                        }
                        MethodInfo method = target.asMethod();
                        if (Modifier.isPrivate(method.flags())) {
                            if (beanScope.getDotName().equals(BuiltinScope.SINGLETON.getName())) {
                                // we can safely transform in this case
                                producer.produce(new BytecodeTransformerBuildItem(method.declaringClass().name().toString(),
                                        new BiFunction<>() {
                                            @Override
                                            public ClassVisitor apply(String cls, ClassVisitor clsVisitor) {
                                                var classTransformer = new ClassTransformer(cls);
                                                classTransformer.modifyMethod(MethodDescriptor.of(method))
                                                        .removeModifiers(Modifier.PRIVATE);
                                                return classTransformer.applyTo(clsVisitor);
                                            }
                                        }));
                            } else {
                                // we can't transform because the client proxy does not know about the transformation and
                                // will therefore simply copy the private method which will then likely fail because it does
                                // not contain the injected fields
                                throw new IllegalArgumentException(
                                        "Methods that are annotated with JPA Listener annotations should not be private. Offending method is '"
                                                + method.declaringClass().name() + "#" + method.name() + "'");
                            }
                        }
                    }
                }
            } else {
                // we don't really know what to do here, just bail and CDI will figure it out
            }
        }
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

    private static <T> ExtendedBeanConfigurator createSyntheticBean(PersistenceUnitReference puRef,
            Class<T> type, List<DotName> allExposedTypes, boolean defaultBean) {
        ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                // NOTE: this is using ApplicationScope and not Singleton, by design, in order to be mockable
                // See https://github.com/quarkusio/quarkus/issues/16437
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit()
                // Note persistence units _actually_ get started a bit earlier, each in its own thread. See JPAConfig#startAll.
                // This startup() call is only necessary in order to trigger Arc's usage checks (fail startup if bean injected when a PU is inactive).
                .startup()
                .checkActive(puRef.checkActiveSupplier);

        for (DotName exposedType : allExposedTypes) {
            configurator.addType(exposedType);
        }

        if (defaultBean) {
            configurator.defaultBean();
        }

        boolean defaultPuName = PersistenceUnitUtil.isDefaultPersistenceUnit(puRef.persistenceUnitName);
        if (defaultPuName || puRef.forceAllQualifiers) {
            configurator.addQualifier(Default.class);
        }

        configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", puRef.persistenceUnitName).done();
        configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", puRef.persistenceUnitName).done();

        return configurator;
    }

    private void produceSessionBeans(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateOrmRecorder recorder,
            PersistenceUnitReference puRef) {

        // Create Session bean
        producer.produce(createSyntheticBean(puRef,
                Session.class, SESSION_EXPOSED_TYPES, false)
                .createWith(recorder.sessionSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                .done());

        // Create StatelessSession bean
        producer.produce(createSyntheticBean(puRef,
                StatelessSession.class, STATELESS_SESSION_EXPOSED_TYPES, false)
                .createWith(recorder.statelessSessionSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                .done());
    }

    private void produceSessionFactoryBean(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateOrmRecorder recorder,
            PersistenceUnitReference puRef) {

        producer.produce(createSyntheticBean(puRef,
                SessionFactory.class, SESSION_FACTORY_EXPOSED_TYPES, true)
                .createWith(recorder.sessionFactorySupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)))
                .done());
    }

    private void produceFactoryDependentBeans(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateOrmRecorder recorder,
            PersistenceUnitReference puRef) {
        AnnotationInstance sessionFactoryQualifier;
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(puRef.persistenceUnitName)) {
            sessionFactoryQualifier = AnnotationInstance.builder(Default.class).build();
        } else {
            sessionFactoryQualifier = AnnotationInstance
                    .builder(PersistenceUnit.class).add("value", puRef.persistenceUnitName).build();
        }

        // Create CriteriaBuilder bean
        producer.produce(createSyntheticBean(puRef,
                HibernateCriteriaBuilder.class, CRITERIA_BUILDER_EXPOSED_TYPES, false)
                .createWith(recorder.criteriaBuilderSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create Metamodel bean
        producer.produce(createSyntheticBean(puRef,
                jakarta.persistence.metamodel.Metamodel.class, METAMODEL_EXPOSED_TYPES, false)
                .createWith(recorder.metamodelSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create SchemaManager bean
        producer.produce(createSyntheticBean(puRef,
                SchemaManager.class, SCHEMA_MANAGER_EXPOSED_TYPES, false)
                .createWith(recorder.schemaManagerSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create Cache bean
        producer.produce(createSyntheticBean(puRef,
                org.hibernate.Cache.class, CACHE_EXPOSED_TYPES, false)
                .createWith(recorder.cacheSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create PersistenceUnitUtil bean
        producer.produce(createSyntheticBean(puRef,
                jakarta.persistence.PersistenceUnitUtil.class, PERSISTENCE_UNIT_UTIL_EXPOSED_TYPES, false)
                .createWith(recorder.persistenceUnitUtilSupplier(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());
    }

    private record PersistenceUnitReference(
            String persistenceUnitName,
            boolean forceAllQualifiers,
            Supplier<ActiveResult> checkActiveSupplier) {
    }
}
