package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;

import javax.ws.rs.ext.Providers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyConfiguration;

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
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.undertow.deployment.KnownPathsBuildItem;
import io.quarkus.vertx.core.deployment.InternalWebVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class ResteasyStandaloneBuildStep {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    //@BuildStep
    public void substrate(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            Capabilities capabilities,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition,
            ResteasyDeploymentBuildItem deployment) {
        if (deployment == null || capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            return;
        }
        // todo I'm not sure why you do not have to do this when running within a servlet
        serviceProvider.produce(new ServiceProviderBuildItem(Providers.class.getName()));
        // register proxies
        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(HttpRequest.class.getName()));
        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(ResteasyConfiguration.class.getName()));
        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(Providers.class.getName()));
    }

    public static final class ResteasyStandaloneBuildItem extends SimpleBuildItem {
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public ResteasyStandaloneBuildItem setupDeployment(ResteasyStandaloneRecorder recorder,
            Capabilities capabilities,
            ResteasyDeploymentBuildItem deployment,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) {
        if (deployment == null || capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            return null;
        }
        recorder.setupDeployment(deployment.getDeployment());
        return new ResteasyStandaloneBuildItem();

    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            KnownPathsBuildItem known,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DefaultRouteBuildItem> routeProducer,
            InternalWebVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            ResteasyDeploymentBuildItem deployment,
            ResteasyStandaloneBuildItem standalone,
            Optional<RequireVirtualHttpBuildItem> requireVirtual) throws Exception {

        if (deployment == null || standalone == null) {
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
                shutdown,
                beanContainer.getValue(),
                hasClasspathResources,
                isVirtual);

        routeProducer.produce(new DefaultRouteBuildItem(ut));
    }

}
