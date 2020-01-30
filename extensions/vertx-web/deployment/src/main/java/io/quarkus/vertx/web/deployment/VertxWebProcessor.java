package io.quarkus.vertx.web.deployment;

import java.io.IOException;
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
import java.util.function.Function;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.quarkus.vertx.web.RouteFilter;
import io.quarkus.vertx.web.RoutingExchange;
import io.quarkus.vertx.web.runtime.RouteHandler;
import io.quarkus.vertx.web.runtime.RouteMatcher;
import io.quarkus.vertx.web.runtime.RoutingExchangeImpl;
import io.quarkus.vertx.web.runtime.VertxWebRecorder;
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

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.VERTX_WEB);
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
            BeanArchiveIndexBuildItem beanArchive) throws IOException {

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        IndexView index = beanArchive.getIndex();
        Map<RouteMatcher, MethodInfo> matchers = new HashMap<>();

        for (AnnotatedRouteHandlerBuildItem businessMethod : routeHandlerBusinessMethods) {

            String handlerClass = generateHandler(businessMethod.getBean(), businessMethod.getMethod(), classOutput);
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
            String handlerClass = generateHandler(filterMethod.getBean(), filterMethod.getMethod(), classOutput);
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
            Handler<RoutingContext> routingHandler = recorder.createHandler(handlerClass);
            AnnotationValue priorityValue = filterMethod.getRouteFilter().value();
            filterProducer.produce(new FilterBuildItem(routingHandler,
                    priorityValue != null ? priorityValue.asInt() : RouteFilter.DEFAULT_PRIORITY));
        }

        detectConflictingRoutes(matchers);
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (context.getAnnotations().isEmpty() || !BuiltinScope.isIn(context.getAnnotations())) {
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
        if (!method.returnType().kind().equals(Type.Kind.VOID)) {
            throw new IllegalStateException(
                    String.format("Route handler business method must return void [method: %s, bean: %s]", method, bean));
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

    private String generateHandler(BeanInfo bean, MethodInfo method, ClassOutput classOutput) {

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

        // The descriptor is: void invokeBean(Object context)
        MethodCreator invoke = invokerCreator.getMethodCreator("invokeBean", void.class, Object.class);
        // ArcContainer container = Arc.container();
        // InjectableBean<Foo: bean = container.bean("1");
        // InstanceHandle<Foo> handle = container().instance(bean);
        // handle.get().foo(ctx);
        ResultHandle containerHandle = invoke
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle beanHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                containerHandle, invoke.load(bean.getIdentifier()));
        ResultHandle instanceHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, InjectableBean.class),
                containerHandle, beanHandle);
        ResultHandle beanInstanceHandle = invoke
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

        ResultHandle paramHandle;
        MethodDescriptor methodDescriptor;
        if (method.parameters().get(0).name().equals(ROUTING_CONTEXT)) {
            paramHandle = invoke.getMethodParam(0);
            methodDescriptor = MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                    RoutingContext.class);
        } else if (method.parameters().get(0).name().equals(RX_ROUTING_CONTEXT)) {
            paramHandle = invoke.newInstance(
                    MethodDescriptor.ofConstructor(io.vertx.reactivex.ext.web.RoutingContext.class, RoutingContext.class),
                    invoke.getMethodParam(0));
            methodDescriptor = MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                    io.vertx.reactivex.ext.web.RoutingContext.class);
        } else {
            paramHandle = invoke.newInstance(MethodDescriptor.ofConstructor(RoutingExchangeImpl.class, RoutingContext.class),
                    invoke.getMethodParam(0));
            methodDescriptor = MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                    RoutingExchange.class);
        }

        // Invoke the business method handler
        invoke.invokeVirtualMethod(methodDescriptor, beanInstanceHandle, paramHandle);

        // handle.destroy() - destroy dependent instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "destroy", void.class),
                    instanceHandle);
        }
        invoke.returnValue(null);

        invokerCreator.close();
        return generatedName.replace('/', '.');
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
        for (Iterator<Entry<RouteMatcher, MethodInfo>> iterator = matchers.entrySet().iterator(); iterator.hasNext();) {
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
        if (m1.getMethods().length > 0 && m2.getMethods().length > 0 && !Arrays.equals(m1.getMethods(), m2.getMethods())) {
            return false;
        }
        // produces not matching
        if (m1.getProduces().length > 0 && m2.getProduces().length > 0 && !Arrays.equals(m1.getProduces(), m2.getProduces())) {
            return false;
        }
        // consumes not matching
        if (m1.getConsumes().length > 0 && m2.getConsumes().length > 0 && !Arrays.equals(m1.getConsumes(), m2.getConsumes())) {
            return false;
        }
        return true;
    }
}
