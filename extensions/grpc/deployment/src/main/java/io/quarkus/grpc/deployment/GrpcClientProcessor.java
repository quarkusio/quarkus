package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_CLIENT;
import static io.quarkus.grpc.deployment.GrpcDotNames.CONFIGURE_STUB;
import static io.quarkus.grpc.deployment.GrpcDotNames.CREATE_CHANNEL_METHOD;
import static io.quarkus.grpc.deployment.GrpcDotNames.RETRIEVE_CHANNEL_METHOD;
import static io.quarkus.grpc.deployment.ResourceRegistrationUtils.registerResourcesForProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.grpc.Channel;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.deployment.GrpcClientBuildItem.StubInfo;
import io.quarkus.grpc.deployment.GrpcClientBuildItem.StubType;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
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
        // @GrpcClient is a CDI qualifier
        beans.produce(new AdditionalBeanBuildItem(GrpcClient.class));
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(GrpcClientConfigProvider.class,
                GrpcClientInterceptorContainer.class, IOThreadClientInterceptor.class).build());
    }

    @BuildStep
    void discoverInjectedGrpcServices(BeanDiscoveryFinishedBuildItem beanDiscovery,
            BuildProducer<GrpcClientBuildItem> services,
            BuildProducer<FeatureBuildItem> features,
            CombinedIndexBuildItem index) {

        Map<String, GrpcClientBuildItem> items = new HashMap<>();

        for (InjectionPointInfo injectionPoint : beanDiscovery.getInjectionPoints()) {
            AnnotationInstance clientAnnotation = injectionPoint.getRequiredQualifier(GrpcDotNames.GRPC_CLIENT);
            if (clientAnnotation == null) {
                continue;
            }

            String serviceName;
            AnnotationValue serviceNameValue = clientAnnotation.value();
            if (serviceNameValue == null || serviceNameValue.asString().equals(GrpcClient.ELEMENT_NAME)) {
                // Determine the service name from the annotated element
                if (clientAnnotation.target().kind() == Kind.FIELD) {
                    serviceName = clientAnnotation.target().asField().name();
                } else if (clientAnnotation.target().kind() == Kind.METHOD_PARAMETER) {
                    MethodParameterInfo param = clientAnnotation.target().asMethodParameter();
                    serviceName = param.method().parameterName(param.position());
                    if (serviceName == null) {
                        throw new DeploymentException("Unable to determine the service name from the parameter at position "
                                + param.position()
                                + " in method "
                                + param.method().declaringClass().name() + "#" + param.method().name()
                                + "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or use GrpcClient#value() to specify the service name");
                    }
                } else {
                    // This should never happen because @GrpcClient has @Target({ FIELD, PARAMETER })
                    throw new IllegalStateException(clientAnnotation + " may not be declared at " + clientAnnotation.target());
                }
            } else {
                serviceName = serviceNameValue.asString();
            }

            if (serviceName.trim().isEmpty()) {
                throw new DeploymentException(
                        "Invalid @GrpcClient `" + injectionPoint.getTargetInfo() + "` - service name cannot be empty");
            }

            GrpcClientBuildItem item;
            if (items.containsKey(serviceName)) {
                item = items.get(serviceName);
            } else {
                item = new GrpcClientBuildItem(serviceName);
                items.put(serviceName, item);
            }

            Type injectionType = injectionPoint.getRequiredType();

            // Programmatic lookup - take the param type
            if (DotNames.INSTANCE.equals(injectionType.name()) || DotNames.INJECTABLE_INSTANCE.equals(injectionType.name())) {
                injectionType = injectionType.asParameterizedType().arguments().get(0);
            }

            if (injectionType.name().equals(GrpcDotNames.CHANNEL)) {
                // No need to add the stub class for Channel
                continue;
            }

            // Only blocking and Mutiny stubs are supported
            // The required type must have io.grpc.stub.AbstractBlockingStub or io.quarkus.grpc.runtime.MutinyStub in the hierarchy
            // Note that we must use the computing index because the generated stubs are not part of the app index
            Set<DotName> rawTypes = getRawTypeClosure(index.getComputingIndex().getClassByName(injectionType.name()),
                    index.getComputingIndex());

            if (rawTypes.contains(GrpcDotNames.ABSTRACT_BLOCKING_STUB)) {
                item.addStub(injectionType.name(), StubType.BLOCKING);
            } else if (rawTypes.contains(GrpcDotNames.MUTINY_STUB)) {
                item.addStub(injectionType.name(), StubType.MUTINY);
            } else {
                throw new DeploymentException(
                        injectionType + " cannot be injected into " + injectionPoint.getTargetInfo()
                                + " - only blocking stubs, reactive stubs based on Mutiny and io.grpc.Channel can be injected via @GrpcClient");
            }
        }

        if (!items.isEmpty()) {
            for (GrpcClientBuildItem item : items.values()) {
                services.produce(item);
                LOGGER.debugf("Detected GrpcService associated with the '%s' configuration prefix", item.getServiceName());
            }
            features.produce(new FeatureBuildItem(GRPC_CLIENT));
        }
    }

    private static Set<DotName> getRawTypeClosure(ClassInfo classInfo, IndexView index) {
        Set<DotName> types = new HashSet<>();
        types.add(classInfo.name());
        // Interfaces
        for (DotName name : classInfo.interfaceNames()) {
            ClassInfo interfaceClassInfo = index.getClassByName(name);
            if (interfaceClassInfo != null) {
                types.addAll(getRawTypeClosure(interfaceClassInfo, index));
            } else {
                // Interface not found in the index
                types.add(name);
            }
        }
        // Superclass
        DotName superName = classInfo.superName();
        if (superName != null && !DotNames.OBJECT.equals(superName)) {
            ClassInfo superClassInfo = index.getClassByName(superName);
            if (superClassInfo != null) {
                types.addAll(getRawTypeClosure(superClassInfo, index));
            } else {
                // Superclass not found in the index
                types.add(superName);
            }
        }
        return types;
    }

    @BuildStep
    public void generateGrpcServicesProducers(List<GrpcClientBuildItem> services,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        for (GrpcClientBuildItem svc : services) {
            // We generate 3 synthetic beans:
            // 1. the channel
            // 2. the blocking stub - if blocking stub is set
            // 3. the mutiny stub - if mutiny stub is set

            // IMPORTANT: the channel producer relies on the io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider
            // bean that provides the GrpcClientConfiguration for the specific service.

            syntheticBeans.produce(SyntheticBeanBuildItem.configure(GrpcDotNames.CHANNEL)
                    .addQualifier().annotation(GrpcDotNames.GRPC_CLIENT).addValue("value", svc.getServiceName()).done()
                    .scope(Singleton.class)
                    .unremovable()
                    .creator(new Consumer<MethodCreator>() {
                        @Override
                        public void accept(MethodCreator mc) {
                            GrpcClientProcessor.this.generateChannelProducer(mc, svc);
                        }
                    })
                    .destroyer(Channels.ChannelDestroyer.class).done());

            String svcName = svc.getServiceName();
            for (StubInfo stub : svc.getStubs()) {
                syntheticBeans.produce(SyntheticBeanBuildItem.configure(stub.className)
                        .addQualifier().annotation(GrpcDotNames.GRPC_CLIENT).addValue("value", svcName).done()
                        .scope(Singleton.class)
                        .creator(new Consumer<MethodCreator>() {
                            @Override
                            public void accept(MethodCreator mc) {
                                GrpcClientProcessor.this.generateStubProducer(mc, svcName, stub);
                            }
                        }).done());
            }
        }
    }

    @BuildStep
    void registerSslResources(BuildProducer<NativeImageResourceBuildItem> resourceBuildItem) {
        Config config = ConfigProvider.getConfig();
        registerResourcesForProperties(config, resourceBuildItem, TRUST_STORE_PATTERN, CERTIFICATE_PATTERN, KEY_PATTERN);
    }

    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // io.grpc.internal.RetriableStream uses j.u.Ramdom, so needs to be runtime-initialized
        producer.produce(new RuntimeInitializedClassBuildItem("io.grpc.internal.RetriableStream"));
    }

    private void generateChannelProducer(MethodCreator mc, GrpcClientBuildItem svc) {
        ResultHandle name = mc.load(svc.getServiceName());
        ResultHandle result = mc.invokeStaticMethod(CREATE_CHANNEL_METHOD, name);
        mc.returnValue(result);
        mc.close();
    }

    private void generateStubProducer(MethodCreator mc, String svcName, StubInfo stubInfo) {
        ResultHandle serviceName = mc.load(svcName);

        // First obtain the channel instance for the given service name
        ResultHandle channel = mc.invokeStaticMethod(RETRIEVE_CHANNEL_METHOD, serviceName);

        // Then create the stub, e.g. newBlockingStub(channel)
        MethodDescriptor factoryMethod = MethodDescriptor
                .ofMethod(convertToServiceName(stubInfo.className), stubInfo.type.getFactoryMethodName(),
                        stubInfo.className.toString(),
                        Channel.class.getName());
        ResultHandle stub = mc.invokeStaticMethod(factoryMethod, channel);

        // If needed, modify the call options, e.g. stub = stub.withCompression("gzip")
        stub = mc.invokeStaticMethod(CONFIGURE_STUB, serviceName, stub);

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
