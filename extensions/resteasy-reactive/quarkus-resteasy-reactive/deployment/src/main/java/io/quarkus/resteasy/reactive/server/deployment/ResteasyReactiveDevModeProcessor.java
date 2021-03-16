package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.reactive.server.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.devmode.AdditionalRouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

public class ResteasyReactiveDevModeProcessor {

    private static final String META_INF_RESOURCES = "META-INF/resources";

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void setupExceptionMapper(BuildProducer<CustomExceptionMapperBuildItem> customExceptionMappers,
            HttpRootPathBuildItem httpRoot,
            ExceptionMapperRecorder recorder) {
        customExceptionMappers.produce(new CustomExceptionMapperBuildItem(NotFoundExceptionMapper.class.getName()));
        recorder.setHttpRoot(httpRoot.getRootPath());
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addStaticResourcesExceptionMapper(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ExceptionMapperRecorder recorder) {
        recorder.setStaticResourceRoots(applicationArchivesBuildItem.getAllApplicationArchives().stream()
                .map(i -> i.getChildPath(META_INF_RESOURCES))
                .filter(Objects::nonNull)
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.toSet()));
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addReactiveRoutesExceptionMapper(List<RouteDescriptionBuildItem> routeDescriptions,
            ExceptionMapperRecorder recorder) {
        List<RouteDescription> reactiveRoutes = new ArrayList<>();
        for (RouteDescriptionBuildItem description : routeDescriptions) {
            reactiveRoutes.add(description.getDescription());
        }
        recorder.setReactiveRoutes(reactiveRoutes);
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addAdditionalEndpointsExceptionMapper(List<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            ExceptionMapperRecorder recorder, HttpRootPathBuildItem httpRoot) {
        List<AdditionalRouteDescription> endpoints = displayableEndpoints
                .stream()
                .map(v -> new AdditionalRouteDescription(v.getEndpoint(httpRoot), v.getDescription()))
                .sorted()
                .collect(Collectors.toList());

        recorder.setAdditionalEndpoints(endpoints);
    }
}
