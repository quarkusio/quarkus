package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.annotations.Stream;

/**
 * @author Martin Kouba
 */
public class SmallRyeReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger
            .getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    static final String INVOKER_SUFFIX = "_SmallryeMessagingInvoker";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        // We add the connector and stream qualifiers to make them part of the index.
        return new AdditionalBeanBuildItem(SmallRyeReactiveMessagingLifecycle.class, Connector.class, Stream.class);
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformBeanScope(BeanArchiveIndexBuildItem index) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(AnnotationsTransformer.TransformationContext ctx) {
                if (ctx.isClass()) {
                    if (BuiltinScope.isDeclaredOn(ctx.getTarget().asClass())) {
                        return;
                    }
                    ClassInfo clazz = ctx.getTarget().asClass();
                    Map<DotName, List<AnnotationInstance>> annotations = clazz.annotations();
                    if (annotations.containsKey(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.INCOMING)
                            || annotations.containsKey(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.OUTGOING)) {
                        LOGGER.debugf(
                                "Found reactive messaging annotations on a class %s with no scope defined - adding @Dependent",
                                ctx.getTarget());
                        ctx.transform().add(Dependent.class).done();
                    }
                }
            }
        });
    }

    @BuildStep
    void validateBeanDeployment(
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<MediatorBuildItem> mediatorMethods,
            BuildProducer<EmitterBuildItem> emitters,
            BuildProducer<ValidationErrorBuildItem> errors) {

        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        // We need to collect all business methods annotated with @Incoming/@Outgoing first
        for (BeanInfo bean : validationPhase.getContext().get(BuildExtension.Key.BEANS)) {
            if (bean.isClassBean()) {
                // TODO: add support for inherited business methods
                for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                    AnnotationInstance incoming = annotationStore.getAnnotation(method,
                            io.quarkus.smallrye.reactivemessaging.deployment.DotNames.INCOMING);
                    AnnotationInstance outgoing = annotationStore.getAnnotation(method,
                            io.quarkus.smallrye.reactivemessaging.deployment.DotNames.OUTGOING);
                    if (incoming != null || outgoing != null) {
                        if (incoming != null && incoming.value().asString().isEmpty()) {
                            validationPhase.getContext().addDeploymentProblem(
                                    new DeploymentException("Empty @Incoming annotation on method " + method));
                        }
                        if (outgoing != null && outgoing.value().asString().isEmpty()) {
                            validationPhase.getContext().addDeploymentProblem(
                                    new DeploymentException("Empty @Outgoing annotation on method " + method));
                        }
                        // TODO: validate method params and return type?
                        mediatorMethods.produce(new MediatorBuildItem(bean, method));
                        LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                    }
                }
            }
        }

        for (InjectionPointInfo injectionPoint : validationPhase.getContext()
                .get(BuildExtension.Key.INJECTION_POINTS)) {
            if (injectionPoint.getRequiredType().name()
                    .equals(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.EMITTER)) {
                AnnotationInstance instance = injectionPoint
                        .getRequiredQualifier(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.CHANNEL);
                if (instance == null) {
                    instance = injectionPoint
                            .getRequiredQualifier(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.STREAM); //@Channel is the replacement of deprecated @Stream
                }
                if (instance != null) {
                    // Stream.value() is mandatory
                    String name = instance.value().asString();
                    Optional<AnnotationInstance> maybeOverflow = annotationStore.getAnnotations(injectionPoint.getTarget())
                            .stream()
                            .filter(ai -> io.quarkus.smallrye.reactivemessaging.deployment.DotNames.ON_OVERFLOW
                                    .equals(ai.name()))
                            .filter(ai -> {
                                if (ai.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER && injectionPoint.isParam()) {
                                    return ai.target().asMethodParameter().position() == injectionPoint.getPosition();
                                }
                                return true;
                            })
                            .findAny();
                    LOGGER.debugf("Emitter injection point '%s' detected, stream name: '%s'",
                            injectionPoint.getTargetInfo(), name);

                    if (maybeOverflow.isPresent()) {
                        AnnotationInstance annotation = maybeOverflow.get();
                        AnnotationValue maybeBufferSize = annotation.value("bufferSize");
                        int bufferSize = maybeBufferSize != null ? maybeBufferSize.asInt() : 0;
                        emitters.produce(
                                EmitterBuildItem.of(name,
                                        annotation.value().asString(),
                                        bufferSize));
                    } else {
                        emitters.produce(EmitterBuildItem.of(name));
                    }
                }
            }
        }
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> removalExclusions() {
        return Arrays.asList(
                new UnremovableBeanBuildItem(
                        new BeanClassAnnotationExclusion(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.INCOMING)),
                new UnremovableBeanBuildItem(
                        new BeanClassAnnotationExclusion(io.quarkus.smallrye.reactivemessaging.deployment.DotNames.OUTGOING)));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SmallRyeReactiveMessagingRecorder recorder, RecorderContext recorderContext,
            BeanContainerBuildItem beanContainer,
            List<MediatorBuildItem> mediatorMethods,
            List<EmitterBuildItem> emitterFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            DeploymentClassLoaderBuildItem deploymentClassLoaderBuildItem) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(deploymentClassLoaderBuildItem.getClassLoader());
            List<QuarkusMediatorConfiguration> configurations = new ArrayList<>(mediatorMethods.size());

            ClassOutput classOutput = new ClassOutput() {
                @Override
                public void write(String name, byte[] data) {
                    generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
                }
            };

            /*
             * Go through the collected MediatorMethods and build up the corresponding MediaConfiguration
             * This includes generating an invoker for each method
             * The configuration will then be captured and used at static init time to push data into smallrye
             */
            for (MediatorBuildItem mediatorMethod : mediatorMethods) {
                MethodInfo methodInfo = mediatorMethod.getMethod();
                BeanInfo bean = mediatorMethod.getBean();

                String generatedInvokerName = generateInvoker(bean, methodInfo, classOutput);
                /*
                 * We need to register the invoker's constructor for reflection since it will be called inside smallrye.
                 * We could potentially lift this restriction with some extra CDI bean generation but it's probably not worth it
                 */
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, generatedInvokerName));

                try {
                    QuarkusMediatorConfiguration mediatorConfiguration = QuarkusMediatorConfigurationUtil.create(methodInfo,
                            bean,
                            generatedInvokerName, recorderContext);
                    configurations.add(mediatorConfiguration);
                } catch (IllegalArgumentException e) {
                    throw new DeploymentException(e); // needed to pass the TCK
                }
            }

            recorder.registerMediators(configurations, beanContainer.getValue());

            for (EmitterBuildItem it : emitterFields) {
                int defaultBufferSize = ConfigProviderResolver.instance().getConfig()
                        .getOptionalValue("smallrye.messaging.emitter.default-buffer-size", Integer.class).orElse(127);
                if (it.getOverflow() != null) {
                    recorder.configureEmitter(beanContainer.getValue(), it.getName(), it.getOverflow(), it.getBufferSize(),
                            defaultBufferSize);
                } else {
                    recorder.configureEmitter(beanContainer.getValue(), it.getName(), null, 0, defaultBufferSize);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    /**
     * Generates an invoker class that looks like the following:
     * 
     * <pre>
     * public class SomeName implements Invoker {
     *     private Object beanInstance;
     *
     *     public SomeName(Object var1) {
     *         this.beanInstance = var1;
     *     }
     *
     *     public Object invoke(Object[] args) {
     *         return ((BeanType) this.beanInstance).process(var1);
     *     }
     * }
     * </pre>
     */
    private String generateInvoker(BeanInfo bean, MethodInfo method, ClassOutput classOutput) {
        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz().name());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        try (ClassCreator invoker = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(Invoker.class)
                .build()) {

            FieldDescriptor beanInstanceField = invoker.getFieldCreator("beanInstance", Object.class).getFieldDescriptor();

            // generate a constructor that bean instance an argument
            try (MethodCreator ctor = invoker.getMethodCreator("<init>", void.class, Object.class)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ResultHandle self = ctor.getThis();
                ResultHandle config = ctor.getMethodParam(0);
                ctor.writeInstanceField(beanInstanceField, self, config);
                ctor.returnValue(null);
            }

            try (MethodCreator invoke = invoker.getMethodCreator(
                    MethodDescriptor.ofMethod(generatedName, "invoke", Object.class, Object[].class))) {

                int parametersCount = method.parameters().size();
                String[] argTypes = new String[parametersCount];
                ResultHandle[] args = new ResultHandle[parametersCount];
                for (int i = 0; i < parametersCount; i++) {
                    // the only method argument of io.smallrye.reactive.messaging.Invoker is an object array so we need to pull out
                    // each argument and put it in the target method arguments array
                    args[i] = invoke.readArrayValue(invoke.getMethodParam(0), i);
                    argTypes[i] = method.parameters().get(i).name().toString();
                }
                ResultHandle result = invoke.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(method.declaringClass().name().toString(), method.name(),
                                method.returnType().name().toString(), argTypes),
                        invoke.readInstanceField(beanInstanceField, invoke.getThis()), args);
                if (io.quarkus.smallrye.reactivemessaging.deployment.DotNames.VOID.equals(method.returnType().name())) {
                    invoke.returnValue(invoke.loadNull());
                } else {
                    invoke.returnValue(result);
                }
            }
        }

        return generatedName.replace('/', '.');
    }

}
