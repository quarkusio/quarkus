package io.quarkus.deployment.steps;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.graal.InetRunTime;
import io.smallrye.config.SmallRyeConfigProviderResolver;

class ConfigBuildSteps {

    static final String SERVICES_PREFIX = "META-INF/services/";

    // XXX replace this with constant-folded service loader impl
    @BuildStep
    void nativeServiceProviders(
            final DeploymentClassLoaderBuildItem classLoaderItem,
            final BuildProducer<ServiceProviderBuildItem> providerProducer) throws IOException {
        providerProducer.produce(new ServiceProviderBuildItem(ConfigProviderResolver.class.getName(),
                SmallRyeConfigProviderResolver.class.getName()));
        final ClassLoader classLoader = classLoaderItem.getClassLoader();
        classLoader.getResources(SERVICES_PREFIX + ConfigSourceProvider.class.getName());
        for (Class<?> serviceClass : Arrays.asList(
                ConfigSource.class,
                ConfigSourceProvider.class,
                Converter.class)) {
            final String serviceName = serviceClass.getName();
            final Set<String> names = ServiceUtil.classNamesNamedIn(classLoader, SERVICES_PREFIX + serviceName);
            final List<String> list = names.stream()
                    // todo: see https://github.com/quarkusio/quarkus/issues/5492
                    .filter(s -> !s.startsWith("org.jboss.resteasy.microprofile.config.")).collect(Collectors.toList());
            if (!list.isEmpty()) {
                providerProducer.produce(new ServiceProviderBuildItem(serviceName, list));
            }
        }
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        return new RuntimeInitializedClassBuildItem(InetRunTime.class.getName());
    }
}
