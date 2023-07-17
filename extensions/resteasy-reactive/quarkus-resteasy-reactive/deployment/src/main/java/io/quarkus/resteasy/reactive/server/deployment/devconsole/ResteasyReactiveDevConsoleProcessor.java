package io.quarkus.resteasy.reactive.server.deployment.devconsole;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.EndpointScoresSupplier;
import io.quarkus.resteasy.reactive.server.runtime.ExceptionMappersSupplier;
import io.quarkus.resteasy.reactive.server.runtime.ParamConverterProvidersSupplier;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

public class ResteasyReactiveDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectScores(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("endpointScores", new EndpointScoresSupplier(), this.getClass(),
                curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem exceptionMappers(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("exceptionMappers", new ExceptionMappersSupplier(), this.getClass(),
                curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem paramConverterProviders(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("paramConverterProviders", new ParamConverterProvidersSupplier(),
                this.getClass(),
                curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectAdditionalEndpoints(
            List<NotFoundPageDisplayableEndpointBuildItem> additionalEndpoint) {
        return new DevConsoleTemplateInfoBuildItem("additionalEndpointInfo", additionalEndpoint);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectStaticResourcesInfo(
            ApplicationArchivesBuildItem applicationArchivesBuildItem) throws Exception {

        StaticResourceInfo staticResourceInfo = new StaticResourceInfo();

        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            i.accept(tree -> {
                Path resource = tree.getPath(StaticResourcesRecorder.META_INF_RESOURCES);
                if (resource != null && Files.exists(resource)) {
                    collectKnownPaths(resource, staticResourceInfo);
                }
            });

        }
        return new DevConsoleTemplateInfoBuildItem("staticResourceInfo", staticResourceInfo);
    }

    private void collectKnownPaths(Path resource, StaticResourceInfo staticResourceInfo) {

        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<Path>() {
                final Deque<StaticResourceInfo.StaticFile> currentFolder = new ArrayDeque<>();

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                    Path relativeFile = resource.relativize(dir);
                    String relativeFileString = "/" + relativeFile.toString();
                    Path dirName = dir.getFileName();
                    StaticResourceInfo.StaticFile getStaticFile = staticResourceInfo.resourceMap.get(relativeFileString);
                    if (getStaticFile == null) {
                        getStaticFile = new StaticResourceInfo.StaticFile(dirName.toString(), true);
                        staticResourceInfo.resourceMap.put(relativeFileString, getStaticFile);

                        // adding to parent
                        if (!currentFolder.isEmpty()) {
                            currentFolder.peek().children.add(getStaticFile);
                        }
                    }
                    currentFolder.push(getStaticFile);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    StaticResourceInfo.StaticFile newStaticFile = new StaticResourceInfo.StaticFile(fileName, false);

                    List<StaticResourceInfo.StaticFile> childrenList = currentFolder.peek().children;
                    childrenList.add(newStaticFile);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Collections.sort(currentFolder.peek().children);
                    currentFolder.pop();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
