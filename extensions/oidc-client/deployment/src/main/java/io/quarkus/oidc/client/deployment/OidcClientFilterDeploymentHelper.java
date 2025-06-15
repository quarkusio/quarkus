package io.quarkus.oidc.client.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

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

    public OidcClientFilterDeploymentHelper(Class<T> baseClass, BuildProducer<GeneratedBeanBuildItem> generatedBean) {
        this.baseClass = baseClass;
        this.classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);
        this.targetPackage = DotNames.internalPackageNameWithTrailingSlash(DotName.createSimple(baseClass.getName()));
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
}
