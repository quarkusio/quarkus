package io.quarkus.hibernate.validator.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.validation.ClockProvider;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.internal.metadata.core.ConstraintHelper;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.validator.runtime.HibernateValidatorRecorder;
import io.quarkus.hibernate.validator.runtime.ValidatorProvider;
import io.quarkus.hibernate.validator.runtime.interceptor.MethodValidationInterceptor;

class HibernateValidatorProcessor {

    private static final DotName CONSTRAINT_VALIDATOR_FACTORY = DotName
            .createSimple(ConstraintValidatorFactory.class.getName());
    private static final DotName MESSAGE_INTERPOLATOR = DotName.createSimple(MessageInterpolator.class.getName());
    private static final DotName TRAVERSABLE_RESOLVER = DotName.createSimple(TraversableResolver.class.getName());
    private static final DotName PARAMETER_NAME_PROVIDER = DotName.createSimple(ParameterNameProvider.class.getName());
    private static final DotName CLOCK_PROVIDER = DotName.createSimple(ClockProvider.class.getName());
    private static final DotName SCRIPT_EVALUATOR_FACTORY = DotName.createSimple(ScriptEvaluatorFactory.class.getName());
    private static final DotName GETTER_PROPERTY_SELECTION_STRATEGY = DotName
            .createSimple(GetterPropertySelectionStrategy.class.getName());

    private static final DotName CONSTRAINT_VALIDATOR = DotName.createSimple(ConstraintValidator.class.getName());
    private static final DotName VALUE_EXTRACTOR = DotName.createSimple(ValueExtractor.class.getName());

    private static final DotName VALIDATE_ON_EXECUTION = DotName.createSimple(ValidateOnExecution.class.getName());

    private static final DotName VALID = DotName.createSimple(Valid.class.getName());

    @BuildStep
    HotDeploymentWatchedFileBuildItem configFile() {
        return new HotDeploymentWatchedFileBuildItem("META-INF/validation.xml");
    }

    @BuildStep
    LogCleanupFilterBuildItem logCleanup() {
        return new LogCleanupFilterBuildItem("org.hibernate.validator.internal.util.Version", "HV000001:");
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {
        // The bean encapsulating the Validator and ValidatorFactory
        additionalBeans.produce(new AdditionalBeanBuildItem(ValidatorProvider.class));

        // The CDI interceptor which will validate the methods annotated with @MethodValidated
        additionalBeans.produce(new AdditionalBeanBuildItem(MethodValidationInterceptor.class));

        if (isResteasyInClasspath()) {
            // The CDI interceptor which will validate the methods annotated with @JaxrsEndPointValidated
            additionalBeans.produce(new AdditionalBeanBuildItem(
                    "io.quarkus.hibernate.validator.runtime.jaxrs.JaxrsEndPointValidationInterceptor"));
        }

        // Do not remove the Bean Validation beans
        unremovableBean.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                return beanInfo.hasType(CONSTRAINT_VALIDATOR) || beanInfo.hasType(CONSTRAINT_VALIDATOR_FACTORY)
                        || beanInfo.hasType(MESSAGE_INTERPOLATOR) || beanInfo.hasType(TRAVERSABLE_RESOLVER)
                        || beanInfo.hasType(PARAMETER_NAME_PROVIDER) || beanInfo.hasType(CLOCK_PROVIDER)
                        || beanInfo.hasType(VALUE_EXTRACTOR) || beanInfo.hasType(SCRIPT_EVALUATOR_FACTORY)
                        || beanInfo.hasType(GETTER_PROPERTY_SELECTION_STRATEGY);
            }
        }));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(HibernateValidatorRecorder recorder, RecorderContext recorderContext,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            ShutdownContextBuildItem shutdownContext) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.HIBERNATE_VALIDATOR));

        IndexView indexView = combinedIndexBuildItem.getIndex();

        Set<DotName> consideredAnnotations = new HashSet<>();

        // Collect the constraint annotations provided by Hibernate Validator and Bean Validation
        contributeBuiltinConstraints(consideredAnnotations);

        // Add the constraint annotations present in the application itself
        for (AnnotationInstance constraint : indexView.getAnnotations(DotName.createSimple(Constraint.class.getName()))) {
            consideredAnnotations.add(constraint.target().asClass().name());
        }

        // Also consider elements that are marked with @Valid
        consideredAnnotations.add(VALID);

        // Also consider elements that are marked with @ValidateOnExecution
        consideredAnnotations.add(VALIDATE_ON_EXECUTION);

        Set<DotName> classNamesToBeValidated = new HashSet<>();

        for (DotName consideredAnnotation : consideredAnnotations) {
            Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(consideredAnnotation);

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
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    contributeClass(classNamesToBeValidated, indexView,
                            annotation.target().asMethodParameter().method().declaringClass().name());
                    // a getter does not have parameters so it's a pure method: no need for reflection in this case
                    contributeClassMarkedForCascadingValidation(classNamesToBeValidated, indexView, consideredAnnotation,
                            // FIXME this won't work in the case of synthetic parameters
                            annotation.target().asMethodParameter().method().parameters()
                                    .get(annotation.target().asMethodParameter().position()));
                } else if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    contributeClass(classNamesToBeValidated, indexView, annotation.target().asClass().name());
                    // no need for reflection in the case of a class level constraint
                }
            }
        }

        // Add the annotations transformer to add @MethodValidated annotations on the methods requiring validation
        annotationsTransformers
                .produce(new AnnotationsTransformerBuildItem(new MethodValidatedAnnotationsTransformer(consideredAnnotations)));

        Set<Class<?>> classesToBeValidated = new HashSet<>();
        for (DotName className : classNamesToBeValidated) {
            classesToBeValidated.add(recorderContext.classProxy(className.toString()));
        }

        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initializeValidatorFactory(classesToBeValidated, shutdownContext)));
    }

    @BuildStep
    SubstrateConfigBuildItem substrateConfig() {
        return SubstrateConfigBuildItem.builder()
                .addResourceBundle(AbstractMessageInterpolator.DEFAULT_VALIDATION_MESSAGES)
                .addResourceBundle(AbstractMessageInterpolator.USER_VALIDATION_MESSAGES)
                .addResourceBundle(AbstractMessageInterpolator.CONTRIBUTOR_VALIDATION_MESSAGES)
                .build();
    }

    private static void contributeBuiltinConstraints(Set<DotName> constraintCollector) {
        Set<Class<? extends Annotation>> builtinConstraints = new ConstraintHelper().getBuiltinConstraints();
        for (Class<? extends Annotation> builtinConstraint : builtinConstraints) {
            constraintCollector.add(DotName.createSimple(builtinConstraint.getName()));
        }
    }

    private static void contributeClass(Set<DotName> classNamesCollector, IndexView indexView, DotName className) {
        classNamesCollector.add(className);
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

    private static DotName getClassName(Type type) {
        switch (type.kind()) {
            case CLASS:
            case PARAMETERIZED_TYPE:
                return type.name();
            case ARRAY:
                return getClassName(type.asArrayType().component());
            default:
                return null;
        }
    }

    private static boolean isResteasyInClasspath() {
        try {
            Class.forName("org.jboss.resteasy.core.ResteasyContext");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
