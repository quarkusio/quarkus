package io.quarkus.stork.deployment;

import static java.util.Arrays.asList;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.stork.SmallRyeStorkRecorder;
import io.quarkus.stork.SmallRyeStorkRegistrationRecorder;
import io.quarkus.stork.StorkConfigProvider;
import io.quarkus.stork.StorkConfiguration;
import io.quarkus.stork.StorkRegistrarConfigRecorder;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.smallrye.stork.spi.LoadBalancerProvider;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.internal.LoadBalancerLoader;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;
import io.smallrye.stork.spi.internal.ServiceRegistrarLoader;

public class SmallRyeStorkProcessor {

    private static final String KUBERNETES_SERVICE_DISCOVERY_PROVIDER = "io.smallrye.stork.servicediscovery.kubernetes.KubernetesServiceDiscoveryProvider";
    private static final String CONSUL_SERVICE_REGISTRAR_PROVIDER = "io.smallrye.stork.serviceregistration.consul.ConsulServiceRegistrarProvider";
    private static final String EUREKA_SERVICE_REGISTRAR_PROVIDER = "io.smallrye.stork.serviceregistration.eureka.EurekaServiceRegistrarProvider";
    private static final String SERVICE_REGISTRAR_PROVIDER = "io.smallrye.stork.spi.ServiceRegistrarProvider";
    private static final String CONSUL_SERVICE_REGISTRAR_TYPE = "consul";
    private static final String EUREKA_SERVICE_REGISTRAR_TYPE = "eureka";
    private static final Logger LOGGER = Logger.getLogger(SmallRyeStorkProcessor.class.getName());

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(new ServiceProviderBuildItem(io.smallrye.stork.spi.config.ConfigProvider.class.getName(),
                StorkConfigProvider.class.getName()));
        for (Class<?> providerClass : asList(LoadBalancerLoader.class, ServiceDiscoveryLoader.class,
                ServiceRegistrarLoader.class)) {
            services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(providerClass.getName()));
        }
    }

    @BuildStep
    UnremovableBeanBuildItem unremoveableBeans() {
        return UnremovableBeanBuildItem.beanTypes(
                DotName.createSimple(ServiceDiscoveryProvider.class),
                DotName.createSimple(ServiceDiscoveryLoader.class),
                DotName.createSimple(LoadBalancerProvider.class),
                DotName.createSimple(LoadBalancerLoader.class),
                DotName.createSimple(ServiceRegistrarProvider.class),
                DotName.createSimple(ServiceRegistrarLoader.class));
    }

    /**
     * This build step is the fix for <a href="https://github.com/quarkusio/quarkus/issues/24444">#24444</a>.
     * Because Stork itself cannot depend on Quarkus, and we do not want to have extensions for all the service
     * discovery and load-balancer providers, we work around the issue by detecting when the kubernetes service
     * discovery is used and if the kubernetes extension is used.
     */
    @BuildStep
    @Produce(AlwaysBuildItem.class)
    void checkThatTheKubernetesExtensionIsUsedWhenKubernetesServiceDiscoveryInOnTheClasspath(Capabilities capabilities) {
        if (QuarkusClassLoader.isClassPresentAtRuntime(KUBERNETES_SERVICE_DISCOVERY_PROVIDER)) {
            if (!capabilities.isPresent(Capability.KUBERNETES_CLIENT)) {
                LOGGER.warn(
                        "The application is using the Stork Kubernetes Service Discovery provider but does not depend on the `quarkus-kubernetes-client` extension. "
                                +
                                "It is highly recommended to use the `io.quarkus:quarkus-kubernetes-client` extension with the Kubernetes service discovery. \n"
                                +
                                "To add this extension:" +
                                "\n - with the quarkus CLI, run: `quarkus ext add io.quarkus:quarkus-kubernetes-client`" +
                                "\n - with Apache Maven, run: `./mvnw quarkus:add-extension -Dextensions=\"io.quarkus:quarkus-kubernetes-client\"`"
                                +
                                "\n - or just add the `io.quarkus:quarkus-kubernetes-client` dependency to the project");
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(AlwaysBuildItem.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Consume(StorkRegistrationBuildItem.class)
    @Produce(StorkInitializedBuildItem.class)
    void initializeStork(SmallRyeStorkRecorder storkRecorder, ShutdownContextBuildItem shutdown, VertxBuildItem vertx,
            StorkConfiguration configuration) {
        storkRecorder.initialize(shutdown, vertx.getVertx(), configuration);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void checkStorkConsulRegistrar(BuildProducer<StorkRegistrationBuildItem> registration,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            StorkRegistrarConfigRecorder registrarConfigRecorder, StorkConfiguration configuration, Capabilities capabilities,
            CombinedIndexBuildItem index) {
        String smallryeHealthCheckDefaultUrl = "";
        if (QuarkusClassLoader.isClassPresentAtRuntime(CONSUL_SERVICE_REGISTRAR_PROVIDER)) {
            if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
                Config quarkusConfig = ConfigProvider.getConfig();
                smallryeHealthCheckDefaultUrl = quarkusConfig.getConfigValue("quarkus.management.root-path").getValue() + "/"
                        + quarkusConfig.getConfigValue("quarkus.smallrye-health.root-path").getValue() + "/"
                        + quarkusConfig.getConfigValue("quarkus.smallrye-health.liveness-path").getValue();
            }
            registrarConfigRecorder.setupServiceRegistrarConfig(configuration, CONSUL_SERVICE_REGISTRAR_TYPE,
                    smallryeHealthCheckDefaultUrl);
        } else if (QuarkusClassLoader.isClassPresentAtRuntime(EUREKA_SERVICE_REGISTRAR_PROVIDER)) {
            registrarConfigRecorder.setupServiceRegistrarConfig(configuration, EUREKA_SERVICE_REGISTRAR_TYPE,
                    smallryeHealthCheckDefaultUrl);
        }
        registration.produce(new StorkRegistrationBuildItem());

    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerServiceInstance(StorkInitializedBuildItem storkInitializedBuildItem, ShutdownContextBuildItem shutdown,
            SmallRyeStorkRegistrationRecorder registrationRecorder, StorkConfiguration configuration) {
        if (QuarkusClassLoader.isClassPresentAtRuntime(SERVICE_REGISTRAR_PROVIDER)) {
            registrationRecorder.registerServiceInstance(configuration);
            registrationRecorder.deregisterServiceInstance(shutdown, configuration);
        }
    }

    private static final class AlwaysBuildItem extends EmptyBuildItem {
        // Just here to be sure we run the `checkThatTheKubernetesExtensionIsUsedWhenKubernetesServiceDiscoveryInOnTheClasspath` build step.
    }

}
