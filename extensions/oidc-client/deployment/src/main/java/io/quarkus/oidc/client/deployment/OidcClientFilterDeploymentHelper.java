package io.quarkus.oidc.client.deployment;

import java.lang.annotation.RetentionPolicy;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.MethodDescription;

/**
 * Helps generate Oidc request filter based on {@link AbstractTokensProducer}.
 */
public class OidcClientFilterDeploymentHelper<T extends AbstractTokensProducer> {

    public static final String DEFAULT_OIDC_REQUEST_FILTER_NAME = "default-oidc-request-filter";
    private final Map<TokensProducerKey, String> clientNameToGeneratedClass = new HashMap<>();
    private final Map<String, Boolean> restClientToIsMethodAnnotated = new HashMap<>();
    private final Class<T> baseClass;
    private final Gizmo gizmo;
    private final String targetPackage;
    private final boolean refreshOnUnauthorized;

    private record TokensProducerKey(String clientName, MethodInfo methodInfo) {
    }

    public OidcClientFilterDeploymentHelper(Class<T> baseClass, BuildProducer<GeneratedBeanBuildItem> generatedBean,
            boolean refreshOnUnauthorized) {
        this.baseClass = baseClass;
        this.gizmo = Gizmo.create(new GeneratedBeanGizmo2Adaptor(generatedBean));
        this.targetPackage = DotNames
                .packagePrefix(DotName.createSimple(baseClass));
        this.refreshOnUnauthorized = refreshOnUnauthorized;
    }

    /**
     * For {@code baseClass} Xyz creates tokens producer class like follows:
     *
     * <pre>
     * &#64;Singleton
     * &#64;Unremovable
     * public class Xyz_oidcClientName extends Xyz {
     *
     *     &#64;Override
     *     protected Optional<String> clientId() {
     *         return Optional.of("oidcClientName");
     *     }
     * }
     * </pre>
     */
    public String getOrCreateNamedTokensProducerFor(String oidcClientName, AnnotationInstance annotationInstance) {
        Objects.requireNonNull(oidcClientName);
        Objects.requireNonNull(annotationInstance);
        // do not create class for same client twice
        MethodInfo targetMethod = annotationInstance.target().kind() == AnnotationTarget.Kind.METHOD
                ? annotationInstance.target().asMethod()
                : null;
        String generatedProducerName = clientNameToGeneratedClass.computeIfAbsent(
                new TokensProducerKey(oidcClientName, targetMethod),
                new Function<TokensProducerKey, String>() {
                    @Override
                    public String apply(TokensProducerKey key) {
                        final String generatedName = targetPackage + "." + baseClass.getSimpleName() + "_"
                                + sanitize(oidcClientName)
                                + possiblyTargetMethodSuffix(targetMethod);

                        gizmo.class_(generatedName, cc -> {
                            cc.extends_(baseClass);
                            cc.addAnnotation(Singleton.class);
                            cc.addAnnotation(Unremovable.class);
                            cc.defaultConstructor();

                            if (!DEFAULT_OIDC_REQUEST_FILTER_NAME.equals(oidcClientName)) {
                                cc.method("clientId", mc -> {
                                    mc.protected_();
                                    mc.returning(Optional.class);

                                    mc.body(bc -> {
                                        bc.return_(bc.invokeStatic(
                                                MethodDesc.of(Optional.class, "of", Optional.class, Object.class),
                                                Const.of(oidcClientName)));
                                    });
                                });
                            }

                            if (refreshOnUnauthorized) {
                                // protected boolean refreshOnUnauthorized() {
                                //  return true;
                                // }
                                cc.method("refreshOnUnauthorized", mc -> {
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
                            if (targetMethod != null) {
                                cc.method("getMethodDescription", mc -> {
                                    mc.addAnnotation(ClassDesc.of(Override.class.getName()), RetentionPolicy.CLASS, ac -> {
                                    });
                                    mc.protected_();
                                    mc.returning(MethodDescription.class);

                                    mc.body(bc -> {
                                        // String[] paramTypes
                                        var paramTypes = bc.newArray(String.class,
                                                targetMethod.parameterTypes(),
                                                pt -> Const.of(pt.name().toString()));
                                        // new MethodDescription(declaringClassName, methodName, parameterTypes)
                                        var newMethodDescription = bc.new_(
                                                ConstructorDesc.of(MethodDescription.class,
                                                        String.class, String.class, String[].class),
                                                Const.of(targetMethod.declaringClass().name().toString()),
                                                Const.of(targetMethod.name()),
                                                paramTypes);
                                        // return new MethodDescription(declaringClassName, methodName, parameterTypes);
                                        bc.return_(newMethodDescription);
                                    });
                                });
                            }
                        });

                        return generatedName;
                    }
                });

        // validate as it doesn't make sense to combine @OidcClientFilter on method and class-level
        // therefore, this code doesn't support it, and it could result in undefined behavior
        var targetRestClientName = getTargetRestClientName(annotationInstance);
        if (restClientToIsMethodAnnotated.containsKey(targetRestClientName)) {
            var wasMethodAnnotated = restClientToIsMethodAnnotated.get(targetRestClientName);
            if (targetMethod != null) {
                boolean wasClassAnnotated = !wasMethodAnnotated;
                if (wasClassAnnotated) {
                    throwBothMethodAndClassAnnotated(annotationInstance);
                }
            } else if (wasMethodAnnotated) {
                throwBothMethodAndClassAnnotated(annotationInstance);
            }
        } else {
            if (targetMethod != null) {
                restClientToIsMethodAnnotated.put(targetRestClientName, true);
            } else {
                restClientToIsMethodAnnotated.put(targetRestClientName, false);
            }
        }

        return generatedProducerName;
    }

    private static String possiblyTargetMethodSuffix(MethodInfo method) {
        if (method != null) {
            return "_" + sanitize(method.name());
        }
        return "";
    }

    public DotName getOrCreateFilter(String oidcClientName, AnnotationInstance instance) {
        return DotName.createSimple(getOrCreateNamedTokensProducerFor(oidcClientName, instance));
    }

    public static String getClientName(AnnotationInstance annotationInstance) {
        final AnnotationValue annotationValue = annotationInstance.value();
        if (annotationValue != null && !annotationValue.asString().isEmpty()) {
            return annotationValue.asString();
        }
        return null;
    }

    public static ClassInfo getTargetRestClient(AnnotationInstance instance) {
        if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
            return instance.target().asMethod().declaringClass();
        }
        return instance.target().asClass();
    }

    public static String getTargetRestClientName(AnnotationInstance instance) {
        return getTargetRestClient(instance).name().toString();
    }

    public static String sanitize(String oidcClientName) {
        return oidcClientName.replaceAll("\\W+", "");
    }

    public static List<ClassInfo> detectCustomFiltersThatRequireResponseFilter(Class<?> abstractFilterClass,
            Class<?> registerProviderClass, Class<?> registerProvidersClass, IndexView index) {
        List<ClassInfo> result = new ArrayList<>();
        for (ClassInfo subClass : index.getKnownDirectSubclasses(abstractFilterClass)) {
            if (!subClass.isInterface() && !subClass.isAbstract()) {
                var refreshOnUnauthorizedMethod = subClass.method("refreshOnUnauthorized");
                if (refreshOnUnauthorizedMethod != null) {
                    final DotName declaringClassName = refreshOnUnauthorizedMethod.declaringClass().name();
                    boolean declaredOnThisClass = declaringClassName.equals(subClass.name());
                    if (declaredOnThisClass) {
                        result.addAll(index
                                .getAnnotations(registerProviderClass).stream()
                                .filter(ai -> ai.value() != null)
                                .filter(ai -> ai.value().asClass().name().equals(declaringClassName))
                                .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                                .map(ai -> ai.target().asClass())
                                .toList());
                        result.addAll(index
                                .getAnnotations(registerProvidersClass).stream()
                                .filter(ai -> ai.value() != null)
                                .filter(ai -> registersDeclaringClass(ai, declaringClassName))
                                .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                                .map(ai -> ai.target().asClass())
                                .toList());
                    }
                }
            }
        }
        return result;
    }

    private static void throwBothMethodAndClassAnnotated(AnnotationInstance instance) {
        throw new RuntimeException(OidcClientFilter.class.getName() + " annotation can be applied either on class "
                + getTargetRestClient(instance) + " or its methods");
    }

    private static boolean registersDeclaringClass(AnnotationInstance ai, DotName declaringClassName) {
        return Arrays.stream(ai.value().asNestedArray())
                .anyMatch(nestedAi -> nestedAi.value().asClass().name().equals(declaringClassName));
    }
}
