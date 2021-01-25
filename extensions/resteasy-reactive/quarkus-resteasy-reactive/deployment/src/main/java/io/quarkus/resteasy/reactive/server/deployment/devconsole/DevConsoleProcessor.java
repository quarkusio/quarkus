package io.quarkus.resteasy.reactive.server.deployment.devconsole;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.EndpointScoresSupplier;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectScores() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("endpointScores", new EndpointScoresSupplier());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectAdditionalEndpoints(
            List<NotFoundPageDisplayableEndpointBuildItem> additionalEndpoint) {

        return new DevConsoleTemplateInfoBuildItem("additionalEndpointInfo", additionalEndpoint);
    }

    // knownPaths variable contain set of static file resources that are available from classpath
    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectStaticResourcesInfo(
            ApplicationArchivesBuildItem applicationArchivesBuildItem) throws Exception {

        Set<String> knownPaths = new TreeSet<>();
        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            Path resource = i.getChildPath(StaticResourcesRecorder.META_INF_RESOURCES);
            if (resource != null && Files.exists(resource)) {
                collectKnownPaths(resource, knownPaths);
            }
        }

        ClassPathUtils.consumeAsPaths(StaticResourcesRecorder.META_INF_RESOURCES, resource -> {
            collectKnownPaths(resource, knownPaths);
        });

        return new DevConsoleTemplateInfoBuildItem("staticResourcesInfo", knownPaths);
    }

    private void collectKnownPaths(Path resource, Set<String> staticResources) {
        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attributes) throws IOException {
                    String simpleName = p.getFileName().toString();
                    String file = resource.relativize(p).toString();
                    if (simpleName.equals("index.html") || simpleName.equals("index.htm")) {
                        Path parent = resource.relativize(p).getParent();
                        if (parent == null) {
                            staticResources.add("/");
                        } else {
                            String parentString = parent.toString();
                            if (!parentString.startsWith("/")) {
                                parentString = "/" + parentString;
                            }
                            staticResources.add(parentString + "/");
                        }
                    }
                    if (!file.startsWith("/")) {
                        file = "/" + file;
                    }
                    file = file.replace('\\', '/');
                    staticResources.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
