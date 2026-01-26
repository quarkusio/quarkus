package io.quarkus.oidc.token.propagation.common.deployment;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.security.spi.runtime.MethodDescription;

public final class AccessTokenRequestFilterGenerator {

    private static final int AUTHENTICATION = 1000;
    private static final String SEPARATOR = "_";

    private record RequestFilterKey(String clientName, boolean exchangeTokenActivated, MethodDescription methodDescription) {
    }

    private final BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
    private final BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer;
    private final Class<?> requestFilterClass;
    private final Map<RequestFilterKey, String> cache = new HashMap<>();

    public AccessTokenRequestFilterGenerator(BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer, Class<?> requestFilterClass) {
        this.unremovableBeansProducer = unremovableBeansProducer;
        this.reflectiveClassProducer = reflectiveClassProducer;
        this.generatedBeanProducer = generatedBeanProducer;
        this.requestFilterClass = requestFilterClass;
    }

    public String generateClass(AccessTokenInstanceBuildItem instance) {
        MethodDescription methodDescription = createMethodDescription(instance.getTargetMethodInfo());
        return cache.computeIfAbsent(
                new RequestFilterKey(instance.getClientName(), instance.exchangeTokenActivated(), methodDescription),
                i -> {
                    var adaptor = new GeneratedBeanGizmoAdaptor(generatedBeanProducer);
                    String className = createUniqueClassName(i, instance);
                    try (ClassCreator classCreator = ClassCreator.builder()
                            .className(className)
                            .superClass(requestFilterClass)
                            .classOutput(adaptor)
                            .build()) {
                        classCreator.addAnnotation(Priority.class).add("value", AUTHENTICATION);
                        classCreator.addAnnotation(Singleton.class);

                        if (!i.clientName().isEmpty()) {
                            try (var methodCreator = classCreator.getMethodCreator("getClientName", String.class)) {
                                methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                                methodCreator.setModifiers(Modifier.PROTECTED);
                                methodCreator.returnValue(methodCreator.load(i.clientName()));
                            }
                        }
                        if (i.exchangeTokenActivated()) {
                            try (var methodCreator = classCreator.getMethodCreator("isExchangeToken", boolean.class)) {
                                methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                                methodCreator.setModifiers(Modifier.PROTECTED);
                                methodCreator.returnBoolean(true);
                            }
                        }

                        /*
                         * protected MethodDescription getMethodDescription() {
                         * return new MethodDescription(declaringClassName, methodName, parameterTypes);
                         * }
                         */
                        if (i.methodDescription() != null) {
                            try (var methodCreator = classCreator.getMethodCreator("getMethodDescription",
                                    MethodDescription.class)) {
                                methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                                methodCreator.setModifiers(Modifier.PROTECTED);

                                // String methodName
                                var methodName = methodCreator.load(i.methodDescription().getMethodName());
                                // String declaringClassName
                                var declaringClassName = methodCreator.load(i.methodDescription().getClassName());
                                // String[] paramTypes
                                var paramTypes = methodCreator.marshalAsArray(String[].class,
                                        Arrays.stream(i.methodDescription().getParameterTypes()).map(methodCreator::load)
                                                .toArray(ResultHandle[]::new));
                                // new MethodDescription(declaringClassName, methodName, parameterTypes)
                                var methodDescriptionCtor = MethodDescriptor.ofConstructor(MethodDescription.class,
                                        String.class, String.class, String[].class);
                                var newMethodDescription = methodCreator.newInstance(methodDescriptionCtor, declaringClassName,
                                        methodName, paramTypes);
                                // return new MethodDescription(declaringClassName, methodName, parameterTypes);
                                methodCreator.returnValue(newMethodDescription);
                            }
                        }
                    }
                    unremovableBeansProducer.produce(UnremovableBeanBuildItem.beanClassNames(className));
                    reflectiveClassProducer
                            .produce(ReflectiveClassBuildItem.builder(className)
                                    .reason(getClass().getName())
                                    .methods().fields().constructors().build());
                    return className;
                });
    }

    private String createUniqueClassName(RequestFilterKey i, AccessTokenInstanceBuildItem instance) {
        String uniqueClassName = "%s_%sClient_%sTokenExchange".formatted(requestFilterClass.getName(),
                clientName(i.clientName()), exchangeTokenName(i.exchangeTokenActivated()));
        if (i.methodDescription != null) {
            var paramTypesAsString = Arrays.stream(i.methodDescription.getParameterTypes()).sorted()
                    .collect(Collectors.joining(SEPARATOR));
            uniqueClassName += HashUtil.sha1(i.methodDescription.getClassName() + SEPARATOR
                    + i.methodDescription.getMethodName() + SEPARATOR + paramTypesAsString);
        }
        return uniqueClassName;
    }

    private static String clientName(String clientName) {
        if (clientName.isEmpty()) {
            return "Default";
        } else {
            return clientName;
        }
    }

    private static String exchangeTokenName(boolean enabled) {
        if (enabled) {
            return "Enabled";
        } else {
            return "Default";
        }
    }

    private static MethodDescription createMethodDescription(MethodInfo mi) {
        if (mi == null) {
            return null;
        }

        String[] paramTypes = new String[mi.parametersCount()];
        for (int i = 0; i < mi.parametersCount(); i++) {
            paramTypes[i] = mi.parameterTypes().get(i).name().toString();
        }
        return new MethodDescription(mi.declaringClass().name().toString(), mi.name(),
                paramTypes);
    }
}
