package io.quarkus.deployment.steps;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.deployment.ConfigBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.configuration.SystemOnlySourcesConfigBuilder;
import io.quarkus.runtime.graal.InetRunTime;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

class ConfigBuildSteps {
    static final String SERVICES_PREFIX = "META-INF/services/";

    // XXX replace this with constant-folded service loader impl
    @BuildStep
    void nativeServiceProviders(
            final BuildProducer<ServiceProviderBuildItem> providerProducer) throws IOException {
        providerProducer.produce(new ServiceProviderBuildItem(ConfigProviderResolver.class.getName(),
                SmallRyeConfigProviderResolver.class.getName()));
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (Class<?> serviceClass : Arrays.asList(
                Converter.class,
                ConfigSourceInterceptor.class,
                ConfigSourceInterceptorFactory.class,
                SecretKeysHandler.class,
                SecretKeysHandlerFactory.class,
                ConfigValidator.class)) {
            final String serviceName = serviceClass.getName();
            final Set<String> names = ServiceUtil.classNamesNamedIn(classLoader, SERVICES_PREFIX + serviceName);
            if (!names.isEmpty()) {
                providerProducer.produce(new ServiceProviderBuildItem(serviceName, names));
            }
        }
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        return new RuntimeInitializedClassBuildItem(InetRunTime.class.getName());
    }

    @BuildStep(onlyIf = SystemOnlySources.class)
    void systemOnlySources(BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(SystemOnlySourcesConfigBuilder.class.getName()));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(SystemOnlySourcesConfigBuilder.class.getName()));
    }

    private static class SystemOnlySources implements BooleanSupplier {
        ConfigBuildTimeConfig configBuildTimeConfig;

        @Override
        public boolean getAsBoolean() {
            return configBuildTimeConfig.systemOnly();
        }
    }
}
