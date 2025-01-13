package io.quarkus.opentelemetry.deployment.tracing;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.SecurityEvents.SecurityEventType.ALL;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.SecurityEvents.SecurityEventType.AUTHENTICATION_SUCCESS;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.SecurityEvents.SecurityEventType.AUTHORIZATION_FAILURE;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.SecurityEvents.SecurityEventType.AUTHORIZATION_SUCCESS;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.enterprise.inject.spi.EventContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.SecurityEvents.SecurityEventType;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.cdi.TracerProducer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.websockets.WebSocketTracesInterceptorImpl;
import io.quarkus.opentelemetry.runtime.tracing.security.EndUserSpanProcessor;
import io.quarkus.opentelemetry.runtime.tracing.security.SecurityEventUtil;
import io.quarkus.vertx.http.deployment.spi.FrameworkEndpointsBuildItem;
import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;

@BuildSteps(onlyIf = TracerEnabled.class)
public class TracerProcessor {
    private static final Logger LOGGER = Logger.getLogger(TracerProcessor.class.getName());
    private static final DotName ID_GENERATOR = DotName.createSimple(IdGenerator.class.getName());
    private static final DotName RESOURCE = DotName.createSimple(Resource.class.getName());
    private static final DotName SAMPLER = DotName.createSimple(Sampler.class.getName());
    private static final DotName SPAN_EXPORTER = DotName.createSimple(SpanExporter.class.getName());
    private static final DotName SPAN_PROCESSOR = DotName.createSimple(SpanProcessor.class.getName());
    private static final DotName TEXT_MAP_PROPAGATOR = DotName.createSimple(TextMapPropagator.class.getName());
    private static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");

    @BuildStep
    UnremovableBeanBuildItem ensureProducersAreRetained(
            CombinedIndexBuildItem indexBuildItem,
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(TracerProducer.class)
                .build());

        IndexView index = indexBuildItem.getIndex();

        // Find all known SpanExporters and SpanProcessors
        Collection<String> knownClasses = new HashSet<>();
        knownClasses.add(ID_GENERATOR.toString());
        index.getAllKnownImplementors(ID_GENERATOR)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(RESOURCE.toString());
        index.getAllKnownImplementors(RESOURCE)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(SAMPLER.toString());
        index.getAllKnownImplementors(SAMPLER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(SPAN_EXPORTER.toString());
        index.getAllKnownImplementors(SPAN_EXPORTER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(SPAN_PROCESSOR.toString());
        index.getAllKnownImplementors(SPAN_PROCESSOR)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(TEXT_MAP_PROPAGATOR.toString());
        index.getAllKnownImplementors(TEXT_MAP_PROPAGATOR)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

        Set<String> retainProducers = new HashSet<>();

        for (AnnotationInstance annotation : index.getAnnotations(DotNames.PRODUCES)) {
            AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case METHOD:
                    MethodInfo method = target.asMethod();
                    String returnType = method.returnType().name().toString();
                    if (knownClasses.contains(returnType)) {
                        retainProducers.add(method.declaringClass().name().toString());
                    }
                    break;
                case FIELD:
                    FieldInfo field = target.asField();
                    String fieldType = field.type().name().toString();
                    if (knownClasses.contains(fieldType)) {
                        retainProducers.add(field.declaringClass().name().toString());
                    }
                    break;
                default:
                    break;
            }
        }

        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(retainProducers));
    }

    @BuildStep
    void dropNames(
            Optional<FrameworkEndpointsBuildItem> frameworkEndpoints,
            Optional<StaticResourcesBuildItem> staticResources,
            BuildProducer<DropNonApplicationUrisBuildItem> dropNonApplicationUris,
            BuildProducer<DropStaticResourcesBuildItem> dropStaticResources) {

        List<String> nonApplicationUris = new ArrayList<>();

        // Drop framework paths
        frameworkEndpoints.ifPresent(
                frameworkEndpointsBuildItem -> {
                    for (String endpoint : frameworkEndpointsBuildItem.getEndpoints()) {
                        // Management routes are using full urls -> Extract the path.
                        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                            try {
                                nonApplicationUris.add(new URL(endpoint).getPath());
                            } catch (Exception ignored) { // Not an URL
                                nonApplicationUris.add(endpoint);
                            }
                        } else {
                            nonApplicationUris.add(endpoint);
                        }
                    }
                });

        dropNonApplicationUris.produce(new DropNonApplicationUrisBuildItem(nonApplicationUris));

        // Drop Static Resources
        List<String> resources = new ArrayList<>();
        if (staticResources.isPresent()) {
            for (StaticResourcesBuildItem.Entry entry : staticResources.get().getEntries()) {
                if (!entry.isDirectory()) {
                    resources.add(entry.getPath());
                }
            }
        }
        dropStaticResources.produce(new DropStaticResourcesBuildItem(resources));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupSampler(
            TracerRecorder recorder,
            DropNonApplicationUrisBuildItem dropNonApplicationUris,
            DropStaticResourcesBuildItem dropStaticResources) {

        recorder.setupSampler(
                dropNonApplicationUris.getDropNames(),
                dropStaticResources.getDropNames());
    }

    @BuildStep(onlyIf = SecurityEventsEnabled.class)
    void registerSecurityEventObserver(Capabilities capabilities, OTelBuildConfig buildConfig,
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverConfiguratorBuildItem> observerProducer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            if (buildConfig.securityEvents().eventTypes().contains(ALL)) {
                observerProducer.produce(createEventObserver(observerRegistrationPhase, ALL, "addAllEvents"));
            } else {
                for (SecurityEventType eventType : buildConfig.securityEvents().eventTypes()) {
                    observerProducer.produce(createEventObserver(observerRegistrationPhase, eventType, "addEvent"));
                }
            }
        } else {
            LOGGER.warn("""
                    Exporting of Quarkus Security events as OpenTelemetry Span events is enabled,
                    but the Quarkus Security is missing. This feature will only work if you add the Quarkus Security extension.
                    """);
        }
    }

    @BuildStep(onlyIf = EndUserAttributesEnabled.class)
    void addEndUserAttributesSpanProcessor(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem.unremovableOf(EndUserSpanProcessor.class));
        }
    }

    @BuildStep(onlyIf = EndUserAttributesEnabled.class)
    void registerEndUserAttributesEventObserver(Capabilities capabilities,
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverConfiguratorBuildItem> observerProducer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            observerProducer
                    .produce(createEventObserver(observerRegistrationPhase, AUTHENTICATION_SUCCESS, "addEndUserAttributes"));
            observerProducer
                    .produce(createEventObserver(observerRegistrationPhase, AUTHORIZATION_SUCCESS, "updateEndUserAttributes"));
            observerProducer
                    .produce(createEventObserver(observerRegistrationPhase, AUTHORIZATION_FAILURE, "updateEndUserAttributes"));
        }
    }

    @BuildStep
    void registerWebSocketTracesInterceptor(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.WEBSOCKETS_NEXT)) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem.unremovableOf(WebSocketTracesInterceptorImpl.class));
        }
    }

    private static ObserverConfiguratorBuildItem createEventObserver(
            ObserverRegistrationPhaseBuildItem observerRegistrationPhase, SecurityEventType eventType, String utilMethodName) {
        return new ObserverConfiguratorBuildItem(observerRegistrationPhase.getContext()
                .configure()
                .beanClass(DotName.createSimple(TracerProducer.class.getName()))
                .observedType(eventType.getObservedType())
                .notify(mc -> {
                    // Object event = eventContext.getEvent();
                    ResultHandle eventContext = mc.getMethodParam(0);
                    ResultHandle event = mc.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(EventContext.class, "getEvent", Object.class), eventContext);
                    // Call to SecurityEventUtil#addEvent or SecurityEventUtil#addAllEvents, that is:
                    // SecurityEventUtil.addAllEvents((SecurityEvent) event)
                    // SecurityEventUtil.addEvent((AuthenticationSuccessEvent) event)
                    // Method 'addEvent' is overloaded and accepts SecurityEventType#getObservedType
                    mc.invokeStaticMethod(MethodDescriptor.ofMethod(SecurityEventUtil.class, utilMethodName,
                            void.class, eventType.getObservedType()), mc.checkCast(event, eventType.getObservedType()));
                    mc.returnNull();
                }));
    }

    private static boolean containsPathExpression(String value) {
        return value.indexOf('{') != -1;
    }

    private static String sanitizeForTraceless(final String path) {
        int braceIndex = path.indexOf('{');
        if (braceIndex == -1) {
            return path;
        }
        if (braceIndex > 0 && path.charAt(braceIndex - 1) == '/') {
            return path.substring(0, braceIndex - 1);
        } else {
            return path.substring(0, braceIndex);
        }
    }

    private static boolean isClassAnnotatedWithPath(AnnotationInstance annotation) {
        return annotation.target().kind().equals(AnnotationTarget.Kind.CLASS) &&
                annotation.name().equals(PATH);
    }

    private String combinePaths(String basePath, String relativePath) {
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return basePath + relativePath;
    }

    static final class SecurityEventsEnabled implements BooleanSupplier {

        private final boolean enabled;

        SecurityEventsEnabled(OTelBuildConfig config) {
            this.enabled = config.securityEvents().enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }

    static final class EndUserAttributesEnabled implements BooleanSupplier {

        private final boolean enabled;

        EndUserAttributesEnabled(OTelBuildConfig config) {
            this.enabled = config.traces().addEndUserAttributes();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
