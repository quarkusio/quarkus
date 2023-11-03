package io.quarkus.hibernate.validator.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.annotation.Repeatable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;
import jakarta.validation.ClockProvider;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ValidateOnExecution;
import jakarta.validation.valueextraction.ValueExtractor;
import jakarta.ws.rs.Priorities;

import org.hibernate.validator.internal.metadata.core.ConstraintHelper;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigClassBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import io.quarkus.hibernate.validator.runtime.DisableLoggingFeature;
import io.quarkus.hibernate.validator.runtime.HibernateBeanValidationConfigValidator;
import io.quarkus.hibernate.validator.runtime.HibernateValidatorBuildTimeConfig;
import io.quarkus.hibernate.validator.runtime.HibernateValidatorRecorder;
import io.quarkus.hibernate.validator.runtime.ValidationSupport;
import io.quarkus.hibernate.validator.runtime.ValidatorProvider;
import io.quarkus.hibernate.validator.runtime.interceptor.MethodValidationInterceptor;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyConfigSupport;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationExceptionMapper;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyViolationExceptionMapper;
import io.quarkus.hibernate.validator.runtime.jaxrs.ViolationReport;
import io.quarkus.hibernate.validator.spi.BeanValidationAnnotationsBuildItem;
import io.quarkus.jaxrs.spi.deployment.AdditionalJaxRsResourceMethodAnnotationsBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyConfigBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.validator.BeanValidationConfigValidator;

class HibernateValidatorProcessor {

    private static final String META_INF_VALIDATION_XML = "META-INF/validation.xml";

    private static final DotName CONSTRAINT_VALIDATOR_FACTORY = DotName
            .createSimple(ConstraintValidatorFactory.class.getName());
    private static final DotName MESSAGE_INTERPOLATOR = DotName.createSimple(MessageInterpolator.class.getName());
    private static final DotName LOCALE_RESOLVER = DotName.createSimple(LocaleResolver.class.getName());

    private static final DotName TRAVERSABLE_RESOLVER = DotName.createSimple(TraversableResolver.class.getName());
    private static final DotName PARAMETER_NAME_PROVIDER = DotName.createSimple(ParameterNameProvider.class.getName());
    private static final DotName CLOCK_PROVIDER = DotName.createSimple(ClockProvider.class.getName());
    private static final DotName SCRIPT_EVALUATOR_FACTORY = DotName.createSimple(ScriptEvaluatorFactory.class.getName());
    private static final DotName GETTER_PROPERTY_SELECTION_STRATEGY = DotName
            .createSimple(GetterPropertySelectionStrategy.class.getName());
    private static final DotName PROPERTY_NODE_NAME_PROVIDER = DotName
            .createSimple(PropertyNodeNameProvider.class.getName());

    private static final DotName VALIDATOR_FACTORY_CUSTOMIZER = DotName
            .createSimple(ValidatorFactoryCustomizer.class.getName());

    private static final DotName CONSTRAINT_VALIDATOR = DotName.createSimple(ConstraintValidator.class.getName());
    private static final DotName VALUE_EXTRACTOR = DotName.createSimple(ValueExtractor.class.getName());

    private static final DotName VALIDATE_ON_EXECUTION = DotName.createSimple(ValidateOnExecution.class.getName());

    private static final DotName VALID = DotName.createSimple(Valid.class.getName());

    private static final DotName REPEATABLE = DotName.createSimple(Repeatable.class.getName());

    private static final Pattern BUILT_IN_CONSTRAINT_REPEATABLE_CONTAINER_PATTERN = Pattern.compile("\\$List$");

    @BuildStep
    HotDeploymentWatchedFileBuildItem configFile() {
        return new HotDeploymentWatchedFileBuildItem(META_INF_VALIDATION_XML);
    }

    @BuildStep
    LogCleanupFilterBuildItem logCleanup() {
        return new LogCleanupFilterBuildItem("org.hibernate.validator.internal.util.Version", "HV000001:");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }

    @BuildStep
    void beanValidationAnnotations(
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<BeanValidationAnnotationsBuildItem> beanValidationAnnotations) {

        IndexView indexView = CompositeIndex.create(beanArchiveIndexBuildItem.getIndex(), combinedIndexBuildItem.getIndex());

        Set<DotName> constraints = new HashSet<>();
        Set<String> builtinConstraints = ConstraintHelper.getBuiltinConstraints();

        // Collect the constraint annotations provided by Hibernate Validator and Bean Validation
        contributeBuiltinConstraints(builtinConstraints, constraints);

        // Add the constraint annotations present in the application itself
        for (AnnotationInstance constraint : indexView.getAnnotations(DotName.createSimple(Constraint.class.getName()))) {
            constraints.add(constraint.target().asClass().name());

            if (constraint.target().asClass().annotationsMap().containsKey(REPEATABLE)) {
                for (AnnotationInstance repeatableConstraint : constraint.target().asClass().annotationsMap()
                        .get(REPEATABLE)) {
                    constraints.add(repeatableConstraint.value().asClass().name());
                }
            }
        }

        Set<DotName> allConsideredAnnotations = new HashSet<>();
        allConsideredAnnotations.addAll(constraints);

        // Also consider elements that are marked with @Valid
        allConsideredAnnotations.add(VALID);

        // Also consider elements that are marked with @ValidateOnExecution
        allConsideredAnnotations.add(VALIDATE_ON_EXECUTION);

        beanValidationAnnotations.produce(new BeanValidationAnnotationsBuildItem(
                VALID,
                constraints,
                allConsideredAnnotations));
    }

    @BuildStep
    void configValidator(
            CombinedIndexBuildItem combinedIndex,
            List<ConfigClassBuildItem> configClasses,
            BeanValidationAnnotationsBuildItem beanValidationAnnotations,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        Set<DotName> configMappings = new HashSet<>();
        Set<DotName> configClassesToValidate = new HashSet<>();
        for (ConfigClassBuildItem configClass : configClasses) {
            for (String generatedConfigClass : configClass.getGeneratedClasses()) {
                DotName simple = DotName.createSimple(generatedConfigClass);
                configClassesToValidate.add(simple);
            }

            for (ConfigMappingMetadata mappingsMetadata : ConfigMappingLoader
                    .getConfigMappingsMetadata(configClass.getConfigClass())) {
                configMappings.add(DotName.createSimple(mappingsMetadata.getInterfaceType()));
            }
        }

        Set<String> configMappingsConstraints = new HashSet<>();
        for (DotName consideredAnnotation : beanValidationAnnotations.getAllAnnotations()) {
            Collection<AnnotationInstance> annotationInstances = combinedIndex.getIndex().getAnnotations(consideredAnnotation);

            if (annotationInstances.isEmpty()) {
                continue;
            }

            for (AnnotationInstance annotation : annotationInstances) {
                String builtinConstraintCandidate = BUILT_IN_CONSTRAINT_REPEATABLE_CONTAINER_PATTERN
                        .matcher(consideredAnnotation.toString()).replaceAll("");

                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo methodInfo = annotation.target().asMethod();
                    ClassInfo declaringClass = methodInfo.declaringClass();
                    if (configMappings.contains(declaringClass.name())) {
                        configMappingsConstraints.add(builtinConstraintCandidate);
                    }
                } else if (annotation.target().kind() == AnnotationTarget.Kind.TYPE) {
                    AnnotationTarget target = annotation.target().asType().enclosingTarget();
                    if (target.kind() == AnnotationTarget.Kind.METHOD) {
                        MethodInfo methodInfo = target.asMethod();
                        ClassInfo declaringClass = methodInfo.declaringClass();
                        if (configMappings.contains(declaringClass.name())) {
                            configMappingsConstraints.add(builtinConstraintCandidate);
                        }
                    }
                } else if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    ClassInfo classInfo = annotation.target().asClass();
                    if (configMappings.contains(classInfo.name())) {
                        configMappingsConstraints.add(builtinConstraintCandidate);
                    }
                }
            }
        }

        if (configMappingsConstraints.isEmpty()) {
            return;
        }

        String builderClassName = HibernateBeanValidationConfigValidator.class.getName() + "Builder";
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(builderClassName)
                .interfaces(ConfigBuilder.class)
                .setFinal(true)
                .build()) {

            // Static Init Validator
            MethodCreator clinit = classCreator
                    .getMethodCreator(MethodDescriptor.ofMethod(builderClassName, "<clinit>", void.class));
            clinit.setModifiers(Opcodes.ACC_STATIC);

            ResultHandle constraints = clinit.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (String configMappingsConstraint : configMappingsConstraints) {
                clinit.invokeVirtualMethod(MethodDescriptor.ofMethod(HashSet.class, "add", boolean.class, Object.class),
                        constraints, clinit.load(configMappingsConstraint));
            }

            ResultHandle classes = clinit.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (DotName configClassToValidate : configClassesToValidate) {
                clinit.invokeVirtualMethod(MethodDescriptor.ofMethod(HashSet.class, "add", boolean.class, Object.class),
                        classes, clinit.loadClass(configClassToValidate.toString()));
            }

            ResultHandle configValidator = clinit.newInstance(
                    MethodDescriptor.ofConstructor(HibernateBeanValidationConfigValidator.class, Set.class, Set.class),
                    constraints, classes);

            FieldDescriptor configValidatorField = FieldDescriptor.of(builderClassName, "configValidator",
                    BeanValidationConfigValidator.class);
            classCreator.getFieldCreator(configValidatorField)
                    .setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE);
            clinit.writeStaticField(configValidatorField, configValidator);

            clinit.returnNull();
            clinit.close();

            MethodCreator configBuilderMethod = classCreator.getMethodCreator(
                    MethodDescriptor.ofMethod(
                            ConfigBuilder.class, "configBuilder",
                            SmallRyeConfigBuilder.class, SmallRyeConfigBuilder.class));
            ResultHandle configBuilder = configBuilderMethod.getMethodParam(0);

            // Add Validator to the builder
            configBuilderMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "withValidator", SmallRyeConfigBuilder.class,
                            ConfigValidator.class),
                    configBuilder, configBuilderMethod.readStaticField(configValidatorField));

            configBuilderMethod.returnValue(configBuilder);
        }

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(builderClassName).build());
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(builderClassName));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(builderClassName));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void shutdownConfigValidator(HibernateValidatorRecorder hibernateValidatorRecorder,
            ShutdownContextBuildItem shutdownContext) {
        hibernateValidatorRecorder.shutdownConfigValidator(shutdownContext);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerAdditionalBeans(HibernateValidatorRecorder hibernateValidatorRecorder,
            Optional<ResteasyConfigBuildItem> resteasyConfigBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean,
            BuildProducer<AutoAddScopeBuildItem> autoScopes,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItems,
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProvider,
            Capabilities capabilities) {
        // The bean encapsulating the Validator and ValidatorFactory
        additionalBeans.produce(new AdditionalBeanBuildItem(ValidatorProvider.class));

        // The CDI interceptor which will validate the methods annotated with @MethodValidated
        additionalBeans.produce(new AdditionalBeanBuildItem(MethodValidationInterceptor.class));

        additionalBeans.produce(new AdditionalBeanBuildItem(
                "io.quarkus.hibernate.validator.runtime.locale.LocaleResolversWrapper"));

        if (capabilities.isPresent(Capability.RESTEASY)) {
            // The CDI interceptor which will validate the methods annotated with @JaxrsEndPointValidated
            additionalBeans.produce(new AdditionalBeanBuildItem(
                    "io.quarkus.hibernate.validator.runtime.jaxrs.JaxrsEndPointValidationInterceptor"));
            additionalBeans.produce(new AdditionalBeanBuildItem(
                    "io.quarkus.hibernate.validator.runtime.locale.ResteasyClassicLocaleResolver"));
            syntheticBeanBuildItems.produce(SyntheticBeanBuildItem.configure(ResteasyConfigSupport.class)
                    .scope(Singleton.class)
                    .unremovable()
                    .supplier(hibernateValidatorRecorder.resteasyConfigSupportSupplier(
                            resteasyConfigBuildItem.isPresent() ? resteasyConfigBuildItem.get().isJsonDefault() : false))
                    .done());
            resteasyJaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(ResteasyViolationExceptionMapper.class.getName()));

        } else if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            // The CDI interceptor which will validate the methods annotated with @JaxrsEndPointValidated
            additionalBeans.produce(new AdditionalBeanBuildItem(
                    "io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveEndPointValidationInterceptor"));
            additionalBeans.produce(new AdditionalBeanBuildItem(
                    "io.quarkus.hibernate.validator.runtime.locale.ResteasyReactiveLocaleResolver"));
        }

        // A constraint validator with an injection point but no scope is added as @Dependent
        autoScopes.produce(AutoAddScopeBuildItem.builder().implementsInterface(CONSTRAINT_VALIDATOR)
                .requiresContainerServices()
                .defaultScope(BuiltinScope.DEPENDENT).build());

        // Do not remove the Bean Validation beans
        unremovableBean.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                return beanInfo.hasType(CONSTRAINT_VALIDATOR) || beanInfo.hasType(CONSTRAINT_VALIDATOR_FACTORY)
                        || beanInfo.hasType(MESSAGE_INTERPOLATOR) || beanInfo.hasType(TRAVERSABLE_RESOLVER)
                        || beanInfo.hasType(PARAMETER_NAME_PROVIDER) || beanInfo.hasType(CLOCK_PROVIDER)
                        || beanInfo.hasType(SCRIPT_EVALUATOR_FACTORY)
                        || beanInfo.hasType(GETTER_PROPERTY_SELECTION_STRATEGY)
                        || beanInfo.hasType(LOCALE_RESOLVER)
                        || beanInfo.hasType(PROPERTY_NODE_NAME_PROVIDER)
                        || beanInfo.hasType(VALIDATOR_FACTORY_CUSTOMIZER);
            }
        }));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(
            HibernateValidatorRecorder recorder, RecorderContext recorderContext,
            BeanValidationAnnotationsBuildItem beanValidationAnnotations,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            ShutdownContextBuildItem shutdownContext,
            List<ConfigClassBuildItem> configClasses,
            List<AdditionalJaxRsResourceMethodAnnotationsBuildItem> additionalJaxRsResourceMethodAnnotations,
            Capabilities capabilities,
            LocalesBuildTimeConfig localesBuildTimeConfig,
            HibernateValidatorBuildTimeConfig hibernateValidatorBuildTimeConfig) throws Exception {

        feature.produce(new FeatureBuildItem(Feature.HIBERNATE_VALIDATOR));

        // we use both indexes to support both generated beans and jars that contain no CDI beans but only Validation annotations
        IndexView indexView = CompositeIndex.create(beanArchiveIndexBuildItem.getIndex(), combinedIndexBuildItem.getIndex());

        Set<DotName> classNamesToBeValidated = new HashSet<>();
        Map<DotName, Set<SimpleMethodSignatureKey>> methodsWithInheritedValidation = new HashMap<>();
        Set<String> detectedBuiltinConstraints = new HashSet<>();

        for (DotName consideredAnnotation : beanValidationAnnotations.getAllAnnotations()) {
            Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(consideredAnnotation);

            if (annotationInstances.isEmpty()) {
                continue;
            }

            // we trim the repeatable container suffix if needed
            String builtinConstraintCandidate = BUILT_IN_CONSTRAINT_REPEATABLE_CONTAINER_PATTERN
                    .matcher(consideredAnnotation.toString()).replaceAll("");
            if (beanValidationAnnotations.getConstraintAnnotations()
                    .contains(DotName.createSimple(builtinConstraintCandidate))) {
                detectedBuiltinConstraints.add(builtinConstraintCandidate);
            }

            for (AnnotationInstance annotation : annotationInstances) {
                if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    contributeClass(classNamesToBeValidated, indexView, annotation.target().asField().declaringClass().name());
                    reflectiveFields.produce(new ReflectiveFieldBuildItem(annotation.target().asField()));
                    contributeClassMarkedForCascadingValidation(classNamesToBeValidated, indexView, consideredAnnotation,
                            annotation.target().asField().type());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    contributeClass(classNamesToBeValidated, indexView, annotation.target().asMethod().declaringClass().name());
                    // we need to register the method for reflection as it could be a getter
                    reflectiveMethods.produce(new ReflectiveMethodBuildItem(annotation.target().asMethod()));
                    contributeClassMarkedForCascadingValidation(classNamesToBeValidated, indexView, consideredAnnotation,
                            annotation.target().asMethod().returnType());
                    contributeMethodsWithInheritedValidation(methodsWithInheritedValidation, indexView,
                            annotation.target().asMethod());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    contributeClass(classNamesToBeValidated, indexView,
                            annotation.target().asMethodParameter().method().declaringClass().name());
                    // a getter does not have parameters so it's a pure method: no need for reflection in this case
                    contributeClassMarkedForCascadingValidation(classNamesToBeValidated, indexView, consideredAnnotation,
                            // FIXME this won't work in the case of synthetic parameters
                            annotation.target().asMethodParameter().method().parameterTypes()
                                    .get(annotation.target().asMethodParameter().position()));
                    contributeMethodsWithInheritedValidation(methodsWithInheritedValidation, indexView,
                            annotation.target().asMethodParameter().method());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    contributeClass(classNamesToBeValidated, indexView, annotation.target().asClass().name());
                    // no need for reflection in the case of a class level constraint
                } else if (annotation.target().kind() == AnnotationTarget.Kind.TYPE) {
                    // container element constraints
                    AnnotationTarget enclosingTarget = annotation.target().asType().enclosingTarget();
                    if (enclosingTarget.kind() == AnnotationTarget.Kind.FIELD) {
                        contributeClass(classNamesToBeValidated, indexView, enclosingTarget.asField().declaringClass().name());
                        reflectiveFields.produce(new ReflectiveFieldBuildItem(enclosingTarget.asField()));
                        if (annotation.target().asType().target() != null) {
                            contributeClassMarkedForCascadingValidation(classNamesToBeValidated, indexView,
                                    consideredAnnotation,
                                    annotation.target().asType().target());
                        }
                    } else if (enclosingTarget.kind() == AnnotationTarget.Kind.METHOD) {
                        contributeClass(classNamesToBeValidated, indexView, enclosingTarget.asMethod().declaringClass().name());
                        reflectiveMethods.produce(new ReflectiveMethodBuildItem(enclosingTarget.asMethod()));
                        if (annotation.target().asType().target() != null) {
                            contributeClassMarkedForCascadingValidation(classNamesToBeValidated, indexView,
                                    consideredAnnotation,
                                    annotation.target().asType().target());
                        }
                        contributeMethodsWithInheritedValidation(methodsWithInheritedValidation, indexView,
                                enclosingTarget.asMethod());
                    }
                }
            }
        }

        // JAX-RS methods are handled differently by the transformer so those need to be gathered here.
        // Note: The focus only on methods is basically an incomplete solution, since there could also be
        // class-level JAX-RS annotations but currently the transformer only looks at methods.
        Set<DotName> additional = new HashSet<>();
        additionalJaxRsResourceMethodAnnotations.forEach((s) -> additional.addAll(s.getAnnotationClasses()));
        Map<DotName, Set<SimpleMethodSignatureKey>> jaxRsMethods = gatherJaxRsMethods(additional,
                indexView);

        // Add the annotations transformer to add @MethodValidated annotations on the methods requiring validation

        annotationsTransformers
                .produce(new AnnotationsTransformerBuildItem(
                        new MethodValidatedAnnotationsTransformer(beanValidationAnnotations.getAllAnnotations(),
                                jaxRsMethods,
                                methodsWithInheritedValidation)));

        Set<Class<?>> classesToBeValidated = new HashSet<>();
        for (DotName className : classNamesToBeValidated) {
            classesToBeValidated.add(recorderContext.classProxy(className.toString()));
        }

        // Prevent the removal of ValueExtractor beans
        // and collect all classes implementing ValueExtractor (for use in HibernateValidatorRecorder)
        Set<DotName> valueExtractorClassNames = new HashSet<>();
        for (ClassInfo valueExtractorType : indexView.getAllKnownImplementors(VALUE_EXTRACTOR)) {
            valueExtractorClassNames.add(valueExtractorType.name());
        }
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(valueExtractorClassNames));
        Set<Class<?>> valueExtractorClassProxies = new HashSet<>();
        for (DotName className : valueExtractorClassNames) {
            valueExtractorClassProxies.add(recorderContext.classProxy(className.toString()));
        }

        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initializeValidatorFactory(classesToBeValidated, detectedBuiltinConstraints,
                                valueExtractorClassProxies,
                                hasXmlConfiguration(),
                                capabilities.isPresent(Capability.HIBERNATE_ORM),
                                shutdownContext,
                                localesBuildTimeConfig,
                                hibernateValidatorBuildTimeConfig)));
    }

    @BuildStep
    void optionalResourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundles) {
        String[] potentialHibernateValidatorResourceBundles = {
                AbstractMessageInterpolator.DEFAULT_VALIDATION_MESSAGES,
                AbstractMessageInterpolator.USER_VALIDATION_MESSAGES,
                AbstractMessageInterpolator.CONTRIBUTOR_VALIDATION_MESSAGES };

        for (String potentialHibernateValidatorResourceBundle : potentialHibernateValidatorResourceBundles) {
            if (QuarkusClassLoader.isResourcePresentAtRuntime(potentialHibernateValidatorResourceBundle)) {
                resourceBundles.produce(new NativeImageResourceBundleBuildItem(potentialHibernateValidatorResourceBundle));
            }
        }
    }

    @BuildStep
    void exceptionMapper(BuildProducer<ExceptionMapperBuildItem> exceptionMapperProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        exceptionMapperProducer.produce(new ExceptionMapperBuildItem(ResteasyReactiveViolationExceptionMapper.class.getName(),
                ValidationException.class.getName(), Priorities.USER + 1, true));
        reflectiveClassProducer.produce(
                ReflectiveClassBuildItem.builder(ViolationReport.class,
                        ViolationReport.Violation.class).methods().fields().build());
    }

    // We need to make sure that the standard process of obtaining a ValidationFactory is not followed,
    // because it fails in native. So we just redirect the call to our own code that obtains the factory
    // from Arc
    @BuildStep
    void overrideStandardValidationFactoryResolution(BuildProducer<BytecodeTransformerBuildItem> transformer) {
        BytecodeTransformerBuildItem transformation = new BytecodeTransformerBuildItem(Validation.class.getName(),
                (className, classVisitor) -> new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                            String[] exceptions) {
                        MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

                        if (name.equals("buildDefaultValidatorFactory")) {
                            return new MethodVisitor(Gizmo.ASM_API_VERSION, visitor) {
                                @Override
                                public void visitCode() {
                                    super.visitCode();
                                    visitMethodInsn(Opcodes.INVOKESTATIC,
                                            ValidationSupport.class.getName().replace(".", "/"),
                                            "buildDefaultValidatorFactory",
                                            String.format("()L%s;", ValidatorFactory.class.getName().replace(".", "/")), false);
                                    visitInsn(Opcodes.ARETURN);
                                }
                            };
                        }

                        // TODO: should intercept the other methods and throw an exception to indicate they are unsupported?

                        return visitor;
                    }
                });
        transformer.produce(transformation);
    }

    private static void contributeBuiltinConstraints(Set<String> builtinConstraints,
            Set<DotName> consideredAnnotationsCollector) {
        for (String builtinConstraint : builtinConstraints) {
            consideredAnnotationsCollector.add(DotName.createSimple(builtinConstraint));

            // for all built-in constraints, we follow a strict convention for repeatable annotations,
            // they are all inner classes called List
            // while not all our built-in constraints are repeatable, let's avoid loading the class to check
            consideredAnnotationsCollector.add(DotName.createSimple(builtinConstraint + "$List"));
        }
    }

    private static void contributeClass(Set<DotName> classNamesCollector, IndexView indexView, DotName className) {
        classNamesCollector.add(className);

        if (DotNames.OBJECT.equals(className)) {
            return;
        }

        for (ClassInfo subclass : indexView.getAllKnownSubclasses(className)) {
            if (Modifier.isAbstract(subclass.flags())) {
                // we can avoid adding the abstract classes here: either they are parent classes
                // and they will be dealt with by Hibernate Validator or they are child classes
                // without any proper implementation and we can ignore them.
                continue;
            }
            classNamesCollector.add(subclass.name());
        }
        for (ClassInfo implementor : indexView.getAllKnownImplementors(className)) {
            if (Modifier.isAbstract(implementor.flags())) {
                // we can avoid adding the abstract classes here: either they are parent classes
                // and they will be dealt with by Hibernate Validator or they are child classes
                // without any proper implementation and we can ignore them.
                continue;
            }
            classNamesCollector.add(implementor.name());
        }
    }

    private static void contributeClassMarkedForCascadingValidation(Set<DotName> classNamesCollector,
            IndexView indexView, DotName consideredAnnotation, Type type) {
        if (VALID != consideredAnnotation) {
            return;
        }

        DotName className = getClassName(type);
        if (className != null) {
            contributeClass(classNamesCollector, indexView, className);
        }
    }

    private static void contributeMethodsWithInheritedValidation(
            Map<DotName, Set<SimpleMethodSignatureKey>> methodsWithInheritedValidation,
            IndexView indexView, MethodInfo method) {
        ClassInfo clazz = method.declaringClass();

        methodsWithInheritedValidation.computeIfAbsent(clazz.name(), k -> new HashSet<>())
                .add(new SimpleMethodSignatureKey(method));

        if (Modifier.isInterface(clazz.flags())) {
            for (ClassInfo implementor : indexView.getAllKnownImplementors(clazz.name())) {
                methodsWithInheritedValidation.computeIfAbsent(implementor.name(), k -> new HashSet<>())
                        .add(new SimpleMethodSignatureKey(method));
            }
        } else {
            for (ClassInfo subclass : indexView.getAllKnownSubclasses(clazz.name())) {
                methodsWithInheritedValidation.computeIfAbsent(subclass.name(), k -> new HashSet<>())
                        .add(new SimpleMethodSignatureKey(method));
            }
        }
    }

    private static Map<DotName, Set<SimpleMethodSignatureKey>> gatherJaxRsMethods(
            Set<DotName> additionalJaxRsResourceMethodAnnotations,
            IndexView indexView) {
        Map<DotName, Set<SimpleMethodSignatureKey>> jaxRsMethods = new HashMap<>();

        Collection<DotName> jaxRsMethodDefiningAnnotations = new ArrayList<>(
                ResteasyDotNames.JAXRS_METHOD_ANNOTATIONS.size() + additionalJaxRsResourceMethodAnnotations.size());
        jaxRsMethodDefiningAnnotations.addAll(ResteasyDotNames.JAXRS_METHOD_ANNOTATIONS);
        jaxRsMethodDefiningAnnotations.addAll(additionalJaxRsResourceMethodAnnotations);

        for (DotName jaxRsAnnotation : jaxRsMethodDefiningAnnotations) {
            Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(jaxRsAnnotation);

            if (annotationInstances.isEmpty()) {
                continue;
            }

            for (AnnotationInstance annotation : annotationInstances) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo method = annotation.target().asMethod();
                    jaxRsMethods.computeIfAbsent(method.declaringClass().name(), k -> new HashSet<>())
                            .add(new SimpleMethodSignatureKey(method));

                    if (Modifier.isInterface(method.declaringClass().flags())) {
                        for (ClassInfo implementor : indexView.getAllKnownImplementors(method.declaringClass().name())) {
                            jaxRsMethods.computeIfAbsent(implementor.name(), k -> new HashSet<>())
                                    .add(new SimpleMethodSignatureKey(method));
                        }
                    } else {
                        for (ClassInfo subclass : indexView.getAllKnownSubclasses(method.declaringClass().name())) {
                            jaxRsMethods.computeIfAbsent(subclass.name(), k -> new HashSet<>())
                                    .add(new SimpleMethodSignatureKey(method));
                        }
                    }
                }
            }
        }
        return jaxRsMethods;
    }

    private static DotName getClassName(Type type) {
        switch (type.kind()) {
            case CLASS:
            case PARAMETERIZED_TYPE:
                return type.name();
            case ARRAY:
                return getClassName(type.asArrayType().constituent());
            default:
                return null;
        }
    }

    private static boolean hasXmlConfiguration() {
        return Thread.currentThread().getContextClassLoader().getResource(META_INF_VALIDATION_XML) != null;
    }

}
