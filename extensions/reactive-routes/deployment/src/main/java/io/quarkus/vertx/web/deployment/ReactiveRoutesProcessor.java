package io.quarkus.vertx.web.deployment;

import static io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON;
import static io.quarkus.vertx.web.ReactiveRoutes.EVENT_STREAM;
import static io.quarkus.vertx.web.ReactiveRoutes.JSON_STREAM;
import static io.quarkus.vertx.web.ReactiveRoutes.ND_JSON;
import static java.lang.constant.ConstantDescs.CD_Object;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ContextNotActiveException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
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
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.This;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.hibernate.validator.spi.BeanValidationAnnotationsBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.HttpCompression;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteFilter;
import io.quarkus.vertx.web.runtime.RouteHandler;
import io.quarkus.vertx.web.runtime.RouteMatcher;
import io.quarkus.vertx.web.runtime.RoutingExchangeImpl;
import io.quarkus.vertx.web.runtime.UniFailureCallback;
import io.quarkus.vertx.web.runtime.VertxWebRecorder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class ReactiveRoutesProcessor {

    private static final Logger LOGGER = Logger.getLogger(ReactiveRoutesProcessor.class.getName());

    private static final String HANDLER_SUFFIX = "_RouteHandler";
    private static final String VALUE_PATH = "path";
    private static final String VALUE_REGEX = "regex";
    private static final String VALUE_PRODUCES = "produces";
    private static final String VALUE_CONSUMES = "consumes";
    private static final String VALUE_METHODS = "methods";
    private static final String VALUE_ORDER = "order";
    private static final String VALUE_TYPE = "type";
    private static final String SLASH = "/";
    private static final DotName ROLES_ALLOWED = DotName.createSimple(RolesAllowed.class.getName());
    private static final DotName AUTHENTICATED = DotName.createSimple(Authenticated.class.getName());
    private static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    private static final DotName PERMISSIONS_ALLOWED = DotName.createSimple(PermissionsAllowed.class.getName());

    private static final List<ParameterInjector> PARAM_INJECTORS = initParamInjectors();

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("[a-zA-Z_0-9]+");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.REACTIVE_ROUTES);
    }

    @BuildStep
    void unremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassAnnotation(DotNames.ROUTE));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassAnnotation(DotNames.ROUTES));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassAnnotation(DotNames.ROUTE_FILTER));
    }

    @BuildStep
    void validateBeanDeployment(
            BeanArchiveIndexBuildItem beanArchive,
            ValidationPhaseBuildItem validationPhase,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<AnnotatedRouteHandlerBuildItem> routeHandlerBusinessMethods,
            BuildProducer<AnnotatedRouteFilterBuildItem> routeFilterBusinessMethods,
            BuildProducer<ValidationErrorBuildItem> errors,
            VertxHttpBuildTimeConfig httpBuildTimeConfig) {

        // Collect all business methods annotated with @Route and @RouteFilter
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans()) {
            // NOTE: inherited business methods are not taken into account
            ClassInfo beanClass = bean.getTarget().get().asClass();
            AnnotationInstance routeBaseAnnotation = beanClass
                    .declaredAnnotation(DotNames.ROUTE_BASE);
            for (MethodInfo method : beanClass.methods()) {
                if (method.isSynthetic() || Modifier.isStatic(method.flags()) || method.name().equals("<init>")) {
                    continue;
                }

                List<AnnotationInstance> routes = new LinkedList<>();
                AnnotationInstance routeAnnotation = annotationStore.getAnnotation(method,
                        DotNames.ROUTE);
                if (routeAnnotation != null) {
                    validateRouteMethod(bean, method, transformedAnnotations, beanArchive.getIndex(), routeAnnotation);
                    routes.add(routeAnnotation);
                }
                if (routes.isEmpty()) {
                    AnnotationInstance routesAnnotation = annotationStore.getAnnotation(method,
                            DotNames.ROUTES);
                    if (routesAnnotation != null) {
                        for (AnnotationInstance annotation : routesAnnotation.value().asNestedArray()) {
                            validateRouteMethod(bean, method, transformedAnnotations, beanArchive.getIndex(), annotation);
                            routes.add(annotation);
                        }
                    }
                }

                if (!routes.isEmpty()) {
                    LOGGER.debugf("Found route handler business method %s declared on %s", method, bean);

                    HttpCompression compression = HttpCompression.UNDEFINED;
                    if (annotationStore.hasAnnotation(method, DotNames.COMPRESSED)) {
                        compression = HttpCompression.ON;
                    }
                    if (annotationStore.hasAnnotation(method, DotNames.UNCOMPRESSED)) {
                        if (compression == HttpCompression.ON) {
                            errors.produce(new ValidationErrorBuildItem(new IllegalStateException(
                                    String.format(
                                            "@Compressed and @Uncompressed cannot be both declared on business method %s declared on %s",
                                            method, bean))));
                        } else {
                            compression = HttpCompression.OFF;
                        }
                    }

                    // Authenticate user if the proactive authentication is disabled and the route is secured with
                    // an RBAC annotation that requires authentication as io.quarkus.security.runtime.interceptor.SecurityConstrainer
                    // access the SecurityIdentity in a synchronous manner
                    final boolean blocking = annotationStore.hasAnnotation(method, DotNames.BLOCKING);
                    final boolean alwaysAuthenticateRoute;
                    if (!httpBuildTimeConfig.auth().proactive() && !blocking) {
                        final DotName returnTypeName = method.returnType().name();
                        // method either returns 'something' in a synchronous manner or void (in which case we can't tell)
                        final boolean possiblySynchronousResponse = !returnTypeName.equals(DotNames.UNI)
                                && !returnTypeName.equals(DotNames.MULTI) && !returnTypeName.equals(DotNames.COMPLETION_STAGE);
                        final boolean hasRbacAnnotationThatRequiresAuth = annotationStore.hasAnnotation(method, ROLES_ALLOWED)
                                || annotationStore.hasAnnotation(method, AUTHENTICATED)
                                || annotationStore.hasAnnotation(method, PERMISSIONS_ALLOWED)
                                || annotationStore.hasAnnotation(method, DENY_ALL);
                        alwaysAuthenticateRoute = possiblySynchronousResponse && hasRbacAnnotationThatRequiresAuth;
                    } else {
                        alwaysAuthenticateRoute = false;
                    }

                    routeHandlerBusinessMethods
                            .produce(new AnnotatedRouteHandlerBuildItem(bean, method, routes, routeBaseAnnotation,
                                    blocking, compression, alwaysAuthenticateRoute));
                }
                //
                AnnotationInstance filterAnnotation = annotationStore.getAnnotation(method,
                        DotNames.ROUTE_FILTER);
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
    @Record(value = ExecutionTime.STATIC_INIT)
    public void replaceDefaultAuthFailureHandler(VertxWebRecorder recorder, Capabilities capabilities,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer) {
        if (capabilities.isMissing(Capability.RESTEASY_REACTIVE)) {
            // replace default auth failure handler added by vertx-http so that route failure handlers can customize response
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(recorder.addAuthFailureHandler(),
                            SecurityHandlerPriorities.AUTHENTICATION - 1));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void addAdditionalRoutes(
            VertxWebRecorder recorder,
            List<AnnotatedRouteHandlerBuildItem> routeHandlerBusinessMethods,
            List<AnnotatedRouteFilterBuildItem> routeFilterBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
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
            BuildProducer<RouteDescriptionBuildItem> descriptions,
            Capabilities capabilities,
            Optional<BeanValidationAnnotationsBuildItem> beanValidationAnnotations,
            List<ApplicationClassPredicateBuildItem> predicates) {

        Predicate<String> appClassPredicate = new Predicate<String>() {
            @Override
            public boolean test(String name) {
                int idx = name.lastIndexOf(HANDLER_SUFFIX);
                String className = idx != -1 ? name.substring(0, idx) : name;
                for (ApplicationClassPredicateBuildItem i : predicates) {
                    if (i.test(className)) {
                        return true;
                    }
                }
                return GeneratedClassGizmo2Adaptor.isApplicationClass(className);
            }
        };
        Gizmo gizmo = Gizmo.create(new GeneratedClassGizmo2Adaptor(generatedClass, generatedResource, appClassPredicate))
                .withDebugInfo(false)
                .withParameters(false);
        IndexView index = beanArchive.getIndex();
        Map<RouteMatcher, MethodInfo> matchers = new HashMap<>();
        boolean validatorAvailable = capabilities.isPresent(Capability.HIBERNATE_VALIDATOR);

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

                AnnotationValue typeValue = route.value(VALUE_TYPE);
                Route.HandlerType routeHandlerType = typeValue == null ? Route.HandlerType.NORMAL
                        : Route.HandlerType.from(typeValue.asEnum());
                String[] methods = Arrays.stream(methodsValue.asStringArray())
                        .map(String::toUpperCase)
                        .toArray(String[]::new);
                int order = orderValue.asInt();

                if (regexValue == null) {
                    if (pathPrefix != null) {
                        // A path prefix is set
                        StringBuilder prefixed = new StringBuilder();
                        prefixed.append(pathPrefix);
                        if (pathValue == null) {
                            // No path param set - use the method name for non-failure handlers
                            if (routeHandlerType != Route.HandlerType.FAILURE) {
                                prefixed.append(SLASH);
                                prefixed.append(dashify(businessMethod.getMethod().name()));
                            }
                        } else {
                            // Path param set
                            String pathValueStr = pathValue.asString();
                            if (!pathValueStr.isEmpty() && !pathValueStr.startsWith(SLASH)) {
                                prefixed.append(SLASH);
                            }
                            prefixed.append(pathValueStr);
                        }
                        path = prefixed.toString();
                    } else {
                        if (pathValue == null) {
                            // No path param set - use the method name for non-failure handlers
                            if (routeHandlerType != Route.HandlerType.FAILURE) {
                                path = dashify(businessMethod.getMethod().name());
                            }
                        } else {
                            // Path param set
                            path = pathValue.asString();
                        }
                    }
                    if (path != null && !path.startsWith(SLASH)) {
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

                HandlerType handlerType = HandlerType.NORMAL;
                if (routeHandlerType != null) {
                    handlerType = switch (routeHandlerType) {
                        case NORMAL -> HandlerType.NORMAL;
                        case BLOCKING -> HandlerType.BLOCKING;
                        case FAILURE -> HandlerType.FAILURE;
                        default -> throw new IllegalStateException("Unknown type " + routeHandlerType);
                    };
                }

                if (businessMethod.isBlocking()) {
                    if (handlerType == HandlerType.NORMAL) {
                        handlerType = HandlerType.BLOCKING;
                    } else if (handlerType == HandlerType.FAILURE) {
                        throw new IllegalStateException(
                                "Invalid combination - a reactive route cannot use @Blocking and use the type `failure` at the same time: "
                                        + businessMethod.getMethod().toString());
                    }
                }

                if (routeHandler == null) {
                    String handlerClass = generateHandler(
                            new HandlerDescriptor(businessMethod.getMethod(), beanValidationAnnotations.orElse(null),
                                    handlerType == HandlerType.FAILURE, produces),
                            businessMethod.getBean(), businessMethod.getMethod(), gizmo, transformedAnnotations,
                            route, reflectiveHierarchy, produces.length > 0 ? produces[0] : null,
                            validatorAvailable, index);
                    reflectiveClasses
                            .produce(ReflectiveClassBuildItem.builder(handlerClass).build());
                    routeHandler = recorder.createHandler(handlerClass);
                    routeHandlers.put(routeString, routeHandler);
                }

                // Wrap the route handler if necessary
                // Note that route annotations with the same values share a single handler implementation
                routeHandler = recorder.compressRouteHandler(routeHandler, businessMethod.getCompression());
                if (businessMethod.getMethod().hasDeclaredAnnotation(DotNames.RUN_ON_VIRTUAL_THREAD)) {
                    LOGGER.debugf("Route %s#%s() will be executed on a virtual thread",
                            businessMethod.getMethod().declaringClass().name(), businessMethod.getMethod().name());
                    routeHandler = recorder.runOnVirtualThread(routeHandler);
                    // The handler must be executed on the event loop
                    handlerType = HandlerType.NORMAL;
                }

                RouteMatcher matcher = new RouteMatcher(path, regex, produces, consumes, methods, order);
                matchers.put(matcher, businessMethod.getMethod());
                Function<Router, io.vertx.ext.web.Route> routeFunction = recorder.createRouteFunction(matcher,
                        bodyHandler.getHandler(), businessMethod.shouldAlwaysAuthenticateRoute());

                //TODO This needs to be refactored to use routeFunction() taking a Consumer<Route> instead
                RouteBuildItem.Builder builder = RouteBuildItem.builder()
                        .routeFunction(routeFunction)
                        .handlerType(handlerType)
                        .handler(routeHandler);
                routeProducer.produce(builder.build());

                if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)) {
                    if (methods.length == 0) {
                        // No explicit method declared - match all methods
                        methods = Arrays.stream(HttpMethod.values())
                                .map(Enum::name)
                                .toArray(String[]::new);
                    }
                    descriptions.produce(new RouteDescriptionBuildItem(
                            businessMethod.getMethod().declaringClass().name().withoutPackagePrefix() + "#"
                                    + businessMethod.getMethod().name() + "()",
                            regex != null ? regex : path,
                            String.join(", ", methods), produces, consumes));
                }
            }
        }

        for (AnnotatedRouteFilterBuildItem filterMethod : routeFilterBusinessMethods) {
            String handlerClass = generateHandler(
                    new HandlerDescriptor(filterMethod.getMethod(), beanValidationAnnotations.orElse(null), false,
                            new String[0]),
                    filterMethod.getBean(), filterMethod.getMethod(), gizmo, transformedAnnotations,
                    filterMethod.getRouteFilter(), reflectiveHierarchy, null, validatorAvailable, index);
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(handlerClass).build());
            Handler<RoutingContext> routingHandler = recorder.createHandler(handlerClass);
            AnnotationValue priorityValue = filterMethod.getRouteFilter().value();
            filterProducer.produce(new FilterBuildItem(routingHandler,
                    priorityValue != null ? priorityValue.asInt() : RouteFilter.DEFAULT_PRIORITY));
        }

        detectConflictingRoutes(matchers);
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope() {
        return AutoAddScopeBuildItem.builder()
                .containsAnnotations(DotNames.ROUTE,
                        DotNames.ROUTES,
                        DotNames.ROUTE_FILTER)
                .defaultScope(BuiltinScope.SINGLETON)
                .reason("Found route handler business methods").build();
    }

    private void validateRouteFilterMethod(BeanInfo bean, MethodInfo method) {
        if (!method.returnType().kind().equals(Type.Kind.VOID)) {
            throw new IllegalStateException(
                    String.format("Route filter method must return void [method: %s, bean: %s]", method, bean));
        }
        List<Type> params = method.parameterTypes();
        if (params.size() != 1 || !params.get(0).name()
                .equals(DotNames.ROUTING_CONTEXT)) {
            throw new IllegalStateException(String.format(
                    "Route filter method must accept exactly one parameter of type %s: %s [method: %s, bean: %s]",
                    DotNames.ROUTING_CONTEXT, params, method, bean));
        }
    }

    private void validateRouteMethod(BeanInfo bean, MethodInfo method,
            TransformedAnnotationsBuildItem transformedAnnotations, IndexView index, AnnotationInstance routeAnnotation) {
        List<Type> params = method.parameterTypes();
        if (params.isEmpty()) {
            if (method.returnType().kind() == Kind.VOID && params.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "Route method that returns void must accept at least one injectable parameter [method: %s, bean: %s]",
                        method, bean));
            }
        } else {
            AnnotationValue typeValue = routeAnnotation.value(VALUE_TYPE);
            Route.HandlerType handlerType = typeValue == null
                    ? Route.HandlerType.NORMAL
                    : Route.HandlerType.from(typeValue.asEnum());

            DotName returnTypeName = method.returnType().name();

            if ((returnTypeName.equals(DotNames.UNI)
                    || returnTypeName.equals(DotNames.MULTI)
                    || returnTypeName.equals(DotNames.COMPLETION_STAGE))
                    && method.returnType().kind() == Kind.CLASS) {
                throw new IllegalStateException(String.format(
                        "Route business method returning a Uni/Multi/CompletionStage must declare a type argument on the return type [method: %s, bean: %s]",
                        method, bean));
            }
            boolean canEndResponse = false;
            int idx = 0;
            int failureParams = 0;
            for (Type paramType : params) {
                Set<AnnotationInstance> paramAnnotations = Annotations.getParameterAnnotations(transformedAnnotations,
                        method, idx);
                List<ParameterInjector> injectors = getMatchingInjectors(paramType, paramAnnotations, index);
                if (injectors.isEmpty()) {
                    throw new IllegalStateException(String.format(
                            "No parameter injector found for parameter %s of route method %s declared on %s", idx,
                            method, bean));
                }
                if (injectors.size() > 1) {
                    throw new IllegalStateException(String.format(
                            "Multiple parameter injectors found for parameter %s of route method %s declared on %s",
                            idx, method, bean));
                }
                ParameterInjector injector = injectors.get(0);
                if (injector.getTargetHandlerType() != null && !injector.getTargetHandlerType().equals(handlerType)) {
                    throw new IllegalStateException(String.format(
                            "HandlerType.%s is not legal for parameter %s of route method %s declared on %s",
                            injector.getTargetHandlerType(), idx, method, bean));
                }

                // A param injector may validate the parameter annotations
                injector.validate(bean, method, routeAnnotation, paramType, paramAnnotations);

                if (injector.canEndResponse) {
                    canEndResponse = true;
                }

                if (Route.HandlerType.FAILURE == handlerType && isThrowable(paramType, index)) {
                    failureParams++;
                }
                idx++;
            }

            if (method.returnType().kind() == Kind.VOID && !canEndResponse) {
                throw new IllegalStateException(String.format(
                        "Route method that returns void must accept at least one parameter that can end the response [method: %s, bean: %s]",
                        method, bean));
            }

            if (failureParams > 1) {
                throw new IllegalStateException(String.format(
                        "A failure handler may only define one failure parameter - route method %s declared on %s",
                        method, bean));
            }
        }
    }

    private String generateHandler(HandlerDescriptor desc, BeanInfo bean, MethodInfo method, Gizmo gizmo,
            TransformedAnnotationsBuildItem transformedAnnotations, AnnotationInstance routeAnnotation,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, String defaultProduces,
            boolean validatorAvailable, IndexView index) {

        if (desc.requireValidation() && !validatorAvailable) {
            throw new IllegalStateException(
                    "A route requires validation, but the Hibernate Validator extension is not present");
        }

        StringBuilder signature = new StringBuilder();
        signature.append(method.name()).append("_").append(method.returnType().name());
        for (Type parameterType : method.parameterTypes()) {
            signature.append(parameterType.name());
        }
        signature.append(routeAnnotation.toString(true));

        String generatedName = bean.getImplClazz().name().toString() + HANDLER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(signature.toString());

        gizmo.class_(generatedName, cc -> {
            cc.extends_(RouteHandler.class);

            FieldDesc beanField = cc.field("bean", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(InjectableBean.class);
            });

            FieldDesc contextField;
            FieldDesc containerField;
            if (BuiltinScope.APPLICATION.is(bean.getScope()) || BuiltinScope.SINGLETON.is(bean.getScope())) {
                // Singleton and application contexts are always active and unambiguous
                contextField = cc.field("context", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(InjectableContext.class);
                });
                containerField = null;
            } else {
                contextField = null;
                containerField = cc.field("container", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(ArcContainer.class);
                });
            }

            FieldDesc validatorField;
            if (desc.isProducedResponseValidated()) {
                // If the produced item needs to be validated, we inject the Validator
                validatorField = cc.field("validator", fc -> {
                    fc.public_(); // TODO why???
                    fc.final_();
                    fc.setType(Methods.VALIDATION_VALIDATOR);
                });
            } else {
                validatorField = null;
            }

            generateConstructor(cc, bean, beanField, contextField, containerField, validatorField);
            generateInvoke(cc, desc, bean, method, beanField, contextField, containerField, validatorField,
                    transformedAnnotations, reflectiveHierarchy, defaultProduces, index);
        });

        return generatedName;
    }

    void generateConstructor(io.quarkus.gizmo2.creator.ClassCreator cc, BeanInfo btBean, FieldDesc beanField,
            FieldDesc contextField, FieldDesc containerField, FieldDesc validatorField) {
        cc.constructor(mc -> {
            mc.body(bc -> {
                bc.invokeSpecial(Methods.ROUTE_HANDLER_CTOR, cc.this_());

                LocalVar arc = bc.localVar("arc", bc.invokeStatic(Methods.ARC_CONTAINER));
                LocalVar rtBean = bc.localVar("bean",
                        bc.invokeInterface(Methods.ARC_CONTAINER_BEAN, arc, Const.of(btBean.getIdentifier())));
                bc.set(cc.this_().field(beanField), rtBean);
                if (contextField != null) {
                    Expr scope = bc.invokeInterface(Methods.BEAN_GET_SCOPE, rtBean);
                    Expr context = bc.invokeInterface(Methods.ARC_CONTAINER_GET_ACTIVE_CONTEXT, arc, scope);
                    bc.set(cc.this_().field(contextField), context);
                }
                if (containerField != null) {
                    bc.set(cc.this_().field(containerField), arc);
                }
                if (validatorField != null) {
                    bc.set(cc.this_().field(validatorField), bc.invokeStatic(Methods.VALIDATION_GET_VALIDATOR, arc));
                }
                bc.return_();
            });
        });
    }

    void generateInvoke(ClassCreator cc, HandlerDescriptor descriptor, BeanInfo btBean, MethodInfo method,
            FieldDesc beanField, FieldDesc contextField, FieldDesc containerField, FieldDesc validatorField,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, String defaultProduces, IndexView index) {
        cc.method("invoke", mc -> {
            mc.returning(void.class);
            ParamVar routingContext = mc.parameter("routingContext", RoutingContext.class);
            mc.body(b0 -> {
                // For failure handlers attempt to match the failure type
                if (descriptor.isFailureHandler()) {
                    Type failureType = getFailureType(method.parameterTypes(), index);
                    if (failureType != null) {
                        LocalVar failure = b0.localVar("failure", b0.invokeInterface(Methods.FAILURE, routingContext));
                        b0.ifNull(failure, b1 -> {
                            b1.invokeInterface(Methods.NEXT, routingContext);
                            b1.return_();
                        });
                        Expr isAssignable = b0.invokeVirtual(Methods.IS_ASSIGNABLE_FROM, Const.of(classDescOf(failureType)),
                                b0.withObject(failure).getClass_());
                        b0.ifNot(isAssignable, b1 -> {
                            b1.invokeInterface(Methods.NEXT, routingContext);
                            b1.return_();
                        });
                    }
                }

                FieldVar rtBean = cc.this_().field(beanField);
                LocalVar creationalContext = BuiltinScope.DEPENDENT.is(btBean.getScope())
                        ? b0.localVar("cc", b0.new_(Methods.CREATIONAL_CONTEXT_IMPL_CTOR, rtBean))
                        : null;
                LocalVar beanInstance = b0.localVar("beanInstance", b0.blockExpr(CD_Object, b1 -> {
                    if (BuiltinScope.DEPENDENT.is(btBean.getScope())) {
                        b1.yield(b1.invokeInterface(Methods.INJECTABLE_REF_PROVIDER_GET, rtBean, creationalContext));
                    } else {
                        Var context;
                        if (contextField != null) {
                            context = cc.this_().field(contextField);
                        } else {
                            context = b1.localVar("context", b1.invokeInterface(Methods.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                                    cc.this_().field(containerField), b1.invokeInterface(Methods.BEAN_GET_SCOPE, rtBean)));
                            b1.ifNull(context, b2 -> {
                                b2.throw_(ContextNotActiveException.class, "Context not active: "
                                        + btBean.getScope().getDotName());
                            });
                        }

                        // First try to obtain the bean via Context.get(bean)
                        LocalVar tmp = b1.localVar("tmp", b1.invokeInterface(Methods.CONTEXT_GET_IF_PRESENT,
                                context, rtBean));
                        // If not present, try Context.get(bean,creationalContext)
                        b1.ifNull(tmp, b2 -> {
                            b2.set(tmp, b2.invokeInterface(Methods.CONTEXT_GET, context, rtBean,
                                    b2.new_(Methods.CREATIONAL_CONTEXT_IMPL_CTOR, rtBean)));
                        });
                        b1.yield(tmp);
                    }
                }));

                ClassDesc[] params = new ClassDesc[method.parametersCount()];
                LocalVar[] args = new LocalVar[method.parametersCount()];

                int idx = 0;
                for (MethodParameterInfo methodParam : method.parameters()) {
                    Set<AnnotationInstance> paramAnnotations = Annotations.getParameterAnnotations(transformedAnnotations,
                            method, idx);
                    // At this point we can be sure that exactly one matching injector exists
                    args[idx] = getMatchingInjectors(methodParam.type(), paramAnnotations, index).get(0)
                            .getValue(methodParam, paramAnnotations, routingContext, b0, reflectiveHierarchy);
                    params[idx] = classDescOf(methodParam.type());
                    idx++;
                }

                MethodDesc desc = ClassMethodDesc.of(classDescOf(btBean.getImplClazz()), method.name(),
                        MethodTypeDesc.of(classDescOf(descriptor.getReturnType()), params));

                // If no content-type header is set then try to use the most acceptable content type
                // the business method can override this manually if required
                b0.invokeStatic(Methods.ROUTE_HANDLERS_SET_CONTENT_TYPE, routingContext,
                        defaultProduces == null ? Const.ofNull(String.class) : Const.of(defaultProduces));

                // Invoke the business method handler
                LocalVar result;
                if (descriptor.isReturningUni()) {
                    result = b0.localVar("result", Const.ofDefault(Uni.class));
                } else if (descriptor.isReturningMulti()) {
                    result = b0.localVar("result", Const.ofDefault(Multi.class));
                } else if (descriptor.isReturningCompletionStage()) {
                    result = b0.localVar("result", Const.ofDefault(CompletionStage.class));
                } else {
                    result = b0.localVar("result", Const.ofDefault(Object.class));
                }
                if (!descriptor.requireValidation()) {
                    Expr value = b0.invokeVirtual(desc, beanInstance, args);
                    if (!value.isVoid()) {
                        b0.set(result, value);
                    }
                } else {
                    b0.try_(tc -> {
                        tc.body(b1 -> {
                            Expr value = b1.invokeVirtual(desc, beanInstance, args);
                            if (!value.isVoid()) {
                                b1.set(result, value);
                            }
                        });
                        tc.catch_(Methods.VALIDATION_CONSTRAINT_VIOLATION_EXCEPTION, "e", (b1, e) -> {
                            boolean forceJsonEncoding = !descriptor.isPayloadString()
                                    && !descriptor.isPayloadBuffer()
                                    && !descriptor.isPayloadMutinyBuffer();
                            b1.invokeStatic(Methods.VALIDATION_HANDLE_VIOLATION, e, routingContext,
                                    Const.of(forceJsonEncoding));
                            b1.return_();
                        });
                    });
                }

                // Get the response: HttpServerResponse response = rc.response()
                MethodDesc end = Methods.getEndMethodForContentType(descriptor);
                if (descriptor.isReturningUni()) {
                    // The method returns a Uni.
                    // We subscribe to this Uni and write the provided item in the HTTP response
                    // If the method returned null, we fail
                    // If the provided item is null and the method does not return a Uni<Void>, we fail
                    // If the provided item is null, and the method return a Uni<Void>, we reply with a 204 - NO CONTENT
                    // If the provided item is not null, if it's a string or buffer, the response.end method is used to write the response
                    // If the provided item is not null, and it's an object, the item is mapped to JSON and written into the response
                    b0.invokeVirtual(Methods.UNI_SUBSCRIBE_WITH,
                            b0.invokeInterface(Methods.UNI_SUBSCRIBE, result),
                            getUniOnItemCallback(descriptor, b0, routingContext, end, cc.this_(), validatorField),
                            getUniOnFailureCallback(b0, routingContext));
                    registerForReflection(descriptor.getPayloadType(), reflectiveHierarchy);
                } else if (descriptor.isReturningMulti()) {
                    // 3 cases - regular multi vs. sse multi vs. json array multi, we need to check the type.
                    // Let's check if we have a content type and use that one to decide which serialization we need to apply.
                    String contentType = descriptor.getFirstContentType();
                    if (contentType != null) {
                        if (contentType.toLowerCase().startsWith(EVENT_STREAM)) {
                            handleSSEMulti(descriptor, b0, routingContext, result);
                        } else if (contentType.toLowerCase().startsWith(APPLICATION_JSON)) {
                            handleJsonArrayMulti(descriptor, b0, routingContext, result);
                        } else if (contentType.toLowerCase().startsWith(ND_JSON)
                                || contentType.toLowerCase().startsWith(JSON_STREAM)) {
                            handleNdjsonMulti(descriptor, b0, routingContext, result);
                        } else {
                            handleRegularMulti(descriptor, b0, routingContext, result);
                        }
                    } else {
                        // No content type, use the Multi Type - this approach does not work when using Quarkus security
                        // (as it wraps the produced Multi).
                        b0.ifElse(b0.invokeStatic(Methods.IS_SSE, result), b1 -> {
                            handleSSEMulti(descriptor, b1, routingContext, result);
                        }, b1 -> {
                            b1.ifElse(b1.invokeStatic(Methods.IS_NDJSON, result), b2 -> {
                                handleNdjsonMulti(descriptor, b2, routingContext, result);
                            }, b2 -> {
                                b2.ifElse(b2.invokeStatic(Methods.IS_JSON_ARRAY, result), b3 -> {
                                    handleJsonArrayMulti(descriptor, b3, routingContext, result);
                                }, b3 -> {
                                    handleRegularMulti(descriptor, b3, routingContext, result);
                                });
                            });
                        });
                    }
                    registerForReflection(descriptor.getPayloadType(), reflectiveHierarchy);
                } else if (descriptor.isReturningCompletionStage()) {
                    // The method returns a CompletionStage - we write the provided item in the HTTP response
                    // If the method returned null, we fail
                    // If the provided item is null and the method does not return a CompletionStage<Void>, we fail
                    // If the provided item is null, and the method return a CompletionStage<Void>, we reply with a 204 - NO CONTENT
                    // If the provided item is not null, if it's a string or buffer, the response.end method is used to write the response
                    // If the provided item is not null, and it's an object, the item is mapped to JSON and written into the response
                    Expr consumer = getWhenCompleteCallback(descriptor, b0, routingContext, end, cc.this_(), validatorField);
                    b0.invokeInterface(Methods.CS_WHEN_COMPLETE, result, consumer);
                    registerForReflection(descriptor.getPayloadType(), reflectiveHierarchy);
                } else if (descriptor.getPayloadType() != null) {
                    // The method returns "something" in a synchronous manner, write it into the response
                    LocalVar response = b0.localVar("response", b0.invokeInterface(Methods.RESPONSE, routingContext));
                    // If the method returned null, we fail
                    // If the method returns string or buffer, the response.end method is used to write the response
                    // If the method returns an object, the result is mapped to JSON and written into the response
                    Expr content = getContentToWrite(descriptor, response, result, b0, cc.this_(), validatorField);
                    b0.invokeInterface(end, response, content);
                    registerForReflection(descriptor.getPayloadType(), reflectiveHierarchy);
                }

                // Destroy dependent instance afterwards
                if (BuiltinScope.DEPENDENT.is(btBean.getScope())) {
                    b0.invokeInterface(Methods.INJECTABLE_BEAN_DESTROY, rtBean, beanInstance, creationalContext);
                }
                b0.return_();
            });
        });
    }

    private Type getFailureType(List<Type> parameters, IndexView index) {
        for (Type paramType : parameters) {
            if (isThrowable(paramType, index)) {
                return paramType;
            }
        }
        return null;
    }

    private static boolean isThrowable(Type paramType, IndexView index) {
        ClassInfo clazz = index.getClassByName(paramType.name());
        while (clazz != null) {
            if (clazz.superName() == null) {
                break;
            }
            if (DotNames.EXCEPTION.equals(clazz.superName()) || DotNames.THROWABLE.equals(clazz.superName())) {
                return true;
            }
            clazz = index.getClassByName(clazz.superName());
        }
        return false;
    }

    private static final List<DotName> TYPES_IGNORED_FOR_REFLECTION = Arrays.asList(
            DotName.STRING_NAME, DotNames.BUFFER, DotNames.JSON_ARRAY, DotNames.JSON_OBJECT);

    private static void registerForReflection(Type contentType,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
        if (TYPES_IGNORED_FOR_REFLECTION.contains(contentType.name())) {
            return;
        }
        reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                .builder(contentType)
                .ignoreTypePredicate(ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE
                        .or(TYPES_IGNORED_FOR_REFLECTION::contains))
                .source(ReactiveRoutesProcessor.class.getSimpleName() + " > " + contentType)
                .build());
    }

    private void handleRegularMulti(HandlerDescriptor descriptor, BlockCreator bc, Var routingContext,
            Expr res) {
        // The method returns a Multi.
        // We subscribe to this Multi and write the provided items (one by one) in the HTTP response.
        // On completion, we "end" the response
        // If the method returned null, we fail
        // If the provided item is null we fail
        // If the multi is empty, and the method return a Multi<Void>, we reply with a 204 - NO CONTENT
        // If the produce item is a string or buffer, the response.write method is used to write the response
        // If the produce item is an object, the item is mapped to JSON and written into the response. The response is a JSON array.

        if (Methods.isNoContent(descriptor)) { // Multi<Void> - so return a 204.
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_VOID, res, routingContext);
        } else if (descriptor.isPayloadBuffer()) {
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_BUFFER, res, routingContext);
        } else if (descriptor.isPayloadMutinyBuffer()) {
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_MUTINY_BUFFER, res, routingContext);
        } else if (descriptor.isPayloadString()) {
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_STRING, res, routingContext);
        } else { // Multi<Object> - encode to json.
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_OBJECT, res, routingContext);
        }
    }

    private void handleSSEMulti(HandlerDescriptor descriptor, BlockCreator bc, Var routingContext,
            Expr res) {
        // The method returns a Multi that needs to be written as server-sent event.
        // We subscribe to this Multi and write the provided items (one by one) in the HTTP response.
        // On completion, we "end" the response
        // If the method returned null, we fail
        // If the provided item is null we fail
        // If the multi is empty, and the method return a Multi<Void>, we reply with a 204 - NO CONTENT (as regular)
        // If the produced item is a string or buffer, the response.write method is used to write the events in the response
        // If the produced item is an object, the item is mapped to JSON and included in the `data` section of the event.

        if (Methods.isNoContent(descriptor)) { // Multi<Void> - so return a 204.
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_VOID, res, routingContext);
        } else if (descriptor.isPayloadBuffer()) {
            bc.invokeStatic(Methods.MULTI_SSE_SUBSCRIBE_BUFFER, res, routingContext);
        } else if (descriptor.isPayloadMutinyBuffer()) {
            bc.invokeStatic(Methods.MULTI_SSE_SUBSCRIBE_MUTINY_BUFFER, res, routingContext);
        } else if (descriptor.isPayloadString()) {
            bc.invokeStatic(Methods.MULTI_SSE_SUBSCRIBE_STRING, res, routingContext);
        } else { // Multi<Object> - encode to json.
            bc.invokeStatic(Methods.MULTI_SSE_SUBSCRIBE_OBJECT, res, routingContext);
        }
    }

    private void handleNdjsonMulti(HandlerDescriptor descriptor, BlockCreator bc, Var routingContext,
            Expr res) {
        // The method returns a Multi that needs to be written as server-sent event.
        // We subscribe to this Multi and write the provided items (one by one) in the HTTP response.
        // On completion, we "end" the response
        // If the method returned null, we fail
        // If the provided item is null we fail
        // If the multi is empty, and the method return a Multi<Void>, we reply with a 204 - NO CONTENT (as regular)
        // If the produced item is a string or buffer, the response.write method is used to write the events in the response
        // If the produced item is an object, the item is mapped to JSON and included in the `data` section of the event.

        if (Methods.isNoContent(descriptor)) { // Multi<Void> - so return a 204.
            bc.invokeStatic(Methods.MULTI_SUBSCRIBE_VOID, res, routingContext);
        } else if (descriptor.isPayloadString()) {
            bc.invokeStatic(Methods.MULTI_NDJSON_SUBSCRIBE_STRING, res, routingContext);
        } else if (descriptor.isPayloadBuffer() || descriptor.isPayloadMutinyBuffer()) {
            bc.invokeStatic(Methods.MULTI_JSON_FAIL, routingContext);
        } else { // Multi<Object> - encode to json.
            bc.invokeStatic(Methods.MULTI_NDJSON_SUBSCRIBE_OBJECT, res, routingContext);
        }
    }

    private void handleJsonArrayMulti(HandlerDescriptor descriptor, BlockCreator bc, Var routingContext,
            Expr res) {
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
            bc.invokeStatic(Methods.MULTI_JSON_SUBSCRIBE_VOID, res, routingContext);
        } else if (descriptor.isPayloadString()) {
            bc.invokeStatic(Methods.MULTI_JSON_SUBSCRIBE_STRING, res, routingContext);
        } else if (descriptor.isPayloadBuffer() || descriptor.isPayloadMutinyBuffer()) {
            bc.invokeStatic(Methods.MULTI_JSON_FAIL, routingContext);
        } else { // Multi<Object> - encode to json.
            bc.invokeStatic(Methods.MULTI_JSON_SUBSCRIBE_OBJECT, res, routingContext);
        }
    }

    /**
     * Generates the following function depending on the payload type
     * <p>
     * If the method returns a {@code Uni<Void>}
     *
     * <pre>
     *     item -> routingContext.response().setStatusCode(204).end();
     * </pre>
     * <p>
     * If the method returns a {@code Uni<Buffer>}:
     *
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          Buffer buffer = getBuffer(item); // Manage Mutiny buffer
     *          routingContext.response().end(buffer);
     *       } else {
     *           routingContext.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     * <p>
     * If the method returns a {@code Uni<String>} :
     *
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          routingContext.response().end(item);
     *       } else {
     *           routingContext.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     * <p>
     * If the method returns a {@code Uni<T>} :
     *
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          String json = Json.encode(item);
     *          routingContext.response().end(json);
     *       } else {
     *           routingContext.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     * <p>
     * This last version also set the {@code content-type} header to {@code application/json }if not set.
     *
     * @param descriptor the method descriptor
     * @param bc the main bytecode writer
     * @param routingContext the reference to the routing context variable
     * @param end the end method to use
     * @param validatorField the validator field if validation is enabled
     * @return the function creator
     */
    private Expr getUniOnItemCallback(HandlerDescriptor descriptor, BlockCreator bc, Var routingContext,
            MethodDesc end, This this_, FieldDesc validatorField) {
        return bc.lambda(Consumer.class, lc -> {
            Var capturedRoutingContext = lc.capture("routingContext", routingContext);
            Var capturedThis = lc.capture("this_", this_);
            ParamVar item = lc.parameter("item", 0);
            lc.body(lb0 -> {
                LocalVar response = lb0.localVar("response", lb0.invokeInterface(Methods.RESPONSE, capturedRoutingContext));

                if (Methods.isNoContent(descriptor)) { // Uni<Void> - so return a 204.
                    lb0.invokeInterface(Methods.SET_STATUS, response, Const.of(204));
                    lb0.invokeInterface(Methods.END, response);
                } else {
                    // Check if the item is null
                    lb0.ifElse(lb0.isNotNull(item), lb1 -> {
                        Expr content = getContentToWrite(descriptor, response, item, lb1, capturedThis, validatorField);
                        lb1.invokeInterface(end, response, content);
                    }, lb1 -> {
                        lb1.invokeInterface(Methods.FAIL, capturedRoutingContext, Methods.createNpeItemIsNull(lb1));
                    });
                }
                lb0.return_();
            });
        });
    }

    private Expr getUniOnFailureCallback(BlockCreator bc, Var routingContext) {
        return bc.new_(ConstructorDesc.of(UniFailureCallback.class, RoutingContext.class), routingContext);
    }

    private Expr getWhenCompleteCallback(HandlerDescriptor descriptor, BlockCreator bc, Var routingContext,
            MethodDesc end, This this_, FieldDesc validatorField) {
        return bc.lambda(BiConsumer.class, lc -> {
            Var capturedThis = lc.capture("this_", this_);
            Var capturedRoutingContext = lc.capture("routingContext", routingContext);
            ParamVar value = lc.parameter("value", 0);
            ParamVar error = lc.parameter("error", 1);
            lc.body(lb0 -> {
                LocalVar response = lb0.localVar("response", lb0.invokeInterface(Methods.RESPONSE, capturedRoutingContext));
                lb0.ifElse(lb0.isNull(error), lb1 -> {
                    if (Methods.isNoContent(descriptor)) {
                        // CompletionStage<Void> - so always return a 204
                        lb1.invokeInterface(Methods.SET_STATUS, response, Const.of(204));
                        lb1.invokeInterface(Methods.END, response);
                    } else {
                        // First check if the item is null
                        lb1.ifElse(lb1.isNotNull(value), lb2 -> {
                            Expr content = getContentToWrite(descriptor, response, value, lb2, capturedThis, validatorField);
                            lb2.invokeInterface(end, response, content);
                        }, lb2 -> {
                            Expr npe = lb2.new_(ConstructorDesc.of(NullPointerException.class, String.class),
                                    Const.of("Null is not a valid return value for @Route method with return type: "
                                            + descriptor.getReturnType()));
                            lb2.invokeInterface(Methods.FAIL, capturedRoutingContext, npe);
                        });
                    }
                }, lb1 -> {
                    lb1.invokeInterface(Methods.FAIL, capturedRoutingContext, error);
                });
                lb0.return_();
            });
        });
    }

    // `this_` is always either `This` or a captured `Var`
    private Expr getContentToWrite(HandlerDescriptor descriptor, Var response, Var result,
            BlockCreator bc, Expr this_, FieldDesc validatorField) {
        if (descriptor.isPayloadString() || descriptor.isPayloadBuffer()) {
            return result;
        }

        if (descriptor.isPayloadMutinyBuffer()) {
            return bc.invokeVirtual(Methods.MUTINY_GET_DELEGATE, result);
        }

        // Encode to Json
        Methods.setContentTypeToJson(response, bc);
        // Validate result if needed
        if (descriptor.isProducedResponseValidated()
                && (descriptor.isReturningUni() || descriptor.isReturningMulti() || descriptor.isReturningCompletionStage())) {
            return Methods.validateProducedItem(response, bc, result, this_, validatorField);
        } else {
            return bc.invokeStatic(Methods.JSON_ENCODE, result);
        }
    }

    // De-camel-case the name and then join the segments with hyphens
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
        if (m1.getMethods().length > 0 && m2.getMethods().length > 0
                && !Arrays.equals(m1.getMethods(), m2.getMethods())) {
            return false;
        }
        // produces not matching
        if (m1.getProduces().length > 0 && m2.getProduces().length > 0
                && !Arrays.equals(m1.getProduces(), m2.getProduces())) {
            return false;
        }
        // consumes not matching
        if (m1.getConsumes().length > 0 && m2.getConsumes().length > 0
                && !Arrays.equals(m1.getConsumes(), m2.getConsumes())) {
            return false;
        }
        return true;
    }

    private List<ParameterInjector> getMatchingInjectors(Type paramType, Set<AnnotationInstance> paramAnnotations,
            IndexView index) {
        List<ParameterInjector> injectors = new ArrayList<>();
        for (ParameterInjector injector : PARAM_INJECTORS) {
            if (injector.matches(paramType, paramAnnotations, index)) {
                injectors.add(injector);
            }
        }
        return injectors;
    }

    static List<ParameterInjector> initParamInjectors() {
        List<ParameterInjector> injectors = new ArrayList<>();

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.ROUTING_CONTEXT)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return routingContext;
                    }
                }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.ROUTING_EXCHANGE)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return bc.new_(ConstructorDesc.of(RoutingExchangeImpl.class, RoutingContext.class), routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.HTTP_SERVER_REQUEST)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return bc.invokeInterface(Methods.REQUEST, routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.HTTP_SERVER_RESPONSE)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return bc.invokeInterface(Methods.RESPONSE, routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.MUTINY_HTTP_SERVER_REQUEST)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return bc.new_(ConstructorDesc.of(io.vertx.mutiny.core.http.HttpServerRequest.class,
                                HttpServerRequest.class), bc.invokeInterface(Methods.REQUEST, routingContext));
                    }
                }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.MUTINY_HTTP_SERVER_RESPONSE)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return bc.new_(ConstructorDesc.of(io.vertx.mutiny.core.http.HttpServerResponse.class,
                                HttpServerResponse.class), bc.invokeInterface(Methods.RESPONSE, routingContext));
                    }
                }).build());

        injectors.add(ParameterInjector.builder()
                .matchPrimitiveWrappers()
                .matchType(DotName.STRING_NAME)
                .matchOptionalOf(DotName.STRING_NAME)
                .matchListOf(DotName.STRING_NAME)
                .requireAnnotations(DotNames.PARAM)
                .valueProvider(new ParamAndHeaderProvider(DotNames.PARAM, Methods.REQUEST_PARAMS, Methods.REQUEST_GET_PARAM))
                .validate(new ParamValidator() {
                    @Override
                    public void validate(BeanInfo bean, MethodInfo method, AnnotationInstance routeAnnotation, Type paramType,
                            Set<AnnotationInstance> paramAnnotations) {
                        AnnotationInstance paramAnnotation = Annotations.find(paramAnnotations, DotNames.PARAM);
                        AnnotationValue paramNameValue = paramAnnotation.value();
                        if (paramNameValue != null && !paramNameValue.asString().equals(Param.ELEMENT_NAME)) {
                            String paramName = paramNameValue.asString();
                            AnnotationValue regexValue = routeAnnotation.value(VALUE_REGEX);
                            AnnotationValue pathValue = routeAnnotation.value(VALUE_PATH);
                            if (regexValue == null && pathValue != null) {
                                String path = pathValue.asString();
                                // Validate the name if used as a path parameter
                                if (path.contains(":" + paramName) && !PATH_PARAM_PATTERN.matcher(paramName).matches()) {
                                    // TODO This requirement should be relaxed in vertx 4.0.3+
                                    // https://github.com/vert-x3/vertx-web/pull/1881
                                    throw new IllegalStateException(String.format(
                                            "A path param name must only contain word characters (a-zA-Z_0-9): %s [route method %s declared on %s]",
                                            paramName, method, bean.getBeanClass()));
                                }
                            }
                        }
                    }
                })
                .build());

        injectors.add(ParameterInjector.builder()
                .matchPrimitiveWrappers()
                .matchType(DotName.STRING_NAME)
                .matchOptionalOf(DotName.STRING_NAME)
                .matchListOf(DotName.STRING_NAME)
                .requireAnnotations(DotNames.HEADER)
                .valueProvider(new ParamAndHeaderProvider(DotNames.HEADER, Methods.REQUEST_HEADERS, Methods.REQUEST_GET_HEADER))
                .build());

        injectors.add(ParameterInjector.builder()
                .matchType(DotName.STRING_NAME)
                .matchType(DotNames.BUFFER)
                .matchType(DotNames.JSON_OBJECT)
                .matchType(DotNames.JSON_ARRAY)
                .requireAnnotations(DotNames.BODY)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        Type paramType = methodParam.type();
                        if (paramType.name().equals(DotName.STRING_NAME)) {
                            return bc.invokeInterface(Methods.GET_BODY_AS_STRING, routingContext);
                        } else if (paramType.name().equals(DotNames.BUFFER)) {
                            return bc.invokeInterface(Methods.GET_BODY, routingContext);
                        } else if (paramType.name().equals(DotNames.JSON_OBJECT)) {
                            return bc.invokeInterface(Methods.GET_BODY_AS_JSON, routingContext);
                        } else if (paramType.name().equals(DotNames.JSON_ARRAY)) {
                            return bc.invokeInterface(Methods.GET_BODY_AS_JSON_ARRAY, routingContext);
                        }
                        // This should never happen
                        throw new IllegalArgumentException("Unsupported param type: " + paramType);
                    }
                }).build());

        injectors.add(ParameterInjector.builder()
                .skipType(DotName.STRING_NAME)
                .skipType(DotNames.BUFFER)
                .skipType(DotNames.JSON_OBJECT)
                .skipType(DotNames.JSON_ARRAY)
                .requireAnnotations(DotNames.BODY)
                .valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator b0, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        Type paramType = methodParam.type();
                        registerForReflection(paramType, reflectiveHierarchy);
                        LocalVar bodyAsJson = b0.localVar("bodyAsJson",
                                b0.invokeInterface(Methods.GET_BODY_AS_JSON, routingContext));
                        LocalVar result = b0.localVar("result", Const.ofDefault(Object.class));
                        b0.ifNotNull(bodyAsJson, b1 -> {
                            // TODO load `paramType` class from TCCL
                            b1.set(result, b1.invokeVirtual(Methods.JSON_OBJECT_MAP_TO, bodyAsJson,
                                    Const.of(classDescOf(paramType))));
                        });
                        return result;
                    }
                }).build());

        // Add injector for failures
        injectors.add(ParameterInjector.builder().targetHandlerType(Route.HandlerType.FAILURE)
                .match(new TriPredicate<Type, Set<AnnotationInstance>, IndexView>() {
                    @Override
                    public boolean test(Type paramType, Set<AnnotationInstance> paramAnnotations, IndexView index) {
                        return isThrowable(paramType, index) && !Annotations.contains(paramAnnotations, DotNames.BODY);
                    }
                }).valueProvider(new ValueProvider() {
                    @Override
                    public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                            BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        // we can cast here, because we already checked if the failure is assignable to the param
                        // (see the beginning of `generateInvoke()`)
                        return bc.cast(bc.invokeInterface(Methods.FAILURE, routingContext), classDescOf(methodParam.type()));
                    }
                }).build());

        return injectors;
    }

    private static class ParamAndHeaderProvider implements ValueProvider {

        private final DotName annotationName;
        private final MethodDesc multiMapAccessor;
        private final MethodDesc valueAccessor;

        public ParamAndHeaderProvider(DotName annotationName, MethodDesc multiMapAccessor,
                MethodDesc valueAccessor) {
            this.annotationName = annotationName;
            this.multiMapAccessor = multiMapAccessor;
            this.valueAccessor = valueAccessor;
        }

        @Override
        public Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                BlockCreator b0, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
            AnnotationValue paramAnnotationValue = Annotations.find(annotations, annotationName).value();
            String paramName = paramAnnotationValue != null ? paramAnnotationValue.asString() : null;
            if (paramName == null || paramName.equals(Param.ELEMENT_NAME)) {
                paramName = methodParam.name();
            }
            if (paramName == null) {
                throw new IllegalStateException("Unable to determine the name of parameter #" + methodParam.position()
                        + " of " + methodParam.method().declaringClass().name() + "." + methodParam.method().name()
                        + "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or specify the appropriate annotation value");
            }

            Type paramType = methodParam.type();
            LocalVar result = b0.localVar("result", Const.ofDefault(Object.class));
            if (paramType.name().equals(DotNames.LIST)) {
                Type wrappedType = paramType.asParameterizedType().arguments().get(0);
                // List<String> params = routingContext.request().params().getAll(paramName)
                b0.set(result, b0.invokeInterface(Methods.MULTIMAP_GET_ALL,
                        b0.invokeInterface(multiMapAccessor, b0.invokeInterface(Methods.REQUEST, routingContext)),
                        Const.of(paramName)));
                if (!wrappedType.name().equals(DotName.STRING_NAME)) {
                    // Iterate over the list and convert wrapped values
                    LocalVar results = b0.localVar("results",
                            b0.new_(ConstructorDesc.of(ArrayList.class, int.class), b0.withCollection(result).size()));
                    b0.forEach(result, (b1, elem) -> {
                        convertPrimitiveAndSet(elem, wrappedType, b1, methodParam);
                        b1.withCollection(results).add(elem);
                    });
                    b0.set(result, results);
                }
            } else {
                // Object param = routingContext.request().getParam(paramName)
                b0.set(result, b0.invokeInterface(valueAccessor, b0.invokeInterface(Methods.REQUEST, routingContext),
                        Const.of(paramName)));
                if (paramType.name().equals(io.quarkus.arc.processor.DotNames.OPTIONAL)) {
                    Type wrappedType = paramType.asParameterizedType().arguments().get(0);
                    if (!wrappedType.name().equals(DotName.STRING_NAME)) {
                        convertPrimitiveAndSet(result, wrappedType, b0, methodParam);
                    }
                    b0.set(result, b0.invokeStatic(Methods.OPTIONAL_OF_NULLABLE, result));
                } else {
                    if (!paramType.name().equals(DotName.STRING_NAME)) {
                        convertPrimitiveAndSet(result, paramType, b0, methodParam);
                    }
                }
            }
            return result;
        }
    }

    static class ParameterInjector {

        static Builder builder() {
            return new Builder();
        }

        final TriPredicate<Type, Set<AnnotationInstance>, IndexView> predicate;
        final ValueProvider provider;
        final Route.HandlerType targetHandlerType;
        final ParamValidator validator;
        final boolean canEndResponse;

        ParameterInjector(ParameterInjector.Builder builder) {
            if (builder.predicate != null) {
                this.predicate = builder.predicate;
            } else {
                this.predicate = new TriPredicate<>() {
                    final List<Type> matchTypes = builder.matchTypes;
                    final List<Type> skipTypes = builder.skipTypes;
                    final List<DotName> requiredAnnotationNames = builder.requiredAnnotationNames;

                    @Override
                    public boolean test(Type paramType, Set<AnnotationInstance> paramAnnotations, IndexView index) {
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
                                        && skipType.asParameterizedType().arguments()
                                                .equals(paramType.asParameterizedType().arguments())) {
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
                };
            }
            this.provider = builder.provider;
            this.targetHandlerType = builder.targetHandlerType;
            this.validator = builder.validator;
            this.canEndResponse = builder.canEndResponse;
        }

        boolean matches(Type paramType, Set<AnnotationInstance> paramAnnotations, IndexView index) {
            return predicate.test(paramType, paramAnnotations, index);
        }

        Route.HandlerType getTargetHandlerType() {
            return targetHandlerType;
        }

        void validate(BeanInfo bean, MethodInfo method, AnnotationInstance routeInstance, Type paramType,
                Set<AnnotationInstance> paramAnnotations) {
            if (validator != null) {
                validator.validate(bean, method, routeInstance, paramType, paramAnnotations);
            }
        }

        LocalVar getValue(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations,
                Var routingContext, BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
            return bc.localVar("value", provider.get(methodParam, annotations, routingContext, bc, reflectiveHierarchy));
        }

        static class Builder {

            // An injector could provide either a custom predicate or match/skip types and required annotations
            TriPredicate<Type, Set<AnnotationInstance>, IndexView> predicate;
            List<Type> matchTypes;
            List<Type> skipTypes;
            List<DotName> requiredAnnotationNames = Collections.emptyList();
            ValueProvider provider;
            Route.HandlerType targetHandlerType;
            ParamValidator validator;
            boolean canEndResponse;

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

            Builder matchPrimitiveWrappers() {
                List<DotName> primitiveNames = List.of(DotName.BOOLEAN_CLASS_NAME,
                        DotName.BYTE_CLASS_NAME, DotName.SHORT_CLASS_NAME,
                        DotName.INTEGER_CLASS_NAME, DotName.LONG_CLASS_NAME,
                        DotName.FLOAT_CLASS_NAME, DotName.DOUBLE_CLASS_NAME,
                        DotName.CHARACTER_CLASS_NAME);
                for (DotName name : primitiveNames) {
                    matchType(name);
                    matchOptionalOf(name);
                    matchListOf(name);
                }
                return this;
            }

            Builder matchOptionalOf(DotName className) {
                return matchOptionalOf(ClassType.create(className));
            }

            Builder matchListOf(DotName className) {
                return matchListOf(ClassType.create(className));
            }

            Builder matchOptionalOf(Type type) {
                return matchType(ParameterizedType.builder(io.quarkus.arc.processor.DotNames.OPTIONAL)
                        .addArgument(type)
                        .build());
            }

            Builder matchListOf(Type type) {
                return matchType(ParameterizedType.builder(DotNames.LIST).addArgument(type).build());
            }

            Builder skipType(DotName className) {
                return skipType(ClassType.create(className));
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

            Builder valueProvider(ValueProvider provider) {
                this.provider = provider;
                return this;
            }

            Builder match(TriPredicate<Type, Set<AnnotationInstance>, IndexView> predicate) {
                this.predicate = predicate;
                return this;
            }

            Builder targetHandlerType(Route.HandlerType handlerType) {
                this.targetHandlerType = handlerType;
                return this;
            }

            Builder validate(ParamValidator validator) {
                this.validator = validator;
                return this;
            }

            Builder canEndResponse() {
                this.canEndResponse = true;
                return this;
            }

            ParameterInjector build() {
                return new ParameterInjector(this);
            }

        }
    }

    static void convertPrimitiveAndSet(LocalVar param, Type paramType, BlockCreator b0, MethodParameterInfo methodParam) {
        // For example:
        // if (param != null) {
        //    try {
        //       param = Long.valueOf(param);
        //    } catch(Throwable e) {
        //       ...
        //    }
        // }
        b0.ifNotNull(param, b1 -> {
            b1.try_(tc -> {
                tc.body(b2 -> {
                    if (paramType.name().equals(DotName.BOOLEAN_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.BOOLEAN_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.BYTE_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.BYTE_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.SHORT_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.SHORT_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.INTEGER_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.INTEGER_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.LONG_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.LONG_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.FLOAT_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.FLOAT_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.DOUBLE_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.DOUBLE_VALUE_OF, param));
                    } else if (paramType.name().equals(DotName.CHARACTER_CLASS_NAME)) {
                        b2.set(param, b2.invokeStatic(Methods.CHARACTER_VALUE_OF, b2.withString(param).charAt(0)));
                    } else {
                        throw new IllegalArgumentException("Unsupported param type: " + paramType);
                    }
                });
                tc.catch_(Throwable.class, "e", (b2, e) -> {
                    b2.throw_(b2.new_(ConstructorDesc.of(IllegalArgumentException.class, String.class, Throwable.class),
                            Const.of("Error converting parameter #" + methodParam.position() + " of method "
                                    + methodParam.method().declaringClass() + "." + methodParam.method().name() + "()"),
                            e));
                });
            });
        });

    }

    @FunctionalInterface
    interface ValueProvider {
        Expr get(MethodParameterInfo methodParam, Set<AnnotationInstance> annotations, Var routingContext,
                BlockCreator bc, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy);

    }

    @FunctionalInterface
    interface TriPredicate<A, B, C> {
        boolean test(A a, B b, C c);
    }

    @FunctionalInterface
    interface ParamValidator {
        void validate(BeanInfo bean, MethodInfo method, AnnotationInstance routeAnnotation, Type paramType,
                Set<AnnotationInstance> paramAnnotations);
    }

}
