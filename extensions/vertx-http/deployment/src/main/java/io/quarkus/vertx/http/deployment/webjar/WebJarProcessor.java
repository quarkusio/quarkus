package io.quarkus.vertx.http.deployment.webjar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarRecorder;

public class WebJarProcessor {
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIfNot = IsNormal.class)
    WebJarResultsBuildItem processWebJarDevMode(WebJarRecorder recorder, List<WebJarBuildItem> webJars,
            CurateOutcomeBuildItem curateOutcomeBuildItem, ShutdownContextBuildItem shutdownContext,
            ApplicationConfig applicationConfig) throws IOException {

        Map<GACT, WebJarResultsBuildItem.WebJarResult> results = new HashMap<>();

        Path deploymentBasePath = Files.createTempDirectory("quarkus-webjar");
        recorder.shutdownTask(shutdownContext, deploymentBasePath.toString());

        for (WebJarBuildItem webJar : webJars) {
            Path resourcesDirectory = deploymentBasePath
                    .resolve(buildFinalDestination(webJar.getArtifactKey(), webJar.getRoot()));
            ResolvedDependency dependency = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, webJar.getArtifactKey());

            Path staticResourcesPath = WebJarUtil.copyResourcesForDevOrTest(curateOutcomeBuildItem, applicationConfig,
                    webJar, dependency, resourcesDirectory);

            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations = new ArrayList<>();
            webRootConfigurations.add(new FileSystemStaticHandler.StaticWebRootConfiguration(
                    staticResourcesPath.toAbsolutePath().toString(), ""));
            for (Path resolvedPath : dependency.getResolvedPaths()) {
                webRootConfigurations.add(new FileSystemStaticHandler.StaticWebRootConfiguration(
                        resolvedPath.toString(), webJar.getRoot()));
            }

            results.put(webJar.getArtifactKey(), new WebJarResultsBuildItem.WebJarResult(dependency,
                    staticResourcesPath.toAbsolutePath().toString(), webRootConfigurations));
        }

        return new WebJarResultsBuildItem(results);
    }

    @BuildStep(onlyIf = IsNormal.class)
    WebJarResultsBuildItem processWebJarProdMode(List<WebJarBuildItem> webJars,
            CurateOutcomeBuildItem curateOutcomeBuildItem, BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceBuildItemBuildProducer,
            ApplicationConfig applicationConfig) {

        Map<GACT, WebJarResultsBuildItem.WebJarResult> results = new HashMap<>();

        for (WebJarBuildItem webJar : webJars) {

            ResolvedDependency dependency = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, webJar.getArtifactKey());

            Map<String, byte[]> files = WebJarUtil.copyResourcesForProduction(curateOutcomeBuildItem, applicationConfig,
                    webJar, dependency);

            String finalDestination = buildFinalDestination(webJar.getArtifactKey(), webJar.getRoot());
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                String fileName = finalDestination + "/" + file.getKey();
                byte[] fileContent = file.getValue();

                generatedResources.produce(new GeneratedResourceBuildItem(fileName, fileContent));
                nativeImageResourceBuildItemBuildProducer.produce(new NativeImageResourceBuildItem(fileName));
            }

            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations = new ArrayList<>();
            webRootConfigurations.add(new FileSystemStaticHandler.StaticWebRootConfiguration(finalDestination, ""));

            results.put(webJar.getArtifactKey(),
                    new WebJarResultsBuildItem.WebJarResult(dependency, finalDestination, webRootConfigurations));
        }

        return new WebJarResultsBuildItem(results);
    }

    private String buildFinalDestination(GACT artifactKey, String webRoot) {
        String finalDestination = "META-INF/" + artifactKey.toString().replace(":", "_") + "/" + webRoot;

        if (finalDestination.endsWith("/")) {
            finalDestination = finalDestination.substring(0, finalDestination.length() - 1);
        }

        return finalDestination;
    }
}
