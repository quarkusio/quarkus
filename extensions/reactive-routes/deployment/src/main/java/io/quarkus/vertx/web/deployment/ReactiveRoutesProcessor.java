package io.quarkus.vertx.web.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

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
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
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
import io.quarkus.gizmo.WhileLoop;
import io.quarkus.hibernate.validator.spi.BeanValidationAnnotationsBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteFilter;
import io.quarkus.vertx.web.runtime.RouteHandler;
import io.quarkus.vertx.web.runtime.RouteMatcher;
import io.quarkus.vertx.web.runtime.RoutingExchangeImpl;
import io.quarkus.vertx.web.runtime.UniFailureCallback;
import io.quarkus.vertx.web.runtime.VertxWebRecorder;
import io.quarkus.vertx.web.runtime.devmode.ResourceNotFoundRecorder;
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

    private static final List<ParameterInjector> PARAM_INJECTORS = initParamInjectors();

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("[a-zA-Z_0-9]+");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.REACTIVE_ROUTES);
    }

    @BuildStep
    void unremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans
                .produce(UnremovableBeanBuildItem.beanClassAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTE));
        unremovableBeans
                .produce(UnremovableBeanBuildItem.beanClassAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTES));
        unremovableBeans
                .produce(UnremovableBeanBuildItem
                        .beanClassAnnotation(io.quarkus.vertx.web.deployment.DotNames.ROUTE_FILTER));
    }

    @BuildStep
    void validateBeanDeployment(
            BeanArchiveIndexBuildItem beanArchive,
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
                    validateRouteMethod(bean, method, transformedAnnotations, beanArchive.getIndex(), routeAnnotation);
                    routes.add(routeAnnotation);
                }
                if (routes.isEmpty()) {
                    AnnotationInstance routesAnnotation = annotationStore.getAnnotation(method,
                            io.quarkus.vertx.web.deployment.DotNames.ROUTES);
                    if (routesAnnotation != null) {
                        for (AnnotationInstance annotation : routesAnnotation.value().asNestedArray()) {
                            validateRouteMethod(bean, method, transformedAnnotations, beanArchive.getIndex(), annotation);
                            routes.add(annotation);
                        }
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
                return GeneratedClassGizmoAdaptor.isApplicationClass(className);
            }
        };
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, appClassPredicate);
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
                            if (!pathValue.asString().startsWith(SLASH)) {
                                prefixed.append(SLASH);
                            }
                            prefixed.append(pathValue.asString());
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
                    switch (routeHandlerType) {
                        case NORMAL:
                            handlerType = HandlerType.NORMAL;
                            break;
                        case BLOCKING:
                            handlerType = HandlerType.BLOCKING;
                            break;
                        case FAILURE:
                            handlerType = HandlerType.FAILURE;
                            break;
                        default:
                            throw new IllegalStateException("Unknown type " + routeHandlerType);
                    }
                }

                if (businessMethod.getMethod().annotation(DotNames.BLOCKING) != null) {
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
                                    handlerType),
                            businessMethod.getBean(), businessMethod.getMethod(), classOutput, transformedAnnotations,
                            routeString, reflectiveHierarchy, produces.length > 0 ? produces[0] : null,
                            validatorAvailable, index);
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
                    routeHandler = recorder.createHandler(handlerClass);
                    routeHandlers.put(routeString, routeHandler);
                }

                RouteMatcher matcher = new RouteMatcher(path, regex, produces, consumes, methods, order);
                matchers.put(matcher, businessMethod.getMethod());
                Function<Router, io.vertx.ext.web.Route> routeFunction = recorder.createRouteFunction(matcher,
                        bodyHandler.getHandler());

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
                            Arrays.stream(methods).collect(Collectors.joining(", ")), produces,
                            consumes));
                }
            }
        }

        for (AnnotatedRouteFilterBuildItem filterMethod : routeFilterBusinessMethods) {
            String handlerClass = generateHandler(
                    new HandlerDescriptor(filterMethod.getMethod(), beanValidationAnnotations.orElse(null), HandlerType.NORMAL),
                    filterMethod.getBean(), filterMethod.getMethod(), classOutput, transformedAnnotations,
                    filterMethod.getRouteFilter().toString(true), reflectiveHierarchy, null, validatorAvailable, index);
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, handlerClass));
            Handler<RoutingContext> routingHandler = recorder.createHandler(handlerClass);
            AnnotationValue priorityValue = filterMethod.getRouteFilter().value();
            filterProducer.produce(new FilterBuildItem(routingHandler,
                    priorityValue != null ? priorityValue.asInt() : RouteFilter.DEFAULT_PRIORITY));
        }

        detectConflictingRoutes(matchers);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(RUNTIME_INIT)
    void routeNotFound(Capabilities capabilities, ResourceNotFoundRecorder recorder, VertxWebRouterBuildItem router,
            List<RouteDescriptionBuildItem> descriptions,
            HttpRootPathBuildItem httpRoot,
            List<NotFoundPageDisplayableEndpointBuildItem> additionalEndpoints) {
        if (capabilities.isMissing(Capability.RESTEASY)) {
            // Register a special error handler if JAX-RS not available
            recorder.registerNotFoundHandler(router.getHttpRouter(), httpRoot.getRootPath(),
                    descriptions.stream().map(RouteDescriptionBuildItem::getDescription).collect(Collectors.toList()),
                    additionalEndpoints.stream()
                            .map(s -> s.isAbsolutePath() ? s.getEndpoint()
                                    : TemplateHtmlBuilder.adjustRoot(httpRoot.getRootPath(), s.getEndpoint()))
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
        if (params.size() != 1 || !params.get(0).name()
                .equals(io.quarkus.vertx.web.deployment.DotNames.ROUTING_CONTEXT)) {
            throw new IllegalStateException(String.format(
                    "Route filter method must accept exactly one parameter of type %s: %s [method: %s, bean: %s]",
                    io.quarkus.vertx.web.deployment.DotNames.ROUTING_CONTEXT, params, method, bean));
        }
    }

    private void validateRouteMethod(BeanInfo bean, MethodInfo method,
            TransformedAnnotationsBuildItem transformedAnnotations, IndexView index, AnnotationInstance routeAnnotation) {
        List<Type> params = method.parameters();
        if (params.isEmpty()) {
            if (method.returnType().kind() == Kind.VOID && params.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "Route method that returns void must accept at least one injectable parameter [method: %s, bean: %s]",
                        method, bean));
            }
        } else {
            AnnotationValue typeValue = routeAnnotation.value(VALUE_TYPE);
            Route.HandlerType handlerType = typeValue == null ? Route.HandlerType.NORMAL
                    : Route.HandlerType.from(typeValue.asEnum());

            DotName returnTypeName = method.returnType().name();

            if ((returnTypeName.equals(DotNames.UNI)
                    || returnTypeName.equals(DotNames.MULTI)
                    || returnTypeName.equals(DotNames.COMPLETION_STAGE))
                    && method.returnType().kind() == Kind.CLASS) {
                throw new IllegalStateException(
                        String.format(
                                "Route business method returning a Uni/Multi/CompletionStage must have a generic parameter [method: %s, bean: %s]",
                                method, bean));
            }
            boolean canEndResponse = false;
            int idx = 0;
            int failureParams = 0;
            for (Type paramType : params) {
                Set<AnnotationInstance> paramAnnotations = Annotations
                        .getParameterAnnotations(transformedAnnotations, method,
                                idx);
                List<ParameterInjector> injectors = getMatchingInjectors(paramType, paramAnnotations, index);
                if (injectors.isEmpty()) {
                    throw new IllegalStateException(String.format(
                            "No parameter injector found for parameter %s of route method %s declared on %s", idx,
                            method,
                            bean));
                }
                if (injectors.size() > 1) {
                    throw new IllegalStateException(String.format(
                            "Multiple parameter injectors found for parameter %s of route method %s declared on %s",
                            idx,
                            method, bean));
                }
                ParameterInjector injector = injectors.get(0);
                if (injector.getTargetHandlerType() != null
                        && !injector.getTargetHandlerType().equals(handlerType)) {
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

    private String generateHandler(HandlerDescriptor desc, BeanInfo bean, MethodInfo method, ClassOutput classOutput,
            TransformedAnnotationsBuildItem transformedAnnotations, String hashSuffix,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, String defaultProduces,
            boolean validatorAvailable, IndexView index) {

        if (desc.requireValidation() && !validatorAvailable) {
            throw new IllegalStateException(
                    "A route requires validation, but the Hibernate Validator extension is not present");
        }

        String baseName = io.quarkus.arc.processor.DotNames.simpleName(bean.getImplClazz().name());
        String targetPackage = io.quarkus.arc.processor.DotNames
                .internalPackageNameWithTrailingSlash(bean.getImplClazz().name());

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = targetPackage + baseName + HANDLER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString() + hashSuffix);

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(RouteHandler.class).build();

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

        FieldCreator validatorField = null;
        if (desc.isProducedResponseValidated()) {
            // If the produced item needs to be validated, we inject the Validator
            validatorField = invokerCreator.getFieldCreator("validator", Methods.VALIDATION_VALIDATOR)
                    .setModifiers(ACC_PUBLIC | ACC_FINAL);
        }

        implementConstructor(bean, invokerCreator, beanField, contextField, containerField, validatorField);
        implementInvoke(desc, bean, method, invokerCreator, beanField, contextField, containerField, validatorField,
                transformedAnnotations, reflectiveHierarchy, defaultProduces, index);

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    void implementConstructor(BeanInfo bean, ClassCreator invokerCreator, FieldCreator beanField,
            FieldCreator contextField,
            FieldCreator containerField, FieldCreator validatorField) {
        MethodCreator constructor = invokerCreator.getMethodCreator("<init>", void.class);
        // Invoke super()
        constructor.invokeSpecialMethod(Methods.ROUTE_HANDLER_CONSTRUCTOR, constructor.getThis());

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

        if (validatorField != null) {
            constructor.writeInstanceField(validatorField.getFieldDescriptor(), constructor.getThis(),
                    constructor.invokeStaticMethod(Methods.VALIDATION_GET_VALIDATOR, containerHandle));
        }

        constructor.returnValue(null);
    }

    void implementInvoke(HandlerDescriptor descriptor, BeanInfo bean, MethodInfo method, ClassCreator invokerCreator,
            FieldCreator beanField, FieldCreator contextField, FieldCreator containerField, FieldCreator validatorField,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, String defaultProduces, IndexView index) {
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
            invoke.assign(beanInstanceHandle,
                    invoke.invokeInterfaceMethod(Methods.CONTEXT_GET_IF_PRESENT, contextInvokeHandle,
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
            Set<AnnotationInstance> paramAnnotations = Annotations
                    .getParameterAnnotations(transformedAnnotations, method, idx);
            // At this point we can be sure that a matching injector is available
            paramHandles[idx] = getMatchingInjectors(paramType, paramAnnotations, index).get(0)
                    .getResultHandle(method, paramType,
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

        // For failure handlers attempt to match the failure type
        if (descriptor.getHandlerType() == HandlerType.FAILURE) {
            Type failureType = getFailureType(parameters, index);
            if (failureType != null) {
                ResultHandle failure = invoke.invokeInterfaceMethod(Methods.FAILURE, routingContext);
                BytecodeCreator failureIsNull = invoke.ifNull(failure).trueBranch();
                failureIsNull.invokeInterfaceMethod(Methods.NEXT, routingContext);
                failureIsNull.returnValue(null);
                ResultHandle failureClass = invoke.invokeVirtualMethod(Methods.GET_CLASS, failure);
                BytecodeCreator failureTypeIsNotAssignable = invoke
                        .ifFalse(invoke.invokeVirtualMethod(Methods.IS_ASSIGNABLE_FROM,
                                invoke.loadClass(failureType.name().toString()), failureClass))
                        .trueBranch();
                failureTypeIsNotAssignable.invokeInterfaceMethod(Methods.NEXT, routingContext);
                failureTypeIsNotAssignable.returnValue(null);
            }
        }

        // Invoke the business method handler
        AssignableResultHandle res;
        if (descriptor.isReturningUni()) {
            res = invoke.createVariable(Uni.class);
        } else if (descriptor.isReturningMulti()) {
            res = invoke.createVariable(Multi.class);
        } else if (descriptor.isReturningCompletionStage()) {
            res = invoke.createVariable(CompletionStage.class);
        } else {
            res = invoke.createVariable(Object.class);
        }
        invoke.assign(res, invoke.loadNull());
        if (!descriptor.requireValidation()) {
            ResultHandle value = invoke.invokeVirtualMethod(methodDescriptor, beanInstanceHandle, paramHandles);
            if (value != null) {
                invoke.assign(res, value);
            }
        } else {
            TryBlock block = invoke.tryBlock();
            ResultHandle value = block.invokeVirtualMethod(methodDescriptor, beanInstanceHandle, paramHandles);
            if (value != null) {
                block.assign(res, value);
            }
            CatchBlockCreator caught = block.addCatch(Methods.VALIDATION_CONSTRAINT_VIOLATION_EXCEPTION);
            boolean forceJsonEncoding = !descriptor.isContentTypeString() && !descriptor.isContentTypeBuffer()
                    && !descriptor.isContentTypeMutinyBuffer();
            caught.invokeStaticMethod(
                    Methods.VALIDATION_HANDLE_VIOLATION_EXCEPTION,
                    caught.getCaughtException(), invoke.getMethodParam(0), invoke.load(forceJsonEncoding));
            caught.returnValue(caught.loadNull());
        }

        // Get the response: HttpServerResponse response = rc.response()
        MethodDescriptor end = Methods.getEndMethodForContentType(descriptor);
        if (descriptor.isReturningUni()) {
            // The method returns a Uni.
            // We subscribe to this Uni and write the provided item in the HTTP response
            // If the method returned null, we fail
            // If the provided item is null and the method does not return a Uni<Void>, we fail
            // If the provided item is null, and the method return a Uni<Void>, we reply with a 204 - NO CONTENT
            // If the provided item is not null, if it's a string or buffer, the response.end method is used to write the response
            // If the provided item is not null, and it's an object, the item is mapped to JSON and written into the response
            FunctionCreator successCallback = getUniOnItemCallback(descriptor, invoke, routingContext, end, validatorField);
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
            BranchResult isItNdJson = isNotSSE.ifTrue(isNotSSE.invokeStaticMethod(Methods.IS_NDJSON, res));
            BytecodeCreator isNdjson = isItNdJson.trueBranch();
            handleNdjsonMulti(descriptor, isNdjson, routingContext, res);
            isNdjson.close();

            BytecodeCreator isNotNdjson = isItNdJson.falseBranch();
            BranchResult isItJson = isNotNdjson.ifTrue(isNotNdjson.invokeStaticMethod(Methods.IS_JSON_ARRAY, res));
            BytecodeCreator isJson = isItJson.trueBranch();
            handleJsonArrayMulti(descriptor, isJson, routingContext, res);
            isJson.close();

            BytecodeCreator isRegular = isItJson.falseBranch();
            handleRegularMulti(descriptor, isRegular, routingContext, res);
            isRegular.close();
            isNotSSE.close();

            registerForReflection(descriptor.getContentType(), reflectiveHierarchy);
        } else if (descriptor.isReturningCompletionStage()) {
            // The method returns a CompletionStage - we write the provided item in the HTTP response
            // If the method returned null, we fail
            // If the provided item is null and the method does not return a CompletionStage<Void>, we fail
            // If the provided item is null, and the method return a CompletionStage<Void>, we reply with a 204 - NO CONTENT
            // If the provided item is not null, if it's a string or buffer, the response.end method is used to write the response
            // If the provided item is not null, and it's an object, the item is mapped to JSON and written into the response
            ResultHandle consumer = getWhenCompleteCallback(descriptor, invoke, routingContext, end, validatorField)
                    .getInstance();
            invoke.invokeInterfaceMethod(Methods.CS_WHEN_COMPLETE, res, consumer);
            registerForReflection(descriptor.getContentType(), reflectiveHierarchy);

        } else if (descriptor.getContentType() != null) {
            // The method returns "something" in a synchronous manner, write it into the response
            ResultHandle response = invoke.invokeInterfaceMethod(Methods.RESPONSE, routingContext);
            // If the method returned null, we fail
            // If the method returns string or buffer, the response.end method is used to write the response
            // If the method returns an object, the result is mapped to JSON and written into the response

            ResultHandle content = getContentToWrite(descriptor, response, res, invoke, validatorField,
                    invoke.getThis());
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

    private Type getFailureType(List<Type> parameters, IndexView index) {
        for (Type paramType : parameters) {
            if (isThrowable(paramType, index)) {
                return paramType;
            }
        }
        return null;
    }

    private static boolean isThrowable(Type paramType, IndexView index) {
        ClassInfo aClass = index.getClassByName(paramType.name());
        while (aClass != null) {
            if (aClass.superName() == null) {
                break;
            }
            if (DotNames.EXCEPTION.equals(aClass.superName()) || DotNames.THROWABLE.equals(aClass.superName())) {
                return true;
            }
            aClass = index.getClassByName(aClass.superName());
        }
        return false;
    }

    private static final List<DotName> TYPES_IGNORED_FOR_REFLECTION = Arrays
            .asList(io.quarkus.arc.processor.DotNames.STRING,
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
                .source(ReactiveRoutesProcessor.class.getSimpleName() + " > " + contentType)
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
        } else if (descriptor.isContentTypeString()) {
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_STRING, res, rc);
        } else { // Multi<Object> - encode to json.
            writer.invokeStaticMethod(Methods.MULTI_SSE_SUBSCRIBE_OBJECT, res, rc);
        }
    }

    private void handleNdjsonMulti(HandlerDescriptor descriptor, BytecodeCreator writer, ResultHandle rc,
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
        } else if (descriptor.isContentTypeString()) {
            writer.invokeStaticMethod(Methods.MULTI_NDJSON_SUBSCRIBE_STRING, res, rc);
        } else if (descriptor.isContentTypeBuffer() || descriptor.isContentTypeMutinyBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_JSON_FAIL, rc);
        } else { // Multi<Object> - encode to json.
            writer.invokeStaticMethod(Methods.MULTI_NDJSON_SUBSCRIBE_OBJECT, res, rc);
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
        } else if (descriptor.isContentTypeBuffer() || descriptor.isContentTypeMutinyBuffer()) {
            writer.invokeStaticMethod(Methods.MULTI_JSON_FAIL, rc);
        } else { // Multi<Object> - encode to json.
            writer.invokeStaticMethod(Methods.MULTI_JSON_SUBSCRIBE_OBJECT, res, rc);
        }
    }

    /**
     * Generates the following function depending on the payload type
     * <p>
     * If the method returns a {@code Uni<Void>}
     *
     * <pre>
     *     item -> rc.response().setStatusCode(204).end();
     * </pre>
     * <p>
     * If the method returns a {@code Uni<Buffer>}:
     *
     * <pre>
     *     item -> {
     *       if (item != null) {
     *          Buffer buffer = getBuffer(item); // Manage Mutiny buffer
     *          rc.response().end(buffer);
     *       } else {
     *           rc.fail(new NullPointerException(...);
     *       }
     *     }
     * </pre>
     * <p>
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
     * <p>
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
     * <p>
     * This last version also set the {@code content-type} header to {@code application/json }if not set.
     *
     * @param descriptor the method descriptor
     * @param invoke the main bytecode writer
     * @param rc the reference to the routing context variable
     * @param end the end method to use
     * @param response the reference to the response variable
     * @param validatorField the validator field if validation is enabled
     * @return the function creator
     */
    private FunctionCreator getUniOnItemCallback(HandlerDescriptor descriptor, MethodCreator invoke, ResultHandle rc,
            MethodDescriptor end, FieldCreator validatorField) {
        FunctionCreator callback = invoke.createFunction(Consumer.class);
        BytecodeCreator creator = callback.getBytecode();
        ResultHandle response = creator.invokeInterfaceMethod(Methods.RESPONSE, rc);

        if (Methods.isNoContent(descriptor)) { // Uni<Void> - so return a 204.
            creator.invokeInterfaceMethod(Methods.SET_STATUS, response, creator.load(204));
            creator.invokeInterfaceMethod(Methods.END, response);
        } else {
            // Check if the item is null
            ResultHandle item = creator.getMethodParam(0);
            BranchResult isItemNull = creator.ifNull(item);

            BytecodeCreator itemIfNotNull = isItemNull.falseBranch();
            ResultHandle content = getContentToWrite(descriptor, response, item, itemIfNotNull, validatorField,
                    invoke.getThis());
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

    private FunctionCreator getWhenCompleteCallback(HandlerDescriptor descriptor, MethodCreator invoke, ResultHandle rc,
            MethodDescriptor end, FieldCreator validatorField) {
        FunctionCreator callback = invoke.createFunction(BiConsumer.class);
        BytecodeCreator creator = callback.getBytecode();
        ResultHandle response = creator.invokeInterfaceMethod(Methods.RESPONSE, rc);

        ResultHandle throwable = creator.getMethodParam(1);
        BranchResult failureCheck = creator.ifNotNull(throwable);

        BytecodeCreator failure = failureCheck.trueBranch();
        failure.invokeInterfaceMethod(Methods.FAIL, rc, throwable);

        BytecodeCreator success = failureCheck.falseBranch();

        if (Methods.isNoContent(descriptor)) {
            // CompletionStage<Void> - so always return a 204
            success.invokeInterfaceMethod(Methods.SET_STATUS, response, success.load(204));
            success.invokeInterfaceMethod(Methods.END, response);
        } else {
            // First check if the item is null
            ResultHandle item = success.getMethodParam(0);
            BranchResult itemNullCheck = success.ifNull(item);

            BytecodeCreator itemNotNull = itemNullCheck.falseBranch();
            ResultHandle content = getContentToWrite(descriptor, response, item, itemNotNull, validatorField,
                    invoke.getThis());
            itemNotNull.invokeInterfaceMethod(end, response, content);

            BytecodeCreator itemNull = itemNullCheck.trueBranch();
            ResultHandle npe = itemNull.newInstance(MethodDescriptor.ofConstructor(NullPointerException.class, String.class),
                    itemNull.load("Null is not a valid return value for @Route method with return type: "
                            + descriptor.getReturnType()));
            itemNull.invokeInterfaceMethod(Methods.FAIL, rc, npe);
        }
        Methods.returnAndClose(creator);
        return callback;
    }

    private ResultHandle getUniOnFailureCallback(MethodCreator writer, ResultHandle routingContext) {
        return writer.newInstance(MethodDescriptor.ofConstructor(UniFailureCallback.class, RoutingContext.class),
                routingContext);
    }

    private ResultHandle getContentToWrite(HandlerDescriptor descriptor, ResultHandle response, ResultHandle res,
            BytecodeCreator writer, FieldCreator validatorField, ResultHandle owner) {
        if (descriptor.isContentTypeString() || descriptor.isContentTypeBuffer()) {
            return res;
        }

        if (descriptor.isContentTypeMutinyBuffer()) {
            return writer.invokeVirtualMethod(Methods.MUTINY_GET_DELEGATE, res);
        }

        // Encode to Json
        Methods.setContentTypeToJson(response, writer);
        // Validate res if needed
        if (descriptor.isProducedResponseValidated()
                && (descriptor.isReturningUni() || descriptor.isReturningMulti() || descriptor.isReturningCompletionStage())) {
            return Methods.validateProducedItem(response, writer, res, validatorField, owner);
        } else {
            return writer.invokeStaticMethod(Methods.JSON_ENCODE, res);
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

        injectors.add(
                ParameterInjector.builder().canEndResponse().matchType(io.quarkus.vertx.web.deployment.DotNames.ROUTING_CONTEXT)
                        .resultHandleProvider(new ResultHandleProvider() {
                            @Override
                            public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                                    ResultHandle routingContext, MethodCreator invoke, int position,
                                    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                                return routingContext;
                            }
                        }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.ROUTING_EXCHANGE)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke
                                .newInstance(
                                        MethodDescriptor.ofConstructor(RoutingExchangeImpl.class, RoutingContext.class),
                                        routingContext);
                    }
                }).build());

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.HTTP_SERVER_REQUEST)
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

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.HTTP_SERVER_RESPONSE)
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

        injectors.add(ParameterInjector.builder().canEndResponse().matchType(DotNames.MUTINY_HTTP_SERVER_REQUEST)
                .resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke.newInstance(
                                MethodDescriptor
                                        .ofConstructor(io.vertx.mutiny.core.http.HttpServerRequest.class,
                                                HttpServerRequest.class),
                                invoke
                                        .invokeInterfaceMethod(Methods.REQUEST,
                                                routingContext));
                    }
                }).build());

        injectors
                .add(ParameterInjector.builder().canEndResponse().matchType(DotNames.MUTINY_HTTP_SERVER_RESPONSE)
                        .resultHandleProvider(new ResultHandleProvider() {
                            @Override
                            public ResultHandle get(MethodInfo method, Type paramType,
                                    Set<AnnotationInstance> annotations,
                                    ResultHandle routingContext, MethodCreator invoke, int position,
                                    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                                return invoke.newInstance(
                                        MethodDescriptor
                                                .ofConstructor(io.vertx.mutiny.core.http.HttpServerResponse.class,
                                                        HttpServerResponse.class),
                                        invoke
                                                .invokeInterfaceMethod(Methods.RESPONSE,
                                                        routingContext));
                            }
                        }).build());

        injectors.add(ParameterInjector.builder().matchPrimitiveWrappers()
                .matchType(io.quarkus.arc.processor.DotNames.STRING)
                .matchOptionalOf(io.quarkus.arc.processor.DotNames.STRING)
                .matchListOf(io.quarkus.arc.processor.DotNames.STRING)
                .requireAnnotations(DotNames.PARAM)
                .resultHandleProvider(
                        new ParamAndHeaderProvider(DotNames.PARAM, Methods.REQUEST_PARAMS, Methods.REQUEST_GET_PARAM))
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
                                            paramName,
                                            method, bean.getBeanClass()));
                                }
                            }
                        }
                    }
                })
                .build());

        injectors.add(ParameterInjector.builder().matchPrimitiveWrappers()
                .matchType(io.quarkus.arc.processor.DotNames.STRING)
                .matchOptionalOf(io.quarkus.arc.processor.DotNames.STRING)
                .matchListOf(io.quarkus.arc.processor.DotNames.STRING)
                .requireAnnotations(DotNames.HEADER)
                .resultHandleProvider(
                        new ParamAndHeaderProvider(DotNames.HEADER, Methods.REQUEST_HEADERS, Methods.REQUEST_GET_HEADER))
                .build());

        injectors
                .add(ParameterInjector.builder()
                        .matchType(io.quarkus.arc.processor.DotNames.STRING)
                        .matchType(DotNames.BUFFER)
                        .matchType(DotNames.JSON_OBJECT)
                        .matchType(DotNames.JSON_ARRAY)
                        .requireAnnotations(DotNames.BODY)
                        .resultHandleProvider(new ResultHandleProvider() {
                            @Override
                            public ResultHandle get(MethodInfo method, Type paramType,
                                    Set<AnnotationInstance> annotations,
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
                            public ResultHandle get(MethodInfo method, Type paramType,
                                    Set<AnnotationInstance> annotations,
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

        // Add injector for failures
        injectors.add(ParameterInjector.builder().targetHandlerType(Route.HandlerType.FAILURE)
                .match(new TriPredicate<Type, Set<AnnotationInstance>, IndexView>() {
                    @Override
                    public boolean test(Type paramType, Set<AnnotationInstance> paramAnnotations, IndexView index) {
                        return isThrowable(paramType, index) && !Annotations.contains(paramAnnotations, DotNames.BODY);
                    }
                }).resultHandleProvider(new ResultHandleProvider() {
                    @Override
                    public ResultHandle get(MethodInfo method, Type paramType,
                            Set<AnnotationInstance> annotations,
                            ResultHandle routingContext, MethodCreator invoke, int position,
                            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
                        return invoke.invokeInterfaceMethod(Methods.FAILURE, routingContext);
                    }
                }).build());

        return injectors;
    }

    private static IllegalStateException parameterNameNotAvailable(int position, MethodInfo method) {
        return new IllegalStateException(
                "Unable to determine the name of the parameter at position " + position + " in method "
                        + method.declaringClass().name() + "#" + method.name()
                        + "() - compile the class with debug info enabled (-g) or parameter names recorded (-parameters), or specify the appropriate annotation value");

    }

    private static class ParamAndHeaderProvider implements ResultHandleProvider {

        private final DotName annotationName;
        private final MethodDescriptor multiMapAccessor;
        private final MethodDescriptor valueAccessor;

        public ParamAndHeaderProvider(DotName annotationName, MethodDescriptor multiMapAccessor,
                MethodDescriptor valueAccessor) {
            this.annotationName = annotationName;
            this.multiMapAccessor = multiMapAccessor;
            this.valueAccessor = valueAccessor;
        }

        @Override
        public ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                ResultHandle routingContext, MethodCreator invoke, int position,
                BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
            AnnotationValue paramAnnotationValue = Annotations
                    .find(annotations, annotationName).value();
            String paramName = paramAnnotationValue != null ? paramAnnotationValue.asString() : null;
            if (paramName == null || paramName.equals(Param.ELEMENT_NAME)) {
                paramName = method.parameterName(position);
            }
            if (paramName == null) {
                throw parameterNameNotAvailable(position, method);
            }
            AssignableResultHandle paramHandle = invoke.createVariable(Object.class);
            if (paramType.name().equals(DotNames.LIST)) {
                Type wrappedType = paramType.asParameterizedType().arguments().get(0);
                // List<String> params = routingContext.request().params().getAll(paramName)
                invoke.assign(paramHandle, invoke.invokeInterfaceMethod(Methods.MULTIMAP_GET_ALL,
                        invoke.invokeInterfaceMethod(multiMapAccessor, invoke
                                .invokeInterfaceMethod(Methods.REQUEST,
                                        routingContext)),
                        invoke.load(paramName)));
                if (!wrappedType.name()
                        .equals(io.quarkus.arc.processor.DotNames.STRING)) {
                    // Iterate over the list and convert wrapped values
                    ResultHandle results = invoke.newInstance(
                            MethodDescriptor.ofConstructor(ArrayList.class, int.class),
                            invoke.invokeInterfaceMethod(Methods.COLLECTION_SIZE, paramHandle));
                    ResultHandle iterator = invoke.invokeInterfaceMethod(Methods.COLLECTION_ITERATOR, paramHandle);
                    WhileLoop loop = invoke.whileLoop(new Function<BytecodeCreator, BranchResult>() {
                        @Override
                        public BranchResult apply(BytecodeCreator bc) {
                            return bc.ifTrue(bc.invokeInterfaceMethod(Methods.ITERATOR_HAS_NEXT, iterator));
                        }
                    });
                    BytecodeCreator block = loop.block();
                    AssignableResultHandle element = block.createVariable(Object.class);
                    block.assign(element, block.invokeInterfaceMethod(Methods.ITERATOR_NEXT, iterator));
                    convertPrimitiveAndSet(element, wrappedType, block, method, position);
                    block.invokeInterfaceMethod(Methods.COLLECTION_ADD, results, element);
                    invoke.assign(paramHandle, results);
                }
            } else {
                // Object param = routingContext.request().getParam(paramName)
                invoke.assign(paramHandle, invoke.invokeInterfaceMethod(valueAccessor, invoke
                        .invokeInterfaceMethod(Methods.REQUEST,
                                routingContext),
                        invoke.load(paramName)));
                if (paramType.name().equals(io.quarkus.arc.processor.DotNames.OPTIONAL)) {
                    Type wrappedType = paramType.asParameterizedType().arguments().get(0);
                    if (!wrappedType.name()
                            .equals(io.quarkus.arc.processor.DotNames.STRING)) {
                        convertPrimitiveAndSet(paramHandle, wrappedType, invoke, method,
                                position);
                    }
                    invoke.assign(paramHandle,
                            invoke.invokeStaticMethod(Methods.OPTIONAL_OF_NULLABLE, paramHandle));
                } else {
                    if (!paramType.name().equals(io.quarkus.arc.processor.DotNames.STRING)) {
                        convertPrimitiveAndSet(paramHandle, paramType, invoke, method, position);
                    }
                }
            }
            return paramHandle;
        }

    }

    static class ParameterInjector {

        static Builder builder() {
            return new Builder();
        }

        final TriPredicate<Type, Set<AnnotationInstance>, IndexView> predicate;
        final ResultHandleProvider provider;
        final Route.HandlerType targetHandlerType;
        final ParamValidator validator;
        final boolean canEndResponse;

        ParameterInjector(ParameterInjector.Builder builder) {
            if (builder.predicate != null) {
                this.predicate = builder.predicate;
            } else {
                this.predicate = new TriPredicate<Type, Set<AnnotationInstance>, IndexView>() {
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

        ResultHandle getResultHandle(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                ResultHandle routingContext, MethodCreator invoke, int position,
                BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
            return provider.get(method, paramType, annotations, routingContext, invoke, position, reflectiveHierarchy);
        }

        static class Builder {

            // An injector could provide either a custom predicate or match/skip types and required annotations
            TriPredicate<Type, Set<AnnotationInstance>, IndexView> predicate;
            List<Type> matchTypes;
            List<Type> skipTypes;
            List<DotName> requiredAnnotationNames = Collections.emptyList();
            ResultHandleProvider provider;
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
                List<DotName> primitiveNames = Arrays.asList(io.quarkus.arc.processor.DotNames.INTEGER,
                        io.quarkus.arc.processor.DotNames.LONG, io.quarkus.arc.processor.DotNames.SHORT,
                        io.quarkus.arc.processor.DotNames.BOOLEAN, io.quarkus.arc.processor.DotNames.BYTE,
                        io.quarkus.arc.processor.DotNames.CHARACTER, io.quarkus.arc.processor.DotNames.DOUBLE,
                        io.quarkus.arc.processor.DotNames.FLOAT);
                for (DotName name : primitiveNames) {
                    matchType(name);
                    matchOptionalOf(name);
                    matchListOf(name);
                }
                return this;
            }

            Builder matchOptionalOf(DotName className) {
                return matchOptionalOf(Type.create(className, Kind.CLASS));
            }

            Builder matchListOf(DotName className) {
                return matchListOf(Type.create(className, Kind.CLASS));
            }

            Builder matchOptionalOf(Type type) {
                return matchType(ParameterizedType.create(io.quarkus.arc.processor.DotNames.OPTIONAL,
                        new Type[] { type }, null));
            }

            Builder matchListOf(Type type) {
                return matchType(ParameterizedType.create(DotNames.LIST,
                        new Type[] { type }, null));
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

    static void convertPrimitiveAndSet(AssignableResultHandle paramHandle, Type paramType, BytecodeCreator invoke,
            MethodInfo method, int position) {
        // For example:
        // if(param != null) {
        //    try {
        //       param = Long.valueOf(param);
        //    } catch(Throwable e) {
        //       ...
        //    }
        // }
        BytecodeCreator notNull = invoke.ifNotNull(paramHandle).trueBranch();
        TryBlock tryBlock = notNull.tryBlock();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.throwException(IllegalArgumentException.class,
                "Error converting parameter at position " + position + " of method " + method,
                catchBlock.getCaughtException());

        if (paramType.name().equals(io.quarkus.arc.processor.DotNames.INTEGER)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.INTEGER_VALUE_OF,
                    paramHandle));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.LONG)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.LONG_VALUE_OF,
                    paramHandle));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.BOOLEAN)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.BOOLEAN_VALUE_OF,
                    paramHandle));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.CHARACTER)) {
            ResultHandle firstChar = tryBlock.invokeVirtualMethod(Methods.STRING_CHAR_AT, paramHandle, tryBlock.load(0));
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.CHARACTER_VALUE_OF,
                    firstChar));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.FLOAT)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.FLOAT_VALUE_OF,
                    paramHandle));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.DOUBLE)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.DOUBLE_VALUE_OF,
                    paramHandle));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.BYTE)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.BYTE_VALUE_OF,
                    paramHandle));
        } else if (paramType.name().equals(io.quarkus.arc.processor.DotNames.SHORT)) {
            tryBlock.assign(paramHandle, tryBlock.invokeStaticMethod(Methods.SHORT_VALUE_OF,
                    paramHandle));
        } else {
            throw new IllegalArgumentException("Unsupported param type: " + paramType);
        }
    }

    interface ResultHandleProvider {

        ResultHandle get(MethodInfo method, Type paramType, Set<AnnotationInstance> annotations,
                ResultHandle routingContext, MethodCreator invoke, int position,
                BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy);

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
