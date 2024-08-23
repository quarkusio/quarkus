package io.quarkus.websockets.next.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.security.spi.SecurityTransformerUtils.hasSecurityAnnotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.InvokerFactoryBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InvokerInfo;
import io.quarkus.arc.processor.KotlinDotNames;
import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.arc.processor.Types;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.security.spi.ClassSecurityCheckAnnotationBuildItem;
import io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem;
import io.quarkus.security.spi.SecurityTransformerUtils;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.InboundProcessingMode;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketException;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.deployment.Callback.MessageType;
import io.quarkus.websockets.next.deployment.Callback.Target;
import io.quarkus.websockets.next.runtime.BasicWebSocketConnectorImpl;
import io.quarkus.websockets.next.runtime.ClientConnectionManager;
import io.quarkus.websockets.next.runtime.Codecs;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.quarkus.websockets.next.runtime.ContextSupport;
import io.quarkus.websockets.next.runtime.JsonTextMessageCodec;
import io.quarkus.websockets.next.runtime.SecurityHttpUpgradeCheck;
import io.quarkus.websockets.next.runtime.SecuritySupport;
import io.quarkus.websockets.next.runtime.WebSocketClientRecorder;
import io.quarkus.websockets.next.runtime.WebSocketClientRecorder.ClientEndpoint;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketConnectorImpl;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.ExecutionModel;
import io.quarkus.websockets.next.runtime.WebSocketEndpointBase;
import io.quarkus.websockets.next.runtime.WebSocketHttpServerOptionsCustomizer;
import io.quarkus.websockets.next.runtime.WebSocketServerRecorder;
import io.quarkus.websockets.next.runtime.WebSocketSessionContext;
import io.quarkus.websockets.next.runtime.kotlin.ApplicationCoroutineScope;
import io.quarkus.websockets.next.runtime.kotlin.CoroutineInvoker;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniCreate;
import io.smallrye.mutiny.groups.UniOnFailure;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WebSocketProcessor {

    static final String SERVER_ENDPOINT_SUFFIX = "_WebSocketServerEndpoint";
    static final String CLIENT_ENDPOINT_SUFFIX = "_WebSocketClientEndpoint";
    static final String NESTED_SEPARATOR = "$_";
    static final DotName HTTP_UPGRADE_CHECK_NAME = DotName.createSimple(HttpUpgradeCheck.class);

    // Parameter names consist of alphanumeric characters and underscore
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+\\}");
    public static final Pattern TRANSLATED_PATH_PARAM_PATTERN = Pattern.compile(":[a-zA-Z0-9_]+");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("websockets-next");
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations.produce(new BeanDefiningAnnotationBuildItem(WebSocketDotNames.WEB_SOCKET, DotNames.SINGLETON));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(WebSocketDotNames.WEB_SOCKET_CLIENT, DotNames.SINGLETON));
    }

    @BuildStep
    AutoAddScopeBuildItem addScopeToGlobalErrorHandlers() {
        return AutoAddScopeBuildItem.builder()
                .containsAnnotations(WebSocketDotNames.ON_ERROR)
                .unremovable()
                .reason("Add @Singleton to a global WebSocket error handler")
                .defaultScope(BuiltinScope.SINGLETON).build();
    }

    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem executionModelAnnotations(
            TransformedAnnotationsBuildItem transformedAnnotations) {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return Annotations.containsAny(transformedAnnotations.getAnnotations(method),
                        WebSocketDotNames.CALLBACK_ANNOTATIONS);
            }
        });
    }

    @BuildStep
    CallbackArgumentsBuildItem collectCallbackArguments(List<CallbackArgumentBuildItem> callbackArguments) {
        List<CallbackArgument> sorted = new ArrayList<>();
        for (CallbackArgumentBuildItem callbackArgument : callbackArguments) {
            sorted.add(callbackArgument.getProvider());
        }
        sorted.sort(Comparator.comparingInt(CallbackArgument::priotity).reversed());
        return new CallbackArgumentsBuildItem(sorted);
    }

    @BuildStep
    void additionalBeans(CombinedIndexBuildItem combinedIndex, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = combinedIndex.getIndex();

        // Always register the removable beans
        AdditionalBeanBuildItem removable = AdditionalBeanBuildItem.builder()
                .setRemovable()
                .addBeanClasses(WebSocketConnectorImpl.class, JsonTextMessageCodec.class)
                .build();
        additionalBeans.produce(removable);

        AdditionalBeanBuildItem.Builder unremovable = AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(Codecs.class, ClientConnectionManager.class, BasicWebSocketConnectorImpl.class);
        if (!index.getAnnotations(WebSocketDotNames.WEB_SOCKET).isEmpty()) {
            unremovable.addBeanClasses(ConnectionManager.class, WebSocketHttpServerOptionsCustomizer.class);
        }
        additionalBeans.produce(unremovable.build());
    }

    @BuildStep
    void produceCoroutineScope(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime("kotlinx.coroutines.CoroutineScope")) {
            return;
        }

        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ApplicationCoroutineScope.class)
                .setUnremovable()
                .build());
    }

    @BuildStep
    ContextConfiguratorBuildItem registerSessionContext(ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(phase.getContext()
                .configure(SessionScoped.class)
                .normal()
                .contextClass(WebSocketSessionContext.class));
    }

    @BuildStep
    CustomScopeBuildItem registerSessionScope() {
        return new CustomScopeBuildItem(DotName.createSimple(SessionScoped.class.getName()));
    }

    @BuildStep
    void builtinCallbackArguments(BuildProducer<CallbackArgumentBuildItem> providers) {
        providers.produce(new CallbackArgumentBuildItem(new MessageCallbackArgument()));
        providers.produce(new CallbackArgumentBuildItem(new ConnectionCallbackArgument()));
        providers.produce(new CallbackArgumentBuildItem(new PathParamCallbackArgument()));
        providers.produce(new CallbackArgumentBuildItem(new HandshakeRequestCallbackArgument()));
        providers.produce(new CallbackArgumentBuildItem(new ErrorCallbackArgument()));
        providers.produce(new CallbackArgumentBuildItem(new CloseReasonCallbackArgument()));
        providers.produce(new CallbackArgumentBuildItem(new KotlinContinuationCallbackArgument()));
    }

    @BuildStep
    void collectGlobalErrorHandlers(BeanArchiveIndexBuildItem beanArchiveIndex,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            BuildProducer<GlobalErrorHandlersBuildItem> globalErrorHandlers,
            CallbackArgumentsBuildItem callbackArguments,
            TransformedAnnotationsBuildItem transformedAnnotations) {

        IndexView index = beanArchiveIndex.getIndex();

        // Collect global error handlers, i.e. handlers that are not declared on an endpoint
        Map<DotName, GlobalErrorHandler> globalErrors = new HashMap<>();
        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            if (beanClass.declaredAnnotation(WebSocketDotNames.WEB_SOCKET) == null
                    && beanClass.declaredAnnotation(WebSocketDotNames.WEB_SOCKET_CLIENT) == null) {
                for (Callback callback : findErrorHandlers(Target.UNDEFINED, index, bean, beanClass, callbackArguments,
                        transformedAnnotations, null)) {
                    GlobalErrorHandler errorHandler = new GlobalErrorHandler(bean, callback);
                    DotName errorTypeName = callback.argumentType(ErrorCallbackArgument::isError).name();
                    if (globalErrors.containsKey(errorTypeName)) {
                        throw new WebSocketException(String.format(
                                "Multiple global @OnError callbacks may not accept the same error parameter: %s\n\t- %s\n\t- %s",
                                errorTypeName,
                                callback.asString(),
                                globalErrors.get(errorTypeName).callback.asString()));
                    }
                    globalErrors.put(errorTypeName, errorHandler);
                }
            }
        }
        globalErrorHandlers.produce(new GlobalErrorHandlersBuildItem(List.copyOf(globalErrors.values())));
    }

    @BuildStep
    public void collectEndpoints(BeanArchiveIndexBuildItem beanArchiveIndex,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            CallbackArgumentsBuildItem callbackArguments,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<WebSocketEndpointBuildItem> endpoints) {

        IndexView index = beanArchiveIndex.getIndex();

        // Collect WebSocket endpoints
        Map<String, DotName> serverIdToEndpoint = new HashMap<>();
        Map<String, DotName> serverPathToEndpoint = new HashMap<>();
        Map<String, DotName> clientIdToEndpoint = new HashMap<>();
        Map<String, DotName> clientPathToEndpoint = new HashMap<>();

        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            AnnotationInstance webSocketAnnotation = beanClass.annotation(WebSocketDotNames.WEB_SOCKET);
            AnnotationInstance webSocketClientAnnotation = beanClass.annotation(WebSocketDotNames.WEB_SOCKET_CLIENT);

            if (webSocketAnnotation == null && webSocketClientAnnotation == null) {
                continue;
            } else if (webSocketAnnotation != null && webSocketClientAnnotation != null) {
                throw new WebSocketException(
                        "Endpoint class may not be annotated with both @WebSocket and @WebSocketClient: " + beanClass);
            }
            String path;
            String id;
            AnnotationValue inboundProcessingMode;
            Target target;

            if (webSocketAnnotation != null) {
                target = Target.SERVER;
                path = getPath(webSocketAnnotation.value("path").asString());
                if (beanClass.nestingType() == NestingType.INNER) {
                    // Sub-websocket - merge the path from the enclosing classes
                    path = mergePath(getPathPrefix(index, beanClass.enclosingClass()), path);
                }
                DotName prevPath = serverPathToEndpoint.put(path, beanClass.name());
                if (prevPath != null) {
                    throw new WebSocketServerException(
                            String.format("Multiple endpoints [%s, %s] define the same path: %s", prevPath, beanClass, path));
                }
                AnnotationValue endpointIdValue = webSocketAnnotation.value("endpointId");
                if (endpointIdValue == null) {
                    id = beanClass.name().toString();
                } else {
                    id = endpointIdValue.asString();
                }
                DotName prevId = serverIdToEndpoint.put(id, beanClass.name());
                if (prevId != null) {
                    throw new WebSocketServerException(
                            String.format("Multiple endpoints [%s, %s] define the same endpoint id: %s", prevId, beanClass,
                                    id));
                }
                inboundProcessingMode = webSocketAnnotation.value("inboundProcessingMode");
            } else {
                target = Target.CLIENT;
                path = getPath(webSocketClientAnnotation.value("path").asString());
                DotName prevPath = clientPathToEndpoint.put(path, beanClass.name());
                if (prevPath != null) {
                    throw new WebSocketServerException(
                            String.format("Multiple client endpoints [%s, %s] define the same path: %s", prevPath, beanClass,
                                    path));
                }
                AnnotationValue clientIdValue = webSocketClientAnnotation.value("clientId");
                if (clientIdValue == null) {
                    id = beanClass.name().toString();
                } else {
                    id = clientIdValue.asString();
                }
                DotName prevId = clientIdToEndpoint.put(id, beanClass.name());
                if (prevId != null) {
                    throw new WebSocketServerException(
                            String.format("Multiple client endpoints [%s, %s] define the same endpoint id: %s", prevId,
                                    beanClass,
                                    id));
                }
                inboundProcessingMode = webSocketClientAnnotation.value("inboundProcessingMode");
            }

            Callback onOpen = findCallback(target, beanArchiveIndex.getIndex(), bean, beanClass,
                    WebSocketDotNames.ON_OPEN, callbackArguments, transformedAnnotations, path);
            Callback onTextMessage = findCallback(target, beanArchiveIndex.getIndex(), bean, beanClass,
                    WebSocketDotNames.ON_TEXT_MESSAGE, callbackArguments, transformedAnnotations, path);
            Callback onBinaryMessage = findCallback(target, beanArchiveIndex.getIndex(), bean, beanClass,
                    WebSocketDotNames.ON_BINARY_MESSAGE, callbackArguments, transformedAnnotations, path);
            Callback onPongMessage = findCallback(target, beanArchiveIndex.getIndex(), bean, beanClass,
                    WebSocketDotNames.ON_PONG_MESSAGE, callbackArguments, transformedAnnotations, path,
                    this::validateOnPongMessage);
            Callback onClose = findCallback(target, beanArchiveIndex.getIndex(), bean, beanClass,
                    WebSocketDotNames.ON_CLOSE, callbackArguments, transformedAnnotations, path,
                    this::validateOnClose);
            if (onOpen == null && onTextMessage == null && onBinaryMessage == null && onPongMessage == null) {
                throw new WebSocketServerException(
                        "The endpoint must declare at least one method annotated with @OnTextMessage, @OnBinaryMessage, @OnPongMessage or @OnOpen: "
                                + beanClass);
            }
            endpoints.produce(new WebSocketEndpointBuildItem(target == Target.CLIENT, bean, path, id,
                    inboundProcessingMode != null ? InboundProcessingMode.valueOf(inboundProcessingMode.asEnum())
                            : InboundProcessingMode.SERIAL,
                    onOpen,
                    onTextMessage,
                    onBinaryMessage,
                    onPongMessage,
                    onClose,
                    findErrorHandlers(target, index, bean, beanClass, callbackArguments, transformedAnnotations, path)));
        }
    }

    @BuildStep
    public void validateConnectorInjectionPoints(List<WebSocketEndpointBuildItem> endpoints,
            ValidationPhaseBuildItem validationPhase, BuildProducer<ValidationErrorBuildItem> validationErrors) {
        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            if (injectionPoint.getRequiredType().name().equals(WebSocketDotNames.WEB_SOCKET_CONNECTOR)
                    && injectionPoint.hasDefaultedQualifier()) {
                Type clientEndpointType = injectionPoint.getRequiredType().asParameterizedType().arguments().get(0);
                if (endpoints.stream()
                        .filter(WebSocketEndpointBuildItem::isClient)
                        .map(WebSocketEndpointBuildItem::beanClassName)
                        .noneMatch(clientEndpointType.name()::equals)) {
                    validationErrors.produce(
                            new ValidationErrorBuildItem(new WebSocketClientException(String.format(
                                    "Type argument [%s] of the injected WebSocketConnector is not a @WebSocketClient endpoint: %s",
                                    clientEndpointType, injectionPoint.getTargetInfo()))));
                }
            }
        }
    }

    @BuildStep
    public void generateEndpoints(BeanArchiveIndexBuildItem index, List<WebSocketEndpointBuildItem> endpoints,
            CallbackArgumentsBuildItem argumentProviders,
            TransformedAnnotationsBuildItem transformedAnnotations,
            GlobalErrorHandlersBuildItem globalErrorHandlers,
            InvokerFactoryBuildItem invokerFactory,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new Function<String, String>() {
            @Override
            public String apply(String name) {
                int idx = name.indexOf(CLIENT_ENDPOINT_SUFFIX);
                if (idx == -1) {
                    idx = name.indexOf(SERVER_ENDPOINT_SUFFIX);
                }
                if (idx != -1) {
                    name = name.substring(0, idx);
                }
                if (name.contains(NESTED_SEPARATOR)) {
                    name = name.replace(NESTED_SEPARATOR, "$");
                }
                return name;
            }
        });
        for (WebSocketEndpointBuildItem endpoint : endpoints) {
            // For each WebSocket endpoint bean we generate an implementation of WebSocketEndpoint
            // A new instance of this generated endpoint is created for each client connection
            // The generated endpoint ensures the correct execution model is used
            // and delegates callback invocations to the endpoint bean
            String generatedName = generateEndpoint(endpoint, argumentProviders, transformedAnnotations,
                    index.getIndex(), classOutput, globalErrorHandlers,
                    endpoint.isClient() ? CLIENT_ENDPOINT_SUFFIX : SERVER_ENDPOINT_SUFFIX,
                    invokerFactory);
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(generatedName).constructors().build());
            generatedEndpoints
                    .produce(new GeneratedEndpointBuildItem(endpoint.id, endpoint.bean.getImplClazz().name().toString(),
                            generatedName, endpoint.path, endpoint.isClient));
        }
    }

    @Consume(RuntimeConfigSetupCompleteBuildItem.class) // HTTP Upgrade checks may need config during the initialization
    @Consume(SyntheticBeansRuntimeInitBuildItem.class) // SecurityHttpUpgradeCheck is runtime init due to runtime config
    @Record(RUNTIME_INIT)
    @BuildStep
    public void registerRoutes(WebSocketServerRecorder recorder, List<GeneratedEndpointBuildItem> generatedEndpoints,
            HttpBuildTimeConfig httpConfig, Capabilities capabilities,
            BuildProducer<RouteBuildItem> routes) {
        for (GeneratedEndpointBuildItem endpoint : generatedEndpoints.stream().filter(GeneratedEndpointBuildItem::isServer)
                .toList()) {
            RouteBuildItem.Builder builder = RouteBuildItem.builder();
            if (capabilities.isPresent(Capability.SECURITY) && !httpConfig.auth.proactive) {
                // Add a special handler so that it's possible to capture the SecurityIdentity before the HTTP upgrade
                builder.routeFunction(endpoint.path, recorder.initializeSecurityHandler());
            } else {
                builder.route(endpoint.path);
            }
            builder
                    .displayOnNotFoundPage("WebSocket Endpoint")
                    .handlerType(HandlerType.NORMAL)
                    .handler(recorder.createEndpointHandler(endpoint.generatedClassName, endpoint.endpointId));
            routes.produce(builder.build());
        }
    }

    @BuildStep
    UnremovableBeanBuildItem makeHttpUpgradeChecksUnremovable() {
        // we access the checks programmatically
        return UnremovableBeanBuildItem.beanTypes(HTTP_UPGRADE_CHECK_NAME);
    }

    @BuildStep
    List<ValidationPhaseBuildItem.ValidationErrorBuildItem> validateHttpUpgradeCheckNotRequestScoped(
            ValidationPhaseBuildItem validationPhase) {
        return validationPhase
                .getContext()
                .beans()
                .withBeanType(HTTP_UPGRADE_CHECK_NAME)
                .filter(b -> {
                    var targetScope = BuiltinScope.from(b.getScope().getDotName());
                    return BuiltinScope.APPLICATION != targetScope
                            && BuiltinScope.SINGLETON != targetScope
                            && BuiltinScope.DEPENDENT != targetScope;
                })
                .stream()
                .map(b -> new ValidationErrorBuildItem(new RuntimeException(("Bean '%s' scope is '%s', but the '%s' "
                        + "implementors must be one either `@ApplicationScoped', '@Singleton' or '@Dependent' beans")
                        .formatted(b.getBeanClass(), b.getScope().getDotName(), HTTP_UPGRADE_CHECK_NAME))))
                .toList();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void serverSyntheticBeans(WebSocketServerRecorder recorder, List<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        List<GeneratedEndpointBuildItem> serverEndpoints = generatedEndpoints.stream()
                .filter(GeneratedEndpointBuildItem::isServer).toList();
        if (serverEndpoints.isEmpty()) {
            return;
        }
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(WebSocketConnection.class)
                .scope(SessionScoped.class)
                .setRuntimeInit()
                .supplier(recorder.connectionSupplier())
                .unremovable()
                .done());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void clientSyntheticBeans(WebSocketClientRecorder recorder, List<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        List<GeneratedEndpointBuildItem> clientEndpoints = generatedEndpoints.stream()
                .filter(GeneratedEndpointBuildItem::isClient).toList();
        if (!clientEndpoints.isEmpty()) {
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(WebSocketClientConnection.class)
                    .scope(SessionScoped.class)
                    .setRuntimeInit()
                    .supplier(recorder.connectionSupplier())
                    .unremovable()
                    .done());
        }
        // ClientEndpointsContext is always registered but is removable
        Map<String, ClientEndpoint> endpointMap = new HashMap<>();
        for (GeneratedEndpointBuildItem generatedEndpoint : clientEndpoints) {
            endpointMap.put(generatedEndpoint.endpointClassName, new ClientEndpoint(generatedEndpoint.endpointId,
                    getOriginalPath(generatedEndpoint.path), generatedEndpoint.generatedClassName));
        }
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(WebSocketClientRecorder.ClientEndpointsContext.class)
                .setRuntimeInit()
                .supplier(recorder.createContext(endpointMap))
                .done());
    }

    @BuildStep
    void createSecurityChecksForHttpUpgradeCheck(Capabilities capabilities,
            BuildProducer<ClassSecurityCheckAnnotationBuildItem> producer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            producer.produce(new ClassSecurityCheckAnnotationBuildItem(WebSocketDotNames.WEB_SOCKET));
        }
    }

    @BuildStep
    void preventRepeatedSecurityChecksForHttpUpgrade(Capabilities capabilities,
            BuildProducer<AnnotationsTransformerBuildItem> producer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            producer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                    .forClasses()
                    .whenAnyMatch(WebSocketDotNames.WEB_SOCKET)
                    .transform(ctx -> ctx.remove(SecurityTransformerUtils::isStandardSecurityAnnotation))));
        }
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void createSecurityHttpUpgradeCheck(Capabilities capabilities, BuildProducer<SyntheticBeanBuildItem> producer,
            Optional<ClassSecurityCheckStorageBuildItem> storageItem,
            BeanArchiveIndexBuildItem indexItem,
            WebSocketServerRecorder recorder, List<WebSocketEndpointBuildItem> endpoints) {
        if (capabilities.isPresent(Capability.SECURITY) && storageItem.isPresent()) {
            var endpointIdToSecurityCheck = collectEndpointSecurityChecks(endpoints, storageItem.get(), indexItem.getIndex());
            if (!endpointIdToSecurityCheck.isEmpty()) {
                producer.produce(SyntheticBeanBuildItem
                        .configure(HttpUpgradeCheck.class)
                        .scope(BuiltinScope.SINGLETON.getInfo())
                        .priority(SecurityHttpUpgradeCheck.BEAN_PRIORITY)
                        .setRuntimeInit()
                        .supplier(recorder.createSecurityHttpUpgradeCheck(endpointIdToSecurityCheck))
                        .done());
            }
        }
    }

    private static Map<String, SecurityCheck> collectEndpointSecurityChecks(List<WebSocketEndpointBuildItem> endpoints,
            ClassSecurityCheckStorageBuildItem storage, IndexView index) {
        return endpoints
                .stream().<Map.Entry<String, SecurityCheck>> mapMulti((endpoint, consumer) -> {
                    var beanName = endpoint.beanClassName();
                    if (storage.getSecurityCheck(beanName) instanceof SecurityCheck check) {
                        consumer.accept(Map.entry(endpoint.id, check));
                    } else if (hasSecurityAnnotation(index.getClassByName(beanName))) {
                        throw new IllegalStateException("WebSocket endpoint '%s' requires ".formatted(beanName)
                                + "secured HTTP upgrade but Quarkus did not configure security check "
                                + "correctly. Please open issue in Quarkus project");
                    }
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static String mergePath(String prefix, String path) {
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return prefix + path;
    }

    static String getPath(String path) {
        StringBuilder sb = new StringBuilder();
        Matcher m = PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            // Replace {foo} with :foo
            String match = m.group();
            int end = m.end();
            if (end < path.length()) {
                char nextChar = path.charAt(end);
                if (Character.isAlphabetic(nextChar) || Character.isDigit(nextChar) || nextChar == '_') {
                    throw new WebSocketServerException("Path parameter " + match
                            + " may not be followed by an alphanumeric character or underscore: " + path);
                }
            }
            m.appendReplacement(sb, ":" + match.subSequence(1, match.length() - 1));
        }
        m.appendTail(sb);
        return path.startsWith("/") ? sb.toString() : "/" + sb.toString();
    }

    public static String getOriginalPath(String path) {
        StringBuilder sb = new StringBuilder();
        Matcher m = TRANSLATED_PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            // Replace :foo with {foo}
            String match = m.group();
            m.appendReplacement(sb, "{" + match.subSequence(1, match.length()) + "}");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String getPathPrefix(IndexView index, DotName enclosingClassName) {
        ClassInfo enclosingClass = index.getClassByName(enclosingClassName);
        if (enclosingClass == null) {
            throw new WebSocketServerException("Enclosing class not found in index: " + enclosingClass);
        }
        AnnotationInstance webSocketAnnotation = enclosingClass.annotation(WebSocketDotNames.WEB_SOCKET);
        if (webSocketAnnotation != null) {
            String path = getPath(webSocketAnnotation.value("path").asString());
            if (enclosingClass.nestingType() == NestingType.INNER) {
                return mergePath(getPathPrefix(index, enclosingClass.enclosingClass()), path);
            } else {
                return path.endsWith("/") ? path.substring(path.length() - 1) : path;
            }
        }
        return "";
    }

    private void validateOnPongMessage(Callback callback) {
        if (KotlinUtils.isKotlinMethod(callback.method)) {
            if (!callback.isReturnTypeVoid() && !callback.isKotlinSuspendFunctionReturningUnit()) {
                throw new WebSocketServerException(
                        "@OnPongMessage callback must return Unit: " + callback.asString());
            }
        } else {
            if (callback.returnType().kind() != Kind.VOID && !WebSocketProcessor.isUniVoid(callback.returnType())) {
                throw new WebSocketServerException(
                        "@OnPongMessage callback must return void or Uni<Void>: " + callback.asString());
            }
        }
        Type messageType = callback.argumentType(MessageCallbackArgument::isMessage);
        if (messageType == null || !messageType.name().equals(WebSocketDotNames.BUFFER)) {
            throw new WebSocketServerException(
                    "@OnPongMessage callback must accept exactly one message parameter of type io.vertx.core.buffer.Buffer: "
                            + callback.asString());
        }
    }

    private void validateOnClose(Callback callback) {
        if (KotlinUtils.isKotlinMethod(callback.method)) {
            if (!callback.isReturnTypeVoid() && !callback.isKotlinSuspendFunctionReturningUnit()) {
                throw new WebSocketServerException(
                        "@OnClose callback must return Unit: " + callback.asString());
            }
        } else {
            if (callback.returnType().kind() != Kind.VOID && !WebSocketProcessor.isUniVoid(callback.returnType())) {
                throw new WebSocketServerException(
                        "@OnClose callback must return void or Uni<Void>: " + callback.asString());
            }
        }
    }

    /**
     * The generated endpoint class looks like:
     *
     * <pre>
     * public class Echo_WebSocketEndpoint extends WebSocketEndpointBase {
     *
     *     public WebSocket.ExecutionMode executionMode() {
     *         return WebSocket.ExecutionMode.SERIAL;
     *     }
     *
     *     public Echo_WebSocketEndpoint(WebSocketConnection connection, Codecs codecs,
     *             WebSocketRuntimeConfig config, ContextSupport contextSupport, SecuritySupport securitySupport) {
     *         super(connection, codecs, config, contextSupport, securitySupport);
     *     }
     *
     *     public Uni doOnTextMessage(String message) {
     *         Uni uni = ((Echo) super.beanInstance().echo((String) message);
     *         if (uni != null) {
     *             // The lambda is implemented as a generated function: Echo_WebSocketEndpoint$$function$$1
     *             return uni.chain(m -> sendText(m, false));
     *         } else {
     *             return Uni.createFrom().voidItem();
     *         }
     *     }
     *
     *     public Uni doOnTextMessage(Object message) {
     *         Object bean = super.beanInstance();
     *         try {
     *             String ret = ((EchoEndpoint) bean).echo((String) message);
     *             return ret != null ? super.sendText(ret, false) : Uni.createFrom().voidItem();
     *         } catch (Throwable t) {
     *             return ((WebSocketEndpointBase) this).doOnError(t);
     *         }
     *     }
     *
     *     public Uni doOnError(Throwable t) {
     *         if (!(t instanceof IllegalStateException)) {
     *             return Uni.createFrom().failure(t);
     *         } else {
     *             1 fun = new 1(this);
     *             ExecutionModel em = ExecutionModel.EVENT_LOOP;
     *             return doErrorExecute(t, em, (Function)fun);
     *         }
     *     }
     *
     *     public WebSocketEndpoint.ExecutionModel onTextMessageExecutionModel() {
     *         return ExecutionModel.EVENT_LOOP;
     *     }
     *
     *     public String beanIdentifier() {
     *        return "egBJQ7_QAFkQlYXSTKE0XlN3wow";
     *     }
     * }
     * </pre>
     *
     * @param endpoint
     * @param classOutput
     * @return the name of the generated class
     */
    static String generateEndpoint(WebSocketEndpointBuildItem endpoint,
            CallbackArgumentsBuildItem argumentProviders,
            TransformedAnnotationsBuildItem transformedAnnotations,
            IndexView index,
            ClassOutput classOutput,
            GlobalErrorHandlersBuildItem globalErrorHandlers,
            String endpointSuffix,
            InvokerFactoryBuildItem invokerFactory) {
        ClassInfo implClazz = endpoint.bean.getImplClazz();
        String baseName;
        if (implClazz.enclosingClass() != null) {
            baseName = DotNames.simpleName(implClazz.enclosingClass()) + NESTED_SEPARATOR
                    + DotNames.simpleName(implClazz);
        } else {
            baseName = DotNames.simpleName(implClazz.name());
        }
        String generatedName = DotNames.internalPackageNameWithTrailingSlash(implClazz.name()) + baseName
                + endpointSuffix;

        ClassCreator endpointCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(WebSocketEndpointBase.class)
                .build();

        MethodCreator constructor = endpointCreator.getConstructorCreator(WebSocketConnectionBase.class,
                Codecs.class, ContextSupport.class, SecuritySupport.class);
        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(WebSocketEndpointBase.class, WebSocketConnectionBase.class,
                        Codecs.class, ContextSupport.class, SecuritySupport.class),
                constructor.getThis(), constructor.getMethodParam(0), constructor.getMethodParam(1),
                constructor.getMethodParam(2), constructor.getMethodParam(3));

        MethodCreator inboundProcessingMode = endpointCreator.getMethodCreator("inboundProcessingMode",
                InboundProcessingMode.class);
        inboundProcessingMode.returnValue(inboundProcessingMode.load(endpoint.inboundProcessingMode));

        MethodCreator beanIdentifier = endpointCreator.getMethodCreator("beanIdentifier", String.class);
        beanIdentifier.returnValue(beanIdentifier.load(endpoint.bean.getIdentifier()));

        if (endpoint.onOpen != null) {
            Callback callback = endpoint.onOpen;
            MethodCreator doOnOpen = endpointCreator.getMethodCreator("doOnOpen", Uni.class, Object.class);
            // Foo foo = beanInstance();
            ResultHandle beanInstance = doOnOpen.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class), doOnOpen.getThis());
            // Call the business method
            TryBlock tryBlock = onErrorTryBlock(doOnOpen, doOnOpen.getThis());
            ResultHandle[] args = callback.generateArguments(tryBlock.getThis(), tryBlock, transformedAnnotations, index);
            ResultHandle ret = callBusinessMethod(endpointCreator, constructor, callback, "Open", tryBlock,
                    beanInstance, args, invokerFactory);
            encodeAndReturnResult(tryBlock.getThis(), tryBlock, callback, globalErrorHandlers, endpoint, ret);

            MethodCreator onOpenExecutionModel = endpointCreator.getMethodCreator("onOpenExecutionModel",
                    ExecutionModel.class);
            onOpenExecutionModel.returnValue(onOpenExecutionModel.load(callback.executionModel));
        }

        generateOnMessage(endpointCreator, constructor, endpoint, endpoint.onBinaryMessage, argumentProviders,
                transformedAnnotations, index, globalErrorHandlers, invokerFactory);
        generateOnMessage(endpointCreator, constructor, endpoint, endpoint.onTextMessage, argumentProviders,
                transformedAnnotations, index, globalErrorHandlers, invokerFactory);
        generateOnMessage(endpointCreator, constructor, endpoint, endpoint.onPongMessage, argumentProviders,
                transformedAnnotations, index, globalErrorHandlers, invokerFactory);

        if (endpoint.onClose != null) {
            Callback callback = endpoint.onClose;
            MethodCreator doOnClose = endpointCreator.getMethodCreator("doOnClose", Uni.class, Object.class);
            // Foo foo = beanInstance("foo");
            ResultHandle beanInstance = doOnClose.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class), doOnClose.getThis());
            // Call the business method
            TryBlock tryBlock = onErrorTryBlock(doOnClose, doOnClose.getThis());
            ResultHandle[] args = callback.generateArguments(tryBlock.getThis(), tryBlock, transformedAnnotations, index);
            ResultHandle ret = callBusinessMethod(endpointCreator, constructor, callback, "Close", tryBlock,
                    beanInstance, args, invokerFactory);
            encodeAndReturnResult(tryBlock.getThis(), tryBlock, callback, globalErrorHandlers, endpoint, ret);

            MethodCreator onCloseExecutionModel = endpointCreator.getMethodCreator("onCloseExecutionModel",
                    ExecutionModel.class);
            onCloseExecutionModel.returnValue(onCloseExecutionModel.load(callback.executionModel));
        }

        generateOnError(endpointCreator, constructor, endpoint, transformedAnnotations, globalErrorHandlers, index,
                invokerFactory);

        // we write into the constructor when generating callback invokers, so need to finish it late
        constructor.returnVoid();

        endpointCreator.close();
        return generatedName.replace('/', '.');
    }

    private static void generateOnError(ClassCreator endpointCreator, MethodCreator constructor,
            WebSocketEndpointBuildItem endpoint, TransformedAnnotationsBuildItem transformedAnnotations,
            GlobalErrorHandlersBuildItem globalErrorHandlers, IndexView index, InvokerFactoryBuildItem invokerFactory) {

        Map<DotName, Callback> errors = new HashMap<>();
        List<ThrowableInfo> throwableInfos = new ArrayList<>();
        for (Callback callback : endpoint.onErrors) {
            DotName errorTypeName = callback.argumentType(ErrorCallbackArgument::isError).name();
            if (errors.containsKey(errorTypeName)) {
                throw new WebSocketException(String.format(
                        "Multiple @OnError callbacks may not accept the same error parameter: %s\n\t- %s\n\t- %s",
                        errorTypeName,
                        callback.asString(), errors.get(errorTypeName).asString()));
            }
            errors.put(errorTypeName, callback);
            throwableInfos.add(new ThrowableInfo(endpoint.bean, callback, throwableHierarchy(errorTypeName, index)));
        }
        for (GlobalErrorHandler globalErrorHandler : endpoint.isClient ? globalErrorHandlers.forClient()
                : globalErrorHandlers.forServer()) {
            Callback callback = globalErrorHandler.callback;
            DotName errorTypeName = callback.argumentType(ErrorCallbackArgument::isError).name();
            if (!errors.containsKey(errorTypeName)) {
                // Endpoint callbacks take precedence over global handlers
                throwableInfos
                        .add(new ThrowableInfo(globalErrorHandler.bean, callback, throwableHierarchy(errorTypeName, index)));
            }
        }

        if (throwableInfos.isEmpty()) {
            return;
        }

        MethodCreator doOnError = endpointCreator.getMethodCreator("doOnError", Uni.class, Throwable.class);
        // Most specific errors go first
        throwableInfos.sort(Comparator.comparingInt(ThrowableInfo::level).reversed());
        ResultHandle endpointThis = doOnError.getThis();

        for (ThrowableInfo throwableInfo : throwableInfos) {
            BytecodeCreator throwableMatches = doOnError
                    .ifTrue(doOnError.instanceOf(doOnError.getMethodParam(0), throwableInfo.hierarchy.get(0).toString()))
                    .trueBranch();
            Callback callback = throwableInfo.callback;

            FunctionCreator fun = throwableMatches.createFunction(Function.class);
            BytecodeCreator funBytecode = fun.getBytecode();

            // Call the business method
            TryBlock tryBlock = uniFailureTryBlock(funBytecode);
            ResultHandle beanInstance = tryBlock.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class, String.class),
                    endpointThis, funBytecode.load(throwableInfo.bean().getIdentifier()));
            ResultHandle[] args = callback.generateArguments(endpointThis, tryBlock, transformedAnnotations, index);
            ResultHandle ret = callBusinessMethod(endpointCreator, constructor, callback, "Error", tryBlock,
                    beanInstance, args, invokerFactory);
            encodeAndReturnResult(endpointThis, tryBlock, callback, globalErrorHandlers, endpoint, ret);

            // return doErrorExecute()
            throwableMatches.returnValue(
                    throwableMatches.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "doErrorExecute", Uni.class, Throwable.class,
                                    WebSocketEndpoint.ExecutionModel.class, Function.class),
                            throwableMatches.getThis(), throwableMatches.getMethodParam(0),
                            throwableMatches.load(callback.executionModel), fun.getInstance()));
        }

        ResultHandle uniCreate = doOnError
                .invokeStaticInterfaceMethod(MethodDescriptor.ofMethod(Uni.class, "createFrom", UniCreate.class));
        doOnError.returnValue(doOnError.invokeVirtualMethod(
                MethodDescriptor.ofMethod(UniCreate.class, "failure", Uni.class, Throwable.class), uniCreate,
                doOnError.getMethodParam(0)));
    }

    private static List<DotName> throwableHierarchy(DotName throwableName, IndexView index) {
        // TextDecodeException -> [TextDecodeException, WebSocketServerException, RuntimeException, Exception, Throwable]
        List<DotName> ret = new ArrayList<>();
        addToThrowableHierarchy(throwableName, index, ret);
        return ret;
    }

    private static void addToThrowableHierarchy(DotName throwableName, IndexView index, List<DotName> hierarchy) {
        hierarchy.add(throwableName);
        ClassInfo errorClass = index.getClassByName(throwableName);
        if (errorClass == null) {
            throw new IllegalArgumentException("The class " + throwableName + " not found in the index");
        }
        if (errorClass.superName().equals(DotName.OBJECT_NAME)) {
            return;
        }
        addToThrowableHierarchy(errorClass.superName(), index, hierarchy);
    }

    record ThrowableInfo(BeanInfo bean, Callback callback, List<DotName> hierarchy) {

        public int level() {
            return hierarchy.size();
        }

    }

    record GlobalErrorHandler(BeanInfo bean, Callback callback) {

    }

    private static void generateOnMessage(ClassCreator endpointCreator, MethodCreator constructor,
            WebSocketEndpointBuildItem endpoint, Callback callback,
            CallbackArgumentsBuildItem callbackArguments, TransformedAnnotationsBuildItem transformedAnnotations,
            IndexView index, GlobalErrorHandlersBuildItem globalErrorHandlers, InvokerFactoryBuildItem invokerFactory) {
        if (callback == null) {
            return;
        }
        String messageType;
        Class<?> methodParameterType;
        switch (callback.messageType()) {
            case BINARY:
                messageType = "Binary";
                methodParameterType = Object.class;
                break;
            case TEXT:
                messageType = "Text";
                methodParameterType = Object.class;
                break;
            case PONG:
                messageType = "Pong";
                methodParameterType = Buffer.class;
                break;
            default:
                throw new IllegalArgumentException();
        }
        MethodCreator doOnMessage = endpointCreator.getMethodCreator("doOn" + messageType + "Message", Uni.class,
                methodParameterType);

        TryBlock tryBlock = onErrorTryBlock(doOnMessage, doOnMessage.getThis());
        // Foo foo = beanInstance();
        ResultHandle beanInstance = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class), tryBlock.getThis());
        ResultHandle[] args = callback.generateArguments(tryBlock.getThis(), tryBlock, transformedAnnotations, index);
        // Call the business method
        ResultHandle ret = callBusinessMethod(endpointCreator, constructor, callback, messageType, tryBlock, beanInstance, args,
                invokerFactory);
        encodeAndReturnResult(tryBlock.getThis(), tryBlock, callback, globalErrorHandlers, endpoint, ret);

        MethodCreator onMessageExecutionModel = endpointCreator.getMethodCreator("on" + messageType + "MessageExecutionModel",
                ExecutionModel.class);
        onMessageExecutionModel.returnValue(onMessageExecutionModel.load(callback.executionModel));

        if (callback.acceptsMulti() && callback.messageType != MessageType.PONG) {
            Type multiItemType = callback.messageParamType().asParameterizedType().arguments().get(0);
            MethodCreator consumedMultiType = endpointCreator.getMethodCreator("consumed" + messageType + "MultiType",
                    java.lang.reflect.Type.class);
            consumedMultiType.returnValue(Types.getTypeHandle(consumedMultiType, multiItemType));

            MethodCreator decodeMultiItem = endpointCreator.getMethodCreator("decode" + messageType + "MultiItem",
                    Object.class, Object.class);
            decodeMultiItem
                    .returnValue(decodeMessage(decodeMultiItem.getThis(), decodeMultiItem, callback.acceptsBinaryMessage(),
                            multiItemType, decodeMultiItem.getMethodParam(0), callback));
        }
    }

    private static ResultHandle callBusinessMethod(ClassCreator clazz, MethodCreator constructor, Callback callback,
            String messageType, BytecodeCreator bytecode, ResultHandle beanInstance, ResultHandle[] args,
            InvokerFactoryBuildItem invokerFactory) {
        // using CDI method invokers for Kotlin `suspend` functions to minimize custom bytecode generation
        // other methods are invoked directly
        if (KotlinUtils.isKotlinSuspendMethod(callback.method)) {
            InvokerInfo invoker = invokerFactory.createInvoker(callback.bean, callback.method)
                    .withInvocationWrapper(CoroutineInvoker.class, "inNewCoroutine")
                    .build();
            // create a field in the endpoint class and put the created invoker into it in the `constructor`
            FieldCreator invokerField = clazz.getFieldCreator("invokerFor" + messageType, Invoker.class)
                    .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
            constructor.writeInstanceField(invokerField.getFieldDescriptor(), constructor.getThis(),
                    constructor.newInstance(MethodDescriptor.ofConstructor(invoker.getClassName())));
            // use the invoker to invoke the business method in the endpoint method (`bytecode`)
            ResultHandle invokerHandle = bytecode.readInstanceField(invokerField.getFieldDescriptor(), bytecode.getThis());
            ResultHandle argsArray = bytecode.newArray(Object.class, args.length);
            for (int i = 0; i < args.length; i++) {
                bytecode.writeArrayValue(argsArray, i, args[i]);
            }
            return bytecode.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Invoker.class, "invoke", Object.class, Object.class, Object[].class),
                    invokerHandle, beanInstance, argsArray);
        } else {
            return bytecode.invokeVirtualMethod(MethodDescriptor.of(callback.method), beanInstance, args);
        }
    }

    private static TryBlock uniFailureTryBlock(BytecodeCreator method) {
        TryBlock tryBlock = method.tryBlock();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        // return Uni.createFrom().failure(t);
        ResultHandle uniCreate = catchBlock
                .invokeStaticInterfaceMethod(MethodDescriptor.ofMethod(Uni.class, "createFrom", UniCreate.class));
        catchBlock.returnValue(catchBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(UniCreate.class, "failure", Uni.class, Throwable.class), uniCreate,
                catchBlock.getCaughtException()));
        return tryBlock;
    }

    private static TryBlock onErrorTryBlock(BytecodeCreator method, ResultHandle endpointThis) {
        TryBlock tryBlock = method.tryBlock();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        // return doOnError(t);
        catchBlock.returnValue(catchBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "doOnError", Uni.class, Throwable.class),
                endpointThis, catchBlock.getCaughtException()));
        return tryBlock;
    }

    static ResultHandle decodeMessage(
            // WebSocketEndpointBase reference
            ResultHandle endpointThis,
            BytecodeCreator method, boolean binaryMessage, Type valueType, ResultHandle value, Callback callback) {
        if (WebSocketDotNames.MULTI.equals(valueType.name())) {
            // Multi is decoded at runtime in the recorder
            return value;
        } else if (binaryMessage) {
            // Binary message
            if (WebSocketDotNames.BUFFER.equals(valueType.name())) {
                return value;
            } else if (WebSocketProcessor.isByteArray(valueType)) {
                // byte[] message = buffer.getBytes();
                return method.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "getBytes", byte[].class), value);
            } else if (WebSocketDotNames.STRING.equals(valueType.name())) {
                // String message = buffer.toString();
                return method.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "toString", String.class), value);
            } else if (WebSocketDotNames.JSON_OBJECT.equals(valueType.name())) {
                // JsonObject message = new JsonObject(buffer);
                return method.newInstance(
                        MethodDescriptor.ofConstructor(JsonObject.class, Buffer.class), value);
            } else if (WebSocketDotNames.JSON_ARRAY.equals(valueType.name())) {
                // JsonArray message = new JsonArray(buffer);
                return method.newInstance(
                        MethodDescriptor.ofConstructor(JsonArray.class, Buffer.class), value);
            } else {
                // Try to use codecs
                DotName inputCodec = callback.getInputCodec();
                ResultHandle type = Types.getTypeHandle(method, valueType);
                ResultHandle decoded = method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "decodeBinary", Object.class, java.lang.reflect.Type.class, Buffer.class, Class.class),
                        endpointThis, type,
                        value, inputCodec != null ? method.loadClass(inputCodec.toString()) : method.loadNull());
                return decoded;
            }
        } else {
            // Text message
            if (WebSocketDotNames.STRING.equals(valueType.name())) {
                // String message = string;
                return value;
            } else if (WebSocketDotNames.JSON_OBJECT.equals(valueType.name())) {
                // JsonObject message = new JsonObject(string);
                return method.newInstance(
                        MethodDescriptor.ofConstructor(JsonObject.class, String.class), value);
            } else if (WebSocketDotNames.JSON_ARRAY.equals(valueType.name())) {
                // JsonArray message = new JsonArray(string);
                return method.newInstance(
                        MethodDescriptor.ofConstructor(JsonArray.class, String.class), value);
            } else if (WebSocketDotNames.BUFFER.equals(valueType.name())) {
                // Buffer message = Buffer.buffer(string);
                return method.invokeStaticInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, String.class), value);
            } else if (WebSocketProcessor.isByteArray(valueType)) {
                // byte[] message = Buffer.buffer(string).getBytes();
                ResultHandle buffer = method.invokeStaticInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, byte[].class), value);
                return method.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "getBytes", byte[].class), buffer);
            } else {
                // Try to use codecs
                DotName inputCodec = callback.getInputCodec();
                ResultHandle type = Types.getTypeHandle(method, valueType);
                ResultHandle decoded = method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "decodeText", Object.class, java.lang.reflect.Type.class, String.class, Class.class), endpointThis,
                        type, value, inputCodec != null ? method.loadClass(inputCodec.toString()) : method.loadNull());
                return decoded;
            }
        }
    }

    private static ResultHandle uniOnFailureDoOnError(ResultHandle endpointThis, BytecodeCreator method, Callback callback,
            ResultHandle uni, WebSocketEndpointBuildItem endpoint, GlobalErrorHandlersBuildItem globalErrorHandlers) {
        if (callback.isOnError()
                || (globalErrorHandlers.handlers.isEmpty() && (endpoint == null || endpoint.onErrors.isEmpty()))) {
            // @OnError or no error handlers available
            return uni;
        }
        // return uniMessage.onFailure().recoverWithUni(t -> {
        //    return doOnError(t);
        // });
        FunctionCreator fun = method.createFunction(Function.class);
        BytecodeCreator funBytecode = fun.getBytecode();
        funBytecode.returnValue(funBytecode.invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "doOnError", Uni.class, Throwable.class),
                endpointThis, funBytecode.getMethodParam(0)));
        ResultHandle uniOnFailure = method.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Uni.class, "onFailure", UniOnFailure.class), uni);
        return method.invokeVirtualMethod(
                MethodDescriptor.ofMethod(UniOnFailure.class, "recoverWithUni", Uni.class, Function.class),
                uniOnFailure, fun.getInstance());
    }

    private static ResultHandle encodeMessage(ResultHandle endpointThis, BytecodeCreator method, Callback callback,
            GlobalErrorHandlersBuildItem globalErrorHandlers, WebSocketEndpointBuildItem endpoint,
            ResultHandle value) {
        if (callback.acceptsBinaryMessage()
                || isOnOpenWithBinaryReturnType(callback)) {
            // ----------------------
            // === Binary message ===
            // ----------------------
            if (callback.isReturnTypeUni() || callback.isKotlinSuspendFunction()) {
                Type messageType = callback.isReturnTypeUni()
                        ? callback.returnType().asParameterizedType().arguments().get(0)
                        : KotlinUtils.getKotlinSuspendMethodResult(callback.method);
                if (messageType.name().equals(KotlinDotNames.UNIT)) {
                    value = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(Uni.class, "replaceWithVoid", Uni.class),
                            value);
                    messageType = ClassType.create(WebSocketDotNames.VOID);
                }
                if (messageType.name().equals(WebSocketDotNames.VOID)) {
                    // Uni<Void>
                    return uniOnFailureDoOnError(endpointThis, method, callback, value, endpoint, globalErrorHandlers);
                } else {
                    // return uniMessage.chain(m -> {
                    //    Buffer buffer = encodeBuffer(m);
                    //    return sendBinary(buffer,broadcast);
                    // });
                    FunctionCreator fun = method.createFunction(Function.class);
                    BytecodeCreator funBytecode = fun.getBytecode();
                    ResultHandle buffer = encodeBuffer(funBytecode, messageType,
                            funBytecode.getMethodParam(0), endpointThis, callback);
                    funBytecode.returnValue(funBytecode.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                    "sendBinary", Uni.class, Buffer.class, boolean.class),
                            endpointThis, buffer,
                            funBytecode.load(callback.broadcast())));
                    ResultHandle uniChain = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Uni.class, "chain", Uni.class, Function.class), value,
                            fun.getInstance());
                    return uniOnFailureDoOnError(endpointThis, method, callback, uniChain, endpoint, globalErrorHandlers);
                }
            } else if (callback.isReturnTypeMulti()) {
                //    try {
                //      Buffer buffer = encodeBuffer(m);
                //      return sendBinary(buffer,broadcast);
                //    } catch(Throwable t) {
                //      return doOnError(t);
                //    }
                FunctionCreator fun = method.createFunction(Function.class);
                BytecodeCreator funBytecode = fun.getBytecode();
                // This checkcast should not be necessary but we need to use the endpoint in the function bytecode
                // otherwise gizmo does not access the endpoint reference correcly
                ResultHandle endpointBase = funBytecode.checkCast(endpointThis, WebSocketEndpointBase.class);
                TryBlock tryBlock = onErrorTryBlock(fun.getBytecode(), endpointBase);
                ResultHandle buffer = encodeBuffer(tryBlock, callback.returnType().asParameterizedType().arguments().get(0),
                        tryBlock.getMethodParam(0), endpointThis, callback);
                tryBlock.returnValue(tryBlock.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                "sendBinary", Uni.class, Buffer.class, boolean.class),
                        endpointThis, buffer,
                        tryBlock.load(callback.broadcast())));
                return method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "multiBinary", Uni.class, Multi.class, Function.class), endpointThis,
                        value,
                        fun.getInstance());
            } else {
                // return sendBinary(encodeBuffer(b),broadcast);
                ResultHandle buffer = encodeBuffer(method, callback.returnType(), value, endpointThis, callback);
                return method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "sendBinary", Uni.class, Buffer.class, boolean.class), endpointThis, buffer,
                        method.load(callback.broadcast()));
            }
        } else {
            // ----------------------
            // === Text message ===
            // ----------------------
            if (callback.isReturnTypeUni() || callback.isKotlinSuspendFunction()) {
                Type messageType = callback.isReturnTypeUni()
                        ? callback.returnType().asParameterizedType().arguments().get(0)
                        : KotlinUtils.getKotlinSuspendMethodResult(callback.method);
                if (messageType.name().equals(KotlinDotNames.UNIT)) {
                    value = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(Uni.class, "replaceWithVoid", Uni.class),
                            value);
                    messageType = ClassType.create(WebSocketDotNames.VOID);
                }
                if (messageType.name().equals(WebSocketDotNames.VOID)) {
                    // Uni<Void>
                    return uniOnFailureDoOnError(endpointThis, method, callback, value, endpoint, globalErrorHandlers);
                } else {
                    // return uniMessage.chain(m -> {
                    //    String text = encodeText(m);
                    //    return sendText(string,broadcast);
                    // });
                    FunctionCreator fun = method.createFunction(Function.class);
                    BytecodeCreator funBytecode = fun.getBytecode();
                    ResultHandle text = encodeText(funBytecode, messageType,
                            funBytecode.getMethodParam(0), endpointThis, callback);
                    funBytecode.returnValue(funBytecode.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                    "sendText", Uni.class, String.class, boolean.class),
                            endpointThis, text,
                            funBytecode.load(callback.broadcast())));
                    ResultHandle uniChain = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Uni.class, "chain", Uni.class, Function.class), value,
                            fun.getInstance());
                    return uniOnFailureDoOnError(endpointThis, method, callback, uniChain, endpoint, globalErrorHandlers);
                }
            } else if (callback.isReturnTypeMulti()) {
                // return multiText(multi, m -> {
                //    try {
                //      String text = encodeText(m);
                //      return sendText(buffer,broadcast);
                //    } catch(Throwable t) {
                //      return doOnError(t);
                //    }
                //});
                FunctionCreator fun = method.createFunction(Function.class);
                BytecodeCreator funBytecode = fun.getBytecode();
                // This checkcast should not be necessary but we need to use the endpoint in the function bytecode
                // otherwise gizmo does not access the endpoint reference correcly
                ResultHandle endpointBase = funBytecode.checkCast(endpointThis, WebSocketEndpointBase.class);
                TryBlock tryBlock = onErrorTryBlock(fun.getBytecode(), endpointBase);
                ResultHandle text = encodeText(tryBlock, callback.returnType().asParameterizedType().arguments().get(0),
                        tryBlock.getMethodParam(0), endpointThis, callback);
                tryBlock.returnValue(tryBlock.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                "sendText", Uni.class, String.class, boolean.class),
                        endpointThis, text,
                        tryBlock.load(callback.broadcast())));
                return method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "multiText", Uni.class, Multi.class, Function.class), endpointThis,
                        value,
                        fun.getInstance());
            } else {
                // return sendText(text,broadcast);
                ResultHandle text = encodeText(method, callback.returnType(), value, endpointThis, callback);
                return method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "sendText", Uni.class, String.class, boolean.class), endpointThis, text,
                        method.load(callback.broadcast()));
            }
        }
    }

    private static ResultHandle encodeBuffer(BytecodeCreator method, Type messageType, ResultHandle value,
            ResultHandle endpointThis, Callback callback) {
        ResultHandle buffer;
        if (messageType.name().equals(WebSocketDotNames.BUFFER)) {
            buffer = value;
        } else if (WebSocketProcessor.isByteArray(messageType)) {
            buffer = method.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, byte[].class), value);
        } else if (messageType.name().equals(WebSocketDotNames.STRING)) {
            buffer = method.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, String.class), value);
        } else if (messageType.name().equals(WebSocketDotNames.JSON_OBJECT)) {
            buffer = method.invokeVirtualMethod(MethodDescriptor.ofMethod(JsonObject.class, "toBuffer", Buffer.class),
                    value);
        } else if (messageType.name().equals(WebSocketDotNames.JSON_ARRAY)) {
            buffer = method.invokeVirtualMethod(MethodDescriptor.ofMethod(JsonArray.class, "toBuffer", Buffer.class),
                    value);
        } else {
            // Try to use codecs
            DotName outputCodec = callback.getOutputCodec();
            buffer = method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                    "encodeBinary", Buffer.class, Object.class, Class.class), endpointThis, value,
                    outputCodec != null ? method.loadClass(outputCodec.toString()) : method.loadNull());
        }
        return buffer;
    }

    private static ResultHandle encodeText(BytecodeCreator method, Type messageType, ResultHandle value,
            ResultHandle endpointThis, Callback callback) {
        ResultHandle text;
        if (messageType.name().equals(WebSocketDotNames.BUFFER)) {
            text = method.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "toString", String.class), value);
        } else if (WebSocketProcessor.isByteArray(messageType)) {
            ResultHandle buffer = method.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, byte[].class), value);
            text = method.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "toString", String.class), buffer);
        } else if (messageType.name().equals(WebSocketDotNames.STRING)) {
            text = value;
        } else if (messageType.name().equals(WebSocketDotNames.JSON_OBJECT)) {
            text = method.invokeVirtualMethod(MethodDescriptor.ofMethod(JsonObject.class, "encode", String.class),
                    value);
        } else if (messageType.name().equals(WebSocketDotNames.JSON_ARRAY)) {
            text = method.invokeVirtualMethod(MethodDescriptor.ofMethod(JsonArray.class, "encode", String.class),
                    value);
        } else {
            // Try to use codecs
            DotName outputCodec = callback.getOutputCodec();
            text = method.invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                    "encodeText", String.class, Object.class, Class.class), endpointThis, value,
                    outputCodec != null ? method.loadClass(outputCodec.toString()) : method.loadNull());
        }
        return text;
    }

    private static ResultHandle uniVoid(BytecodeCreator method) {
        ResultHandle uniCreate = method
                .invokeStaticInterfaceMethod(MethodDescriptor.ofMethod(Uni.class, "createFrom", UniCreate.class));
        return method.invokeVirtualMethod(MethodDescriptor.ofMethod(UniCreate.class, "voidItem", Uni.class), uniCreate);
    }

    private static void encodeAndReturnResult(ResultHandle endpointThis, BytecodeCreator method, Callback callback,
            GlobalErrorHandlersBuildItem globalErrorHandlers, WebSocketEndpointBuildItem endpoint,
            ResultHandle result) {
        // The result must be always Uni<Void>
        if (callback.isReturnTypeVoid()) {
            // return Uni.createFrom().void()
            method.returnValue(uniVoid(method));
        } else {
            // Skip response
            BytecodeCreator isNull = method.ifNull(result).trueBranch();
            isNull.returnValue(uniVoid(isNull));
            method.returnValue(encodeMessage(endpointThis, method, callback, globalErrorHandlers, endpoint, result));
        }
    }

    static List<Callback> findErrorHandlers(Target target, IndexView index, BeanInfo bean, ClassInfo beanClass,
            CallbackArgumentsBuildItem callbackArguments, TransformedAnnotationsBuildItem transformedAnnotations,
            String endpointPath) {
        List<AnnotationInstance> annotations = findCallbackAnnotations(index, beanClass, WebSocketDotNames.ON_ERROR);
        if (annotations.isEmpty()) {
            return List.of();
        }
        List<Callback> errorHandlers = new ArrayList<>();
        for (AnnotationInstance annotation : annotations) {
            MethodInfo method = annotation.target().asMethod();
            // If a global error handler accepts a connection param we change the target appropriately
            if (method.parameterTypes().stream().map(Type::name).anyMatch(WebSocketDotNames.WEB_SOCKET_CONNECTION::equals)) {
                target = Target.SERVER;
            } else if (method.parameterTypes().stream().map(Type::name)
                    .anyMatch(WebSocketDotNames.WEB_SOCKET_CLIENT_CONNECTION::equals)) {
                target = Target.CLIENT;
            }
            Callback callback = new Callback(target, annotation, bean, method,
                    executionModel(method, transformedAnnotations), callbackArguments, transformedAnnotations,
                    endpointPath, index);
            long errorArguments = callback.arguments.stream().filter(ca -> ca instanceof ErrorCallbackArgument).count();
            if (errorArguments != 1) {
                throw new WebSocketException(
                        String.format("@OnError callback must accept exactly one error parameter; found %s: %s",
                                errorArguments,
                                callback.asString()));
            }
            errorHandlers.add(callback);
        }
        return errorHandlers;
    }

    private static List<AnnotationInstance> findCallbackAnnotations(IndexView index, ClassInfo beanClass,
            DotName annotationName) {
        ClassInfo aClass = beanClass;
        List<AnnotationInstance> annotations = new ArrayList<>();
        while (aClass != null) {
            List<AnnotationInstance> declared = aClass.annotationsMap().get(annotationName);
            if (declared != null) {
                annotations.addAll(declared);
            }
            DotName superName = aClass.superName();
            aClass = superName != null && !superName.equals(DotNames.OBJECT)
                    ? index.getClassByName(superName)
                    : null;
        }
        return annotations;
    }

    static Callback findCallback(Target target, IndexView index, BeanInfo bean, ClassInfo beanClass,
            DotName annotationName, CallbackArgumentsBuildItem callbackArguments,
            TransformedAnnotationsBuildItem transformedAnnotations, String endpointPath) {
        return findCallback(target, index, bean, beanClass, annotationName, callbackArguments,
                transformedAnnotations, endpointPath, null);
    }

    private static Callback findCallback(Target target, IndexView index, BeanInfo bean, ClassInfo beanClass,
            DotName annotationName, CallbackArgumentsBuildItem callbackArguments,
            TransformedAnnotationsBuildItem transformedAnnotations, String endpointPath,
            Consumer<Callback> validator) {
        List<AnnotationInstance> annotations = findCallbackAnnotations(index, beanClass, annotationName);
        if (annotations.isEmpty()) {
            return null;
        } else if (annotations.size() == 1) {
            AnnotationInstance annotation = annotations.get(0);
            MethodInfo method = annotation.target().asMethod();
            Callback callback = new Callback(target, annotation, bean, method,
                    executionModel(method, transformedAnnotations), callbackArguments, transformedAnnotations,
                    endpointPath, index);
            long messageArguments = callback.arguments.stream().filter(ca -> ca instanceof MessageCallbackArgument).count();
            if (callback.acceptsMessage()) {
                if (messageArguments > 1) {
                    throw new WebSocketException(
                            String.format("@%s callback may accept at most 1 message parameter; found %s: %s",
                                    DotNames.simpleName(callback.annotation.name()),
                                    messageArguments,
                                    callback.asString()));
                }
            } else {
                if (messageArguments != 0) {
                    throw new WebSocketException(
                            String.format("@%s callback must not accept a message parameter; found %s: %s",
                                    DotNames.simpleName(callback.annotation.name()),
                                    messageArguments,
                                    callback.asString()));
                }
            }
            if (target == Target.CLIENT && callback.broadcast()) {
                throw new WebSocketClientException(
                        String.format("@%s callback declared on a client endpoint must not broadcast messages: %s",
                                DotNames.simpleName(callback.annotation.name()),
                                callback.asString()));
            }
            if (validator != null) {
                validator.accept(callback);
            }
            return callback;
        }
        throw new WebSocketException(
                String.format("There can be only one callback annotated with %s declared on %s", annotationName, beanClass));
    }

    private static ExecutionModel executionModel(MethodInfo method, TransformedAnnotationsBuildItem transformedAnnotations) {
        if (KotlinUtils.isKotlinSuspendMethod(method)
                && (transformedAnnotations.hasAnnotation(method, WebSocketDotNames.RUN_ON_VIRTUAL_THREAD)
                        || transformedAnnotations.hasAnnotation(method, WebSocketDotNames.BLOCKING)
                        || transformedAnnotations.hasAnnotation(method, WebSocketDotNames.NON_BLOCKING))) {
            throw new WebSocketException("Kotlin `suspend` functions in WebSockets Next endpoints may not be "
                    + "annotated @Blocking, @NonBlocking or @RunOnVirtualThread: " + method);
        }

        if (transformedAnnotations.hasAnnotation(method, WebSocketDotNames.RUN_ON_VIRTUAL_THREAD)) {
            return ExecutionModel.VIRTUAL_THREAD;
        } else if (transformedAnnotations.hasAnnotation(method, WebSocketDotNames.BLOCKING)) {
            return ExecutionModel.WORKER_THREAD;
        } else if (transformedAnnotations.hasAnnotation(method, WebSocketDotNames.NON_BLOCKING)) {
            return ExecutionModel.EVENT_LOOP;
        } else {
            return hasBlockingSignature(method) ? ExecutionModel.WORKER_THREAD : ExecutionModel.EVENT_LOOP;
        }
    }

    static boolean hasBlockingSignature(MethodInfo method) {
        if (KotlinUtils.isKotlinSuspendMethod(method)) {
            return false;
        }

        switch (method.returnType().kind()) {
            case VOID:
            case CLASS:
                return true;
            case PARAMETERIZED_TYPE:
                // Uni, Multi -> non-blocking
                DotName name = method.returnType().asParameterizedType().name();
                return !name.equals(WebSocketDotNames.UNI) && !name.equals(WebSocketDotNames.MULTI);
            default:
                throw new WebSocketServerException(
                        "Unsupported return type:" + methodToString(method));
        }
    }

    static boolean isUniVoid(Type type) {
        return WebSocketDotNames.UNI.equals(type.name())
                && type.asParameterizedType().arguments().get(0).name().equals(WebSocketDotNames.VOID);
    }

    static boolean isByteArray(Type type) {
        return type.kind() == Kind.ARRAY && PrimitiveType.BYTE.equals(type.asArrayType().constituent());
    }

    static String methodToString(MethodInfo method) {
        return method.declaringClass().name() + "#" + method.name() + "()";
    }

    private static boolean isOnOpenWithBinaryReturnType(Callback callback) {
        if (callback.isOnOpen()) {
            Type returnType = callback.returnType();
            if (callback.isReturnTypeUni() || callback.isReturnTypeMulti()) {
                returnType = callback.returnType().asParameterizedType().arguments().get(0);
            } else if (callback.isKotlinSuspendFunction()) {
                returnType = KotlinUtils.getKotlinSuspendMethodResult(callback.method);
            }
            return WebSocketDotNames.BUFFER.equals(returnType.name())
                    || (returnType.kind() == Kind.ARRAY && PrimitiveType.BYTE.equals(returnType.asArrayType().constituent()));
        }
        return false;
    }
}
