package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.resteasy.deployment.RestPathAnnotationProcessor.getAllClassInterfaces;
import static io.quarkus.resteasy.deployment.RestPathAnnotationProcessor.isRestEndpointMethod;
import static io.quarkus.security.spi.SecurityTransformerUtils.hasSecurityAnnotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.runtime.CompositeExceptionMapper;
import io.quarkus.resteasy.runtime.EagerSecurityFilter;
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.runtime.SecurityContextFilter;
import io.quarkus.resteasy.runtime.StandardSecurityCheckInterceptor;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.runtime.vertx.JsonArrayReader;
import io.quarkus.resteasy.runtime.vertx.JsonArrayWriter;
import io.quarkus.resteasy.runtime.vertx.JsonObjectReader;
import io.quarkus.resteasy.runtime.vertx.JsonObjectWriter;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.security.spi.AdditionalSecuredMethodsBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.devmode.AdditionalRouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

public class ResteasyBuiltinsProcessor {

    protected static final String META_INF_RESOURCES = "META-INF/resources";
    private static final Logger LOG = Logger.getLogger(ResteasyBuiltinsProcessor.class);

    @BuildStep
    void setUpDenyAllJaxRs(CombinedIndexBuildItem index,
            JaxRsSecurityConfig config,
            ResteasyDeploymentBuildItem resteasyDeployment,
            BuildProducer<AdditionalSecuredMethodsBuildItem> additionalSecuredClasses) {
        if (resteasyDeployment != null && (config.denyJaxRs || config.defaultRolesAllowed.isPresent())) {
            final List<MethodInfo> methods = new ArrayList<>();

            // add endpoints
            List<String> resourceClasses = resteasyDeployment.getDeployment().getScannedResourceClasses();
            for (String className : resourceClasses) {
                ClassInfo classInfo = index.getIndex().getClassByName(DotName.createSimple(className));
                if (classInfo == null)
                    throw new IllegalStateException("Unable to find class info for " + className);
                // add unannotated class endpoints as well as parent class unannotated endpoints
                addAllUnannotatedEndpoints(index, classInfo, methods);

                // interface endpoints implemented on resources are already in, now we need to resolve default interface
                // methods as there, CDI interceptors won't work, therefore neither will our additional secured methods
                Collection<ClassInfo> interfaces = getAllClassInterfaces(index, List.of(classInfo), new ArrayList<>());
                if (!interfaces.isEmpty()) {
                    final List<MethodInfo> interfaceEndpoints = new ArrayList<>();
                    for (ClassInfo anInterface : interfaces) {
                        addUnannotatedEndpoints(index, anInterface, interfaceEndpoints);
                    }
                    // look for implementors as implementors on resource classes are secured by CDI interceptors
                    if (!interfaceEndpoints.isEmpty()) {
                        interfaceBlock: for (MethodInfo interfaceEndpoint : interfaceEndpoints) {
                            if (interfaceEndpoint.isDefault()) {
                                for (MethodInfo endpoint : methods) {
                                    boolean nameParamsMatch = endpoint.name().equals(interfaceEndpoint.name())
                                            && (interfaceEndpoint.parameterTypes().equals(endpoint.parameterTypes()));
                                    if (nameParamsMatch) {
                                        // whether matched method is declared on class that implements interface endpoint
                                        Predicate<DotName> isEndpointInterface = interfaceEndpoint.declaringClass()
                                                .name()::equals;
                                        if (endpoint.declaringClass().interfaceNames().stream().anyMatch(isEndpointInterface)) {
                                            continue interfaceBlock;
                                        }
                                    }
                                }
                                String configProperty = config.denyJaxRs ? "quarkus.security.jaxrs.deny-unannotated-endpoints"
                                        : "quarkus.security.jaxrs.default-roles-allowed";
                                // this is logging only as I'm a bit worried about false positives and breaking things
                                // for what is very much edge case
                                LOG.warn("Default interface method '" + interfaceEndpoint
                                        + "' cannot be secured with the '" + configProperty
                                        + "' configuration property. Please implement this method for CDI "
                                        + "interceptor binding to work");
                            }
                        }
                    }
                }
            }

            if (!methods.isEmpty()) {
                if (config.denyJaxRs) {
                    additionalSecuredClasses.produce(new AdditionalSecuredMethodsBuildItem(methods));
                } else {
                    additionalSecuredClasses
                            .produce(new AdditionalSecuredMethodsBuildItem(methods, config.defaultRolesAllowed));
                }
            }
        }
    }

    private static void addAllUnannotatedEndpoints(CombinedIndexBuildItem index, ClassInfo classInfo,
            List<MethodInfo> methods) {
        if (classInfo == null) {
            return;
        }
        addUnannotatedEndpoints(index, classInfo, methods);
        if (classInfo.superClassType() != null && !classInfo.superClassType().name().equals(DotName.OBJECT_NAME)) {
            addAllUnannotatedEndpoints(index, index.getIndex().getClassByName(classInfo.superClassType().name()), methods);
        }
    }

    private static void addUnannotatedEndpoints(CombinedIndexBuildItem index, ClassInfo classInfo, List<MethodInfo> methods) {
        if (!hasSecurityAnnotation(classInfo)) {
            for (MethodInfo methodInfo : classInfo.methods()) {
                if (isRestEndpointMethod(index, methodInfo) && !hasSecurityAnnotation(methodInfo)) {
                    methods.add(methodInfo);
                }
            }
        }
    }

    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setUpSecurity(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItem, Capabilities capabilities) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(UnauthorizedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(ForbiddenExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationFailedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationRedirectExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationCompletionExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(CompositeExceptionMapper.class.getName()));
        if (capabilities.isPresent(Capability.SECURITY)) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(SecurityContextFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(SecurityContextFilter.class));
            providers.produce(new ResteasyJaxrsProviderBuildItem(EagerSecurityFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(EagerSecurityFilter.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.RolesAllowedInterceptor.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem
                    .unremovableOf(StandardSecurityCheckInterceptor.PermissionsAllowedInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.PermitAllInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.AuthenticatedInterceptor.class));
        }
    }

    @BuildStep
    void vertxProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        // These providers should work even if jackson-databind is not on the classpath
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayWriter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectWriter.class.getName()));
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void setupExceptionMapper(BuildProducer<ResteasyJaxrsProviderBuildItem> providers, HttpRootPathBuildItem httpRoot,
            ExceptionMapperRecorder recorder) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(NotFoundExceptionMapper.class.getName()));
        recorder.setHttpRoot(httpRoot.getRootPath());
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addStaticResourcesExceptionMapper(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ExceptionMapperRecorder recorder) {
        recorder.setStaticResourceRoots(applicationArchivesBuildItem.getAllApplicationArchives().stream()
                .map(i -> i.apply(t -> {
                    var p = t.getPath(META_INF_RESOURCES);
                    return p == null ? null : p.toAbsolutePath().toString();
                }))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addAdditionalEndpointsExceptionMapper(List<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            ExceptionMapperRecorder recorder, HttpRootPathBuildItem httpRoot) {
        List<AdditionalRouteDescription> endpoints = displayableEndpoints
                .stream()
                .map(displayableAdditionalBuildItem -> new AdditionalRouteDescription(
                        displayableAdditionalBuildItem.getEndpoint(httpRoot), displayableAdditionalBuildItem.getDescription()))
                .sorted()
                .collect(Collectors.toList());

        recorder.setAdditionalEndpoints(endpoints);
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addReactiveRoutesExceptionMapper(List<RouteDescriptionBuildItem> routeDescriptions,
            ExceptionMapperRecorder recorder, HttpRootPathBuildItem httpRoot) {
        List<RouteDescription> reactiveRoutes = new ArrayList<>();
        for (RouteDescriptionBuildItem description : routeDescriptions) {
            reactiveRoutes.add(description.getDescription());
        }
        recorder.setReactiveRoutes(reactiveRoutes);
    }
}
