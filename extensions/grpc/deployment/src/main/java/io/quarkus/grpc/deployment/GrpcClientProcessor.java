package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_CLIENT;
import static io.quarkus.grpc.deployment.GrpcDotNames.CREATE_CHANNEL_METHOD;
import static io.quarkus.grpc.deployment.GrpcDotNames.RETRIEVE_CHANNEL_METHOD;
import static io.quarkus.grpc.deployment.ResourceRegistrationUtils.registerResourcesForProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.grpc.Channel;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.grpc.runtime.supports.Channels;
import io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider;
import io.quarkus.grpc.runtime.supports.IOThreadClientInterceptor;

public class GrpcClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(GrpcClientProcessor.class.getName());

    private static final String SSL_PREFIX = "quarkus\\.grpc\\.clients\\..*.ssl\\.";
    private static final Pattern KEY_PATTERN = Pattern.compile(SSL_PREFIX + "key");
    private static final Pattern CERTIFICATE_PATTERN = Pattern.compile(SSL_PREFIX + "certificate");
    private static final Pattern TRUST_STORE_PATTERN = Pattern.compile(SSL_PREFIX + "trust-store");

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcService.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcClientConfigProvider.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcClientInterceptorContainer.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(IOThreadClientInterceptor.class));
    }

    @BuildStep
    void discoverInjectedGrpcServices(
            BeanRegistrationPhaseBuildItem phase,
            BuildProducer<GrpcServiceBuildItem> services,
            BuildProducer<FeatureBuildItem> features) {

        Map<String, GrpcServiceBuildItem> items = new HashMap<>();

        for (InjectionPointInfo injectionPoint : phase.getContext()
                .get(BuildExtension.Key.INJECTION_POINTS)) {
            AnnotationInstance instance = injectionPoint.getRequiredQualifier(GrpcDotNames.GRPC_SERVICE);
            if (instance == null) {
                continue;
            }

            String name = instance.value().asString();
            if (name.trim().isEmpty()) {
                throw new DeploymentException(
                        "Invalid @GrpcService `" + injectionPoint.getTargetInfo() + "` - missing configuration key");
            }

            GrpcServiceBuildItem item;
            if (items.containsKey(name)) {
                item = items.get(name);
            } else {
                item = new GrpcServiceBuildItem(name);
                items.put(name, item);
            }

            Type injectionType = injectionPoint.getRequiredType();
            ClassType type;
            if (injectionType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                // Instance<X>
                type = injectionType.asParameterizedType().arguments().get(0).asClassType();
            } else {
                // X directly
                type = injectionType.asClassType();
            }
            if (!type.name().equals(GrpcDotNames.CHANNEL)) {
                item.addStubClass(type);
            }
        }

        items.values().forEach(new Consumer<GrpcServiceBuildItem>() {
            @Override
            public void accept(GrpcServiceBuildItem item) {
                services.produce(item);
                LOGGER.debugf("Detected GrpcService associated with the '%s' configuration prefix", item.name);
            }
        });

        if (!items.isEmpty()) {
            features.produce(new FeatureBuildItem(GRPC_CLIENT));
        }
    }

    private boolean isMutinyStub(DotName name) {
        return name.local().startsWith("Mutiny") && name.local().endsWith("Stub");
    }

    @BuildStep
    public void generateGrpcServicesProducers(List<GrpcServiceBuildItem> services,
            BeanRegistrationPhaseBuildItem phase,
            BuildProducer<BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem> beans) {

        for (GrpcServiceBuildItem svc : services) {
            // We generate 3 producers:
            // 1. the channel
            // 2. the blocking stub - if blocking stub is set
            // 3. the mutiny stub - if mutiny stub is set

            // IMPORTANT: the channel producer relies on the io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider
            // bean that provides the GrpcClientConfiguration for the specific service.

            BeanConfigurator<Object> channelProducer = phase.getContext()
                    .configure(GrpcDotNames.CHANNEL)
                    .types(Channel.class)
                    .addQualifier().annotation(GrpcDotNames.GRPC_SERVICE).addValue("value", svc.getServiceName()).done()
                    .scope(Singleton.class)
                    .unremovable()
                    .creator(new Consumer<MethodCreator>() {
                        @Override
                        public void accept(MethodCreator mc) {
                            GrpcClientProcessor.this.generateChannelProducer(mc, svc);
                        }
                    })
                    .destroyer(Channels.ChannelDestroyer.class);
            channelProducer.done();
            beans.produce(new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem(channelProducer));

            String svcName = svc.getServiceName();
            for (ClassType stubClass : svc.getStubClasses()) {
                DotName stubClassName = stubClass.name();
                BeanConfigurator<Object> stubProducer = phase.getContext()
                        .configure(stubClassName)
                        .types(stubClass)
                        .addQualifier().annotation(GrpcDotNames.GRPC_SERVICE).addValue("value", svcName).done()
                        .scope(Singleton.class)
                        .creator(new Consumer<MethodCreator>() {
                            @Override
                            public void accept(MethodCreator mc) {
                                GrpcClientProcessor.this.generateStubProducer(mc, svcName, stubClassName,
                                        isMutinyStub(stubClassName));
                            }
                        });
                stubProducer.done();
                beans.produce(new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem(stubProducer));
            }
        }
    }

    @BuildStep
    void registerSslResources(BuildProducer<NativeImageResourceBuildItem> resourceBuildItem) {
        Config config = ConfigProvider.getConfig();
        registerResourcesForProperties(config, resourceBuildItem, TRUST_STORE_PATTERN, CERTIFICATE_PATTERN, KEY_PATTERN);
    }

    private void generateChannelProducer(MethodCreator mc, GrpcServiceBuildItem svc) {
        ResultHandle name = mc.load(svc.getServiceName());
        ResultHandle result = mc.invokeStaticMethod(CREATE_CHANNEL_METHOD, name);
        mc.returnValue(result);
        mc.close();
    }

    private void generateStubProducer(MethodCreator mc, String svcName, DotName stubClassName, boolean mutiny) {
        ResultHandle prefix = mc.load(svcName);
        ResultHandle channel = mc.invokeStaticMethod(RETRIEVE_CHANNEL_METHOD, prefix);

        MethodDescriptor descriptor;
        if (mutiny) {
            descriptor = MethodDescriptor
                    .ofMethod(convertToServiceName(stubClassName), "newMutinyStub",
                            stubClassName.toString(),
                            Channel.class.getName());
        } else {
            descriptor = MethodDescriptor
                    .ofMethod(convertToServiceName(stubClassName), "newBlockingStub",
                            stubClassName.toString(),
                            Channel.class.getName());
        }

        ResultHandle stub = mc.invokeStaticMethod(descriptor, channel);
        mc.returnValue(stub);
        mc.close();
    }

    private String convertToServiceName(DotName stubName) {
        if (stubName.isInner()) {
            return stubName.prefix().toString();
        } else {
            return stubName.toString();
        }
    }
}
