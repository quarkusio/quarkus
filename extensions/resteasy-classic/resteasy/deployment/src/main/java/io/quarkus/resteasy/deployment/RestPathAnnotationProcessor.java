package io.quarkus.resteasy.deployment;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.resteasy.runtime.QuarkusRestPathTemplate;
import io.quarkus.resteasy.runtime.QuarkusRestPathTemplateInterceptor;
import io.quarkus.resteasy.server.common.spi.ResteasyJaxrsConfigBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

public class RestPathAnnotationProcessor {

    static final DotName REST_PATH = DotName.createSimple("javax.ws.rs.Path");
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

                AnnotationInstance annotation = methodInfo.annotation(REST_PATH);
                if (annotation == null) {
                    return;
                }
                // Don't create annotations for rest clients
                if (classInfo.classAnnotation(REGISTER_REST_CLIENT) != null) {
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder(slashify(annotation.value().asString()));

                // Look for @Path annotation on the class
                annotation = classInfo.classAnnotation(REST_PATH);
                if (annotation != null) {
                    stringBuilder.insert(0, slashify(annotation.value().asString()));
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

    private boolean notRequired(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        return capabilities.isMissing(Capability.RESTEASY) ||
                (capabilities.isMissing(Capability.OPENTELEMETRY_TRACER) &&
                        !(metricsCapability.isPresent()
                                && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));
    }
}
