package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.security.spi.SecurityTransformerUtils.hasSecurityAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

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
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.runtime.SecurityContextFilter;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.runtime.vertx.JsonArrayReader;
import io.quarkus.resteasy.runtime.vertx.JsonArrayWriter;
import io.quarkus.resteasy.runtime.vertx.JsonObjectReader;
import io.quarkus.resteasy.runtime.vertx.JsonObjectWriter;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.security.spi.AdditionalSecuredClassesBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.devmode.AdditionalRouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

public class ResteasyBuiltinsProcessor {

    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @BuildStep
    void setUpDenyAllJaxRs(CombinedIndexBuildItem index,
            JaxRsSecurityConfig config,
            ResteasyDeploymentBuildItem resteasyDeployment,
            BuildProducer<AdditionalSecuredClassesBuildItem> additionalSecuredClasses) {
        if ((config.denyJaxRs) && (resteasyDeployment != null)) {
            final List<ClassInfo> classes = new ArrayList<>();

            List<String> resourceClasses = resteasyDeployment.getDeployment().getScannedResourceClasses();
            for (String className : resourceClasses) {
                ClassInfo classInfo = index.getIndex().getClassByName(DotName.createSimple(className));
                if (!hasSecurityAnnotation(classInfo)) {
                    classes.add(classInfo);
                }
            }

            additionalSecuredClasses.produce(new AdditionalSecuredClassesBuildItem(classes));
        } else if (config.defaultRolesAllowed.isPresent() && resteasyDeployment != null) {
            final List<ClassInfo> classes = new ArrayList<>();

            List<String> resourceClasses = resteasyDeployment.getDeployment().getScannedResourceClasses();
            for (String className : resourceClasses) {
                ClassInfo classInfo = index.getIndex().getClassByName(DotName.createSimple(className));
                if (!hasSecurityAnnotation(classInfo)) {
                    classes.add(classInfo);
                }
            }
            additionalSecuredClasses.produce(new AdditionalSecuredClassesBuildItem(classes, config.defaultRolesAllowed));
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
                .map(i -> i.getChildPath(META_INF_RESOURCES))
                .filter(p -> p != null)
                .map(p -> p.toAbsolutePath().toString())
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
