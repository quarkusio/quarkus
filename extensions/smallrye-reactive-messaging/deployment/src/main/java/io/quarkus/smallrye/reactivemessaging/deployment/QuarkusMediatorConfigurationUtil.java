package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.ACKNOWLEDGMENT;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BROADCAST;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.COMPLETION_STAGE;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.INCOMING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.KOTLIN_UNIT;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.MERGE;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.OUTGOING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.SMALLRYE_BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.VOID_CLASS;

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
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.smallrye.reactive.messaging.MediatorConfigurationSupport;
import io.smallrye.reactive.messaging.Shape;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Merge;

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
            parameterTypeClasses = new Class[methodInfo.parameters().size() - 1];
            for (int i = 0; i < methodInfo.parameters().size() - 1; i++) {
                parameterTypeClasses[i] = load(methodInfo.parameters().get(i).name().toString(), cl);
            }
            // the generated invoker will always return a CompletionStage
            // TODO: avoid hard coding this and use an SPI to communicate the info with the invoker generation code
            returnTypeClass = CompletionStage.class;
            genericReturnTypeAssignable = new JandexGenericTypeAssignable(determineReturnTypeOfSuspendMethod(methodInfo), cl);
        } else {
            parameterTypeClasses = new Class[methodInfo.parameters().size()];
            for (int i = 0; i < methodInfo.parameters().size(); i++) {
                parameterTypeClasses[i] = load(methodInfo.parameters().get(i).name().toString(), cl);
            }
            returnTypeClass = load(methodInfo.returnType().name().toString(), cl);
            genericReturnTypeAssignable = new ReturnTypeGenericTypeAssignable(methodInfo, cl);
        }

        QuarkusMediatorConfiguration configuration = new QuarkusMediatorConfiguration();
        MediatorConfigurationSupport mediatorConfigurationSupport = new MediatorConfigurationSupport(
                fullMethodName(methodInfo), returnTypeClass, parameterTypeClasses,
                genericReturnTypeAssignable,
                methodInfo.parameters().isEmpty() ? new AlwaysInvalidIndexGenericTypeAssignable()
                        : new MethodParamGenericTypeAssignable(methodInfo, 0, cl));

        if (strict) {
            mediatorConfigurationSupport.strict();
        }

        configuration.setBeanId(bean.getIdentifier());
        configuration.setMethodName(methodInfo.name());

        String returnTypeName = returnTypeClass.getName();
        configuration.setReturnType(recorderContext.classProxy(returnTypeName));
        Class<?>[] parameterTypes = new Class[methodInfo.parameters().size()];
        for (int i = 0; i < methodInfo.parameters().size(); i++) {
            parameterTypes[i] = recorderContext.classProxy(methodInfo.parameters().get(i).name().toString());
        }
        configuration.setParameterTypes(parameterTypes);

        List<String> incomingValues = getValues(methodInfo, INCOMING);
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
        if (validationOutput.getUseBuilderTypes()) {
            configuration.setUseBuilderTypes(validationOutput.getUseBuilderTypes());
        } else {
            configuration.setUseBuilderTypes(false);
        }

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
        if (blockingAnnotation != null || smallryeBlockingAnnotation != null) {
            mediatorConfigurationSupport.validateBlocking(validationOutput);
            configuration.setBlocking(true);
            if (blockingAnnotation != null) {
                AnnotationValue ordered = blockingAnnotation.value("ordered");
                configuration.setBlockingExecutionOrdered(ordered == null || ordered.asBoolean());
                String poolName;
                if (blockingAnnotation.value() != null &&
                        !(poolName = blockingAnnotation.value().asString()).equals(Blocking.DEFAULT_WORKER_POOL)) {
                    configuration.setWorkerPoolName(poolName);
                }
            } else {
                configuration.setBlockingExecutionOrdered(true);
            }
        }

        return configuration;
    }

    // TODO: avoid hard coding CompletionStage handling
    private static Type determineReturnTypeOfSuspendMethod(MethodInfo methodInfo) {
        Type lastParamType = methodInfo.parameters().get(methodInfo.parameters().size() - 1);
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
                return null;
            } else {
                return null;
            }
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

        private static Type getGenericParameterType(MethodInfo method, int paramIndex) {
            List<Type> parameters = method.parameters();
            if (parameters.size() < paramIndex + 1) {
                throw new IllegalArgumentException("Method " + method + " only has " + parameters.size()
                        + " so parameter with index " + paramIndex + " cannot be retrieved");
            }
            return parameters.get(paramIndex);
        }
    }
}
