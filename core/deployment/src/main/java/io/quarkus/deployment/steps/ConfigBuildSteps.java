package io.quarkus.deployment.steps;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.graal.InetRunTime;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.SmallRyeConfigProviderResolver;

class ConfigBuildSteps {

    static final String PROVIDER_CLASS_NAME = "io.quarkus.runtime.generated.ConfigSourceProviderImpl";

    static final String SERVICES_PREFIX = "META-INF/services/";

    @BuildStep
    void generateConfigSources(List<RunTimeConfigurationSourceBuildItem> runTimeSources,
            final BuildProducer<GeneratedClassBuildItem> generatedClass,
            LiveReloadBuildItem liveReloadBuildItem) {
        if (liveReloadBuildItem.isLiveReload()) {
            return;
        }
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, false);

        try (ClassCreator cc = ClassCreator.builder().interfaces(ConfigSourceProvider.class).setFinal(true)
                .className(PROVIDER_CLASS_NAME)
                .classOutput(classOutput).build()) {
            try (MethodCreator mc = cc.getMethodCreator(MethodDescriptor.ofMethod(ConfigSourceProvider.class,
                    "getConfigSources", Iterable.class, ClassLoader.class))) {

                final ResultHandle array = mc.newArray(ConfigSource.class, mc.load(runTimeSources.size()));
                for (int i = 0; i < runTimeSources.size(); i++) {
                    final RunTimeConfigurationSourceBuildItem runTimeSource = runTimeSources.get(i);
                    final String className = runTimeSource.getClassName();
                    final OptionalInt priority = runTimeSource.getPriority();
                    ResultHandle value;
                    if (priority.isPresent()) {
                        value = mc.newInstance(MethodDescriptor.ofConstructor(className, int.class),
                                mc.load(priority.getAsInt()));
                    } else {
                        value = mc.newInstance(MethodDescriptor.ofConstructor(className));
                    }
                    mc.writeArrayValue(array, i, value);
                }
                final ResultHandle list = mc.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Arrays.class, "asList", List.class, Object[].class), array);
                mc.returnValue(list);
            }
        }
    }

    // XXX replace this with constant-folded service loader impl
    @BuildStep
    void nativeServiceProviders(
            final BuildProducer<ServiceProviderBuildItem> providerProducer) throws IOException {
        providerProducer.produce(new ServiceProviderBuildItem(ConfigProviderResolver.class.getName(),
                SmallRyeConfigProviderResolver.class.getName()));
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        classLoader.getResources(SERVICES_PREFIX + ConfigSourceProvider.class.getName());
        for (Class<?> serviceClass : Arrays.asList(
                ConfigSource.class,
                ConfigSourceProvider.class,
                Converter.class,
                ConfigSourceInterceptor.class,
                ConfigSourceInterceptorFactory.class,
                ConfigSourceFactory.class,
                ConfigValidator.class)) {
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
