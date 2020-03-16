package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class ResteasyStandaloneBuildStep {

    protected static final String META_INF_RESOURCES_SLASH = "META-INF/resources/";
    protected static final String META_INF_RESOURCES = "META-INF/resources";

    public static final class ResteasyStandaloneBuildItem extends SimpleBuildItem {

        final String deploymentRootPath;

        public ResteasyStandaloneBuildItem(String deploymentRootPath) {
            if (deploymentRootPath != null) {
                this.deploymentRootPath = deploymentRootPath.startsWith("/") ? deploymentRootPath : "/" + deploymentRootPath;
            } else {
                this.deploymentRootPath = null;
            }
        }

    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(ResteasyStandaloneRecorder recorder,
            Capabilities capabilities,
            ResteasyDeploymentBuildItem deployment,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady,
            HttpBuildTimeConfig httpConfig,
            BuildProducer<ResteasyStandaloneBuildItem> standalone) throws Exception {
        if (capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            return;
        }

        Set<String> knownPaths = getClasspathResources(applicationArchivesBuildItem);
        String deploymentRootPath = null;
        // The context path + the resources path
        String rootPath = httpConfig.rootPath;

        if (deployment != null) {
            deploymentRootPath = deployment.getRootPath();
            if (rootPath.endsWith("/")) {
                if (deploymentRootPath.startsWith("/")) {
                    rootPath += deploymentRootPath.substring(1);
                } else {
                    rootPath += deploymentRootPath;
                }
            } else if (!deploymentRootPath.equals("/")) {
                if (!deploymentRootPath.startsWith("/")) {
                    rootPath += "/";
                }
                rootPath += deploymentRootPath;
            }
            recorder.staticInit(deployment.getDeployment(), rootPath, knownPaths);

        } else if (!knownPaths.isEmpty()) {
            recorder.staticInit(null, rootPath, knownPaths);
        }

        if (deployment != null || !knownPaths.isEmpty()) {
            standalone.produce(new ResteasyStandaloneBuildItem(deploymentRootPath));
        }
    }

    /**
     * Find all static file resources that are available from classpath.
     *
     * @param applicationArchivesBuildItem
     * @return
     * @throws Exception
     */
    private Set<String> getClasspathResources(ApplicationArchivesBuildItem applicationArchivesBuildItem) throws Exception {
        Set<String> knownPaths = new HashSet<>();
        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            Path resource = i.getChildPath(META_INF_RESOURCES);
            if (resource != null && Files.exists(resource)) {
                collectKnownPaths(resource, knownPaths);
            }
        }

        ClassPathUtils.consumeAsPaths(META_INF_RESOURCES, resource -> {
            collectKnownPaths(resource, knownPaths);
        });

        return knownPaths;
    }

    private void collectKnownPaths(Path resource, Set<String> knownPaths) {
        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs)
                        throws IOException {
                    String file = resource.relativize(p).toString();
                    if (file.equals("index.html") || file.equals("index.htm")) {
                        knownPaths.add("/");
                    }
                    if (!file.startsWith("/")) {
                        file = "/" + file;
                    }
                    // Windows has a backslash
                    file = file.replace('\\', '/');
                    knownPaths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            CoreVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            ResteasyStandaloneBuildItem standalone,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            ExecutorBuildItem executorBuildItem,
            HttpConfiguration httpConfiguration) throws Exception {

        if (standalone == null) {
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY));

        boolean isDefaultOrNullDeploymentPath = standalone.deploymentRootPath == null
                || standalone.deploymentRootPath.equals("/");
        if (!isDefaultOrNullDeploymentPath) {
            // We need to register a special handler for non-default deployment path (specified as application path or resteasyConfig.path)
            Handler<RoutingContext> handler = recorder.vertxRequestHandler(vertx.getVertx(), beanContainer.getValue(),
                    executorBuildItem.getExecutorProxy(), httpConfiguration);
            // Exact match for resources matched to the root path
            routes.produce(new RouteBuildItem(standalone.deploymentRootPath, handler, false));
            String matchPath = standalone.deploymentRootPath;
            if (matchPath.endsWith("/")) {
                matchPath += "*";
            } else {
                matchPath += "/*";
            }
            // Match paths that begin with the deployment path
            routes.produce(new RouteBuildItem(matchPath, handler, false));
        }

        boolean isVirtual = requireVirtual.isPresent();
        Consumer<Route> ut = recorder.start(vertx.getVertx(),
                shutdown,
                beanContainer.getValue(),
                isVirtual, isDefaultOrNullDeploymentPath, executorBuildItem.getExecutorProxy(), httpConfiguration);

        defaultRoutes.produce(new DefaultRouteBuildItem(ut));
    }

}
