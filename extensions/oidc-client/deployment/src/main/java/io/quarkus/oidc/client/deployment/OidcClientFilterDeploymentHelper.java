package io.quarkus.oidc.client.deployment;

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

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;

/**
 * Helps generate Oidc request filter based on {@link AbstractTokensProducer}.
 */
public class OidcClientFilterDeploymentHelper<T extends AbstractTokensProducer> {

    private final Map<String, String> clientNameToGeneratedClass = new HashMap<>();
    private final Class<T> baseClass;
    private final ClassOutput classOutput;
    private final String targetPackage;
    private final boolean refreshOnUnauthorized;

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
    public String getOrCreateNamedTokensProducerFor(String oidcClientName) {
        Objects.requireNonNull(oidcClientName);
        // do not create class for same client twice
        return clientNameToGeneratedClass.computeIfAbsent(oidcClientName, new Function<String, String>() {
            @Override
            public String apply(String s) {
                final String generatedName = targetPackage + baseClass.getSimpleName() + "_" + sanitize(oidcClientName);

                try (ClassCreator creator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                        .superClass(baseClass).build()) {
                    creator.addAnnotation(DotNames.SINGLETON.toString());
                    creator.addAnnotation(DotNames.UNREMOVABLE.toString());

                    try (MethodCreator clientIdMethod = creator.getMethodCreator("clientId", Optional.class)) {
                        clientIdMethod.setModifiers(Modifier.PROTECTED);

                        clientIdMethod.returnValue(clientIdMethod.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                                clientIdMethod.load(oidcClientName)));
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
                }

                return generatedName.replace('/', '.');
            }
        });
    }

    public DotName getOrCreateFilter(String oidcClientName) {
        return DotName.createSimple(getOrCreateNamedTokensProducerFor(oidcClientName));
    }

    public static String getClientName(AnnotationInstance annotationInstance) {
        final AnnotationValue annotationValue = annotationInstance.value();
        if (annotationValue != null && !annotationValue.asString().isEmpty()) {
            return annotationValue.asString();
        }
        return null;
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
}
