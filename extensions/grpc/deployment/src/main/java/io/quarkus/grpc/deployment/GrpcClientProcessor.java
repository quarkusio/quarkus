package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.builditem.FeatureBuildItem.GRPC_CLIENT;
import static io.quarkus.grpc.deployment.GrpcDotNames.CREATE_CHANNEL_METHOD;
import static io.quarkus.grpc.deployment.GrpcDotNames.RETRIEVE_CHANNEL_METHOD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Singleton;

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
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider;

public class GrpcClientProcessor {

    private static final Logger LOGGER = Logger.getLogger(GrpcClientProcessor.class.getName());

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(GRPC_CLIENT);
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcService.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcClientConfigProvider.class));
    }

    @BuildStep
    void discoverInjectedGrpcServices(
            BeanRegistrationPhaseBuildItem phase,
            BuildProducer<GrpcServiceBuildItem> services) {

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
                if (isMutinyStub(type.name())) {
                    item.setMutinyStubClass(type);
                } else {
                    item.setBlockingStubClass(type);
                }
            }
        }

        items.values().forEach(item -> {
            services.produce(item);
            LOGGER.infof("Detected GrpcService associated with the '%s' configuration prefix", item.name);
        });
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
                    .creator(mc -> generateChannelProducer(mc, svc));
            channelProducer.done();
            beans.produce(new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem(channelProducer));

            if (svc.blockingStubClass != null) {
                BeanConfigurator<Object> blockingStubProducer = phase.getContext()
                        .configure(svc.blockingStubClass.name())
                        .types(svc.blockingStubClass)
                        .addQualifier().annotation(GrpcDotNames.GRPC_SERVICE).addValue("value", svc.getServiceName()).done()
                        .scope(Singleton.class)
                        .creator(mc -> generateStubProducer(mc, svc, false));
                blockingStubProducer.done();
                beans.produce(new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem(blockingStubProducer));
            }

            if (svc.mutinyStubClass != null) {
                BeanConfigurator<Object> blockingStubProducer = phase.getContext()
                        .configure(svc.mutinyStubClass.name())
                        .types(svc.mutinyStubClass)
                        .addQualifier().annotation(GrpcDotNames.GRPC_SERVICE).addValue("value", svc.getServiceName()).done()
                        .scope(Singleton.class)
                        .creator(mc -> generateStubProducer(mc, svc, true));
                blockingStubProducer.done();
                beans.produce(new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem(blockingStubProducer));
            }
        }
    }

    private void generateChannelProducer(MethodCreator mc, GrpcServiceBuildItem svc) {
        ResultHandle name = mc.load(svc.getServiceName());
        ResultHandle result = mc.invokeStaticMethod(CREATE_CHANNEL_METHOD, name);
        mc.returnValue(result);
        mc.close();
    }

    private void generateStubProducer(MethodCreator mc, GrpcServiceBuildItem svc, boolean mutiny) {
        ResultHandle prefix = mc.load(svc.getServiceName());
        ResultHandle channel = mc.invokeStaticMethod(RETRIEVE_CHANNEL_METHOD, prefix);

        MethodDescriptor descriptor;
        if (mutiny) {
            descriptor = MethodDescriptor
                    .ofMethod(svc.getMutinyGrpcServiceName(), "newMutinyStub", svc.mutinyStubClass.name().toString(),
                            Channel.class.getName());
        } else {
            descriptor = MethodDescriptor
                    .ofMethod(svc.getBlockingGrpcServiceName(), "newBlockingStub",
                            svc.blockingStubClass.name().toString(),
                            Channel.class.getName());
        }

        ResultHandle stub = mc.invokeStaticMethod(descriptor, channel);
        mc.returnValue(stub);
        mc.close();
    }
}
