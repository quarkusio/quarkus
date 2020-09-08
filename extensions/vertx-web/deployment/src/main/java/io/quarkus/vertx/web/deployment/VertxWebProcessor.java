package io.quarkus.vertx.web.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.vertx.web.deployment.DotNames.PARAM;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.RouteFilter;
import io.quarkus.vertx.web.runtime.RouteHandler;
import io.quarkus.vertx.web.runtime.RouteMatcher;
import io.quarkus.vertx.web.runtime.RoutingExchangeImpl;
import io.quarkus.vertx.web.runtime.UniFailureCallback;
import io.quarkus.vertx.web.runtime.VertxWebRecorder;
import io.quarkus.vertx.web.runtime.devmode.ResourceNotFoundRecorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxWebProcessor {

    private static final Logger LOGGER = Logger.getLogger(VertxWebProcessor.class.getName());

    private static final String HANDLER_SUFFIX = "_RouteHandler";
    private static final String VALUE_PATH = "path";
    private static final String VALUE_REGEX = "regex";
    private static final String VALUE_PRODUCES = "produces";
    private static final String VALUE_CONSUMES = "consumes";
    private static final String VALUE_METHODS = "methods";
    private static final String VALUE_ORDER = "order";
    private static final String SLASH = "/";

    private static final List<ParameterInjector> PARAM_INJECTORS = initParamInjectors();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.VERTX_WEB);
    }

    @BuildStep
    void unremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTE));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTES));
        unremovableBeans
                .produce(UnremovableBeanBuildItem.beanClassAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTE_FILTER));
    }

    @BuildStep
    void validateBeanDeployment(
            ValidationPhaseBuildItem validationPhase,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<AnnotatedRouteHandlerBuildItem> routeHandlerBusinessMethods,
            BuildProducer<AnnotatedRouteFilterBuildItem> routeFilterBusinessMethods,
            BuildProducer<ValidationErrorBuildItem> errors) {

        // Collect all business methods annotated with @Route and @RouteFilter
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans()) {
            // NOTE: inherited business methods are not taken into account
            ClassInfo beanClass = bean.getTarget().get().asClass();
            AnnotationInstance routeBaseAnnotation = beanClass
                    .classAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTE_BASE);
            for (MethodInfo method : beanClass.methods()) {
                List<AnnotationInstance> routes = new LinkedList<>();
                AnnotationInstance routeAnnotation = annotationStore.getAnnotation(method,
                        io.quarkus.vertx.web.deployment.DotNames.ROUTE);
                if (routeAnnotation != null) {
                    validateRouteMethod(bean, method, transformedAnnotations);
                    routes.add(routeAnnotation);
                }
                if (routes.isEmpty()) {
                    AnnotationInstance routesAnnotation = annotationStore.getAnnotation(method,
                            io.quarkus.vertx.web.deployment.DotNames.ROUTES);
                    if (routesAnnotation != null) {
                        validateRouteMethod(bean, method, transformedAnnotations);
                        Collections.addAll(routes, routesAnnotation.value().asNestedArray());
                    }
                }
                if (!routes.isEmpty()) {
                    LOGGER.debugf("Found route handler business method %s declared on %s", method, bean);
                    routeHandlerBusinessMethods
                            .produce(new AnnotatedRouteHandlerBuildItem(bean, method, routes, routeBaseAnnotation));
                }
                //
                AnnotationInstance filterAnnotation = annotationStore.getAnnotation(method,
                        io.quarkus.vertx.web.deployment.DotNames.ROUTE_FILTER);
                if (filterAnnotation != null) {
                    if (!routes.isEmpty()) {
                        errors.produce(new ValidationErrorBuildItem(new IllegalStateException(
                                String.format(
                                        "@Route and @RouteFilter cannot be declared on business method %s declared on %s",
                                        method, bean))));
                    } else {
                        validateRouteFilterMethod(bean, method);
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
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            io.quarkus.vertx.http.deployment.BodyHandlerBuildItem bodyHandler,
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<FilterBuildItem> filterProducer,
            List<RequireBodyHandlerBuildItem> bodyHandlerRequired,
            BeanArchiveIndexBuildItem beanArchive,
            TransformedAnnotationsBuildItem transformedAnnotations,
            ShutdownContextBuildItem shutdown,
            LaunchModeBuildItem launchMode,
            BuildProducer<RouteDescriptionBuildItem> descriptions) throws IOException {

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        IndexView index = beanArchive.getIndex();
        Map<RouteMatcher, MethodInfo> matchers = new HashMap<>();

        for (AnnotatedRouteHandlerBuildItem businessMethod : routeHandlerBusinessMethods) {
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

            // Route annotations with the same values share a single handler instance
            // @Route string value -> handler
            Map<String, Handler<RoutingContext>> routeHandlers = new HashMap<>();

            for (AnnotationInstance route : businessMethod.getRoutes()) {
                String routeString = route.toString(true);
                Handler<RoutingContext> routeHandler = routeHandlers.get(routeString);

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

                if (routeHandler == null) {
                    String handlerClass = generateHandler(new HandlerDescriptor(businessMethod.getMethod()),
                            businessMethod.getBean(), businessMethod.getMethod(), classOutput, transformedAnnotations,
                            routeString, reflectiveHierarchy, produces.length > 0 ? produces[0] : null);
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
                    routeHandler = recorder.createHandler(handlerClass);
                    routeHandlers.put(routeString, routeHandler);
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
                routeProducer.produce(new RouteBuildItem(routeFunction, routeHandler, handlerType));

                if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)) {
                    if (methods.length == 0) {
                        // No explicit method declared - match all methods
                        methods = HttpMethod.values();
                    }
                    descriptions.produce(new RouteDescriptionBuildItem(
                            businessMethod.getMethod().declaringClass().name().withoutPackagePrefix() + "#"
                                    + businessMethod.getMethod().name() + "()",
                            regex != null ? regex : path,
                            Arrays.stream(methods).map(Object::toString).collect(Collectors.joining(", ")), produces,
                            consumes));
                }
            }
        }

        for (AnnotatedRouteFilterBuildItem filterMethod : routeFilterBusinessMethods) {
            String handlerClass = generateHandler(new HandlerDescriptor(filterMethod.getMethod()),
                    filterMethod.getBean(), filterMethod.getMethod(), classOutput, transformedAnnotations,
                    filterMethod.getRouteFilter().toString(true), reflectiveHierarchy, null);
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
            Handler<RoutingContext> routingHandler = recorder.createHandler(handlerClass);
            AnnotationValue priorityValue = filterMethod.getRouteFilter().value();
            filterProducer.produce(new FilterBuildItem(routingHandler,
                    priorityValue != null ? priorityValue.asInt() : RouteFilter.DEFAULT_PRIORITY));
        }

        detectConflictingRoutes(matchers);

        recorder.clearCacheOnShutdown(shutdown);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(RUNTIME_INIT)
    void routeNotFound(Capabilities capabilities, ResourceNotFoundRecorder recorder, VertxWebRouterBuildItem router,
            List<RouteDescriptionBuildItem> descriptions,
            HttpRootPathBuildItem httpRoot,
            List<NotFoundPageDisplayableEndpointBuildItem> additionalEndpoints) {
        if (capabilities.isMissing(Capability.RESTEASY)) {
            // Register a special error handler if JAX-RS not available
            recorder.registerNotFoundHandler(router.getRouter(), httpRoot.getRootPath(),
                    descriptions.stream().map(RouteDescriptionBuildItem::getDescription).collect(Collectors.toList()),
                    additionalEndpoints.stream().map(NotFoundPageDisplayableEndpointBuildItem::getEndpoint)
                            .collect(Collectors.toList()));
        }
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope() {
        return AutoAddScopeBuildItem.builder()
                .containsAnnotations(io.quarkus.vertx.web.deployment.DotNames.ROUTE,
                        io.quarkus.vertx.web.deployment.DotNames.ROUTES,
                        io.quarkus.vertx.web.deployment.DotNames.ROUTE_FILTER)
                .defaultScope(BuiltinScope.SINGLETON)
                .reason("Found route handler business methods").build();
    }

    private void validateRouteFilterMethod(BeanInfo bean, MethodInfo method) {
        if (!method.returnType().kind().equals(Type.Kind.VOID)) {
            throw new IllegalStateException(
                    String.format("Route filter method must return void [method: %s, bean: %s]", method, bean));
        }
        List<Type> params = method.parameters();
        if (params.size() != 1 || !params.get(0).name().equals(io.quarkus.vertx.web.deployment.DotNames.ROUTING_CONTEXT)) {
            throw new IllegalStateException(String.format(
                    "Route filter method must accept exactly one parameter of type %s: %s [method: %s, bean: %s]",
                    io.quarkus.vertx.web.deployment.DotNames.ROUTING_CONTEXT, params, method, bean));
        }
    }

    private void validateRouteMethod(BeanInfo bean, MethodInfo method, TransformedAnnotationsBuildItem transformedAnnotations) {
        List<Type> params = method.parameters();
        if (params.isEmpty()) {
            if (method.returnType().kind() == Kind.VOID && params.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "Route method that returns void must accept at least one injectable parameter [method: %s, bean: %s]",
                        method, bean));
            }
        } else {
            if ((method.returnType().name().equals(io.quarkus.vertx.web.deployment.DotNames.UNI)
                    || method.returnType().name().equals(io.quarkus.vertx.web.deployment.DotNames.MULTI))
                    && method.returnType().kind() == Kind.CLASS) {
                throw new IllegalStateException(
                        String.format(
                                "Route business method returning a Uni/Multi must have a generic parameter [method: %s, bean: %s]",
                                method, bean));
            }
            int idx = 0;
            for (Type paramType : params) {
                Set<AnnotationInstance> paramAnnotations = Annotations.getParameterAnnotations(transformedAnnotations, method,
                        idx);
                List<ParameterInjector> injectors = getMatchingInjectors(paramType, paramAnnotations);
                if (injectors.isEmpty()) {
                    throw new IllegalStateException(String.format(
                            "No parameter injector found for parameter %s of route method %s declared on %s", idx, method,
                            bean));
                }
                if (injectors.size() > 1) {
                    throw new IllegalStateException(String.format(
                            "Multiple parameter injectors found for parameter %s of route method %s declared on %s", idx,
                            method, bean));
                }
                idx++;
            }
        }
    }

    private String generateHandler(HandlerDescriptor desc, BeanInfo bean, MethodInfo method, ClassOutput classOutput,
            TransformedAnnotationsBuildItem transformedAnnotations, String hashSuffix,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, String defaultProduces) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = io.quarkus.arc.processor.DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + io.quarkus.arc.processor.DotNames.simpleName(bean.getImplClazz().name());
        } else {
            baseName = io.quarkus.arc.processor.DotNames.simpleName(bean.getImplClazz().name());
        }
        String targetPackage = io.quarkus.arc.processor.DotNames.packageName(bean.getImplClazz().name());

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + HANDLER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString() + hashSuffix);

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
        implementInvoke(desc, bean, method, invokerCreator, beanField, contextField, containerField, transformedAnnotations,
                reflectiveHierarchy, defaultProduces);

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    void implementConstructor(BeanInfo bean, ClassCreator invokerCreator, FieldCreator beanField,
            FieldCreator contextField,
            FieldCreator containerField) {
        MethodCreator constructor = invokerCreator.getMethodCreator("<init>", void.class);
        // Invoke super()
        constructor.invokeSpecialMethod(Methods.OBJECT_CONSTRUCTOR, constructor.getThis());

        ResultHandle containerHandle = constructor
                .invokeStaticMethod(Methods.ARC_CONTAINER);
        ResultHandle beanHandle = constructor.invokeInterfaceMethod(
                Methods.ARC_CONTAINER_BEAN,
                containerHandle, constructor.load(bean.getIdentifier()));
        constructor.writeInstanceField(beanField.getFieldDescriptor(), constructor.getThis(), beanHandle);
        if (contextField != null) {
            constructor.writeInstanceField(contextField.getFieldDescriptor(), constructor.getThis(),
                    constructor.invokeInterfaceMethod(
                            Methods.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                            containerHandle, constructor
                                    .invokeInterfaceMethod(
                                            Methods.BEAN_GET_SCOPE,
                                            beanHandle)));
        } else {
            constructor.writeInstanceField(containerField.getFieldDescriptor(), constructor.getThis(), containerHandle);
        }
        constructor.returnValue(null);
    }

    void implementInvoke(HandlerDescriptor descriptor, BeanInfo bean, MethodInfo method, ClassCreator invokerCreator,
            FieldCreator beanField, FieldCreator contextField, FieldCreator containerField,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, String defaultProduces) {
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
                    Methods.INJECTABLE_REF_PROVIDER_GET, beanHandle,
                    creationlContextHandle));
        } else {
            ResultHandle contextInvokeHandle;
            if (contextField != null) {
                contextInvokeHandle = invoke.readInstanceField(contextField.getFieldDescriptor(), invoke.getThis());
            } else {
                ResultHandle containerInvokeHandle = invoke.readInstanceField(containerField.getFieldDescriptor(),
                        invoke.getThis());
                contextInvokeHandle = invoke.invokeInterfaceMethod(
                        Methods.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                        containerInvokeHandle, invoke
                                .invokeInterfaceMethod(
                                        Methods.BEAN_GET_SCOPE,
                                        beanHandle));
                invoke.ifNull(contextInvokeHandle).trueBranch().throwException(ContextNotActiveException.class,
                        "Context not active: " + bean.getScope().getDotName());
            }
            // First try to obtain the bean via Context.get(bean)
            invoke.assign(beanInstanceHandle, invoke.invokeInterfaceMethod(Methods.CONTEXT_GET_IF_PRESENT, contextInvokeHandle,
                    beanHandle));
            // If not present, try Context.get(bean,creationalContext)
            BytecodeCreator doesNotExist = invoke.ifNull(beanInstanceHandle).trueBranch();
            doesNotExist.assign(beanInstanceHandle,
                    doesNotExist.invokeInterfaceMethod(Methods.CONTEXT_GET, contextInvokeHandle, beanHandle,
                            doesNotExist.newInstance(
                                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                                    beanHandle)));
        }

        List<Type> parameters = method.parameters();
        ResultHandle[] paramHandles = new ResultHandle[parameters.size()];
        String returnType = descriptor.getReturnType().name().toString();
        String[] parameterTypes = new String[parameters.size()];
        ResultHandle routingContext = invoke.getMethodParam(0);

        int idx = 0;
        for (Type paramType : parameters) {
            Set<AnnotationInstance> paramAnnotations = Annotations.getParameterAnnotations(transformedAnnotations, method, idx);
            // At this point we can be sure that a matching injector is available
            paramHandles[idx] = getMatchingInjectors(paramType, paramAnnotations).get(0).getResultHandle(method, paramType,
                    paramAnnotations, routingContext, invoke, idx, reflectiveHierarchy);
            parameterTypes[idx] = paramType.name().toString();
            idx++;
        }

        MethodDescriptor methodDescriptor = MethodDescriptor
                .ofMethod(bean.getImplClazz().name().toString(), method.name(), returnType, parameterTypes);

        // If no content-type header is set then try to use the most acceptable content type
        // the business method can override this manually if required
        invoke.invokeStaticMethod(Methods.ROUTE_HANDLERS_SET_CONTENT_TYPE, routingContext,
                defaultProduces == null ? invoke.loadNull() : invoke.load(defaultProduces));

        // Invoke the business method handler
        ResultHandle res = invoke.invokeVirtualMethod(methodDescriptor, beanInstanceHandle, paramHandles);

        // Get the response: HttpServerResponse response = rc.response()
        MethodDescriptor end = Methods.getEndMethodForContentType(descriptor);
        if (descriptor.isReturningUni()) {
            ResultHandle response = invoke.invokeInterfaceMethod(Methods.RESPONSE, routingContext);
            // The method returns a Uni.
            // We subscribe to this Uni and write the provided item in the HTTP response
            // If the method returned null, we fail
            // If the provided item is null and the method does not return a Uni<Void>, we fail
            // If the provided item is null, and the method return a Uni<Void>, we reply with a 204 - NO CONTENT
            // If the provided item is not null, if it's a string or buffer, the response.end method is used to write the response
            // If the provided item is not null, and it's an object, the item is mapped to JSON and written into the response

            FunctionCreator successCallback = getUniOnItemCallback(descriptor, invoke, routingContext, end, response);
            ResultHandle failureCallback = getUniOnFailureCallback(invoke, routingContext);

            ResultHandle sub = invoke.invokeInterfaceMethod(Methods.UNI_SUBSCRIBE, res);
            invoke.invokeVirtualMethod(Methods.UNI_SUBSCRIBE_WITH, sub, successCallback.getInstance(),
                    failureCallback);

            registerForReflection(descriptor.getContentType(), reflectiveHierarchy);

        } else if (descriptor.isReturningMulti()) {

            // 3 cases - regular multi vs. sse multi vs. json array multi, we need to check the type.
            BranchResult isItSSE = invoke.ifTrue(invoke.invokeStaticMethod(Methods.IS_SSE, res));
            BytecodeCreator isSSE = isItSSE.trueBranch();
            handleSSEMulti(descriptor, isSSE, routingContext, res);
            isSSE.close();

            BytecodeCreator isNotSSE = isItSSE.falseBranch();
            BranchResult isItJson = isNotSSE.ifTrue(isNotSSE.invokeStaticMethod(Methods.IS_JSON_ARRAY, res));
            BytecodeCreator isJson = isItJson.trueBranch();
            handleJsonArrayMulti(descriptor, isJson, routingContext, res);
            isJson.close();
            BytecodeCreator isRegular = isItJson.falseBranch();
            handleRegularMulti(descriptor, isRegular, routingContext, res);
            isRegular.close();
            isNotSSE.close();

            registerForReflection(descriptor.getContentType(), reflectiveHierarchy);

        } else if (descriptor.getContentType() != null) {
            // The method returns "something" in a synchronous manner, write it into the response
            ResultHandle response = invoke.invokeInterfaceMethod(Methods.RESPONSE, routingContext);
            // If the method returned null, we fail
            // If the method returns string or buffer, the response.end method is used to write the response
            // If the method returns an object, the result is mapped to JSON and written into the response
            ResultHandle content = getContentToWrite(descriptor, response, res, invoke);
            invoke.invokeInterfaceMethod(end, response, content);

            registerForReflection(descriptor.getContentType(), reflectiveHierarchy);
        }

        // Destroy dependent instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            invoke.invokeInterfaceMethod(Methods.INJECTABLE_BEAN_DESTROY, beanHandle,
                    beanInstanceHandle, creationlContextHandle);
        }
        invoke.returnValue(null);
    }

    private static final List<DotName> TYPES_IGNORED_FOR_REFLECTION = Arrays.asList(io.quarkus.arc.processor.DotNames.STRING,
            DotNames.BUFFER, DotNames.JSON_ARRAY, DotNames.JSON_OBJECT);

    private static void registerForReflection(Type contentType,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
        if (TYPES_IGNORED_FOR_REFLECTION.contains(contentType.name())) {
            return;
        }
        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(contentType)
                .ignoreTypePredicate(ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE
                        .or(TYPES_IGNORED_FOR_REFLECTION::contains))
                .source(VertxWebProcessor.class.getSimpleName() + " > " + contentType)
                .build());
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

    private ResultHandle getUniOnFailureCallback(MethodCreator writer, ResultHandle routingContext) {
        // new UniFailureCallback(ctx)
        return writer.newInstance(MethodDescriptor.ofConstructor(UniFailureCallback.class, RoutingContext.class),
                routingContext);
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

    private List<ParameterInjector> getMatchingInjectors(Type paramType, Set<AnnotationInstance> paramAnnotations) {
        List<ParameterInjector> injectors = new ArrayList<>();
        for (ParameterInjector injector : PARAM_INJECTORS) {
            if (injector.matches(paramType, paramAnnotations)) {
                injectors.add(injector);
            }
        }
        return injectors;
    }

    static List<ParameterInjector> initParamInjectors() {
        List<ParameterInjector> injectors = new ArrayList<>();

        injectors.add(ParameterInjector.builder().matchType(io.quarkus.vertx.web.deployment.DotNames.ROUTING_CONTEXT)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return routingContext;
                    }
                }).build());

        injectors.add(ParameterInjector.builder().matchType(DotNames.RX_ROUTING_CONTEXT)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke.newInstance(
                                MethodDescriptor
                                        .ofConstructor(io.vertx.reactivex.ext.web.RoutingContext.class, RoutingContext.class),
                                routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().matchType(DotNames.ROUTING_EXCHANGE)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke
                                .newInstance(MethodDescriptor.ofConstructor(RoutingExchangeImpl.class, RoutingContext.class),
                                        routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().matchType(DotNames.HTTP_SERVER_REQUEST)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke
                                .invokeInterfaceMethod(Methods.REQUEST,
                                        routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().matchType(DotNames.HTTP_SERVER_RESPONSE)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke
                                .invokeInterfaceMethod(Methods.RESPONSE,
                                        routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().matchType(DotNames.RX_HTTP_SERVER_REQUEST)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke.newInstance(
                                MethodDescriptor
                                        .ofConstructor(io.vertx.reactivex.core.http.HttpServerRequest.class,
                                                HttpServerRequest.class),
                                invoke
                                        .invokeInterfaceMethod(Methods.REQUEST,
                                                routingContext));
                    }
                }).build());

        injectors
                .add(ParameterInjector.builder().matchType(DotNames.RX_HTTP_SERVER_RESPONSE)
                        .resultHandleProvider(new ResultHandleProvider() {
                            @Override
                            public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                                    ResultHandle routingContext, MethodCreator invoke, int position,
                                    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                                return invoke.newInstance(
                                        MethodDescriptor
                                                .ofConstructor(io.vertx.reactivex.core.http.HttpServerResponse.class,
                                                        HttpServerResponse.class),
                                        invoke
                                                .invokeInterfaceMethod(Methods.RESPONSE,
                                                        routingContext));
                            }
                        }).build());

        injectors.add(ParameterInjector.builder().matchType(io.quarkus.arc.processor.DotNames.STRING)
                .matchType(ParameterizedType.create(io.quarkus.arc.processor.DotNames.OPTIONAL,
                        new Type[] { Type.create(io.quarkus.arc.processor.DotNames.STRING, Kind.CLASS) }, null))
                .matchType(ParameterizedType.create(DotNames.LIST,
                        new Type[] { Type.create(io.quarkus.arc.processor.DotNames.STRING, Kind.CLASS) }, null))
                .requireAnnotations(PARAM)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        AnnotationValue paramAnnotationValue = Annotations
                                .find(annotations, PARAM).value();
                        String paramName = paramAnnotationValue != null ? paramAnnotationValue.asString() : null;
                        if (paramName == null || paramName.equals(Param.ELEMENT_NAME)) {
                            paramName = method.parameterName(position);
                        }
                        if (paramName == null) {
                            throw parameterNameNotAvailable(position, method);
                        }
                        ResultHandle paramHandle;
                        if (paramType.name().equals(DotNames.LIST)) {
                            // routingContext.request().params().getAll(paramName)
                            paramHandle = invoke.invokeInterfaceMethod(Methods.MULTIMAP_GET_ALL,
                                    invoke.invokeInterfaceMethod(Methods.REQUEST_PARAMS, invoke
                                            .invokeInterfaceMethod(Methods.REQUEST,
                                                    routingContext)),
                                    invoke.load(paramName));
                        } else {
                            // routingContext.request().getParam(paramName)
                            paramHandle = invoke.invokeInterfaceMethod(Methods.REQUEST_GET_PARAM, invoke
                                    .invokeInterfaceMethod(Methods.REQUEST,
                                            routingContext),
                                    invoke.load(paramName));
                            if (paramType.name().equals(io.quarkus.arc.processor.DotNames.OPTIONAL)) {
                                paramHandle = invoke.invokeStaticMethod(Methods.OPTIONAL_OF_NULLABLE, paramHandle);
                            }
                        }
                        return paramHandle;
                    }
                }).build());

        injectors.add(ParameterInjector.builder().matchType(io.quarkus.arc.processor.DotNames.STRING)
                .matchType(ParameterizedType.create(io.quarkus.arc.processor.DotNames.OPTIONAL,
                        new Type[] { Type.create(io.quarkus.arc.processor.DotNames.STRING, Kind.CLASS) }, null))
                .matchType(ParameterizedType.create(DotNames.LIST,
                        new Type[] { Type.create(io.quarkus.arc.processor.DotNames.STRING, Kind.CLASS) }, null))
                .requireAnnotations(DotNames.HEADER)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        AnnotationValue paramAnnotationValue = Annotations
                                .find(annotations, DotNames.HEADER).value();
                        String paramName = paramAnnotationValue != null ? paramAnnotationValue.asString() : null;
                        if (paramName == null || paramName.equals(Header.ELEMENT_NAME)) {
                            paramName = method.parameterName(position);
                        }
                        if (paramName == null) {
                            throw parameterNameNotAvailable(position, method);
                        }
                        ResultHandle paramHandle;
                        if (paramType.name().equals(DotNames.LIST)) {
                            // routingContext.request().headers().getAll(paramName)
                            paramHandle = invoke.invokeInterfaceMethod(Methods.MULTIMAP_GET_ALL,
                                    invoke.invokeInterfaceMethod(Methods.REQUEST_HEADERS, invoke
                                            .invokeInterfaceMethod(Methods.REQUEST,
                                                    routingContext)),
                                    invoke.load(paramName));
                        } else {
                            // routingContext.request().getHeader(paramName)
                            paramHandle = invoke.invokeInterfaceMethod(Methods.REQUEST_GET_HEADER, invoke
                                    .invokeInterfaceMethod(Methods.REQUEST,
                                            routingContext),
                                    invoke.load(paramName));
                            if (paramType.name().equals(io.quarkus.arc.processor.DotNames.OPTIONAL)) {
                                paramHandle = invoke.invokeStaticMethod(Methods.OPTIONAL_OF_NULLABLE, paramHandle);
                            }

                        }
                        return paramHandle;
                    }
                }).build());

        injectors
                .add(ParameterInjector.builder()
                        .matchType(io.quarkus.arc.processor.DotNames.STRING)
                        .matchType(DotNames.BUFFER)
                        .matchType(DotNames.JSON_OBJECT)
                        .matchType(DotNames.JSON_ARRAY)
                        .requireAnnotations(DotNames.BODY)
                        .resultHandleProvider(new ResultHandleProvider() {
                            @Override
                            public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                                    ResultHandle routingContext, MethodCreator invoke, int position,
                                    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                                if (paramType.name().equals(io.quarkus.arc.processor.DotNames.STRING)) {
                                    return invoke.invokeInterfaceMethod(Methods.GET_BODY_AS_STRING, routingContext);
                                } else if (paramType.name().equals(DotNames.BUFFER)) {
                                    return invoke.invokeInterfaceMethod(Methods.GET_BODY, routingContext);
                                } else if (paramType.name().equals(DotNames.JSON_OBJECT)) {
                                    return invoke.invokeInterfaceMethod(Methods.GET_BODY_AS_JSON, routingContext);
                                } else if (paramType.name().equals(DotNames.JSON_ARRAY)) {
                                    return invoke.invokeInterfaceMethod(Methods.GET_BODY_AS_JSON_ARRAY, routingContext);
                                }
                                // This should never happen
                                throw new IllegalArgumentException("Unsupported param type: " + paramType);
                            }
                        }).build());

        injectors
                .add(ParameterInjector.builder()
                        .skipType(io.quarkus.arc.processor.DotNames.STRING)
                        .skipType(DotNames.BUFFER)
                        .skipType(DotNames.JSON_OBJECT)
                        .skipType(DotNames.JSON_ARRAY)
                        .requireAnnotations(DotNames.BODY)
                        .resultHandleProvider(new ResultHandleProvider() {
                            @Override
                            public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                                    ResultHandle routingContext, MethodCreator invoke, int position,
                                    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                                registerForReflection(paramType, reflectiveHierarchy);
                                AssignableResultHandle ret = invoke.createVariable(Object.class);
                                ResultHandle bodyAsJson = invoke.invokeInterfaceMethod(Methods.GET_BODY_AS_JSON,
                                        routingContext);
                                BranchResult bodyIfNotNull = invoke.ifNotNull(bodyAsJson);
                                BytecodeCreator bodyNotNull = bodyIfNotNull.trueBranch();
                                bodyNotNull.assign(ret, bodyNotNull.invokeVirtualMethod(Methods.JSON_OBJECT_MAP_TO,
                                        bodyAsJson,
                                        invoke.loadClass(paramType.name().toString())));
                                BytecodeCreator bodyNull = bodyIfNotNull.falseBranch();
                                bodyNull.assign(ret, bodyNull.loadNull());
                                return ret;
                            }
                        }).build());

        return injectors;
    }

    private static IllegalStateException parameterNameNotAvailable(int position, MethodInfo method) {
        return new IllegalStateException("Unable to determine the name of the parameter at position " + position + " in method "
                + method.declaringClass().name() + "#" + method.name()
                + "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or specify the appropriate annotation value");

    }

    static class ParameterInjector {

        static Builder builder() {
            return new Builder();
        }

        final List<Type> matchTypes;
        final List<Type> skipTypes;
        final List<DotName> requiredAnnotationNames;
        final ResultHandleProvider provider;

        ParameterInjector(ParameterInjector.Builder builder) {
            this.matchTypes = builder.matchTypes;
            this.skipTypes = builder.skipTypes;
            this.requiredAnnotationNames = builder.requiredAnnotationNames;
            this.provider = builder.provider;
        }

        boolean matches(Type paramType, Set<AnnotationInstance> paramAnnotations) {
            // First iterate over all types that should be skipped
            if (skipTypes != null) {
                for (Type skipType : skipTypes) {
                    if (skipType.kind() != paramType.kind()) {
                        continue;
                    }
                    if (skipType.kind() == Kind.CLASS && skipType.name().equals(paramType.name())) {
                        return false;
                    }
                    if (skipType.kind() == Kind.PARAMETERIZED_TYPE && skipType.name().equals(paramType.name())
                            && skipType.asParameterizedType().arguments().equals(paramType.asParameterizedType().arguments())) {
                        return false;
                    }
                }
            }
            // Match any of the specified types
            if (matchTypes != null) {
                boolean matches = false;
                for (Type matchType : matchTypes) {
                    if (matchType.kind() != paramType.kind()) {
                        continue;
                    }
                    if (matchType.kind() == Kind.CLASS && matchType.name().equals(paramType.name())) {
                        matches = true;
                        break;
                    }
                    if (matchType.kind() == Kind.PARAMETERIZED_TYPE && matchType.name().equals(paramType.name())
                            && matchType.asParameterizedType().arguments()
                                    .equals(paramType.asParameterizedType().arguments())) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) {
                    return false;
                }
            }
            // Find required annotations if specified
            if (!requiredAnnotationNames.isEmpty()) {
                for (DotName annotationName : requiredAnnotationNames) {
                    if (!Annotations.contains(paramAnnotations, annotationName)) {
                        return false;
                    }
                }
            }
            return true;
        }

        ResultHandle getResultHandle(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                ResultHandle routingContext, MethodCreator invoke, int position,
                BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
            return provider.get(method, paramType, annotations, routingContext, invoke, position, reflectiveHierarchy);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ParameterInjector [matchTypes=").append(matchTypes).append(", skipTypes=").append(skipTypes)
                    .append(", requiredAnnotationNames=")
                    .append(requiredAnnotationNames).append("]");
            return builder.toString();
        }

        static class Builder {

            List<Type> matchTypes;
            List<Type> skipTypes;
            List<DotName> requiredAnnotationNames = Collections.emptyList();
            ResultHandleProvider provider;

            Builder matchType(DotName className) {
                return matchType(Type.create(className, Kind.CLASS));
            }

            Builder matchType(Type type) {
                if (matchTypes == null) {
                    matchTypes = new ArrayList<>();
                }
                matchTypes.add(type);
                return this;
            }

            Builder skipType(DotName className) {
                return skipType(Type.create(className, Kind.CLASS));
            }

            Builder skipType(Type type) {
                if (skipTypes == null) {
                    skipTypes = new ArrayList<>();
                }
                skipTypes.add(type);
                return this;
            }

            Builder requireAnnotations(DotName... names) {
                this.requiredAnnotationNames = Arrays.asList(names);
                return this;
            }

            Builder resultHandleProvider(ResultHandleProvider provider) {
                this.provider = provider;
                return this;
            }

            ParameterInjector build() {
                return new ParameterInjector(this);
            }

        }

    }

    interface ResultHandleProvider {

        ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                ResultHandle routingContext, MethodCreator invoke, int position,
                BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy);

    }

}
