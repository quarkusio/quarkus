package io.quarkus.vertx.http.deployment;

import static io.quarkus.vertx.http.runtime.GeneratedStaticResourcesRecorder.META_INF_RESOURCES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.paths.FilteredPathTree;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathVisitor;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.spi.AdditionalStaticResourceBuildItem;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.quarkus.vertx.http.runtime.GeneratedStaticResourcesRecorder;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.quarkus.vertx.http.runtime.handlers.DevClasspathStaticHandler;

/**
 * {@link GeneratedStaticResourcesProcessor} is responsible for dealing {@link GeneratedStaticResourceBuildItem}
 * creating a {@link DevClasspathStaticHandler} to handle all static resources
 * generated from extensions through {@link GeneratedStaticResourceBuildItem} build item.
 */
public class GeneratedStaticResourcesProcessor {

    private static final int ROUTE_ORDER = RouteConstants.ROUTE_ORDER_BEFORE_DEFAULT + 60;
    private static final String META_INF_GENERATED_RESOURCES = "META-INF/generated-resources";

    @BuildStep
    public void produceResources(List<GeneratedStaticResourceBuildItem> generatedStaticResources,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<AdditionalStaticResourceBuildItem> additionalStaticResourcesProducer) {

        for (GeneratedStaticResourceBuildItem generatedStaticResource : generatedStaticResources) {
            String generatedStaticResourceLocation = buildGeneratedStaticResourceLocation(generatedStaticResource);

            generatedResourceBuildItem.produce(
                    new GeneratedResourceBuildItem(generatedStaticResourceLocation,
                            generatedStaticResource.getContent(), false));
            // We can't use the vert.x StaticHandler for tests as it doesn't support 'quarkus' protocol
            if (!launchModeBuildItem.getLaunchMode().isDevOrTest()) {
                additionalStaticResourcesProducer.produce(
                        new AdditionalStaticResourceBuildItem(generatedStaticResource.getPath(), false));
                nativeImageResourcesProducer.produce(new NativeImageResourceBuildItem(generatedStaticResourceLocation));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void process(List<GeneratedStaticResourceBuildItem> generatedStaticResources,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<RouteBuildItem> routes, GeneratedStaticResourcesRecorder generatedStaticResourcesRecorder,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageProducer) {
        if (!launchModeBuildItem.getLaunchMode().isDevOrTest() || generatedStaticResources.isEmpty()) {
            return;
        }
        Set<String> paths = generatedStaticResources.stream().map(GeneratedStaticResourceBuildItem::getPath)
                .peek(path -> notFoundPageProducer.produce(new NotFoundPageDisplayableEndpointBuildItem(path)))
                .collect(Collectors.toSet());
        routes.produce(RouteBuildItem.builder()
                .orderedRoute("/*", ROUTE_ORDER, generatedStaticResourcesRecorder.createRouteCustomizer())
                .handler(generatedStaticResourcesRecorder.createHandler(paths))
                .build());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void devMode(
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeployment,
            BuildProducer<GeneratedStaticResourceBuildItem> generatedStaticResourceProducer) throws IOException {
        hotDeployment.produce(HotDeploymentWatchedFileBuildItem.builder()
                .setRestartNeeded(true)
                .setLocationPredicate(l -> l.startsWith(META_INF_GENERATED_RESOURCES))
                .build());

        Map<String, Path> classpathResources = getClasspathResources();
        for (Map.Entry<String, Path> entries : classpathResources.entrySet()) {
            byte[] bytes = Files.readAllBytes(entries.getValue());
            generatedStaticResourceProducer.produce(new GeneratedStaticResourceBuildItem(
                    "/" + entries.getKey(), bytes));
        }
    }

    private Map<String, Path> getClasspathResources() {
        Map<String, Path> knownPaths = new HashMap<>();
        visitRuntimeMetaInfResources(visit -> {
            if (!Files.isDirectory(visit.getPath())) {
                String resourcePath = visit.getRelativePath("/")
                        .substring("/".concat(META_INF_GENERATED_RESOURCES).length());
                knownPaths.put(resourcePath, visit.getPath());
            }
        });
        return knownPaths;
    }

    private static void visitRuntimeMetaInfResources(PathVisitor visitor) {
        final List<ClassPathElement> elements = QuarkusClassLoader.getElements(META_INF_GENERATED_RESOURCES,
                false);
        if (!elements.isEmpty()) {
            final PathFilter filter = PathFilter.forIncludes(List.of(
                    META_INF_GENERATED_RESOURCES + "/**",
                    META_INF_GENERATED_RESOURCES));
            for (var element : elements) {
                if (element.isRuntime()) {
                    element.apply(tree -> {
                        new FilteredPathTree(tree, filter).walk(visitor);
                        return null;
                    });
                }
            }
        }
    }

    private static String buildGeneratedStaticResourceLocation(
            GeneratedStaticResourceBuildItem generatedStaticResourceBuildItem) {
        return META_INF_RESOURCES +
                generatedStaticResourceBuildItem.getPath();
    }
}
