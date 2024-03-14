package io.quarkus.websockets.next.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.SessionScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Types;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.websockets.next.TextMessageCodec;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.WebSocketsRuntimeConfig;
import io.quarkus.websockets.next.deployment.WebSocketEndpointBuildItem.Callback;
import io.quarkus.websockets.next.deployment.WebSocketEndpointBuildItem.Callback.MessageType;
import io.quarkus.websockets.next.runtime.Codecs;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.quarkus.websockets.next.runtime.ContextSupport;
import io.quarkus.websockets.next.runtime.JsonTextMessageCodec;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.ExecutionModel;
import io.quarkus.websockets.next.runtime.WebSocketEndpointBase;
import io.quarkus.websockets.next.runtime.WebSocketServerRecorder;
import io.quarkus.websockets.next.runtime.WebSocketSessionContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniCreate;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WebSocketServerProcessor {

    static final String ENDPOINT_SUFFIX = "_WebSocketEndpoint";
    static final String NESTED_SEPARATOR = "$_";

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+\\}");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("websockets-next");
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem beanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(WebSocketDotNames.WEB_SOCKET, DotNames.SINGLETON);
    }

    @BuildStep
    void unremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TextMessageCodec.class));
    }

    @BuildStep
    public void collectEndpoints(BeanArchiveIndexBuildItem beanArchiveIndex,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            BuildProducer<WebSocketEndpointBuildItem> endpoints) {

        IndexView index = beanArchiveIndex.getIndex();
        Map<String, DotName> pathToEndpoint = new HashMap<>();

        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            AnnotationInstance webSocketAnnotation = beanClass.annotation(WebSocketDotNames.WEB_SOCKET);
            if (webSocketAnnotation != null) {
                String path = getPath(webSocketAnnotation.value("path").asString());
                if (beanClass.nestingType() == NestingType.INNER) {
                    // Sub-websocket - merge the path from the enclosing classes
                    path = mergePath(getPathPrefix(index, beanClass.enclosingClass()), path);
                }
                DotName previous = pathToEndpoint.put(path, beanClass.name());
                if (previous != null) {
                    throw new WebSocketServerException(
                            String.format("Multiple endpoints [%s, %s] define the same path: %s", previous, beanClass, path));
                }
                Callback onOpen = findCallback(beanArchiveIndex.getIndex(), beanClass, WebSocketDotNames.ON_OPEN,
                        this::validateOnOpen);
                Callback onTextMessage = findCallback(beanArchiveIndex.getIndex(), beanClass, WebSocketDotNames.ON_TEXT_MESSAGE,
                        this::validateOnTextMessage);
                Callback onBinaryMessage = findCallback(beanArchiveIndex.getIndex(), beanClass,
                        WebSocketDotNames.ON_BINARY_MESSAGE,
                        this::validateOnBinaryMessage);
                Callback onPongMessage = findCallback(beanArchiveIndex.getIndex(), beanClass, WebSocketDotNames.ON_PONG_MESSAGE,
                        this::validateOnPongMessage);
                Callback onClose = findCallback(beanArchiveIndex.getIndex(), beanClass, WebSocketDotNames.ON_CLOSE,
                        this::validateOnClose);
                if (onOpen == null && onTextMessage == null && onBinaryMessage == null && onPongMessage == null) {
                    throw new WebSocketServerException(
                            "The endpoint must declare at least one method annotated with @OnTextMessage, @OnBinaryMessage, @OnPongMessage or @OnOpen: "
                                    + beanClass);
                }
                AnnotationValue executionMode = webSocketAnnotation.value("executionMode");
                endpoints.produce(new WebSocketEndpointBuildItem(bean, path,
                        executionMode != null ? WebSocket.ExecutionMode.valueOf(executionMode.asEnum())
                                : WebSocket.ExecutionMode.SERIAL,
                        onOpen,
                        onTextMessage,
                        onBinaryMessage,
                        onPongMessage,
                        onClose));
            }
        }
    }

    @BuildStep
    public void generateEndpoints(List<WebSocketEndpointBuildItem> endpoints,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new Function<String, String>() {
            @Override
            public String apply(String name) {
                int idx = name.indexOf(ENDPOINT_SUFFIX);
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
            // For each WebSocket endpoint bean generate an implementation of WebSocketEndpoint
            // A new instance of this generated endpoint is created for each client connection
            // The generated endpoint ensures the correct execution model is used
            // and delegates callback invocations to the endpoint bean
            String generatedName = generateEndpoint(endpoint, classOutput);
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(generatedName).constructors().build());
            generatedEndpoints.produce(new GeneratedEndpointBuildItem(generatedName, endpoint.path));
        }
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    public void registerRoutes(WebSocketServerRecorder recorder, HttpRootPathBuildItem httpRootPath,
            List<GeneratedEndpointBuildItem> generatedEndpoints,
            BuildProducer<RouteBuildItem> routes) {

        for (GeneratedEndpointBuildItem endpoint : generatedEndpoints) {
            RouteBuildItem.Builder builder = RouteBuildItem.builder()
                    .route(httpRootPath.relativePath(endpoint.path))
                    .handlerType(HandlerType.NORMAL)
                    .handler(recorder.createEndpointHandler(endpoint.className));
            routes.produce(builder.build());
        }
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(Codecs.class, JsonTextMessageCodec.class, ConnectionManager.class).build();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void syntheticBeans(WebSocketServerRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(WebSocketConnection.class)
                .scope(SessionScoped.class)
                .setRuntimeInit()
                .supplier(recorder.connectionSupplier())
                .unremovable()
                .done());
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
            m.appendReplacement(sb, ":" + match.subSequence(1, match.length() - 1));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String callbackToString(MethodInfo callback) {
        return callback.declaringClass().name() + "#" + callback.name() + "()";
    }

    private String getPathPrefix(IndexView index, DotName enclosingClassName) {
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

    private void validateOnOpen(MethodInfo callback) {
        if (!callback.parameters().isEmpty()) {
            throw new WebSocketServerException(
                    "@OnOpen callback must not accept any parameters: " + callbackToString(callback));
        }
    }

    private void validateOnTextMessage(MethodInfo callback) {
        if (callback.parameters().size() != 1) {
            throw new WebSocketServerException(
                    "@OnTextMessage callback must accept exactly one parameter: " + callbackToString(callback));
        }
    }

    private void validateOnBinaryMessage(MethodInfo callback) {
        if (callback.parameters().size() != 1) {
            throw new WebSocketServerException(
                    "@OnTextMessage callback must accept exactly one parameter: " + callbackToString(callback));
        }
    }

    private void validateOnPongMessage(MethodInfo callback) {
        if (callback.returnType().kind() != Kind.VOID && !WebSocketServerProcessor.isUniVoid(callback.returnType())) {
            throw new WebSocketServerException(
                    "@OnPongMessage callback must return void or Uni<Void>: " + callbackToString(callback));
        }
        if (callback.parameters().size() != 1 || !callback.parameterType(0).name().equals(WebSocketDotNames.BUFFER)) {
            throw new WebSocketServerException(
                    "@OnPongMessage callback must accept exactly one parameter of type io.vertx.core.buffer.Buffer: "
                            + callbackToString(callback));
        }
    }

    private void validateOnClose(MethodInfo callback) {
        if (callback.returnType().kind() != Kind.VOID && !WebSocketServerProcessor.isUniVoid(callback.returnType())) {
            throw new WebSocketServerException(
                    "@OnClose callback must return void or Uni<Void>: " + callbackToString(callback));
        }
        if (!callback.parameters().isEmpty()) {
            throw new WebSocketServerException(
                    "@OnClose callback must not accept any parameters: " + callbackToString(callback));
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
     *             WebSocketRuntimeConfig config, ContextSupport contextSupport) {
     *         super(context, connection, codecs, config, contextActivator);
     *     }
     *
     *     public Uni doOnTextMessage(String message) {
     *         Uni uni = ((Echo) super.beanInstance("MTd91f3oxHtG8gnznR7XcZBCLdE")).echo((String) message);
     *         if (uni != null) {
     *             // The lambda is implemented as a generated function: Echo_WebSocketEndpoint$$function$$1
     *             return uni.chain(m -> sendText(m, false));
     *         } else {
     *             return Uni.createFrom().voidItem();
     *         }
     *     }
     *
     *     public WebSocketEndpoint.ExecutionModel onTextMessageExecutionModel() {
     *         return ExecutionModel.EVENT_LOOP;
     *     }
     * }
     * </pre>
     *
     * @param endpoint
     * @param classOutput
     * @return the name of the generated class
     */
    private String generateEndpoint(WebSocketEndpointBuildItem endpoint, ClassOutput classOutput) {
        ClassInfo implClazz = endpoint.bean.getImplClazz();
        String baseName;
        if (implClazz.enclosingClass() != null) {
            baseName = DotNames.simpleName(implClazz.enclosingClass()) + NESTED_SEPARATOR
                    + DotNames.simpleName(implClazz);
        } else {
            baseName = DotNames.simpleName(implClazz.name());
        }
        String generatedName = DotNames.internalPackageNameWithTrailingSlash(implClazz.name()) + baseName
                + ENDPOINT_SUFFIX;

        ClassCreator endpointCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(WebSocketEndpointBase.class)
                .build();

        MethodCreator constructor = endpointCreator.getConstructorCreator(WebSocketConnection.class,
                Codecs.class, WebSocketsRuntimeConfig.class, ContextSupport.class);
        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(WebSocketEndpointBase.class, WebSocketConnection.class,
                        Codecs.class, WebSocketsRuntimeConfig.class, ContextSupport.class),
                constructor.getThis(), constructor.getMethodParam(0), constructor.getMethodParam(1),
                constructor.getMethodParam(2), constructor.getMethodParam(3));
        constructor.returnNull();

        MethodCreator executionMode = endpointCreator.getMethodCreator("executionMode", WebSocket.ExecutionMode.class);
        executionMode.returnValue(executionMode.load(endpoint.executionMode));

        if (endpoint.onOpen != null) {
            MethodCreator doOnOpen = endpointCreator.getMethodCreator("doOnOpen", Uni.class, Object.class);
            // Foo foo = beanInstance("foo");
            ResultHandle beanInstance = doOnOpen.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class, String.class),
                    doOnOpen.getThis(), doOnOpen.load(endpoint.bean.getIdentifier()));
            // Call the business method
            TryBlock tryBlock = uniFailureTryBlock(doOnOpen);
            ResultHandle ret = tryBlock.invokeVirtualMethod(MethodDescriptor.of(endpoint.onOpen.method), beanInstance);
            encodeAndReturnResult(tryBlock, endpoint.onOpen, ret);

            MethodCreator onOpenExecutionModel = endpointCreator.getMethodCreator("onOpenExecutionModel",
                    ExecutionModel.class);
            onOpenExecutionModel.returnValue(onOpenExecutionModel.load(endpoint.onOpen.executionModel));
        }

        generateOnMessage(endpointCreator, endpoint, endpoint.onBinaryMessage);
        generateOnMessage(endpointCreator, endpoint, endpoint.onTextMessage);
        generateOnMessage(endpointCreator, endpoint, endpoint.onPongMessage);

        if (endpoint.onClose != null) {
            MethodCreator doOnClose = endpointCreator.getMethodCreator("doOnClose", Uni.class, Object.class);
            // Foo foo = beanInstance("foo");
            ResultHandle beanInstance = doOnClose.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class, String.class),
                    doOnClose.getThis(), doOnClose.load(endpoint.bean.getIdentifier()));
            // Call the business method
            TryBlock tryBlock = uniFailureTryBlock(doOnClose);
            ResultHandle ret = tryBlock.invokeVirtualMethod(MethodDescriptor.of(endpoint.onClose.method), beanInstance);
            encodeAndReturnResult(tryBlock, endpoint.onClose, ret);

            MethodCreator onCloseExecutionModel = endpointCreator.getMethodCreator("onCloseExecutionModel",
                    ExecutionModel.class);
            onCloseExecutionModel.returnValue(onCloseExecutionModel.load(endpoint.onClose.executionModel));
        }

        endpointCreator.close();
        return generatedName.replace('/', '.');
    }

    private void generateOnMessage(ClassCreator endpointCreator, WebSocketEndpointBuildItem endpoint, Callback callback) {
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
        // Foo foo = beanInstance("foo");
        ResultHandle beanInstance = doOnMessage.invokeSpecialMethod(
                MethodDescriptor.ofMethod(WebSocketEndpointBase.class, "beanInstance", Object.class, String.class),
                doOnMessage.getThis(), doOnMessage.load(endpoint.bean.getIdentifier()));
        ResultHandle[] args;
        if (callback.acceptsMessage()) {
            args = new ResultHandle[] { decodeMessage(doOnMessage, callback.acceptsBinaryMessage(),
                    callback.method.parameterType(0), doOnMessage.getMethodParam(0), callback) };
        } else {
            args = new ResultHandle[] {};
        }
        // Call the business method
        TryBlock tryBlock = uniFailureTryBlock(doOnMessage);
        ResultHandle ret = tryBlock.invokeVirtualMethod(MethodDescriptor.of(callback.method), beanInstance,
                args);
        encodeAndReturnResult(tryBlock, callback, ret);

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
            decodeMultiItem.returnValue(decodeMessage(decodeMultiItem, callback.acceptsBinaryMessage(),
                    multiItemType, decodeMultiItem.getMethodParam(0), callback));
        }
    }

    private TryBlock uniFailureTryBlock(BytecodeCreator method) {
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

    private ResultHandle decodeMessage(MethodCreator method, boolean binaryMessage, Type valueType, ResultHandle value,
            Callback callback) {
        if (WebSocketDotNames.MULTI.equals(valueType.name())) {
            // Multi is decoded at runtime in the recorder
            return value;
        } else if (binaryMessage) {
            // Binary message
            if (WebSocketDotNames.BUFFER.equals(valueType.name())) {
                return value;
            } else if (WebSocketServerProcessor.isByteArray(valueType)) {
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
                ResultHandle decoded = method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "decodeBinary", Object.class, java.lang.reflect.Type.class, Buffer.class, Class.class),
                        method.getThis(), type,
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
            } else if (WebSocketServerProcessor.isByteArray(valueType)) {
                // byte[] message = Buffer.buffer(string).getBytes();
                ResultHandle buffer = method.invokeStaticInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, byte[].class), value);
                return method.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Buffer.class, "getBytes", byte[].class), buffer);
            } else {
                // Try to use codecs
                DotName inputCodec = callback.getInputCodec();
                ResultHandle type = Types.getTypeHandle(method, valueType);
                ResultHandle decoded = method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "decodeText", Object.class, java.lang.reflect.Type.class, String.class, Class.class), method.getThis(),
                        type, value, inputCodec != null ? method.loadClass(inputCodec.toString()) : method.loadNull());
                return decoded;
            }
        }
    }

    private ResultHandle encodeMessage(BytecodeCreator method, Callback callback, ResultHandle value) {
        if (callback.acceptsBinaryMessage()) {
            // ----------------------
            // === Binary message ===
            // ----------------------
            if (callback.isReturnTypeUni()) {
                Type messageType = callback.returnType().asParameterizedType().arguments().get(0);
                if (messageType.name().equals(WebSocketDotNames.VOID)) {
                    // Uni<Void>
                    return value;
                } else {
                    // return uniMessage.chain(m -> {
                    //    Buffer buffer = encodeBuffer(m);
                    //    return sendBinary(buffer,broadcast);
                    // });
                    FunctionCreator fun = method.createFunction(Function.class);
                    BytecodeCreator funBytecode = fun.getBytecode();
                    ResultHandle buffer = encodeBuffer(funBytecode,
                            callback.returnType().asParameterizedType().arguments().get(0),
                            funBytecode.getMethodParam(0), method.getThis(), callback);
                    funBytecode.returnValue(funBytecode.invokeSpecialMethod(
                            MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                    "sendBinary", Uni.class, Buffer.class, boolean.class),
                            method.getThis(), buffer,
                            funBytecode.load(callback.broadcast())));
                    ResultHandle uniChain = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Uni.class, "chain", Uni.class, Function.class), value,
                            fun.getInstance());
                    return uniChain;
                }
            } else if (callback.isReturnTypeMulti()) {
                // return multiBinary(multi, broadcast, m -> {
                //    Buffer buffer = encodeBuffer(m);
                //    return sendBinary(buffer,broadcast);
                //});
                FunctionCreator fun = method.createFunction(Function.class);
                BytecodeCreator funBytecode = fun.getBytecode();
                ResultHandle buffer = encodeBuffer(funBytecode, callback.returnType().asParameterizedType().arguments().get(0),
                        funBytecode.getMethodParam(0), method.getThis(), callback);
                funBytecode.returnValue(funBytecode.invokeSpecialMethod(
                        MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                "sendBinary", Uni.class, Buffer.class, boolean.class),
                        method.getThis(), buffer,
                        funBytecode.load(callback.broadcast())));
                return method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "multiBinary", Uni.class, Multi.class, boolean.class, Function.class), method.getThis(),
                        value,
                        method.load(callback.broadcast()),
                        fun.getInstance());
            } else {
                // return sendBinary(buffer,broadcast);
                ResultHandle buffer = encodeBuffer(method, callback.returnType(), value, method.getThis(), callback);
                return method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "sendBinary", Uni.class, Buffer.class, boolean.class), method.getThis(), buffer,
                        method.load(callback.broadcast()));
            }
        } else {
            // ----------------------
            // === Text message ===
            // ----------------------
            if (callback.isReturnTypeUni()) {
                Type messageType = callback.returnType().asParameterizedType().arguments().get(0);
                if (messageType.name().equals(WebSocketDotNames.VOID)) {
                    // Uni<Void>
                    return value;
                } else {
                    // return uniMessage.chain(m -> {
                    //    String text = encodeText(m);
                    //    return sendText(string,broadcast);
                    // });
                    FunctionCreator fun = method.createFunction(Function.class);
                    BytecodeCreator funBytecode = fun.getBytecode();
                    ResultHandle text = encodeText(funBytecode, callback.returnType().asParameterizedType().arguments().get(0),
                            funBytecode.getMethodParam(0), method.getThis(), callback);
                    funBytecode.returnValue(funBytecode.invokeSpecialMethod(
                            MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                    "sendText", Uni.class, String.class, boolean.class),
                            method.getThis(), text,
                            funBytecode.load(callback.broadcast())));
                    ResultHandle uniChain = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Uni.class, "chain", Uni.class, Function.class), value,
                            fun.getInstance());
                    return uniChain;
                }
            } else if (callback.isReturnTypeMulti()) {
                // return multiText(multi, broadcast, m -> {
                //    String text = encodeText(m);
                //    return sendText(buffer,broadcast);
                //});
                FunctionCreator fun = method.createFunction(Function.class);
                BytecodeCreator funBytecode = fun.getBytecode();
                ResultHandle text = encodeText(funBytecode, callback.returnType().asParameterizedType().arguments().get(0),
                        funBytecode.getMethodParam(0), method.getThis(), callback);
                funBytecode.returnValue(funBytecode.invokeSpecialMethod(
                        MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                                "sendText", Uni.class, String.class, boolean.class),
                        method.getThis(), text,
                        funBytecode.load(callback.broadcast())));
                return method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "multiText", Uni.class, Multi.class, boolean.class, Function.class), method.getThis(),
                        value,
                        method.load(callback.broadcast()),
                        fun.getInstance());
            } else {
                // return sendText(text,broadcast);
                ResultHandle text = encodeText(method, callback.returnType(), value, method.getThis(), callback);
                return method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                        "sendText", Uni.class, String.class, boolean.class), method.getThis(), text,
                        method.load(callback.broadcast()));
            }
        }
    }

    private ResultHandle encodeBuffer(BytecodeCreator method, Type messageType, ResultHandle value,
            ResultHandle defaultWebSocketEndpoint, Callback callback) {
        ResultHandle buffer;
        if (messageType.name().equals(WebSocketDotNames.BUFFER)) {
            buffer = value;
        } else if (WebSocketServerProcessor.isByteArray(messageType)) {
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
            buffer = method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                    "encodeBinary", Buffer.class, Object.class, Class.class), defaultWebSocketEndpoint, value,
                    outputCodec != null ? method.loadClass(outputCodec.toString()) : method.loadNull());
        }
        return buffer;
    }

    private ResultHandle encodeText(BytecodeCreator method, Type messageType, ResultHandle value,
            ResultHandle defaultWebSocketEndpoint, Callback callback) {
        ResultHandle text;
        if (messageType.name().equals(WebSocketDotNames.BUFFER)) {
            text = method.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "toString", String.class), value);
        } else if (WebSocketServerProcessor.isByteArray(messageType)) {
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
            text = method.invokeSpecialMethod(MethodDescriptor.ofMethod(WebSocketEndpointBase.class,
                    "encodeText", String.class, Object.class, Class.class), defaultWebSocketEndpoint, value,
                    outputCodec != null ? method.loadClass(outputCodec.toString()) : method.loadNull());
        }
        return text;
    }

    private ResultHandle uniVoid(BytecodeCreator method) {
        ResultHandle uniCreate = method
                .invokeStaticInterfaceMethod(MethodDescriptor.ofMethod(Uni.class, "createFrom", UniCreate.class));
        return method.invokeVirtualMethod(MethodDescriptor.ofMethod(UniCreate.class, "voidItem", Uni.class), uniCreate);
    }

    private void encodeAndReturnResult(BytecodeCreator method, Callback callback, ResultHandle result) {
        // The result must be always Uni<Void>
        if (callback.isReturnTypeVoid()) {
            // return Uni.createFrom().void()
            method.returnValue(uniVoid(method));
        } else {
            // Skip response
            BytecodeCreator isNull = method.ifNull(result).trueBranch();
            isNull.returnValue(uniVoid(isNull));
            method.returnValue(encodeMessage(method, callback, result));
        }
    }

    private Callback findCallback(IndexView index, ClassInfo beanClass, DotName annotationName,
            Consumer<MethodInfo> validator) {
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

        if (annotations.isEmpty()) {
            return null;
        } else if (annotations.size() == 1) {
            AnnotationInstance annotation = annotations.get(0);
            MethodInfo method = annotation.target().asMethod();
            validator.accept(method);
            return new Callback(annotation, method, executionModel(method));
        }
        throw new WebSocketServerException(
                String.format("There can be only one callback annotated with %s declared on %s", annotationName, beanClass));
    }

    ExecutionModel executionModel(MethodInfo method) {
        if (hasBlockingSignature(method)) {
            return method.hasDeclaredAnnotation(WebSocketDotNames.RUN_ON_VIRTUAL_THREAD) ? ExecutionModel.VIRTUAL_THREAD
                    : ExecutionModel.WORKER_THREAD;
        }
        return method.hasDeclaredAnnotation(WebSocketDotNames.BLOCKING) ? ExecutionModel.WORKER_THREAD
                : ExecutionModel.EVENT_LOOP;
    }

    boolean hasBlockingSignature(MethodInfo method) {
        switch (method.returnType().kind()) {
            case VOID:
            case CLASS:
                return true;
            case PARAMETERIZED_TYPE:
                // Uni, Multi -> non-blocking
                DotName name = method.returnType().asParameterizedType().name();
                return !name.equals(WebSocketDotNames.UNI) && !name.equals(WebSocketDotNames.MULTI);
            default:
                throw new WebSocketServerException("Unsupported return type:" + callbackToString(method));
        }
    }

    static boolean isUniVoid(Type type) {
        return WebSocketDotNames.UNI.equals(type.name())
                && type.asParameterizedType().arguments().get(0).name().equals(WebSocketDotNames.VOID);
    }

    static boolean isByteArray(Type type) {
        return type.kind() == Kind.ARRAY && PrimitiveType.BYTE.equals(type.asArrayType().constituent());
    }

}
