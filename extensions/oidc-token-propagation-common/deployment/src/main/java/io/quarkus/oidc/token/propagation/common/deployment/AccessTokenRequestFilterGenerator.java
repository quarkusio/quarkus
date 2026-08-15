package io.quarkus.oidc.token.propagation.common.deployment;

import java.lang.annotation.RetentionPolicy;
import java.lang.constant.ClassDesc;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.security.spi.runtime.MethodDescription;

public final class AccessTokenRequestFilterGenerator {

    private static final int AUTHENTICATION = 1000;
    private static final String SEPARATOR = "_";

    private record RequestFilterKey(String clientName, boolean exchangeTokenActivated, MethodDescription methodDescription) {
    }

    private final BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer;
    private final BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer;
    private final Class<?> requestFilterClass;
    private final Map<RequestFilterKey, String> cache = new HashMap<>();

    public AccessTokenRequestFilterGenerator(BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer, Class<?> requestFilterClass) {
        this.unremovableBeansProducer = unremovableBeansProducer;
        this.generatedBeanProducer = generatedBeanProducer;
        this.requestFilterClass = requestFilterClass;
    }

    public String generateClass(AccessTokenInstanceBuildItem instance) {
        MethodDescription methodDescription = createMethodDescription(instance.getTargetMethodInfo());
        return cache.computeIfAbsent(
                new RequestFilterKey(instance.getClientName(), instance.exchangeTokenActivated(), methodDescription),
                i -> {
                    Gizmo gizmo = Gizmo.create(new GeneratedBeanGizmo2Adaptor(generatedBeanProducer));
                    String className = createUniqueClassName(i, instance);
                    gizmo.class_(className, cc -> {
                        cc.extends_(requestFilterClass);
                        cc.addAnnotation(Priority.class, ac -> ac.add("value", AUTHENTICATION));
                        cc.addAnnotation(Singleton.class);
                        cc.defaultConstructor();

                        if (!i.clientName().isEmpty()) {
                            cc.method("getClientName", mc -> {
                                mc.addAnnotation(ClassDesc.of(Override.class.getName()), RetentionPolicy.CLASS, ac -> {
                                });
                                mc.protected_();
                                mc.returning(String.class);

                                mc.body(bc -> {
                                    bc.return_(i.clientName());
                                });
                            });
                        }
                        if (i.exchangeTokenActivated()) {
                            cc.method("isExchangeToken", mc -> {
                                mc.addAnnotation(ClassDesc.of(Override.class.getName()), RetentionPolicy.CLASS, ac -> {
                                });
                                mc.protected_();
                                mc.returning(boolean.class);

                                mc.body(bc -> {
                                    bc.return_(true);
                                });
                            });
                        }

                        /*
                         * protected MethodDescription getMethodDescription() {
                         * return new MethodDescription(declaringClassName, methodName, parameterTypes);
                         * }
                         */
                        if (i.methodDescription() != null) {
                            cc.method("getMethodDescription", mc -> {
                                mc.addAnnotation(ClassDesc.of(Override.class.getName()), RetentionPolicy.CLASS, ac -> {
                                });
                                mc.protected_();
                                mc.returning(MethodDescription.class);

                                mc.body(bc -> {
                                    // String[] paramTypes
                                    var paramTypes = bc.newArray(String.class,
                                            Arrays.asList(i.methodDescription().getParameterTypes()),
                                            Const::of);
                                    // new MethodDescription(declaringClassName, methodName, parameterTypes)
                                    var newMethodDescription = bc.new_(
                                            ConstructorDesc.of(MethodDescription.class,
                                                    String.class, String.class, String[].class),
                                            Const.of(i.methodDescription().getClassName()),
                                            Const.of(i.methodDescription().getMethodName()),
                                            paramTypes);
                                    // return new MethodDescription(declaringClassName, methodName, parameterTypes);
                                    bc.return_(newMethodDescription);
                                });
                            });
                        }
                    });
                    unremovableBeansProducer.produce(UnremovableBeanBuildItem.beanClassNames(className));
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
