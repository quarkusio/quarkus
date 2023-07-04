package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.ACKNOWLEDGMENT;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BROADCAST;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.COMPLETION_STAGE;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.INCOMING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.INCOMINGS;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.KOTLIN_UNIT;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.MERGE;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.OUTGOING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.RUN_ON_VIRTUAL_THREAD;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.SMALLRYE_BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.TRANSACTIONAL;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.VOID_CLASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusParameterDescriptor;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusWorkerPoolRegistry;
import io.quarkus.smallrye.reactivemessaging.runtime.TypeInfo;
import io.smallrye.reactive.messaging.Shape;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.keyed.KeyValueExtractor;
import io.smallrye.reactive.messaging.providers.MediatorConfigurationSupport;

public final class QuarkusMediatorConfigurationUtil {

    private QuarkusMediatorConfigurationUtil() {
    }

    public static QuarkusMediatorConfiguration create(MethodInfo methodInfo, boolean isSuspendMethod, BeanInfo bean,
            RecorderContext recorderContext,
            ClassLoader cl, boolean strict) {

        Class[] parameterTypeClasses;
        Class<?> returnTypeClass;
        MediatorConfigurationSupport.GenericTypeAssignable genericReturnTypeAssignable;
        if (isSuspendMethod) {
            parameterTypeClasses = new Class[methodInfo.parametersCount() - 1];
            for (int i = 0; i < methodInfo.parametersCount() - 1; i++) {
                parameterTypeClasses[i] = load(methodInfo.parameterType(i).name().toString(), cl);
            }
            // the generated invoker will always return a CompletionStage
            // TODO: avoid hard coding this and use an SPI to communicate the info with the invoker generation code
            returnTypeClass = CompletionStage.class;
            genericReturnTypeAssignable = new JandexGenericTypeAssignable(determineReturnTypeOfSuspendMethod(methodInfo), cl);
        } else {
            parameterTypeClasses = new Class[methodInfo.parametersCount()];
            for (int i = 0; i < methodInfo.parametersCount(); i++) {
                parameterTypeClasses[i] = load(methodInfo.parameterType(i).name().toString(), cl);
            }
            returnTypeClass = load(methodInfo.returnType().name().toString(), cl);
            genericReturnTypeAssignable = new ReturnTypeGenericTypeAssignable(methodInfo, cl);
        }

        QuarkusMediatorConfiguration configuration = new QuarkusMediatorConfiguration();

        List<TypeInfo> gen = new ArrayList<>();
        for (int i = 0; i < methodInfo.parameterTypes().size(); i++) {
            TypeInfo ti = new TypeInfo();
            Type type = methodInfo.parameterTypes().get(i);
            ti.setName(recorderContext.classProxy(type.name().toString()));
            List<Class<?>> inner = new ArrayList<>();
            if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                inner = type.asParameterizedType().arguments().stream()
                        .map(t -> recorderContext.classProxy(t.name().toString()))
                        .collect(Collectors.toList());
            }
            ti.setGenerics(inner);
            gen.add(ti);
        }

        QuarkusParameterDescriptor descriptor = new QuarkusParameterDescriptor(gen);
        configuration.setParameterDescriptor(descriptor);

        // Extract @Keyed, key type and value type
        handleKeyedMulti(methodInfo, recorderContext, configuration);

        MediatorConfigurationSupport mediatorConfigurationSupport = new MediatorConfigurationSupport(
                fullMethodName(methodInfo), returnTypeClass, parameterTypeClasses,
                genericReturnTypeAssignable,
                methodInfo.parameterTypes().isEmpty() ? new AlwaysInvalidIndexGenericTypeAssignable()
                        : new MethodParamGenericTypeAssignable(methodInfo, 0, cl));

        if (strict) {
            mediatorConfigurationSupport.strict();
        }

        configuration.setBeanId(bean.getIdentifier());
        configuration.setMethodName(methodInfo.name());

        String returnTypeName = returnTypeClass.getName();
        configuration.setReturnType(recorderContext.classProxy(returnTypeName));

        // We need to extract the value of @Incoming and @Incomings (which contains an array of @Incoming)
        List<String> incomingValues = new ArrayList<>(getValues(methodInfo, INCOMING));
        incomingValues.addAll(getIncomingValues(methodInfo));
        configuration.setIncomings(incomingValues);

        String outgoingValue = getValue(methodInfo, OUTGOING);
        configuration.setOutgoing(outgoingValue);

        Shape shape = mediatorConfigurationSupport.determineShape(incomingValues, outgoingValue);
        configuration.setShape(shape);
        Acknowledgment.Strategy acknowledgment = mediatorConfigurationSupport
                .processSuppliedAcknowledgement(incomingValues,
                        () -> {
                            AnnotationInstance instance = methodInfo.annotation(ACKNOWLEDGMENT);
                            if (instance != null) {
                                return Acknowledgment.Strategy.valueOf(instance.value().asEnum());
                            }
                            return null;
                        });
        configuration.setAcknowledgment(acknowledgment);

        MediatorConfigurationSupport.ValidationOutput validationOutput = mediatorConfigurationSupport.validate(shape,
                acknowledgment);
        configuration.setProduction(validationOutput.getProduction());
        configuration.setConsumption(validationOutput.getConsumption());
        configuration.setIngestedPayloadType(validationOutput.getIngestedPayloadType());
        configuration.setUseBuilderTypes(validationOutput.getUseBuilderTypes());
        configuration.setUseReactiveStreams(validationOutput.getUseReactiveStreams());

        if (acknowledgment == null) {
            acknowledgment = mediatorConfigurationSupport.processDefaultAcknowledgement(shape,
                    validationOutput.getConsumption(), validationOutput.getProduction());
            configuration.setAcknowledgment(acknowledgment);
        }

        configuration.setMerge(mediatorConfigurationSupport.processMerge(incomingValues, new Supplier<Merge.Mode>() {
            @Override
            public Merge.Mode get() {
                AnnotationInstance instance = methodInfo.annotation(MERGE);
                if (instance != null) {
                    AnnotationValue value = instance.value();
                    if (value == null) {
                        return Merge.Mode.MERGE; // the default value of @Merge
                    }
                    return Merge.Mode.valueOf(value.asEnum());
                }
                return null;
            }
        }));

        configuration.setBroadcastValue(mediatorConfigurationSupport.processBroadcast(outgoingValue,
                new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        AnnotationInstance instance = methodInfo.annotation(BROADCAST);
                        if (instance != null) {
                            AnnotationValue value = instance.value();
                            if (value == null) {
                                return 0; // the default value of @Broadcast
                            }
                            return value.asInt();
                        }
                        return null;
                    }
                }));

        AnnotationInstance blockingAnnotation = methodInfo.annotation(BLOCKING);
        AnnotationInstance smallryeBlockingAnnotation = methodInfo.annotation(SMALLRYE_BLOCKING);
        AnnotationInstance transactionalAnnotation = methodInfo.annotation(TRANSACTIONAL);
        AnnotationInstance runOnVirtualThreadAnnotation = methodInfo.annotation(RUN_ON_VIRTUAL_THREAD);
        if (blockingAnnotation != null || smallryeBlockingAnnotation != null || transactionalAnnotation != null
                || runOnVirtualThreadAnnotation != null) {
            mediatorConfigurationSupport.validateBlocking(validationOutput);
            configuration.setBlocking(true);
            if (blockingAnnotation != null) {
                AnnotationValue ordered = blockingAnnotation.value("ordered");
                if (runOnVirtualThreadAnnotation != null) {
                    if (ordered != null && ordered.asBoolean()) {
                        throw new ConfigurationException(
                                "The method `" + methodInfo.name()
                                        + "` is using `@RunOnVirtualThread` but explicitly set as `@Blocking(ordered = true)`");
                    }
                    configuration.setBlockingExecutionOrdered(false);
                    configuration.setWorkerPoolName(QuarkusWorkerPoolRegistry.DEFAULT_VIRTUAL_THREAD_WORKER);
                } else {
                    configuration.setBlockingExecutionOrdered(ordered == null || ordered.asBoolean());
                }
                String poolName;
                if (blockingAnnotation.value() != null &&
                        !(poolName = blockingAnnotation.value().asString()).equals(Blocking.DEFAULT_WORKER_POOL)) {
                    configuration.setWorkerPoolName(poolName);
                }
            } else if (runOnVirtualThreadAnnotation != null) {
                configuration.setBlockingExecutionOrdered(false);
                configuration.setWorkerPoolName(QuarkusWorkerPoolRegistry.DEFAULT_VIRTUAL_THREAD_WORKER);
            } else {
                configuration.setBlockingExecutionOrdered(true);
            }
        }

        return configuration;
    }

    private static void handleKeyedMulti(MethodInfo methodInfo,
            RecorderContext recorderContext, QuarkusMediatorConfiguration configuration) {
        if (methodInfo.parametersCount() == 1) { // @Keyed can only be used with a single parameter, a keyed multi
            var info = methodInfo.parameters().get(0);
            var annotation = info.annotation(ReactiveMessagingDotNames.KEYED);
            if (annotation != null) {
                // Make sure we have a keyed multi and an incoming.
                if (methodInfo.annotation(INCOMING) == null && methodInfo.annotation(INCOMINGS) == null) {
                    throw new ConfigurationException(
                            "The method `" + methodInfo.name() + "` is using `@Keyed` but is not annotated with `@Incoming`");
                }
                if (info.type().kind() != Type.Kind.PARAMETERIZED_TYPE
                        || !info.type().asParameterizedType().name().equals(ReactiveMessagingDotNames.KEYED_MULTI)) {
                    throw new ConfigurationException("The method `" + methodInfo.name()
                            + "` is using `@Keyed` but the annotated parameter is not a `KeyedMulti`");
                }
                var extractor = (Class<? extends KeyValueExtractor>) recorderContext
                        .classProxy(annotation.value().asClass().name().toString());
                configuration.setKeyed(extractor);
            }
            if (info.type().kind() == Type.Kind.PARAMETERIZED_TYPE
                    && info.type().asParameterizedType().name().equals(ReactiveMessagingDotNames.KEYED_MULTI)) {
                var args = info.type().asParameterizedType().arguments();
                configuration.setKeyType(recorderContext.classProxy(args.get(0).name().toString()));
                configuration.setValueType(recorderContext.classProxy(args.get(1).name().toString()));
            }
        }
    }

    // TODO: avoid hard coding CompletionStage handling
    private static Type determineReturnTypeOfSuspendMethod(MethodInfo methodInfo) {
        Type lastParamType = methodInfo.parameterType(methodInfo.parametersCount() - 1);
        if (lastParamType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            throw new IllegalStateException("Something went wrong during parameter type resolution - expected "
                    + lastParamType + " to be a Continuation with a generic type");
        }
        lastParamType = lastParamType.asParameterizedType().arguments().get(0);
        if (lastParamType.kind() != Type.Kind.WILDCARD_TYPE) {
            throw new IllegalStateException("Something went wrong during parameter type resolution - expected "
                    + lastParamType + " to be a Continuation with a generic type");
        }
        lastParamType = lastParamType.asWildcardType().superBound();
        if (lastParamType.name().equals(KOTLIN_UNIT)) {
            lastParamType = Type.create(VOID_CLASS, Type.Kind.CLASS);
        }
        lastParamType = ParameterizedType.create(COMPLETION_STAGE, new Type[] { lastParamType },
                Type.create(COMPLETION_STAGE, Type.Kind.CLASS));
        return lastParamType;
    }

    private static Class<?> load(String className, ClassLoader cl) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
        }
        try {
            return Class.forName(className, false, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getValue(MethodInfo methodInfo, DotName dotName) {
        AnnotationInstance annotationInstance = methodInfo.annotation(dotName);
        String value = null;

        if (annotationInstance != null) {
            if (annotationInstance.value() != null) {
                value = annotationInstance.value().asString();

            }
            if ((value == null) || value.isEmpty()) {
                throw new IllegalArgumentException(
                        "@" + dotName.withoutPackagePrefix() + " value cannot be blank. Offending method is: "
                                + fullMethodName(methodInfo));
            }

            // TODO: does it make sense to validate the name with the supplied configuration?
        }

        return value;
    }

    private static List<String> getValues(MethodInfo methodInfo, DotName dotName) {
        return methodInfo.annotations().stream().filter(ai -> ai.name().equals(dotName))
                .map(ai -> ai.value().asString())
                .collect(Collectors.toList());
    }

    private static List<String> getIncomingValues(MethodInfo methodInfo) {
        return methodInfo.annotations().stream().filter(ai -> ai.name().equals(INCOMINGS))
                .flatMap(incomings -> Arrays.stream(incomings.value().asNestedArray()))
                .map(incoming -> incoming.value().asString())
                .collect(Collectors.toList());
    }

    private static String fullMethodName(MethodInfo methodInfo) {
        return methodInfo.declaringClass() + "#" + methodInfo.name();
    }

    public static class ReturnTypeGenericTypeAssignable extends JandexGenericTypeAssignable {

        public ReturnTypeGenericTypeAssignable(MethodInfo method, ClassLoader classLoader) {
            super(method.returnType(), classLoader);
        }
    }

    private static class JandexGenericTypeAssignable implements MediatorConfigurationSupport.GenericTypeAssignable {

        // will be used when we need to check assignability
        private final ClassLoader classLoader;
        private final Type type;

        public JandexGenericTypeAssignable(Type type, ClassLoader classLoader) {
            this.classLoader = classLoader;
            this.type = type;
        }

        @Override
        public Result check(Class<?> target, int index) {
            if (type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                return Result.NotGeneric;
            }
            List<Type> arguments = type.asParameterizedType().arguments();
            if (arguments.size() >= index + 1) {
                Class<?> argumentClass = load(arguments.get(index).name().toString(), classLoader);
                return target.isAssignableFrom(argumentClass) ? Result.Assignable : Result.NotAssignable;
            } else {
                return Result.InvalidIndex;
            }
        }

        @Override
        public java.lang.reflect.Type getType(int index) {
            Type t = extract(type, index);
            if (t != null) {
                return load(t.name().toString(), classLoader);
            }
            return null;
        }

        private Type extract(Type type, int index) {
            if (type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                return null;
            } else {
                List<Type> arguments = type.asParameterizedType().arguments();
                if (arguments.size() >= index + 1) {
                    Type result = arguments.get(index);
                    if (result.kind() == Type.Kind.WILDCARD_TYPE) {
                        return null;
                    }
                    return result;
                } else {
                    return null;
                }
            }
        }

        @Override
        public java.lang.reflect.Type getType(int index, int subIndex) {
            Type generic = extract(type, index);
            if (generic != null) {
                Type t = extract(generic, subIndex);
                if (t != null) {
                    return load(t.name().toString(), classLoader);
                }
            }
            return null;
        }
    }

    public static class AlwaysInvalidIndexGenericTypeAssignable
            implements MediatorConfigurationSupport.GenericTypeAssignable {

        @Override
        public Result check(Class<?> target, int index) {
            return Result.InvalidIndex;
        }

        @Override
        public java.lang.reflect.Type getType(int index) {
            return null;
        }

        @Override
        public java.lang.reflect.Type getType(int index, int subIndex) {
            return null;
        }
    }

    public static class MethodParamGenericTypeAssignable extends JandexGenericTypeAssignable {

        public MethodParamGenericTypeAssignable(MethodInfo method, int paramIndex, ClassLoader classLoader) {
            super(getGenericParameterType(method, paramIndex), classLoader);
        }

        public MethodParamGenericTypeAssignable(Type type, ClassLoader classLoader) {
            super(type, classLoader);
        }

        private static Type getGenericParameterType(MethodInfo method, int paramIndex) {
            List<Type> parameters = method.parameterTypes();
            if (parameters.size() < paramIndex + 1) {
                throw new IllegalArgumentException("Method " + method + " only has " + parameters.size()
                        + " so parameter with index " + paramIndex + " cannot be retrieved");
            }
            return parameters.get(paramIndex);
        }
    }
}
