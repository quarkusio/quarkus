package io.quarkus.resteasy.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.runtime.QuarkusRestPathTemplate;
import io.quarkus.resteasy.runtime.QuarkusRestPathTemplateInterceptor;
import io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

public class RestPathAnnotationProcessor {

    static final DotName REST_PATH = DotName.createSimple("jakarta.ws.rs.Path");
    static final DotName REGISTER_REST_CLIENT = DotName
            .createSimple("org.eclipse.microprofile.rest.client.inject.RegisterRestClient");
    static final DotName TEMPLATE_PATH = DotName.createSimple(QuarkusRestPathTemplate.class.getName());
    static final DotName TEMPLATE_PATH_INTERCEPTOR = DotName.createSimple(QuarkusRestPathTemplateInterceptor.class.getName());

    public static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");

    @BuildStep()
    AdditionalBeanBuildItem registerBeanClasses(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (notRequired(capabilities, metricsCapability)) {
            return null;
        }

        return AdditionalBeanBuildItem.builder()
                .addBeanClass(TEMPLATE_PATH.toString())
                .addBeanClass(TEMPLATE_PATH_INTERCEPTOR.toString())
                .build();
    }

    @BuildStep
    void findRestPaths(
            CombinedIndexBuildItem index,
            Capabilities capabilities, Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            Optional<ResteasyJaxrsConfigBuildItem> restApplicationPathBuildItem) {

        if (notRequired(capabilities, metricsCapability)) {
            // Don't create transformer if Micrometer or OpenTelemetry are not present
            return;
        }

        String pathPrefix = null;
        if (restApplicationPathBuildItem.isPresent()) {
            ResteasyJaxrsConfigBuildItem pathItem = restApplicationPathBuildItem.get();
            if (!pathItem.getDefaultPath().equals(pathItem.getRootPath())) {
                pathPrefix = slashify(pathItem.getDefaultPath()); // This is just the @ApplicationPath
            }
        }

        final String restPathPrefix = pathPrefix;

        transformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                AnnotationTarget target = ctx.getTarget();
                MethodInfo methodInfo = target.asMethod();
                ClassInfo classInfo = methodInfo.declaringClass();

                if (!isRestEndpointMethod(index, methodInfo)) {
                    return;
                }
                // Don't create annotations for rest clients
                if (classInfo.declaredAnnotation(REGISTER_REST_CLIENT) != null) {
                    return;
                }

                AnnotationInstance annotation = methodInfo.annotation(REST_PATH);
                StringBuilder stringBuilder;
                if (annotation != null) {
                    stringBuilder = new StringBuilder(slashify(annotation.value().asString()));
                } else {
                    // Fallback: look for @Path on interface-method with same name
                    stringBuilder = searchPathAnnotationOnInterfaces(index, methodInfo)
                            .map(annotationInstance -> new StringBuilder(slashify(annotationInstance.value().asString())))
                            .orElse(new StringBuilder());
                }

                // Look for @Path annotation on the class
                annotation = classInfo.declaredAnnotation(REST_PATH);
                if (annotation != null) {
                    stringBuilder.insert(0, slashify(annotation.value().asString()));
                } else {
                    // Fallback: look for @Path on interfaces
                    getAllClassInterfaces(index, List.of(classInfo), new ArrayList<>()).stream()
                            .filter(interfaceClassInfo -> interfaceClassInfo.hasDeclaredAnnotation(REST_PATH))
                            .findFirst()
                            .map(interfaceClassInfo -> interfaceClassInfo.declaredAnnotation(REST_PATH).value())
                            .ifPresent(annotationValue -> stringBuilder.insert(0, slashify(annotationValue.asString())));
                }

                if (restPathPrefix != null) {
                    stringBuilder.insert(0, restPathPrefix);
                }

                // Now make sure there is a leading path, and no duplicates
                String templatePath = MULTIPLE_SLASH_PATTERN.matcher('/' + stringBuilder.toString()).replaceAll("/");

                // resulting path (used as observability attributes) should start with a '/'
                ctx.transform()
                        .add(TEMPLATE_PATH, AnnotationValue.createStringValue("value", templatePath))
                        .done();
            }
        }));
    }

    String slashify(String path) {
        // avoid doubles later. Empty for now
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        // remove doubles
        path = MULTIPLE_SLASH_PATTERN.matcher(path).replaceAll("/");
        // Label value consistency: result should not end with a slash
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty() || path.startsWith("/")) {
            return path;
        }
        return '/' + path;
    }

    /**
     * Searches for the same method as passed in methodInfo parameter in all implemented interfaces and yields an
     * Optional containing the JAX-RS Path annotation.
     *
     * @param index Jandex-Index for additional lookup
     * @param methodInfo the method to find
     * @return Optional with the annotation if found. Never null.
     */
    static Optional<AnnotationInstance> searchPathAnnotationOnInterfaces(CombinedIndexBuildItem index, MethodInfo methodInfo) {

        Collection<ClassInfo> allClassInterfaces = getAllClassInterfaces(index, List.of(methodInfo.declaringClass()),
                new ArrayList<>());

        return allClassInterfaces.stream()
                .map(interfaceClassInfo -> interfaceClassInfo.method(
                        methodInfo.name(),
                        methodInfo.parameterTypes().toArray(new Type[] {})))
                .filter(Objects::nonNull)
                .findFirst()
                .flatMap(resolvedMethodInfo -> Optional.ofNullable(resolvedMethodInfo.annotation(REST_PATH)));
    }

    /**
     * Recursively get all interfaces given as classInfo collection.
     *
     * @param index Jandex-Index for additional lookup
     * @param classInfos the class(es) to search. Ends the recursion when empty.
     * @param resultAcc accumulator for tail-recursion
     * @return Collection of all interfaces und their parents. Never null.
     */
    static Collection<ClassInfo> getAllClassInterfaces(
            CombinedIndexBuildItem index,
            Collection<ClassInfo> classInfos,
            List<ClassInfo> resultAcc) {
        Objects.requireNonNull(index);
        Objects.requireNonNull(classInfos);
        Objects.requireNonNull(resultAcc);
        if (classInfos.isEmpty()) {
            return resultAcc;
        }
        List<ClassInfo> interfaces = classInfos.stream()
                .flatMap(classInfo -> classInfo.interfaceNames().stream())
                .map(dotName -> index.getIndex().getClassByName(dotName))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
        resultAcc.addAll(interfaces);
        return getAllClassInterfaces(index, interfaces, resultAcc);
    }

    static boolean isRestEndpointMethod(CombinedIndexBuildItem index, MethodInfo methodInfo) {

        if (!methodInfo.hasAnnotation(REST_PATH)) {
            // Check for @Path on class and not method
            for (AnnotationInstance annotation : methodInfo.annotations()) {
                if (ResteasyDotNames.JAXRS_METHOD_ANNOTATIONS.contains(annotation.name())) {
                    return true;
                }
            }
            // Search for interface
            return searchPathAnnotationOnInterfaces(index, methodInfo).isPresent();
        }
        return true;
    }

    private boolean notRequired(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        return capabilities.isMissing(Capability.RESTEASY) ||
                (capabilities.isMissing(Capability.OPENTELEMETRY_TRACER) &&
                        !(metricsCapability.isPresent()
                                && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));
    }
}
