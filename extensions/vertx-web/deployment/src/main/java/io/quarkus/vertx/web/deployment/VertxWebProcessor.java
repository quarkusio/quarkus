package io.quarkus.vertx.web.deployment;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.inject.Singleton;

import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import io.quarkus.arc.*;
import io.quarkus.arc.deployment.*;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.*;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.*;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.quarkus.vertx.web.RouteFilter;
import io.quarkus.vertx.web.RoutingExchange;
import io.quarkus.vertx.web.runtime.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxWebProcessor {

    private static final Logger LOGGER = Logger.getLogger(VertxWebProcessor.class.getName());

    private static final DotName ROUTE = DotName.createSimple(Route.class.getName());
    private static final DotName ROUTES = DotName.createSimple(Route.Routes.class.getName());
    private static final DotName ROUTE_FILTER = DotName.createSimple(RouteFilter.class.getName());
    private static final DotName ROUTE_BASE = DotName.createSimple(RouteBase.class.getName());
    private static final DotName ROUTING_CONTEXT = DotName.createSimple(RoutingContext.class.getName());
    private static final DotName RX_ROUTING_CONTEXT = DotName
            .createSimple(io.vertx.reactivex.ext.web.RoutingContext.class.getName());
    private static final DotName ROUTING_EXCHANGE = DotName.createSimple(RoutingExchange.class.getName());
    private static final String HANDLER_SUFFIX = "_RouteHandler";
    private static final DotName[] ROUTE_PARAM_TYPES = { ROUTING_CONTEXT, RX_ROUTING_CONTEXT, ROUTING_EXCHANGE };
    private static final DotName[] ROUTE_FILTER_TYPES = { ROUTING_CONTEXT };

    private static final String VALUE_PATH = "path";
    private static final String VALUE_REGEX = "regex";
    private static final String VALUE_PRODUCES = "produces";
    private static final String VALUE_CONSUMES = "consumes";
    private static final String VALUE_METHODS = "methods";
    private static final String VALUE_ORDER = "order";
    private static final String SLASH = "/";

    private static final MethodDescriptor ARC_CONTAINER = MethodDescriptor
            .ofMethod(Arc.class, "container", ArcContainer.class);
    private static final MethodDescriptor ARC_CONTAINER_GET_ACTIVE_CONTEXT = MethodDescriptor
            .ofMethod(ArcContainer.class,
                    "getActiveContext", InjectableContext.class, Class.class);
    private static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    private static final MethodDescriptor BEAN_GET_SCOPE = MethodDescriptor.ofMethod(InjectableBean.class, "getScope",
            Class.class);
    private static final MethodDescriptor CONTEXT_GET = MethodDescriptor.ofMethod(Context.class, "get", Object.class,
            Contextual.class,
            CreationalContext.class);
    private static final MethodDescriptor CONTEXT_GET_IF_PRESENT = MethodDescriptor
            .ofMethod(Context.class, "get", Object.class,
                    Contextual.class);
    private static final MethodDescriptor INJECTABLE_REF_PROVIDER_GET = MethodDescriptor.ofMethod(
            InjectableReferenceProvider.class,
            "get", Object.class,
            CreationalContext.class);
    private static final MethodDescriptor INJECTABLE_BEAN_DESTROY = MethodDescriptor
            .ofMethod(InjectableBean.class, "destroy",
                    void.class, Object.class,
                    CreationalContext.class);
    static final MethodDescriptor OBJECT_CONSTRUCTOR = MethodDescriptor.ofConstructor(Object.class);
    public static final DotName DOTNAME_UNI = DotName.createSimple(Uni.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.VERTX_WEB);
    }

    @BuildStep
    void unremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(ROUTE)));
        unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(ROUTES)));
        unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(ROUTE_FILTER)));
    }

    @BuildStep
    void validateBeanDeployment(
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<AnnotatedRouteHandlerBuildItem> routeHandlerBusinessMethods,
            BuildProducer<AnnotatedRouteFilterBuildItem> routeFilterBusinessMethods,
            BuildProducer<ValidationErrorBuildItem> errors) {

        // Collect all business methods annotated with @Route and @RouteFilter
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans()) {
            // NOTE: inherited business methods are not taken into account
            ClassInfo beanClass = bean.getTarget().get().asClass();
            AnnotationInstance routeBaseAnnotation = beanClass.classAnnotation(ROUTE_BASE);
            for (MethodInfo method : beanClass.methods()) {
                List<AnnotationInstance> routes = new LinkedList<>();
                AnnotationInstance routeAnnotation = annotationStore.getAnnotation(method, ROUTE);
                if (routeAnnotation != null) {
                    validateRouteMethod(bean, method, ROUTE_PARAM_TYPES);
                    routes.add(routeAnnotation);
                }
                if (routes.isEmpty()) {
                    AnnotationInstance routesAnnotation = annotationStore.getAnnotation(method, ROUTES);
                    if (routesAnnotation != null) {
                        validateRouteMethod(bean, method, ROUTE_PARAM_TYPES);
                        Collections.addAll(routes, routesAnnotation.value().asNestedArray());
                    }
                }
                if (!routes.isEmpty()) {
                    LOGGER.debugf("Found route handler business method %s declared on %s", method, bean);
                    routeHandlerBusinessMethods
                            .produce(new AnnotatedRouteHandlerBuildItem(bean, method, routes, routeBaseAnnotation));
                }
                //
                AnnotationInstance filterAnnotation = annotationStore.getAnnotation(method, ROUTE_FILTER);
                if (filterAnnotation != null) {
                    if (!routes.isEmpty()) {
                        errors.produce(new ValidationErrorBuildItem(new IllegalStateException(
                                String.format(
                                        "@Route and @RouteFilter cannot be declared on business method %s declared on %s",
                                        method, bean))));
                    } else {
                        validateRouteMethod(bean, method, ROUTE_FILTER_TYPES);
                        routeFilterBusinessMethods
                                .produce(new AnnotatedRouteFilterBuildItem(bean, method, filterAnnotation));
                        LOGGER.debugf("Found route filter business method %s declared on %s", method, bean);
                    }
                }
            }
        }
    }

    @BuildStep
    BodyHandlerBuildItem bodyHandler(io.quarkus.vertx.http.deployment.BodyHandlerBuildItem realOne) {
        return new BodyHandlerBuildItem(realOne.getHandler());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void addAdditionalRoutes(
            VertxWebRecorder recorder,
            List<AnnotatedRouteHandlerBuildItem> routeHandlerBusinessMethods,
            List<AnnotatedRouteFilterBuildItem> routeFilterBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            io.quarkus.vertx.http.deployment.BodyHandlerBuildItem bodyHandler,
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<FilterBuildItem> filterProducer,
            List<RequireBodyHandlerBuildItem> bodyHandlerRequired,
            BeanArchiveIndexBuildItem beanArchive,
            ShutdownContextBuildItem shutdown) throws IOException {

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        IndexView index = beanArchive.getIndex();
        Map<RouteMatcher, MethodInfo> matchers = new HashMap<>();

        for (AnnotatedRouteHandlerBuildItem businessMethod : routeHandlerBusinessMethods) {
            String handlerClass = generateHandler(new HandlerDescriptor(businessMethod.getMethod()),
                    businessMethod.getBean(), businessMethod.getMethod(), classOutput);
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
            Handler<RoutingContext> routingHandler = recorder.createHandler(handlerClass);

            AnnotationInstance routeBaseAnnotation = businessMethod.getRouteBase();
            String pathPrefix = null;
            String[] baseProduces = null;
            String[] baseConsumes = null;

            if (routeBaseAnnotation != null) {
                AnnotationValue pathPrefixValue = routeBaseAnnotation.value(VALUE_PATH);
                if (pathPrefixValue != null) {
                    pathPrefix = pathPrefixValue.asString();
                }
                AnnotationValue producesValue = routeBaseAnnotation.value(VALUE_PRODUCES);
                if (producesValue != null) {
                    baseProduces = producesValue.asStringArray();
                }
                AnnotationValue consumesValue = routeBaseAnnotation.value(VALUE_CONSUMES);
                if (consumesValue != null) {
                    baseConsumes = consumesValue.asStringArray();
                }
            }

            for (AnnotationInstance route : businessMethod.getRoutes()) {
                AnnotationValue regexValue = route.value(VALUE_REGEX);
                AnnotationValue pathValue = route.value(VALUE_PATH);
                AnnotationValue orderValue = route.valueWithDefault(index, VALUE_ORDER);
                AnnotationValue producesValue = route.valueWithDefault(index, VALUE_PRODUCES);
                AnnotationValue consumesValue = route.valueWithDefault(index, VALUE_CONSUMES);
                AnnotationValue methodsValue = route.valueWithDefault(index, VALUE_METHODS);

                String path = null;
                String regex = null;
                String[] produces = producesValue.asStringArray();
                String[] consumes = consumesValue.asStringArray();
                HttpMethod[] methods = Arrays.stream(methodsValue.asEnumArray()).map(HttpMethod::valueOf)
                        .toArray(HttpMethod[]::new);
                Integer order = orderValue.asInt();

                if (regexValue == null) {
                    if (pathPrefix != null) {
                        StringBuilder prefixedPath = new StringBuilder();
                        prefixedPath.append(pathPrefix);
                        if (pathValue == null) {
                            prefixedPath.append(SLASH);
                            prefixedPath.append(dashify(businessMethod.getMethod().name()));
                        } else {
                            if (!pathValue.asString().startsWith(SLASH)) {
                                prefixedPath.append(SLASH);
                            }
                            prefixedPath.append(pathValue.asString());
                        }
                        path = prefixedPath.toString();
                    } else {
                        path = pathValue != null ? pathValue.asString() : dashify(businessMethod.getMethod().name());
                    }
                    if (!path.startsWith(SLASH)) {
                        path = SLASH + path;
                    }
                } else {
                    regex = regexValue.asString();
                }

                if (route.value(VALUE_PRODUCES) == null && baseProduces != null) {
                    produces = baseProduces;
                }
                if (route.value(VALUE_CONSUMES) == null && baseConsumes != null) {
                    consumes = baseConsumes;
                }

                RouteMatcher matcher = new RouteMatcher(path, regex, produces, consumes, methods, order);
                matchers.put(matcher, businessMethod.getMethod());
                Function<Router, io.vertx.ext.web.Route> routeFunction = recorder.createRouteFunction(matcher,
                        bodyHandler.getHandler());
                AnnotationValue typeValue = route.value("type");
                HandlerType handlerType = HandlerType.NORMAL;
                if (typeValue != null) {
                    String typeString = typeValue.asEnum();
                    switch (typeString) {
                        case "NORMAL":
                            handlerType = HandlerType.NORMAL;
                            break;
                        case "BLOCKING":
                            handlerType = HandlerType.BLOCKING;
                            break;
                        case "FAILURE":
                            handlerType = HandlerType.FAILURE;
                            break;
                        default:
                            throw new IllegalStateException("Unkown type " + typeString);
                    }
                }
                routeProducer.produce(new RouteBuildItem(routeFunction, routingHandler, handlerType));
            }
        }

        for (AnnotatedRouteFilterBuildItem filterMethod : routeFilterBusinessMethods) {
            String handlerClass = generateHandler(new HandlerDescriptor(filterMethod.getMethod()),
                    filterMethod.getBean(), filterMethod.getMethod(), classOutput);
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
            Handler<RoutingContext> routingHandler = recorder.createHandler(handlerClass);
            AnnotationValue priorityValue = filterMethod.getRouteFilter().value();
            filterProducer.produce(new FilterBuildItem(routingHandler,
                    priorityValue != null ? priorityValue.asInt() : RouteFilter.DEFAULT_PRIORITY));
        }

        detectConflictingRoutes(matchers);

        recorder.clearCacheOnShutdown(shutdown);
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer(CustomScopeAnnotationsBuildItem scopes) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (!scopes.isScopeIn(context.getAnnotations())) {
                    // Class with no scope annotation but with a method annotated with @Route, @RouteFilter
                    ClassInfo target = context.getTarget().asClass();
                    if (target.annotations().containsKey(ROUTE) || target.annotations().containsKey(ROUTES)
                            || target.annotations().containsKey(ROUTE_FILTER)) {
                        LOGGER.debugf(
                                "Found route handler business methods on a class %s with no scope annotation - adding @Singleton",
                                context.getTarget());
                        context.transform().add(Singleton.class).done();
                    }
                }
            }
        });
    }

    private void validateRouteMethod(BeanInfo bean, MethodInfo method, DotName[] validParamTypes) {
        if (method.returnType().name().equals(DOTNAME_UNI)) {
            List<Type> types = method.returnType().asParameterizedType().arguments();
            if (types.isEmpty()) {
                throw new IllegalStateException(
                        String.format(
                                "Route handler business returning a `Uni` must have a generic parameter [method: %s, bean: %s]",
                                method, bean));
            }
        }

        List<Type> params = method.parameters();
        boolean hasInvalidParam = true;
        if (params.size() == 1) {
            DotName paramTypeName = params.get(0).name();
            for (DotName type : validParamTypes) {
                if (type.equals(paramTypeName)) {
                    hasInvalidParam = false;
                }
            }
        }
        if (hasInvalidParam) {
            throw new IllegalStateException(String.format(
                    "Route business method must accept exactly one parameter of type %s: %s [method: %s, bean: %s]",
                    validParamTypes, params, method, bean));
        }
    }

    private String generateHandler(HandlerDescriptor desc, BeanInfo bean, MethodInfo method, ClassOutput classOutput) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz().name());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + HANDLER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(RouteHandler.class).build();

        // Initialized state
        FieldCreator beanField = invokerCreator.getFieldCreator("bean", InjectableBean.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator contextField = null;
        FieldCreator containerField = null;
        if (BuiltinScope.APPLICATION.is(bean.getScope()) || BuiltinScope.SINGLETON.is(bean.getScope())) {
            // Singleton and application contexts are always active and unambiguous
            contextField = invokerCreator.getFieldCreator("context", InjectableContext.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        } else {
            containerField = invokerCreator.getFieldCreator("container", ArcContainer.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        implementConstructor(bean, invokerCreator, beanField, contextField, containerField);
        implementInvoke(desc, bean, method, invokerCreator, beanField, contextField, containerField);

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    void implementConstructor(BeanInfo bean, ClassCreator invokerCreator, FieldCreator beanField,
            FieldCreator contextField,
            FieldCreator containerField) {
        MethodCreator constructor = invokerCreator.getMethodCreator("<init>", void.class);
        // Invoke super()
        constructor.invokeSpecialMethod(OBJECT_CONSTRUCTOR, constructor.getThis());

        ResultHandle containerHandle = constructor
                .invokeStaticMethod(ARC_CONTAINER);
        ResultHandle beanHandle = constructor.invokeInterfaceMethod(
                ARC_CONTAINER_BEAN,
                containerHandle, constructor.load(bean.getIdentifier()));
        constructor.writeInstanceField(beanField.getFieldDescriptor(), constructor.getThis(), beanHandle);
        if (contextField != null) {
            constructor.writeInstanceField(contextField.getFieldDescriptor(), constructor.getThis(),
                    constructor.invokeInterfaceMethod(
                            ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                            containerHandle, constructor
                                    .invokeInterfaceMethod(
                                            BEAN_GET_SCOPE,
                                            beanHandle)));
        } else {
            constructor.writeInstanceField(containerField.getFieldDescriptor(), constructor.getThis(), containerHandle);
        }
        constructor.returnValue(null);
    }

    void implementInvoke(HandlerDescriptor descriptor, BeanInfo bean, MethodInfo method, ClassCreator invokerCreator,
            FieldCreator beanField,
            FieldCreator contextField, FieldCreator containerField) {
        // The descriptor is: void invoke(RoutingContext rc)
        MethodCreator invoke = invokerCreator.getMethodCreator("invoke", void.class, RoutingContext.class);
        ResultHandle beanHandle = invoke.readInstanceField(beanField.getFieldDescriptor(), invoke.getThis());
        AssignableResultHandle beanInstanceHandle = invoke.createVariable(Object.class);
        AssignableResultHandle creationlContextHandle = invoke.createVariable(CreationalContextImpl.class);

        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            // Always create a new dependent instance
            invoke.assign(creationlContextHandle,
                    invoke.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                            beanHandle));
            invoke.assign(beanInstanceHandle, invoke.invokeInterfaceMethod(
                    INJECTABLE_REF_PROVIDER_GET, beanHandle,
                    creationlContextHandle));
        } else {
            ResultHandle contextInvokeHandle;
            if (contextField != null) {
                contextInvokeHandle = invoke.readInstanceField(contextField.getFieldDescriptor(), invoke.getThis());
            } else {
                ResultHandle containerInvokeHandle = invoke.readInstanceField(containerField.getFieldDescriptor(),
                        invoke.getThis());
                contextInvokeHandle = invoke.invokeInterfaceMethod(
                        ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                        containerInvokeHandle, invoke
                                .invokeInterfaceMethod(
                                        BEAN_GET_SCOPE,
                                        beanHandle));
                invoke.ifNull(contextInvokeHandle).trueBranch().throwException(ContextNotActiveException.class,
                        "Context not active: " + bean.getScope().getDotName());
            }
            // First try to obtain the bean via Context.get(bean)
            invoke.assign(beanInstanceHandle, invoke.invokeInterfaceMethod(CONTEXT_GET_IF_PRESENT, contextInvokeHandle,
                    beanHandle));
            // If not present, try Context.get(bean,creationalContext)
            BytecodeCreator doesNotExist = invoke.ifNull(beanInstanceHandle).trueBranch();
            doesNotExist.assign(beanInstanceHandle,
                    doesNotExist.invokeInterfaceMethod(CONTEXT_GET, contextInvokeHandle, beanHandle,
                            doesNotExist.newInstance(
                                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                                    beanHandle)));
        }

        ResultHandle paramHandle;
        MethodDescriptor methodDescriptor;
        String returnType = descriptor.getReturnType().name().toString();

        // TODO Make Routing Context optional, allow injected Response and Request individually.
        ResultHandle rc = invoke.getMethodParam(0);
        if (method.parameters().get(0).name().equals(ROUTING_CONTEXT)) {
            paramHandle = rc;
            methodDescriptor = MethodDescriptor
                    .ofMethod(bean.getImplClazz().name().toString(), method.name(), returnType,
                            RoutingContext.class);
        } else if (method.parameters().get(0).name().equals(RX_ROUTING_CONTEXT)) {
            paramHandle = invoke.newInstance(
                    MethodDescriptor
                            .ofConstructor(io.vertx.reactivex.ext.web.RoutingContext.class, RoutingContext.class),
                    rc);
            methodDescriptor = MethodDescriptor
                    .ofMethod(bean.getImplClazz().name().toString(), method.name(), returnType,
                            io.vertx.reactivex.ext.web.RoutingContext.class);
        } else {
            paramHandle = invoke
                    .newInstance(MethodDescriptor.ofConstructor(RoutingExchangeImpl.class, RoutingContext.class),
                            rc);
            methodDescriptor = MethodDescriptor
                    .ofMethod(bean.getImplClazz().name().toString(), method.name(), returnType,
                            RoutingExchange.class);
        }

        // Invoke the business method handler
        ResultHandle res = invoke.invokeVirtualMethod(methodDescriptor, beanInstanceHandle, paramHandle);

        // Get the response: HttpServerResponse response = rc.response()
        ResultHandle response = invoke.invokeInterfaceMethod(Methods.RESPONSE, rc);
        MethodDescriptor end = Methods.getEndMethodForContentType(descriptor);
        if (descriptor.isReturningUni()) {
            // The method returns a Uni.
            // We subscribe to this Uni and write the provided item in the HTTP response
            // If the method returned null, we fail
            // If the provided item is null and the method does not return a Uni<Void>, we fail
            // If the provided item is null, and the method return a Uni<Void>, we reply with a 204 - NO CONTENT
            // If the provided item is not null, if it's a string or buffer, the response.end method is used to write the response
            // If the provided item is not null, and it's an object, the item is mapped to JSON and written into the response

            FunctionCreator successCallback = getUniOnItemCallback(descriptor, invoke, rc, end, response);
            FunctionCreator failureCallback = getUniOnFailureCallback(invoke, rc);

            ResultHandle sub = invoke.invokeInterfaceMethod(Methods.UNI_SUBSCRIBE, res);
            invoke.invokeVirtualMethod(Methods.UNI_SUBSCRIBE_WITH, sub, successCallback.getInstance(),
                    failureCallback.getInstance());
        } else if (descriptor.isReturningMulti()) {

            // 3 cases - regular multi vs. sse multi vs. json array multi, we need to check the type.
            BranchResult isItSSE = invoke.ifTrue(invoke.invokeStaticMethod(Methods.IS_SSE, res));
            BytecodeCreator isSSE = isItSSE.trueBranch();
            handleSSEMulti(descriptor, isSSE, rc, res);
            isSSE.close();

            BytecodeCreator isNotSSE = isItSSE.falseBranch();
            BranchResult isItJson = isNotSSE.ifTrue(isNotSSE.invokeStaticMethod(Methods.IS_JSON_ARRAY, res));
            BytecodeCreator isJson = isItJson.trueBranch();
            handleJsonArrayMulti(descriptor, isJson, rc, res);
            isJson.close();
            BytecodeCreator isRegular = isItJson.falseBranch();
            handleRegularMulti(descriptor, isRegular, rc, res);
            isRegular.close();
            isNotSSE.close();

        } else if (descriptor.getContentType() != null) {
            // The method returns "something" in a synchronous manner, write it into the response

            // If the method returned null, we fail
            // If the method returns string or buffer, the response.end method is used to write the response
            // If the method returns an object, the result is mapped to JSON and written into the response
            ResultHandle content = getContentToWrite(descriptor, response, res, invoke);
            invoke.invokeInterfaceMethod(end, response, content);
        }

        // Destroy dependent instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            invoke.invokeInterfaceMethod(INJECTABLE_BEAN_DESTROY, beanHandle,
                    beanInstanceHandle, creationlContextHandle);
        }
        invoke.returnValue(null);
    }

    private void handleRegularMulti(HandlerDescriptor descriptor, BytecodeCreator writer, ResultHandle rc,
            ResultHandle res) {
        // The method returns a Multi.
        // We subscribe to this Multi and write the provided items (one by one) in the HTTP response.
        // On completion, we "end" the response
        // If the method returned null, we fail
        // If the provided item is null we fail
        // If the multi is empty, and the method return a Multi<Void>, we reply with a 204 - NO CONTENT
        // If the produce item is a string or buffer, the response.write method is used to write the response
        // If the produce item is an object, the item is mapped to JSON and written into the response. The response is a JSON array.

        if (Methods.isNoContent(descriptor)) { // Multi<Void> - so return a 204.
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_VOID, res, rc);
        } else if (descriptor.isContentTypeBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_BUFFER, res, rc);
        } else if (descriptor.isContentTypeMutinyBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_MUTINY_BUFFER, res, rc);
        } else if (descriptor.isContentTypeRxBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_RX_BUFFER, res, rc);
        } else if (descriptor.isContentTypeString()) {
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_STRING, res, rc);
        } else { // Multi<Object> - encode to json.
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_OBJECT, res, rc);
        }
    }

    private void handleSSEMulti(HandlerDescriptor descriptor, BytecodeCreator writer, ResultHandle rc,
            ResultHandle res) {
        // The method returns a Multi that needs to be written as server-sent event.
        // We subscribe to this Multi and write the provided items (one by one) in the HTTP response.
        // On completion, we "end" the response
        // If the method returned null, we fail
        // If the provided item is null we fail
        // If the multi is empty, and the method return a Multi<Void>, we reply with a 204 - NO CONTENT (as regular)
        // If the produced item is a string or buffer, the response.write method is used to write the events in the response
        // If the produced item is an object, the item is mapped to JSON and included in the `data` section of the event.

        if (Methods.isNoContent(descriptor)) { // Multi<Void> - so return a 204.
            writer.invokeStaticMethod(Methods.MULTI_SUBSCRIBE_VOID, res, rc);
        } else if (descriptor.isContentTypeBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_BUFFER, res, rc);
        } else if (descriptor.isContentTypeMutinyBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_MUTINY_BUFFER, res, rc);
        } else if (descriptor.isContentTypeRxBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_RX_BUFFER, res, rc);
        } else if (descriptor.isContentTypeString()) {
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_STRING, res, rc);
        } else { // Multi<Object> - encode to json.
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_OBJECT, res, rc);
        }
    }

    private void handleJsonArrayMulti(HandlerDescriptor descriptor, BytecodeCreator writer, ResultHandle rc,
            ResultHandle res) {
        // The method returns a Multi that needs to be written as JSON Array.
        // We subscribe to this Multi and write the provided items (one by one) in the HTTP response.
        // On completion, we "end" the response
        // If the method returned null, we fail
        // If the provided item is null we fail
        // If the multi is empty, we send an empty JSON array
        // If the produced item is a string, the response.write method is used to write the events in the response
        // If the produced item is an object, the item is mapped to JSON and included in the `data` section of the event.
        // If the produced item is a buffer, we fail

        if (Methods.isNoContent(descriptor)) { // Multi<Void> - so return a 204.
            writer.invokeStaticMethod(Methods.MULTI_JSON_SUBSCRIBE_VOID, res, rc);
        } else if (descriptor.isContentTypeString()) {
            writer.invokeStaticMethod(Methods.MULTI_JSON_SUBSCRIBE_STRING, res, rc);
        } else if (descriptor.isContentTypeBuffer() || descriptor.isContentTypeRxBuffer()
                || descriptor.isContentTypeMutinyBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_JSON_FAIL, rc);
        } else { // Multi<Object> - encode to json.
            writer.invokeStaticMethod(Methods.MULTI_JSON_SUBSCRIBE_OBJECT, res, rc);
        }
    }

    /**
     * Generates the following function depending on the payload type
     *
     * If the method returns a {@code Uni<Void>}
     * 
     * <pre>
     *     item -> rc.response().setStatusCode(204).end();
     * </pre>
     *
     * If the method returns a {@code Uni<Buffer>}:
     * 
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          Buffer buffer = getBuffer(item); // Manage RX and Mutiny buffer
     *          rc.response().end(buffer);
     *       } else {
     *           rc.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     *
     * If the method returns a {@code Uni<String>} :
     * 
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          rc.response().end(item);
     *       } else {
     *           rc.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     *
     * If the method returns a {@code Uni<T>} :
     * 
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          String json = Json.encode(item);
     *          rc.response().end(json);
     *       } else {
     *           rc.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     *
     * This last version also set the {@code content-type} header to {@code application/json }if not set.
     *
     * @param descriptor the method descriptor
     * @param invoke the main bytecode writer
     * @param rc the reference to the routing context variable
     * @param end the end method to use
     * @param response the reference to the response variable
     * @return the function creator
     */
    private FunctionCreator getUniOnItemCallback(HandlerDescriptor descriptor, MethodCreator invoke, ResultHandle rc,
            MethodDescriptor end, ResultHandle response) {
        FunctionCreator callback = invoke.createFunction(Consumer.class);
        BytecodeCreator creator = callback.getBytecode();
        if (Methods.isNoContent(descriptor)) { // Uni<Void> - so return a 204.
            creator.invokeInterfaceMethod(Methods.SET_STATUS, response, creator.load(204));
            creator.invokeInterfaceMethod(Methods.END, response);
        } else {
            // Check if the item is null
            ResultHandle item = creator.getMethodParam(0);
            BranchResult isItemNull = creator.ifNull(item);

            BytecodeCreator itemIfNotNull = isItemNull.falseBranch();
            ResultHandle content = getContentToWrite(descriptor, response, item, itemIfNotNull);
            itemIfNotNull.invokeInterfaceMethod(end, response, content);
            itemIfNotNull.close();

            BytecodeCreator resultNull = isItemNull.trueBranch();
            ResultHandle npe = Methods.createNpeBecauseItemIfNull(resultNull);
            resultNull.invokeInterfaceMethod(Methods.FAIL, rc, npe);
            resultNull.close();
        }
        Methods.returnAndClose(creator);
        return callback;
    }

    /**
     * Generates the following function:
     *
     * <pre>
     *     throwable -> rc.fail(throwable);
     * </pre>
     *
     * @param writer the bytecode writer
     * @param rc the reference to the RoutingContext variable
     * @return the function creator.
     */
    private FunctionCreator getUniOnFailureCallback(MethodCreator writer, ResultHandle rc) {
        FunctionCreator callback = writer.createFunction(Consumer.class);
        BytecodeCreator creator = callback.getBytecode();
        Methods.fail(creator, rc, creator.getMethodParam(0));
        Methods.returnAndClose(creator);
        return callback;
    }

    private ResultHandle getContentToWrite(HandlerDescriptor descriptor, ResultHandle response, ResultHandle res,
            BytecodeCreator writer) {
        if (descriptor.isContentTypeString() || descriptor.isContentTypeBuffer()) {
            return res;
        }

        if (descriptor.isContentTypeRxBuffer()) {
            return writer.invokeVirtualMethod(Methods.RX_GET_DELEGATE, res);
        }

        if (descriptor.isContentTypeMutinyBuffer()) {
            return writer.invokeVirtualMethod(Methods.MUTINY_GET_DELEGATE, res);
        }

        // Encode to Json
        Methods.setContentTypeToJson(response, writer);
        return writer.invokeStaticMethod(Methods.JSON_ENCODE, res);
    }

    private static String dashify(String value) {
        StringBuilder ret = new StringBuilder();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (i != 0 && i != (chars.length - 1) && Character.isUpperCase(c)) {
                ret.append('-');
            }
            ret.append(Character.toLowerCase(c));
        }
        return ret.toString();
    }

    private void detectConflictingRoutes(Map<RouteMatcher, MethodInfo> matchers) {
        if (matchers.isEmpty()) {
            return;
        }
        // First we need to group matchers that could potentially match the same request 
        Set<LinkedHashSet<RouteMatcher>> groups = new HashSet<>();
        for (Iterator<Entry<RouteMatcher, MethodInfo>> iterator = matchers.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<RouteMatcher, MethodInfo> entry = iterator.next();
            LinkedHashSet<RouteMatcher> group = new LinkedHashSet<>();
            group.add(entry.getKey());
            matchers.entrySet().stream().filter(e -> {
                if (e.getKey().equals(entry.getKey())) {
                    // Skip - the same matcher
                    return false;
                }
                if (e.getValue().equals(entry.getValue())) {
                    // Skip - the same method
                    return false;
                }
                if (e.getKey().getOrder() != entry.getKey().getOrder()) {
                    // Skip - different order set
                    return false;
                }
                return canMatchSameRequest(entry.getKey(), e.getKey());
            }).map(Entry::getKey).forEach(group::add);
            groups.add(group);
        }
        // Log a warning for any group that contains more than one member
        boolean conflictExists = false;
        for (Set<RouteMatcher> group : groups) {
            if (group.size() > 1) {
                Iterator<RouteMatcher> it = group.iterator();
                RouteMatcher firstMatcher = it.next();
                MethodInfo firstMethod = matchers.get(firstMatcher);
                conflictExists = true;
                StringBuilder conflictingRoutes = new StringBuilder();
                while (it.hasNext()) {
                    RouteMatcher rm = it.next();
                    MethodInfo method = matchers.get(rm);
                    conflictingRoutes.append("\n\t- ").append(method.declaringClass().name().toString()).append("#")
                            .append(method.name()).append("()");
                }
                LOGGER.warnf(
                        "Route %s#%s() can match the same request and has the same order [%s] as:%s",
                        firstMethod.declaringClass().name(),
                        firstMethod.name(), firstMatcher.getOrder(), conflictingRoutes);
            }
        }
        if (conflictExists) {
            LOGGER.warn("You can use @Route#order() to ensure the routes are not executed in random order");
        }
    }

    static boolean canMatchSameRequest(RouteMatcher m1, RouteMatcher m2) {
        // regex not null and other not equal
        if (m1.getRegex() != null) {
            if (!Objects.equals(m1.getRegex(), m2.getRegex())) {
                return false;
            }
        } else {
            // path not null and other not equal
            if (m1.getPath() != null && !Objects.equals(m1.getPath(), m2.getPath())) {
                return false;
            }
        }
        // methods not matching
        if (m1.getMethods().length > 0 && m2.getMethods().length > 0 && !Arrays
                .equals(m1.getMethods(), m2.getMethods())) {
            return false;
        }
        // produces not matching
        if (m1.getProduces().length > 0 && m2.getProduces().length > 0 && !Arrays
                .equals(m1.getProduces(), m2.getProduces())) {
            return false;
        }
        // consumes not matching
        if (m1.getConsumes().length > 0 && m2.getConsumes().length > 0 && !Arrays
                .equals(m1.getConsumes(), m2.getConsumes())) {
            return false;
        }
        return true;
    }
}
