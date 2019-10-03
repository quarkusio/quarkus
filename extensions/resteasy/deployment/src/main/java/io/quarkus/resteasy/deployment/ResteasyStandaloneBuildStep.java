package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.vertx.core.deployment.InternalWebVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.HttpBuildTimeConfig;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.vertx.ext.web.Route;

public class ResteasyStandaloneBuildStep {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");
    protected static final String META_INF_RESOURCES_SLASH = "META-INF/resources/";
    protected static final String META_INF_RESOURCES = "META-INF/resources";

    public static final class ResteasyStandaloneBuildItem extends SimpleBuildItem {

    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(ResteasyStandaloneRecorder recorder,
            Capabilities capabilities,
            ResteasyDeploymentBuildItem deployment,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady,
            HttpBuildTimeConfig httpBuildTimeConfig,
            BuildProducer<ResteasyStandaloneBuildItem> standalone) throws Exception {
        if (deployment == null || capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            return;
        }

        String rootPath = deployment.getRootPath();
        if (!httpBuildTimeConfig.rootPath.equals("/")) {
            rootPath = httpBuildTimeConfig.rootPath + rootPath;
        }
        Set<String> knownPaths = getClasspathResources(applicationArchivesBuildItem);
        recorder.staticInit(deployment.getDeployment(), rootPath, knownPaths);
        standalone.produce(new ResteasyStandaloneBuildItem());

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
                Files.walk(resource).forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path path) {
                        // Skip META-INF/resources entry
                        if (resource.equals(path)) {
                            return;
                        }
                        Path rel = resource.relativize(path);
                        if (!Files.isDirectory(path)) {
                            String file = rel.toString();
                            if (file.equals("index.html") || file.equals("index.htm")) {
                                knownPaths.add("/");
                            }
                            if (!file.startsWith("/")) {
                                file = "/" + file;
                            }
                            // Windows has a backslash
                            file = file.replace('\\', '/');
                            knownPaths.add(file);
                        }
                    }
                });
            }
        }
        Enumeration<URL> resources = getClass().getClassLoader().getResources(META_INF_RESOURCES);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if (url.getProtocol().equals("jar")) {
                JarURLConnection jar = (JarURLConnection) url.openConnection();
                Enumeration<JarEntry> entries = jar.getJarFile().entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(META_INF_RESOURCES_SLASH)) {
                        String sub = entry.getName().substring(META_INF_RESOURCES_SLASH.length());
                        if (!sub.isEmpty()) {
                            if (sub.equals("index.html") || sub.equals("index.htm")) {
                                knownPaths.add("/");
                            }
                            if (!sub.startsWith("/")) {
                                sub = "/" + sub;
                            }
                            knownPaths.add(sub);
                        }
                    }
                }
            }
            if (url.getProtocol().equals("file")) {
                Path resource = Paths.get(url.toURI());
                if (resource != null && Files.exists(resource)) {
                    Files.walk(resource).forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            // Skip META-INF/resources entry
                            if (resource.equals(path)) {
                                return;
                            }
                            Path rel = resource.relativize(path);
                            if (!Files.isDirectory(path)) {
                                String file = rel.toString();
                                if (file.equals("index.html") || file.equals("index.htm")) {
                                    knownPaths.add("/");
                                }
                                if (!file.startsWith("/")) {
                                    file = "/" + file;
                                }
                                // Windows has a backslash
                                file = file.replace('\\', '/');
                                knownPaths.add(file);
                            }
                        }
                    });
                }
            }
        }
        return knownPaths;
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DefaultRouteBuildItem> routeProducer,
            InternalWebVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            ResteasyStandaloneBuildItem standalone,
            List<RequireVirtualHttpBuildItem> requireVirtual) throws Exception {

        if (standalone == null) {
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY));

        boolean isVirtual = !requireVirtual.isEmpty();
        Consumer<Route> ut = recorder.start(vertx.getVertx(),
                shutdown,
                beanContainer.getValue(),
                isVirtual);

        routeProducer.produce(new DefaultRouteBuildItem(ut));
    }

}
