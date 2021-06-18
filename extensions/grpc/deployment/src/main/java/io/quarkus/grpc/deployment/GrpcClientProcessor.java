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
import java.util.function.BiFunction;
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
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.grpc.Channel;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
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
import io.quarkus.grpc.deployment.GrpcClientBuildItem.ClientInfo;
import io.quarkus.grpc.deployment.GrpcClientBuildItem.ClientType;
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
            BuildProducer<GrpcClientBuildItem> clients,
            BuildProducer<FeatureBuildItem> features,
            CombinedIndexBuildItem index) {

        Map<String, GrpcClientBuildItem> items = new HashMap<>();

        // Collect a map of service interface name to the generated client class
        Map<DotName, DotName> generatedClients = new HashMap<>();
        for (ClassInfo generatedClient : index.getIndex().getKnownDirectImplementors(GrpcDotNames.MUTINY_CLIENT)) {
            // Mutiny client implements MutinyClient and the service interface
            DotName serviceInterface = null;
            for (DotName name : generatedClient.interfaceNames()) {
                if (!name.equals(GrpcDotNames.MUTINY_CLIENT)) {
                    serviceInterface = name;
                    break;
                }
            }
            if (serviceInterface == null) {
                throw new IllegalStateException(
                        "Unable to derive the service interface for the generated Mutiny client: " + generatedClient);
            }
            generatedClients.put(serviceInterface, generatedClient.name());
        }

        for (InjectionPointInfo injectionPoint : beanDiscovery.getInjectionPoints()) {
            AnnotationInstance clientAnnotation = injectionPoint.getRequiredQualifier(GrpcDotNames.GRPC_CLIENT);
            if (clientAnnotation == null) {
                continue;
            }

            String clientName;
            AnnotationValue clientNameValue = clientAnnotation.value();
            if (clientNameValue == null || clientNameValue.asString().equals(GrpcClient.ELEMENT_NAME)) {
                // Determine the service name from the annotated element
                if (clientAnnotation.target().kind() == Kind.FIELD) {
                    clientName = clientAnnotation.target().asField().name();
                } else if (clientAnnotation.target().kind() == Kind.METHOD_PARAMETER) {
                    MethodParameterInfo param = clientAnnotation.target().asMethodParameter();
                    clientName = param.method().parameterName(param.position());
                    if (clientName == null) {
                        throw new DeploymentException("Unable to determine the client name from the parameter at position "
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
                clientName = clientNameValue.asString();
            }

            if (clientName.trim().isEmpty()) {
                throw new DeploymentException(
                        "Invalid @GrpcClient `" + injectionPoint.getTargetInfo() + "` - client name cannot be empty");
            }

            GrpcClientBuildItem item;
            if (items.containsKey(clientName)) {
                item = items.get(clientName);
            } else {
                item = new GrpcClientBuildItem(clientName);
                items.put(clientName, item);
            }

            Type injectionType = injectionPoint.getRequiredType();
            if (injectionType.name().equals(GrpcDotNames.CHANNEL)) {
                // No need to add the stub class for Channel
                continue;
            }

            // Clients supported: blocking stubs, Mutiny stubs, Mutiny client implementing the service interface
            // The required type must have AbstractBlockingStub, MutinyStub or MutinyService in the hierarchy
            // Note that we must use the computing index because the generated stubs don't need to be part of the app index
            Set<DotName> rawTypes = getRawTypeClosure(index.getComputingIndex().getClassByName(injectionType.name()),
                    index.getComputingIndex());

            if (rawTypes.contains(GrpcDotNames.ABSTRACT_BLOCKING_STUB)) {
                item.addClient(new ClientInfo(injectionType.name(), ClientType.BLOCKING_STUB));
            } else if (rawTypes.contains(GrpcDotNames.MUTINY_STUB)) {
                item.addClient(new ClientInfo(injectionType.name(), ClientType.MUTINY_STUB));
            } else if (rawTypes.contains(GrpcDotNames.MUTINY_SERVICE)) {
                DotName generatedClient = generatedClients.get(injectionType.name());
                if (generatedClient == null) {
                    throw invalidInjectionPoint(injectionPoint);
                }
                item.addClient(new ClientInfo(injectionType.name(), ClientType.MUTINY_CLIENT,
                        generatedClient));
            } else {
                throw invalidInjectionPoint(injectionPoint);
            }
        }

        if (!items.isEmpty()) {
            for (GrpcClientBuildItem item : items.values()) {
                clients.produce(item);
                LOGGER.debugf("Detected GrpcService associated with the '%s' configuration prefix", item.getClientName());
            }
            features.produce(new FeatureBuildItem(GRPC_CLIENT));
        }
    }

    @BuildStep
    public void generateGrpcClientProducers(List<GrpcClientBuildItem> clients,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        for (GrpcClientBuildItem client : clients) {
            // For every service we register:
            // 1. the channel
            // 2. the blocking stub - if needed
            // 3. the mutiny stub - if needed
            // 4. the mutiny client - if needed 

            // IMPORTANT: the channel producer relies on the io.quarkus.grpc.runtime.supports.GrpcClientConfigProvider
            // bean that provides the GrpcClientConfiguration for the specific service.

            syntheticBeans.produce(SyntheticBeanBuildItem.configure(GrpcDotNames.CHANNEL)
                    .addQualifier().annotation(GrpcDotNames.GRPC_CLIENT).addValue("value", client.getClientName()).done()
                    .scope(Singleton.class)
                    .unremovable()
                    .creator(new Consumer<MethodCreator>() {
                        @Override
                        public void accept(MethodCreator mc) {
                            GrpcClientProcessor.this.generateChannelProducer(mc, client);
                        }
                    })
                    .destroyer(Channels.ChannelDestroyer.class).done());

            String svcName = client.getClientName();
            for (ClientInfo clientInfo : client.getClients()) {
                syntheticBeans.produce(SyntheticBeanBuildItem.configure(clientInfo.className)
                        .addQualifier().annotation(GrpcDotNames.GRPC_CLIENT).addValue("value", svcName).done()
                        .scope(Singleton.class)
                        .creator(new Consumer<MethodCreator>() {
                            @Override
                            public void accept(MethodCreator mc) {
                                GrpcClientProcessor.this.generateClientProducer(mc, svcName, clientInfo);
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
        // io.grpc.internal.RetriableStream uses j.u.Random, so needs to be runtime-initialized
        producer.produce(new RuntimeInitializedClassBuildItem("io.grpc.internal.RetriableStream"));
    }

    @BuildStep
    public void validateInjectedServiceInterfaces(CombinedIndexBuildItem index,
            // Dummy producer - this build step needs to be executed before the CDI container is initialized
            BuildProducer<UnremovableBeanBuildItem> dummy) {
        // Attempt to detect wrong service interface injection points
        // Note that we cannot use injection points metadata because the build can fail with unsatisfied dependency before
        Set<DotName> serviceInterfaces = new HashSet<>();
        for (ClassInfo serviceInterface : index.getIndex().getKnownDirectImplementors(GrpcDotNames.MUTINY_SERVICE)) {
            serviceInterfaces.add(serviceInterface.name());
        }

        for (AnnotationInstance injectAnnotation : index.getIndex().getAnnotations(DotNames.INJECT)) {
            if (injectAnnotation.target().kind() == Kind.FIELD) {
                FieldInfo field = injectAnnotation.target().asField();
                if (serviceInterfaces.contains(field.type().name()) && field.annotations().size() == 1) {
                    // e.g. @Inject Greeter
                    throw new IllegalStateException("A gRPC service injection is missing the @GrcpClient qualifier: "
                            + field.declaringClass().name() + "#" + field.name());
                }
            } else if (injectAnnotation.target().kind() == Kind.METHOD) {
                // CDI initializer
                MethodInfo method = injectAnnotation.target().asMethod();
                short position = 0;
                for (Type param : method.parameters()) {
                    position++;
                    if (serviceInterfaces.contains(param.name())) {
                        // e.g. @Inject void setGreeter(Greeter greeter)
                        Set<AnnotationInstance> annotations = new HashSet<>();
                        for (AnnotationInstance annotation : method.annotations()) {
                            if (annotation.target().kind() == Kind.METHOD_PARAMETER
                                    && annotation.target().asMethodParameter().position() == position) {
                                annotations.add(annotation);
                            }
                        }
                        if (annotations.size() > 1) {
                            throw new IllegalStateException("A gRPC service injection is missing the @GrcpClient qualifier: "
                                    + method.declaringClass().name() + "#" + method.name() + "()");
                        }
                    }
                }
            }
        }
    }

    private DeploymentException invalidInjectionPoint(InjectionPointInfo injectionPoint) {
        return new DeploymentException(
                injectionPoint.getRequiredType() + " cannot be injected into " + injectionPoint.getTargetInfo()
                        + " - only Mutiny service interfaces, blocking stubs, reactive stubs based on Mutiny and io.grpc.Channel can be injected via @GrpcClient");
    }

    private void generateChannelProducer(MethodCreator mc, GrpcClientBuildItem svc) {
        ResultHandle name = mc.load(svc.getClientName());
        ResultHandle result = mc.invokeStaticMethod(CREATE_CHANNEL_METHOD, name);
        mc.returnValue(result);
        mc.close();
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

    private void generateClientProducer(MethodCreator mc, String svcName, ClientInfo clientInfo) {
        ResultHandle serviceName = mc.load(svcName);

        // First obtain the channel instance for the given service name
        ResultHandle channel = mc.invokeStaticMethod(RETRIEVE_CHANNEL_METHOD, serviceName);
        ResultHandle client;

        if (clientInfo.type == ClientType.MUTINY_CLIENT) {
            // Instantiate the client, e.g. new HealthClient(serviceName,channel,GrpcClientConfigProvider.getStubConfigurator())
            ResultHandle stubConfigurator = mc.invokeStaticMethod(GrpcDotNames.GET_STUB_CONFIGURATOR);
            client = mc.newInstance(
                    MethodDescriptor.ofConstructor(clientInfo.implName.toString(), String.class.getName(),
                            Channel.class.getName(), BiFunction.class),
                    serviceName, channel, stubConfigurator);
        } else {
            // Create the stub, e.g. newBlockingStub(channel)
            MethodDescriptor factoryMethod = MethodDescriptor
                    .ofMethod(convertToServiceName(clientInfo.className), clientInfo.type.getFactoryMethodName(),
                            clientInfo.className.toString(),
                            Channel.class.getName());
            client = mc.invokeStaticMethod(factoryMethod, channel);

            // If needed, modify the call options, e.g. stub = stub.withCompression("gzip")
            client = mc.invokeStaticMethod(CONFIGURE_STUB, serviceName, client);
        }
        mc.returnValue(client);
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
