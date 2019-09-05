package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.undertow.deployment.KnownPathsBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.web.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.web.deployment.RequireVirtualHttpBuildItem;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class ResteasyStandaloneBuildStep {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            KnownPathsBuildItem known,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            Capabilities capabilities,
            BuildProducer<DefaultRouteBuildItem> routeProducer,
            ResteasyDeploymentBuildItem deployment,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) throws Exception {
        if (deployment == null || capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY));

        // We don't want to add a Router if we don't have to so check if META-INF/resources exists anywhere
        boolean hasClasspathResources = !known.knownDirectories.isEmpty() || !known.knownFiles.isEmpty();

        if (!hasClasspathResources) {
            for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
                Path resource = i.getChildPath(ResteasyStandaloneRecorder.META_INF_RESOURCES);
                if (resource != null && Files.exists(resource)) {
                    hasClasspathResources = true;
                    break;
                }
            }

        }

        if (!hasClasspathResources) {
            Enumeration<URL> resources = getClass().getClassLoader()
                    .getResources(ResteasyStandaloneRecorder.META_INF_RESOURCES);
            hasClasspathResources = resources.hasMoreElements();
        }

        boolean isVirtual = requireVirtual.isPresent();
        Handler<HttpServerRequest> ut = recorder.startResteasy(vertx.getVertx(),
                deployment.getRootPath(),
                deployment.getDeployment(),
                shutdown,
                beanContainer.getValue(),
                hasClasspathResources,
                isVirtual);

        routeProducer.produce(new DefaultRouteBuildItem(ut));
    }

}
