package io.quarkus.oidc.token.propagation.common.deployment;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;

public final class AccessTokenRequestFilterGenerator {

    private static final int AUTHENTICATION = 1000;

    private record ClientNameAndExchangeToken(String clientName, boolean exchangeTokenActivated) {
    }

    private final BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
    private final BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer;
    private final Class<?> requestFilterClass;
    private final Map<ClientNameAndExchangeToken, String> cache = new HashMap<>();

    public AccessTokenRequestFilterGenerator(BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer, Class<?> requestFilterClass) {
        this.unremovableBeansProducer = unremovableBeansProducer;
        this.reflectiveClassProducer = reflectiveClassProducer;
        this.generatedBeanProducer = generatedBeanProducer;
        this.requestFilterClass = requestFilterClass;
    }

    public String generateClass(AccessTokenInstanceBuildItem instance) {
        return cache.computeIfAbsent(
                new ClientNameAndExchangeToken(instance.getClientName(), instance.exchangeTokenActivated()), i -> {
                    var adaptor = new GeneratedBeanGizmoAdaptor(generatedBeanProducer);
                    String className = createUniqueClassName(i);
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
                    }
                    unremovableBeansProducer.produce(UnremovableBeanBuildItem.beanClassNames(className));
                    reflectiveClassProducer
                            .produce(ReflectiveClassBuildItem.builder(className)
                                    .reason(getClass().getName())
                                    .methods().fields().constructors().build());
                    return className;
                });
    }

    private String createUniqueClassName(ClientNameAndExchangeToken i) {
        return "%s_%sClient_%sTokenExchange".formatted(requestFilterClass.getName(), clientName(i.clientName()),
                exchangeTokenName(i.exchangeTokenActivated()));
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
}
