package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.DotNames.ACKNOWLEDGMENT;
import static io.quarkus.smallrye.reactivemessaging.deployment.DotNames.BROADCAST;
import static io.quarkus.smallrye.reactivemessaging.deployment.DotNames.INCOMING;
import static io.quarkus.smallrye.reactivemessaging.deployment.DotNames.MERGE;
import static io.quarkus.smallrye.reactivemessaging.deployment.DotNames.OUTGOING;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.MediatorConfigurationSupport;
import io.smallrye.reactive.messaging.Shape;
import io.smallrye.reactive.messaging.annotations.Merge;

final class QuarkusMediatorConfigurationUtil {

    private QuarkusMediatorConfigurationUtil() {
    }

    static QuarkusMediatorConfiguration create(MethodInfo methodInfo, BeanInfo bean,
            String generatedInvokerName, RecorderContext recorderContext, ClassLoader cl) {

        Class<?> returnTypeClass = load(methodInfo.returnType().name().toString(), cl);
        Class[] parameterTypeClasses = new Class[methodInfo.parameters().size()];
        for (int i = 0; i < methodInfo.parameters().size(); i++) {
            parameterTypeClasses[i] = load(methodInfo.parameters().get(i).name().toString(), cl);
        }

        QuarkusMediatorConfiguration configuration = new QuarkusMediatorConfiguration();
        MediatorConfigurationSupport mediatorConfigurationSupport = new MediatorConfigurationSupport(
                fullMethodName(methodInfo), returnTypeClass, parameterTypeClasses,
                new ReturnTypeGenericTypeAssignable(methodInfo, cl),
                methodInfo.parameters().isEmpty() ? new AlwaysInvalidIndexGenericTypeAssignable()
                        : new MethodParamGenericTypeAssignable(methodInfo, 0, cl));

        configuration.setBeanId(bean.getIdentifier());
        configuration.setMethodName(methodInfo.name());

        configuration.setInvokerClass((Class<? extends Invoker>) recorderContext.classProxy(generatedInvokerName));
        String returnTypeName = methodInfo.returnType().name().toString();
        configuration.setReturnType(recorderContext.classProxy(returnTypeName));
        Class<?>[] parameterTypes = new Class[methodInfo.parameters().size()];
        for (int i = 0; i < methodInfo.parameters().size(); i++) {
            parameterTypes[i] = recorderContext.classProxy(methodInfo.parameters().get(i).name().toString());
        }
        configuration.setParameterTypes(parameterTypes);

        String incomingValue = getValue(methodInfo, INCOMING);
        configuration.setIncoming(incomingValue);
        String outgoingValue = getValue(methodInfo, OUTGOING);
        configuration.setOutgoing(outgoingValue);

        Shape shape = mediatorConfigurationSupport.determineShape(incomingValue, outgoingValue);
        configuration.setShape(shape);
        Acknowledgment.Strategy acknowledgment = mediatorConfigurationSupport.processSuppliedAcknowledgement(incomingValue,
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
        if (validationOutput.getUseBuilderTypes() != null) {
            configuration.setUseBuilderTypes(validationOutput.getUseBuilderTypes());
        } else {
            configuration.setUseBuilderTypes(false);
        }

        if (acknowledgment == null) {
            acknowledgment = mediatorConfigurationSupport.processDefaultAcknowledgement(shape,
                    validationOutput.getConsumption());
            configuration.setAcknowledgment(acknowledgment);
        }

        configuration.setMerge(mediatorConfigurationSupport.processMerge(incomingValue, () -> {
            AnnotationInstance instance = methodInfo.annotation(MERGE);
            if (instance != null) {
                AnnotationValue value = instance.value();
                if (value == null) {
                    return Merge.Mode.MERGE; // the default value of @Merge
                }
                return Merge.Mode.valueOf(value.asEnum());
            }
            return null;
        }));

        configuration.setBroadcastValue(mediatorConfigurationSupport.processBroadcast(outgoingValue, () -> {
            AnnotationInstance instance = methodInfo.annotation(BROADCAST);
            if (instance != null) {
                AnnotationValue value = instance.value();
                if (value == null) {
                    return 0; // the default value of @Broadcast
                }
                return value.asInt();
            }
            return null;
        }));

        return configuration;
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

    private static String fullMethodName(MethodInfo methodInfo) {
        return methodInfo.declaringClass() + "#" + methodInfo.name();
    }

    private static class ReturnTypeGenericTypeAssignable extends JandexGenericTypeAssignable {

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
    }

    private static class AlwaysInvalidIndexGenericTypeAssignable implements MediatorConfigurationSupport.GenericTypeAssignable {

        @Override
        public Result check(Class<?> target, int index) {
            return Result.InvalidIndex;
        }
    }

    private static class MethodParamGenericTypeAssignable extends JandexGenericTypeAssignable {

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
