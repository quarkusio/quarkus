package io.quarkus.oidc.client.deployment;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
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
    private final ClassOutput classOutput;
    private final String targetPackage;
    private final boolean refreshOnUnauthorized;

    private record TokensProducerKey(String clientName, MethodInfo methodInfo) {
    }

    public OidcClientFilterDeploymentHelper(Class<T> baseClass, BuildProducer<GeneratedBeanBuildItem> generatedBean,
            boolean refreshOnUnauthorized) {
        this.baseClass = baseClass;
        this.classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);
        this.targetPackage = DotNames
                .internalPackageNameWithTrailingSlash(DotName.createSimple(baseClass.getName()));
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
                        final String generatedName = targetPackage + baseClass.getSimpleName() + "_" + sanitize(oidcClientName)
                                + possiblyTargetMethodSuffix(targetMethod);

                        try (ClassCreator creator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                                .superClass(baseClass).build()) {
                            creator.addAnnotation(DotNames.SINGLETON.toString());
                            creator.addAnnotation(DotNames.UNREMOVABLE.toString());

                            if (!DEFAULT_OIDC_REQUEST_FILTER_NAME.equals(oidcClientName)) {
                                try (MethodCreator clientIdMethod = creator.getMethodCreator("clientId", Optional.class)) {
                                    clientIdMethod.setModifiers(Modifier.PROTECTED);

                                    clientIdMethod.returnValue(clientIdMethod.invokeStaticMethod(
                                            MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                                            clientIdMethod.load(oidcClientName)));
                                }
                            }

                            if (refreshOnUnauthorized) {
                                // protected boolean refreshOnUnauthorized() {
                                //  return true;
                                // }
                                try (MethodCreator methodCreator = creator.getMethodCreator("refreshOnUnauthorized",
                                        boolean.class)) {
                                    methodCreator.setModifiers(Modifier.PROTECTED);
                                    methodCreator.returnBoolean(true);
                                }
                            }

                            /*
                             * protected MethodDescription getMethodDescription() {
                             * return new MethodDescription(declaringClassName, methodName, parameterTypes);
                             * }
                             */
                            if (targetMethod != null) {
                                try (var methodCreator = creator.getMethodCreator("getMethodDescription",
                                        MethodDescription.class)) {
                                    methodCreator.addAnnotation(Override.class.getName(), RetentionPolicy.CLASS);
                                    methodCreator.setModifiers(Modifier.PROTECTED);

                                    // String methodName
                                    var methodName = methodCreator.load(targetMethod.name());
                                    // String declaringClassName
                                    var declaringClassName = methodCreator
                                            .load(targetMethod.declaringClass().name().toString());
                                    // String[] paramTypes
                                    var paramTypes = methodCreator.marshalAsArray(String[].class,
                                            targetMethod.parameterTypes().stream()
                                                    .map(pt -> pt.name().toString()).map(methodCreator::load)
                                                    .toArray(ResultHandle[]::new));
                                    // new MethodDescription(declaringClassName, methodName, parameterTypes)
                                    var methodDescriptionCtor = MethodDescriptor.ofConstructor(MethodDescription.class,
                                            String.class, String.class, String[].class);
                                    var newMethodDescription = methodCreator.newInstance(methodDescriptionCtor,
                                            declaringClassName,
                                            methodName, paramTypes);
                                    // return new MethodDescription(declaringClassName, methodName, parameterTypes);
                                    methodCreator.returnValue(newMethodDescription);
                                }
                            }
                        }

                        return generatedName.replace('/', '.');
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
            Class<?> registerProviderClass, IndexView index) {
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
}
