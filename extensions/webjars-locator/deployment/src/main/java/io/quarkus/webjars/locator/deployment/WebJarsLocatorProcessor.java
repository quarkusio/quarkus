package io.quarkus.webjars.locator.deployment;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.webjars.locator.runtime.WebJarsLocatorRecorder;

public class WebJarsLocatorProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findWebjarsAndCreateHandler(
            HttpBuildTimeConfig httpConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            WebJarsLocatorRecorder recorder) throws Exception {
        // The context path + the resources path
        String rootPath = httpConfig.rootPath;
        String webjarRootPath = (rootPath.endsWith("/")) ? rootPath + "webjars/" : rootPath + "/webjars/";
        feature.produce(new FeatureBuildItem(FeatureBuildItem.WEBJARS_LOCATOR));
        final String webjarsFileSystemPath = "META-INF/resources/webjars/";
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        Map<String, String> versionMap = new HashMap<>();
        List<ClassPathElement> resources = cl.getElementsWithResource(webjarsFileSystemPath);
        for (ClassPathElement webJarsElement : resources) {
            Path root = webJarsElement.getRoot();
            if (root == null) {
                continue;
            }
            //we only care about jars
            if (!Files.isDirectory(root)) {
                try (FileSystem jar = ZipUtils.newFileSystem(root)) {
                    Path web = jar.getPath("META-INF", "resources", "webjars");
                    try (Stream<Path> implementations = Files.list(web)) {
                        implementations.forEach(new Consumer<Path>() {
                            @Override
                            public void accept(Path implPath) {
                                try (Stream<Path> version = Files.list(implPath)) {
                                    List<Path> versionList = version.collect(Collectors.toList());
                                    if (versionList.isEmpty()) {
                                        return;
                                    }
                                    if (versionList.size() > 1) {
                                        throw new RuntimeException("Found multiple versions of webjar " + implPath.getFileName()
                                                + " versions " + versionList);
                                    }
                                    versionMap.put(implPath.getFileName().toString(),
                                            versionList.get(0).getFileName().toString());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                }

            }
        }

        routes.produce(
                new RouteBuildItem(webjarRootPath + "*",
                        recorder.getHandler(webjarRootPath, versionMap),
                        false));
    }

}
