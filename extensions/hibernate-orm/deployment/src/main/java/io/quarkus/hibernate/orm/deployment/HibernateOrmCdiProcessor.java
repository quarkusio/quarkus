package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.hasEntities;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
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
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
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
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
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
    private static final List<DotName> SCHEMA_MANAGER_EXPOSED_TYPES = List.of(ClassNames.SCHEMA_MANAGER);
    private static final List<DotName> CACHE_EXPOSED_TYPES = List.of(ClassNames.CACHE, ClassNames.HIBERNATE_CACHE);
    private static final List<DotName> PERSISTENCE_UNIT_UTIL_EXPOSED_TYPES = List.of(ClassNames.PERSISTENCE_UNIT_UTIL);

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
                    new Type[] { ClassType.create(DotName.createSimple(AgroalDataSource.class)) }, null),
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

        Function<String, AnnotationInstance> createPersistenceUnitQualifier = (puName) -> AnnotationInstance
                .builder(PersistenceUnit.class).add("value", puName).build();
        AnnotationInstance defaultQualifierInstance = AnnotationInstance.builder(Default.class).build();

        // we have only one persistence unit defined in a persistence.xml: we make it the default even if it has a name
        // NOTE: In this case we know we're not using Hibernate Reactive, because it doesn't support persistence.xml.
        if (persistenceUnitDescriptors.size() == 1 && persistenceUnitDescriptors.get(0).isFromPersistenceXml()) {
            String persistenceUnitName = persistenceUnitDescriptors.get(0).getPersistenceUnitName();

            produceSessionFactoryBean(syntheticBeanBuildItemBuildProducer, recorder, persistenceUnitName, true, true);

            produceSessionBeans(syntheticBeanBuildItemBuildProducer, recorder, persistenceUnitName, true, true);

            produceFactoryDependentBeans(syntheticBeanBuildItemBuildProducer, recorder, persistenceUnitName,
                    true, true, defaultQualifierInstance);

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
            // Hibernate Reactive does not use the same name for its default persistence unit,
            // but we still want to use the @Default qualifier for that PU.
            // We will need to fix this at some point, see https://github.com/quarkusio/quarkus/issues/21110
            String persistenceUnitConfigName = persistenceUnitDescriptor.getConfigurationName();
            boolean isDefaultPU = PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitConfigName);
            boolean isNamedPU = isFalse(isDefaultPU);
            AnnotationInstance sessionFactoryQualifier;
            if (isDefaultPU) {
                sessionFactoryQualifier = defaultQualifierInstance;
            } else {
                sessionFactoryQualifier = createPersistenceUnitQualifier.apply(persistenceUnitName);
            }

            produceSessionFactoryBean(syntheticBeanBuildItemBuildProducer, recorder, persistenceUnitName, isDefaultPU,
                    isNamedPU);

            produceSessionBeans(syntheticBeanBuildItemBuildProducer, recorder, persistenceUnitName, isDefaultPU, isNamedPU);

            produceFactoryDependentBeans(syntheticBeanBuildItemBuildProducer, recorder, persistenceUnitName,
                    isDefaultPU, isNamedPU, sessionFactoryQualifier);

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
        if (!hasEntities(jpaModel)) {
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

        // Some user-injectable beans are retrieved programmatically and shouldn't be removed
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(AttributeConverter.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(jpaModel.getPotentialCdiBeanClassNames()));
    }

    @BuildStep
    void transformBeans(JpaModelBuildItem jpaModel, JpaModelIndexBuildItem indexBuildItem,
            BeanDiscoveryFinishedBuildItem beans,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        if (!hasEntities(jpaModel)) {
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

    private void produceSessionBeans(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateOrmRecorder recorder,
            String persistenceUnitName,
            boolean isDefaultPU,
            boolean isNamedPU) {

        // Create Session bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                Session.class, SESSION_EXPOSED_TYPES, false)
                .createWith(recorder.sessionSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                .done());

        // Create StatelessSession bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                StatelessSession.class, STATELESS_SESSION_EXPOSED_TYPES, false)
                .createWith(recorder.statelessSessionSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSessions.class)))
                .done());
    }

    private void produceSessionFactoryBean(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateOrmRecorder recorder,
            String persistenceUnitName,
            boolean isDefaultPU,
            boolean isNamedPU) {

        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                SessionFactory.class, SESSION_FACTORY_EXPOSED_TYPES, true)
                .createWith(recorder.sessionFactorySupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)))
                .done());
    }

    private void produceFactoryDependentBeans(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateOrmRecorder recorder,
            String persistenceUnitName,
            boolean isDefaultPU,
            boolean isNamedPU,
            AnnotationInstance sessionFactoryQualifier) {

        // Create CriteriaBuilder bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                HibernateCriteriaBuilder.class, CRITERIA_BUILDER_EXPOSED_TYPES, false)
                .createWith(recorder.criteriaBuilderSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create Metamodel bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                jakarta.persistence.metamodel.Metamodel.class, METAMODEL_EXPOSED_TYPES, false)
                .createWith(recorder.metamodelSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create SchemaManager bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                SchemaManager.class, SCHEMA_MANAGER_EXPOSED_TYPES, false)
                .createWith(recorder.schemaManagerSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create Cache bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                org.hibernate.Cache.class, CACHE_EXPOSED_TYPES, false)
                .createWith(recorder.cacheSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());

        // Create PersistenceUnitUtil bean
        producer.produce(createSyntheticBean(persistenceUnitName,
                isDefaultPU, isNamedPU,
                jakarta.persistence.PersistenceUnitUtil.class, PERSISTENCE_UNIT_UTIL_EXPOSED_TYPES, false)
                .createWith(recorder.persistenceUnitUtilSupplier(persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(SessionFactory.class)),
                        sessionFactoryQualifier)
                .done());
    }

}
