package io.quarkus.vertx.http.deployment;

import static io.quarkus.vertx.http.runtime.GeneratedStaticResourcesRecorder.META_INF_RESOURCES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
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
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.spi.AdditionalStaticResourceBuildItem;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.quarkus.vertx.http.runtime.GeneratedStaticResourcesRecorder;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.quarkus.vertx.http.runtime.handlers.DevStaticHandler;

/**
 * {@link GeneratedStaticResourcesProcessor} is responsible for dealing {@link GeneratedStaticResourceBuildItem}
 * creating a {@link DevStaticHandler} to handle all static resources
 * generated from extensions through {@link GeneratedStaticResourceBuildItem} build item.
 */
public class GeneratedStaticResourcesProcessor {

    private static final int ROUTE_ORDER = RouteConstants.ROUTE_ORDER_BEFORE_DEFAULT + 60;

    @BuildStep
    public void produceResources(List<GeneratedStaticResourceBuildItem> generatedStaticResources,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourcesProducer,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<AdditionalStaticResourceBuildItem> additionalStaticResourcesProducer) {
        for (GeneratedStaticResourceBuildItem generatedStaticResource : generatedStaticResources) {
            String generatedStaticResourceLocation = buildGeneratedStaticResourceLocation(generatedStaticResource);
            if (!generatedStaticResource.isFile()) {
                generatedResourceBuildItem.produce(
                        new GeneratedResourceBuildItem(generatedStaticResourceLocation,
                                generatedStaticResource.getContent(), false));
            } else if (launchModeBuildItem.getLaunchMode() != LaunchMode.DEVELOPMENT) {
                // For files, we need to read it and add it in the classpath for normal and test mode
                try {
                    final byte[] content = Files.readAllBytes(generatedStaticResource.getFile());
                    generatedResourceBuildItem.produce(
                            new GeneratedResourceBuildItem(generatedStaticResourceLocation,
                                    content, false));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // We can't use the vert.x StaticHandler for tests as it doesn't support 'quarkus' protocol
            if (launchModeBuildItem.getLaunchMode() == LaunchMode.NORMAL) {
                additionalStaticResourcesProducer.produce(
                        new AdditionalStaticResourceBuildItem(generatedStaticResource.getEndpoint(), false));
                nativeImageResourcesProducer.produce(new NativeImageResourceBuildItem(generatedStaticResourceLocation));
            }
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void process(List<GeneratedStaticResourceBuildItem> generatedStaticResources,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<RouteBuildItem> routes, GeneratedStaticResourcesRecorder generatedStaticResourcesRecorder,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageProducer) throws BuildException {
        if (generatedStaticResources.isEmpty()) {
            return;
        }

        List<String> duplicates = collectDuplicates(generatedStaticResources);
        if (!duplicates.isEmpty()) {
            throw new BuildException(
                    "Duplicate endpoints detected, the endpoint for static resources must be unique: " + duplicates);
        }

        Map<String, String> generatedFilesResources = generatedStaticResources.stream()
                .peek(path -> notFoundPageProducer.produce(new NotFoundPageDisplayableEndpointBuildItem(path.getEndpoint())))
                .filter(GeneratedStaticResourceBuildItem::isFile)
                .collect(Collectors.toMap(GeneratedStaticResourceBuildItem::getEndpoint,
                        GeneratedStaticResourceBuildItem::getFileAbsolutePath));
        Set<String> generatedClassPathResources = generatedStaticResources.stream()
                .map(GeneratedStaticResourceBuildItem::getEndpoint)
                .collect(Collectors.toSet());
        routes.produce(RouteBuildItem.builder()
                .orderedRoute("/*", ROUTE_ORDER, generatedStaticResourcesRecorder.createRouteCustomizer())
                .handler(generatedStaticResourcesRecorder.createHandler(generatedClassPathResources, generatedFilesResources))
                .build());
    }

    private static String buildGeneratedStaticResourceLocation(
            GeneratedStaticResourceBuildItem generatedStaticResourceBuildItem) {
        return META_INF_RESOURCES +
                generatedStaticResourceBuildItem.getEndpoint();
    }

    // THIS IS TO TEST DEV MODE

    private static final String META_INF_GENERATED_RESOURCES_TEST = "META-INF/generated-resources-test";

    @BuildStep(onlyIf = IsDevelopment.class)
    public void devMode(
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeployment,
            BuildProducer<GeneratedStaticResourceBuildItem> generatedStaticResourceProducer,
            LaunchModeBuildItem launchMode) throws IOException {
        // this is only for dev-mode tests
        if (!launchMode.isTest()) {
            return;
        }

        hotDeployment.produce(HotDeploymentWatchedFileBuildItem.builder()
                .setRestartNeeded(true)
                .setLocationPredicate(l -> l.startsWith(META_INF_GENERATED_RESOURCES_TEST + "/bytes"))
                .build());

        Map<String, Path> classpathResources = getClasspathResources();
        for (Map.Entry<String, Path> entry : classpathResources.entrySet()) {
            final String key = "/" + entry.getKey();
            final GeneratedStaticResourceBuildItem item = key.startsWith("/bytes")
                    ? new GeneratedStaticResourceBuildItem(key, Files.readAllBytes(entry.getValue()))
                    : new GeneratedStaticResourceBuildItem(key, entry.getValue());
            generatedStaticResourceProducer.produce(item);
        }
    }

    private Map<String, Path> getClasspathResources() {
        Map<String, Path> knownPaths = new HashMap<>();
        visitRuntimeMetaInfResources(visit -> {
            if (!Files.isDirectory(visit.getPath())) {
                String resourcePath = visit.getRelativePath("/")
                        .substring("/".concat(META_INF_GENERATED_RESOURCES_TEST).length());
                knownPaths.put(resourcePath, visit.getPath());
            }
        });
        return knownPaths;
    }

    private static void visitRuntimeMetaInfResources(PathVisitor visitor) {
        final List<ClassPathElement> elements = QuarkusClassLoader.getElements(META_INF_GENERATED_RESOURCES_TEST,
                false);
        if (!elements.isEmpty()) {
            final PathFilter filter = PathFilter.forIncludes(List.of(
                    META_INF_GENERATED_RESOURCES_TEST + "/**",
                    META_INF_GENERATED_RESOURCES_TEST));
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

    private static List<String> collectDuplicates(List<GeneratedStaticResourceBuildItem> generatedStaticResources) {
        Set<String> uniques = new HashSet<>();
        return generatedStaticResources.stream()
                .map(GeneratedStaticResourceBuildItem::getEndpoint)
                .filter(e -> !uniques.add(e))
                .toList();
    }
}
