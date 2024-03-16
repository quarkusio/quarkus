package io.quarkus.rest.client.reactive.deployment;

import static io.quarkus.arc.processor.DotNames.STRING;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_BASIC_AUTH;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_FORM_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_FORM_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_HEADER_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_HEADER_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_QUERY_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_QUERY_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_CLIENT_HEADERS;
import static org.jboss.resteasy.reactive.client.impl.RestClientRequestContext.INVOKED_METHOD_PARAMETERS_PROP;
import static org.jboss.resteasy.reactive.client.impl.RestClientRequestContext.INVOKED_METHOD_PROP;
import static org.jboss.resteasy.reactive.common.processor.HashUtil.sha1;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartForm;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ForEachLoop;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.jaxrs.client.reactive.deployment.JaxrsClientReactiveEnricher;
import io.quarkus.rest.client.reactive.ClientFormParam;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.rest.client.reactive.ComputedParamContext;
import io.quarkus.rest.client.reactive.HeaderFiller;
import io.quarkus.rest.client.reactive.deployment.MicroProfileRestClientEnricher.RestClientAnnotationExpressionParser.Node;
import io.quarkus.rest.client.reactive.runtime.BasicAuthUtil;
import io.quarkus.rest.client.reactive.runtime.ClientQueryParamSupport;
import io.quarkus.rest.client.reactive.runtime.ComputedParamContextImpl;
import io.quarkus.rest.client.reactive.runtime.ConfigUtils;
import io.quarkus.rest.client.reactive.runtime.ExtendedHeaderFiller;
import io.quarkus.rest.client.reactive.runtime.HeaderFillerUtil;
import io.quarkus.rest.client.reactive.runtime.MicroProfileRestClientRequestFilter;
import io.quarkus.rest.client.reactive.runtime.NoOpHeaderFiller;
import io.quarkus.runtime.util.HashUtil;

/**
 * Alters client stub generation to add MicroProfile Rest Client features.
 *
 * Used mostly to handle the `@RegisterProvider` annotation that e.g. registers filters
 * and to add support for `@ClientHeaderParam` annotations for specifying (possibly) computed headers via annotations
 */
class MicroProfileRestClientEnricher implements JaxrsClientReactiveEnricher {
    private static final Logger log = Logger.getLogger(MicroProfileRestClientEnricher.class);

    public static final String DEFAULT_HEADERS_FACTORY = DefaultClientHeadersFactoryImpl.class.getName();

    private static final AnnotationInstance[] EMPTY_ANNOTATION_INSTANCES = new AnnotationInstance[0];

    private static final MethodDescriptor INVOCATION_BUILDER_PROPERTY_METHOD = MethodDescriptor.ofMethod(
            Invocation.Builder.class,
            "property", Invocation.Builder.class, String.class, Object.class);
    private static final MethodDescriptor LIST_ADD_METHOD = MethodDescriptor.ofMethod(List.class, "add", boolean.class,
            Object.class);

    private static final MethodDescriptor STRING_BUILDER_APPEND = MethodDescriptor.ofMethod(StringBuilder.class, "append",
            StringBuilder.class,
            String.class);

    private static final MethodDescriptor STRING_LENGTH = MethodDescriptor.ofMethod(String.class, "length", int.class);
    private static final MethodDescriptor MAP_PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class,
            Object.class, Object.class);

    private static final MethodDescriptor HEADER_FILLER_UTIL_SHOULD_ADD_HEADER = MethodDescriptor.ofMethod(
            HeaderFillerUtil.class, "shouldAddHeader",
            boolean.class, String.class, MultivaluedMap.class, ClientRequestContext.class);
    private static final MethodDescriptor WEB_TARGET_IMPL_QUERY_PARAMS = MethodDescriptor.ofMethod(WebTargetImpl.class,
            "queryParam", WebTargetImpl.class, String.class, Collection.class);

    private static final MethodDescriptor ARRAYS_AS_LIST = ofMethod(Arrays.class, "asList", List.class, Object[].class);

    private static final MethodDescriptor COMPUTER_PARAM_CONTEXT_IMPL_CTOR = MethodDescriptor.ofConstructor(
            ComputedParamContextImpl.class, String.class,
            ClientRequestContext.class);

    private static final MethodDescriptor COMPUTER_PARAM_CONTEXT_IMPL_GET_METHOD_PARAM = MethodDescriptor.ofMethod(
            ComputedParamContextImpl.class, "getMethodParameterFromContext", Object.class, ClientRequestContext.class,
            int.class);

    private static final MethodDescriptor MAP_CONTAINS_KEY_METHOD = MethodDescriptor.ofMethod(Map.class,
            "containsKey", boolean.class, Object.class);
    private static final MethodDescriptor MULTIVALUED_MAP_ADD_ALL_METHOD = MethodDescriptor.ofMethod(MultivaluedMap.class,
            "addAll", void.class, Object.class, List.class);
    private static final MethodDescriptor QUARKUS_MULTIPART_FORM_ATTRIBUTE_METHOD = MethodDescriptor.ofMethod(
            ClientMultipartForm.class,
            "attribute", ClientMultipartForm.class, String.class, String.class, String.class);

    private static final Type STRING_TYPE = Type.create(DotName.STRING_NAME, Type.Kind.CLASS);
    private static final MethodDescriptor CONFIG_UTILS_INTERPOLATE = MethodDescriptor.ofMethod(ConfigUtils.class, "interpolate",
            String.class, String.class, boolean.class);

    private final Map<ClassInfo, String> interfaceMocks = new HashMap<>();

    @Override
    public void forClass(MethodCreator constructor, AssignableResultHandle webTargetBase,
            ClassInfo interfaceClass, IndexView index) {

        ResultHandle clientHeadersFactory = null;

        AnnotationInstance registerClientHeaders = interfaceClass.declaredAnnotation(REGISTER_CLIENT_HEADERS);

        if (registerClientHeaders != null) {
            String headersFactoryClass = registerClientHeaders.valueWithDefault(index)
                    .asClass().name().toString();

            if (!headersFactoryClass.equals(DEFAULT_HEADERS_FACTORY)) {
                // Arc.container().instance(...).get():
                ResultHandle containerHandle = constructor
                        .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
                ResultHandle instanceHandle = constructor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                Annotation[].class),
                        containerHandle, constructor.loadClassFromTCCL(headersFactoryClass),
                        constructor.newArray(Annotation.class, 0));
                clientHeadersFactory = constructor
                        .invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                                instanceHandle);
            } else {
                clientHeadersFactory = constructor
                        .newInstance(MethodDescriptor.ofConstructor(DEFAULT_HEADERS_FACTORY));
            }
        } else {
            clientHeadersFactory = constructor.loadNull();
        }

        ResultHandle restClientFilter = constructor.newInstance(
                MethodDescriptor.ofConstructor(MicroProfileRestClientRequestFilter.class, ClientHeadersFactory.class),
                clientHeadersFactory);

        constructor.assign(webTargetBase, constructor.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Configurable.class, "register", Configurable.class, Object.class),
                webTargetBase, restClientFilter));
    }

    @Override
    public void forWebTarget(MethodCreator methodCreator, IndexView index, ClassInfo interfaceClass, MethodInfo method,
            AssignableResultHandle webTarget, BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        Map<String, ParamData> queryParamsByName = new HashMap<>();
        collectClientParamData(interfaceClass, method, queryParamsByName,
                CLIENT_QUERY_PARAM, CLIENT_QUERY_PARAMS, ClientQueryParam.class.getSimpleName());

        for (var queryEntry : queryParamsByName.entrySet()) {
            addQueryParam(method, methodCreator, queryEntry.getValue(), webTarget, generatedClasses, index);
        }
    }

    @Override
    public void forSubResourceWebTarget(MethodCreator methodCreator, IndexView index, ClassInfo rootInterfaceClass,
            ClassInfo subInterfaceClass, MethodInfo rootMethod, MethodInfo subMethod,
            AssignableResultHandle webTarget, BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        Map<String, ParamData> queryParamsByName = new HashMap<>();
        collectClientParamData(rootInterfaceClass, rootMethod, queryParamsByName,
                CLIENT_QUERY_PARAM, CLIENT_QUERY_PARAMS, ClientQueryParam.class.getSimpleName());
        collectClientParamData(subInterfaceClass, subMethod, queryParamsByName,
                CLIENT_QUERY_PARAM, CLIENT_QUERY_PARAMS, ClientQueryParam.class.getSimpleName());

        for (var headerEntry : queryParamsByName.entrySet()) {
            addQueryParam(subMethod, methodCreator, headerEntry.getValue(), webTarget, generatedClasses, index);
        }
    }

    @Override
    public AssignableResultHandle handleFormParams(MethodCreator methodCreator, IndexView index, ClassInfo interfaceClass,
            MethodInfo method, BuildProducer<GeneratedClassBuildItem> generatedClasses, AssignableResultHandle formParams,
            boolean multipart) {

        Map<String, ParamData> formParamsByName = new HashMap<>();
        collectClientParamData(interfaceClass, method, formParamsByName,
                CLIENT_FORM_PARAM, CLIENT_FORM_PARAMS, ClientFormParam.class.getSimpleName());

        if (!formParamsByName.isEmpty() && formParams == null) {
            formParams = createFormData(methodCreator, multipart);
        }

        for (var formEntry : formParamsByName.entrySet()) {
            addFormParam(method, methodCreator, formEntry.getValue(), generatedClasses, index, formParams, multipart);
        }

        return formParams;
    }

    @Override
    public AssignableResultHandle handleFormParamsForSubResource(MethodCreator methodCreator, IndexView index,
            ClassInfo rootInterfaceClass, ClassInfo subInterfaceClass, MethodInfo rootMethod, MethodInfo subMethod,
            AssignableResultHandle webTarget, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            AssignableResultHandle formParams, boolean multipart) {

        Map<String, ParamData> formParamsByName = new HashMap<>();
        collectClientParamData(rootInterfaceClass, rootMethod, formParamsByName,
                CLIENT_FORM_PARAM, CLIENT_FORM_PARAMS, ClientFormParam.class.getSimpleName());
        collectClientParamData(subInterfaceClass, subMethod, formParamsByName,
                CLIENT_FORM_PARAM, CLIENT_FORM_PARAMS, ClientFormParam.class.getSimpleName());

        if (!formParamsByName.isEmpty() && formParams == null) {
            formParams = createFormData(methodCreator, multipart);
        }

        for (var formEntry : formParamsByName.entrySet()) {
            addFormParam(subMethod, methodCreator, formEntry.getValue(), generatedClasses, index, formParams, multipart);
        }

        return formParams;
    }

    private AssignableResultHandle createFormData(BytecodeCreator methodCreator, boolean multipart) {
        AssignableResultHandle formParams;
        if (multipart) {
            formParams = methodCreator.createVariable(QuarkusMultipartForm.class);
            methodCreator.assign(formParams,
                    methodCreator.newInstance(MethodDescriptor.ofConstructor(QuarkusMultipartForm.class)));
        } else {
            formParams = methodCreator.createVariable(MultivaluedMap.class);
            methodCreator.assign(formParams,
                    methodCreator.newInstance(MethodDescriptor.ofConstructor(MultivaluedHashMap.class)));
        }
        return formParams;
    }

    private void addQueryParam(MethodInfo declaringMethod, MethodCreator methodCreator,
            ParamData paramData, AssignableResultHandle webTargetImpl,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, IndexView index) {

        String paramName = paramData.annotation.value("name").asString();

        Supplier<ResultHandle> existenceChecker = () -> methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(ClientQueryParamSupport.class, "isQueryParamPresent", boolean.class,
                        WebTargetImpl.class, String.class),
                webTargetImpl, methodCreator.load(paramName));

        BiConsumer<BytecodeCreator, ResultHandle> paramAdder = (creator, valuesList) -> creator.assign(webTargetImpl,
                creator.invokeVirtualMethod(WEB_TARGET_IMPL_QUERY_PARAMS, webTargetImpl, methodCreator.load(paramName),
                        valuesList));

        addParam(declaringMethod, methodCreator, paramData, generatedClasses, index, CLIENT_QUERY_PARAM,
                ClientQueryParam.class.getSimpleName(), paramName, existenceChecker, paramAdder);
    }

    private void addFormParam(MethodInfo declaringMethod, MethodCreator methodCreator,
            ParamData paramData, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            IndexView index, AssignableResultHandle formParams, boolean multipart) {

        String paramName = paramData.annotation.value("name").asString();

        Supplier<ResultHandle> existenceChecker = () -> methodCreator.invokeInterfaceMethod(MAP_CONTAINS_KEY_METHOD,
                formParams, methodCreator.load(paramName));

        BiConsumer<BytecodeCreator, ResultHandle> paramAdder = (creator, valuesList) -> {
            if (multipart) {
                String filename = null;
                AnnotationInstance partFileName = declaringMethod.annotation(ResteasyReactiveDotNames.PART_FILE_NAME);
                if (partFileName != null && partFileName.value() != null) {
                    filename = partFileName.value().asString();
                }

                ForEachLoop loop = creator.forEach(valuesList);
                BytecodeCreator block = loop.block();

                block.invokeVirtualMethod(QUARKUS_MULTIPART_FORM_ATTRIBUTE_METHOD, formParams, block.load(paramName),
                        loop.element(), block.load(filename));
            } else {
                creator.invokeInterfaceMethod(
                        MULTIVALUED_MAP_ADD_ALL_METHOD, formParams, creator.load(paramName), valuesList);
            }
        };

        addParam(declaringMethod, methodCreator, paramData, generatedClasses, index, CLIENT_FORM_PARAM,
                ClientFormParam.class.getSimpleName(), paramName, existenceChecker, paramAdder);
    }

    private void collectClientParamData(ClassInfo interfaceClass, MethodInfo method,
            Map<String, ParamData> paramFillersByName,
            DotName clientParamAnnotation, DotName clientParamsAnnotation,
            String annotationName) {
        AnnotationInstance classLevelParam = interfaceClass.declaredAnnotation(clientParamAnnotation);
        if (classLevelParam != null) {
            paramFillersByName.put(classLevelParam.value("name").asString(),
                    new ParamData(classLevelParam, interfaceClass));
        }
        putAllParamAnnotations(paramFillersByName,
                interfaceClass,
                extractAnnotations(interfaceClass.declaredAnnotation(clientParamsAnnotation)), annotationName);

        Map<String, ParamData> methodLevelParamsByName = new HashMap<>();
        AnnotationInstance methodLevelParam = method.annotation(clientParamAnnotation);
        if (methodLevelParam != null) {
            methodLevelParamsByName.put(methodLevelParam.value("name").asString(),
                    new ParamData(methodLevelParam, interfaceClass));
        }
        putAllParamAnnotations(methodLevelParamsByName, interfaceClass,
                extractAnnotations(method.annotation(clientParamsAnnotation)), annotationName);

        paramFillersByName.putAll(methodLevelParamsByName);
    }

    private void putAllParamAnnotations(Map<String, ParamData> paramMap, ClassInfo interfaceClass,
            AnnotationInstance[] annotations, String annotationName) {
        for (AnnotationInstance annotation : annotations) {
            String name = annotation.value("name").asString();
            if (paramMap.put(name, new ParamData(annotation, interfaceClass)) != null) {
                throw new RestClientDefinitionException("Duplicate " + annotationName + " annotation for parameter: " + name +
                        " on " + annotation.target());
            }
        }
    }

    private void addParam(MethodInfo declaringMethod, MethodCreator methodCreator,
            ParamData paramData, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            IndexView index, DotName clientParamAnnotation, String annotationName, String paramName,
            Supplier<ResultHandle> existenceChecker,
            BiConsumer<BytecodeCreator, ResultHandle> paramAdder) {

        AnnotationInstance annotation = paramData.annotation;
        ClassInfo declaringClass = paramData.definingClass;

        ResultHandle isParamPresent = existenceChecker.get();
        BytecodeCreator creator = methodCreator.ifTrue(isParamPresent).falseBranch();

        String[] values = annotation.value().asStringArray();

        if (values.length == 0) {
            log.warnv("Ignoring {} that specifies an empty array of values for parameter {} on {}",
                    annotationName, annotation.value("name").asString(), annotation.target());
            return;
        }

        if (values.length > 1 || !(values[0].startsWith("{") && values[0].endsWith("}"))) {
            boolean required = annotation.valueWithDefault(index, "required").asBoolean();
            ResultHandle valuesList = creator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
            for (String value : values) {
                if (value.contains("${")) {
                    ResultHandle paramValueFromConfig = creator.invokeStaticMethod(
                            CONFIG_UTILS_INTERPOLATE,
                            creator.load(value), creator.load(required));
                    creator.ifNotNull(paramValueFromConfig)
                            .trueBranch().invokeInterfaceMethod(LIST_ADD_METHOD, valuesList, paramValueFromConfig);
                } else {
                    creator.invokeInterfaceMethod(LIST_ADD_METHOD, valuesList, creator.load(value));
                }
            }

            paramAdder.accept(creator, valuesList);
        } else { // method call :O {some.package.ClassName.methodName} or {defaultMethodWithinThisInterfaceName}
            // if `!required` an exception on param filling does not fail the invocation:
            boolean required = annotation.valueWithDefault(index, "required").asBoolean();

            BytecodeCreator methodCallCreator = creator;
            TryBlock tryBlock = null;

            if (!required) {
                tryBlock = creator.tryBlock();
                methodCallCreator = tryBlock;
            }
            String methodName = values[0].substring(1, values[0].length() - 1); // strip curly braces

            MethodInfo paramValueMethod;
            ResultHandle paramValue;
            if (methodName.contains(".")) {
                // calling a static method
                int endOfClassName = methodName.lastIndexOf('.');
                String className = methodName.substring(0, endOfClassName);
                String staticMethodName = methodName.substring(endOfClassName + 1);

                ClassInfo clazz = index.getClassByName(DotName.createSimple(className));
                if (clazz == null) {
                    throw new RestClientDefinitionException(
                            "Class " + className + " used in " + annotationName + " on " + declaringClass + " not found");
                }
                paramValueMethod = findMethod(clazz, declaringClass, staticMethodName, clientParamAnnotation.toString());

                if (paramValueMethod.parametersCount() == 0) {
                    paramValue = methodCallCreator.invokeStaticMethod(paramValueMethod);
                } else if (paramValueMethod.parametersCount() == 1 && isString(paramValueMethod.parameterType(0))) {
                    paramValue = methodCallCreator.invokeStaticMethod(paramValueMethod, methodCallCreator.load(paramName));
                } else {
                    throw new RestClientDefinitionException(
                            annotationName + " method " + declaringClass.toString() + "#" + staticMethodName
                                    + " has too many parameters, at most one parameter, param name, expected");
                }
            } else {
                // interface method
                String mockName = mockInterface(declaringClass, generatedClasses, index);
                ResultHandle interfaceMock = methodCallCreator.newInstance(MethodDescriptor.ofConstructor(mockName));

                paramValueMethod = findMethod(declaringClass, declaringClass, methodName, clientParamAnnotation.toString());

                if (paramValueMethod == null) {
                    throw new RestClientDefinitionException(
                            annotationName + " method " + methodName + " not found on " + declaringClass);
                }

                if (paramValueMethod.parametersCount() == 0) {
                    paramValue = methodCallCreator.invokeInterfaceMethod(paramValueMethod, interfaceMock);
                } else if (paramValueMethod.parametersCount() == 1 && isString(paramValueMethod.parameterType(0))) {
                    paramValue = methodCallCreator.invokeInterfaceMethod(paramValueMethod, interfaceMock,
                            methodCallCreator.load(paramName));
                } else {
                    throw new RestClientDefinitionException(
                            annotationName + " method " + declaringClass + "#" + methodName
                                    + " has too many parameters, at most one parameter, param name, expected");
                }

            }

            Type returnType = paramValueMethod.returnType();
            ResultHandle valuesList;
            if (isStringArray(returnType)) {
                // repack array to list
                valuesList = methodCallCreator.invokeStaticMethod(ARRAYS_AS_LIST, paramValue);
            } else if (isString(returnType)) {
                valuesList = methodCallCreator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                methodCallCreator.invokeInterfaceMethod(LIST_ADD_METHOD, valuesList, paramValue);
            } else {
                throw new RestClientDefinitionException("Method " + declaringClass.toString() + "#" + methodName
                        + " has an unsupported return type for " + annotationName + ". " +
                        "Only String and String[] return types are supported");
            }

            paramAdder.accept(methodCallCreator, valuesList);

            if (!required) {
                CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
                ResultHandle log = catchBlock.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Logger.class, "getLogger", Logger.class, String.class),
                        catchBlock.load(declaringClass.name().toString()));
                String errorMessage = String.format(
                        "Invoking param generation method '%s' for '%s' on method '%s#%s' failed",
                        methodName, paramName, declaringClass.name(), declaringMethod.name());
                catchBlock.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Logger.class, "warn", void.class, Object.class, Throwable.class),
                        log,
                        catchBlock.load(errorMessage), catchBlock.getCaughtException());
            }
        }
    }

    @Override
    public void forSubResourceMethod(ClassCreator subClassCreator, MethodCreator subConstructor,
            MethodCreator subClinit, MethodCreator subMethodCreator, ClassInfo rootInterfaceClass,
            ClassInfo subInterfaceClass, MethodInfo subMethod, MethodInfo rootMethod,
            AssignableResultHandle invocationBuilder, // sub-level
            IndexView index, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            int methodIndex, int subMethodIndex, FieldDescriptor javaMethodField) {

        addJavaMethodToContext(javaMethodField, subMethodCreator, invocationBuilder);

        Map<String, ParamData> headerFillersByName = new HashMap<>();
        collectHeaderFillers(rootInterfaceClass, rootMethod, headerFillersByName);
        collectHeaderFillers(subInterfaceClass, subMethod, headerFillersByName);
        String subHeaderFillerName = subInterfaceClass.name().toString() + sha1(rootInterfaceClass.name().toString()) +
                "$$" + methodIndex + "$$" + subMethodIndex;
        createAndReturnHeaderFiller(subClassCreator, subConstructor, subMethodCreator, subMethod,
                invocationBuilder, index, generatedClasses, subMethodIndex, subHeaderFillerName, headerFillersByName,
                Collections.emptyList());
    }

    @Override
    public void forMethod(ClassCreator classCreator, MethodCreator constructor,
            MethodCreator clinit, MethodCreator methodCreator, ClassInfo interfaceClass,
            MethodInfo method, AssignableResultHandle invocationBuilder, IndexView index,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, int methodIndex, FieldDescriptor javaMethodField) {

        addJavaMethodToContext(javaMethodField, methodCreator, invocationBuilder);

        // header filler

        Map<String, ParamData> headerFillersByName = new HashMap<>();

        collectHeaderFillers(interfaceClass, method, headerFillersByName);

        AnnotationInstance clientBasicAuth = interfaceClass.declaredAnnotation(CLIENT_BASIC_AUTH);
        List<AddHeadersEnhancer> enhancers = new ArrayList<>();
        if (clientBasicAuth != null) {
            enhancers.add(new BasicAuthAddHeadersEnhancer(clientBasicAuth.value("username").asString(),
                    clientBasicAuth.value("password").asString()));
        }

        createAndReturnHeaderFiller(classCreator, constructor, methodCreator, method,
                invocationBuilder, index, generatedClasses, methodIndex,
                interfaceClass + "$$" + method.name() + "$$" + methodIndex, headerFillersByName, enhancers);
    }

    private interface AddHeadersEnhancer extends Consumer<AddHeadersEnhancer.Context> {

        interface Context {
            MethodCreator addHeadersMethodCreator();

            ResultHandle headersMapHandle();

            ResultHandle requestContextHandle();
        }
    }

    private static class BasicAuthAddHeadersEnhancer implements AddHeadersEnhancer {

        public static final MethodDescriptor BASIC_AUTH_UTIL_CREATE = MethodDescriptor.ofMethod(BasicAuthUtil.class,
                "headerValue", String.class, String.class, String.class);
        private final String username;
        private final String password;

        public BasicAuthAddHeadersEnhancer(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void accept(AddHeadersEnhancer.Context context) {
            MethodCreator mc = context.addHeadersMethodCreator();

            ResultHandle authHeaderHandle = mc.load("Authorization");
            BytecodeCreator shouldAddMc = mc
                    .ifTrue(mc.invokeStaticMethod(HEADER_FILLER_UTIL_SHOULD_ADD_HEADER,
                            authHeaderHandle, context.headersMapHandle(), context.requestContextHandle()))
                    .trueBranch();

            ResultHandle usernameHandle = shouldAddMc.load(username);
            if (username.contains("${")) {
                usernameHandle = shouldAddMc.invokeStaticMethod(CONFIG_UTILS_INTERPOLATE, usernameHandle,
                        shouldAddMc.load(true));
            }

            ResultHandle passwordHandle = shouldAddMc.load(password);
            if (password.contains("${")) {
                passwordHandle = shouldAddMc.invokeStaticMethod(CONFIG_UTILS_INTERPOLATE, passwordHandle,
                        shouldAddMc.load(true));
            }

            ResultHandle headerValueHandle = shouldAddMc.invokeStaticMethod(BASIC_AUTH_UTIL_CREATE, usernameHandle,
                    passwordHandle);
            ResultHandle headerListHandler = Gizmo.newArrayList(shouldAddMc, 1);
            shouldAddMc.invokeInterfaceMethod(LIST_ADD_METHOD, headerListHandler, headerValueHandle);
            shouldAddMc.invokeInterfaceMethod(MAP_PUT_METHOD, context.headersMapHandle(), authHeaderHandle, headerListHandler);
        }
    }

    private void createAndReturnHeaderFiller(ClassCreator classCreator, MethodCreator constructor,
            MethodCreator methodCreator, MethodInfo method,
            AssignableResultHandle invocationBuilder, IndexView index,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, int methodIndex, String fillerClassName,
            Map<String, ParamData> headerFillersByName,
            List<AddHeadersEnhancer> addHeadersEnhancers) {
        FieldDescriptor headerFillerField = FieldDescriptor.of(classCreator.getClassName(),
                "headerFiller" + methodIndex, HeaderFiller.class);
        classCreator.getFieldCreator(headerFillerField).setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        ResultHandle headerFiller;
        // create header filler for this method if headerFillersByName is not empty
        if (!headerFillersByName.isEmpty() || !addHeadersEnhancers.isEmpty()) {
            GeneratedClassGizmoAdaptor classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
            try (ClassCreator headerFillerClass = ClassCreator.builder().className(fillerClassName)
                    .interfaces(ExtendedHeaderFiller.class)
                    .classOutput(classOutput)
                    .build()) {
                FieldCreator logField = headerFillerClass.getFieldCreator("log", Logger.class);
                logField.setModifiers(Modifier.FINAL | Modifier.STATIC | Modifier.PRIVATE);

                MethodCreator staticConstructor = headerFillerClass.getMethodCreator("<clinit>", void.class);
                staticConstructor.setModifiers(ACC_STATIC);
                ResultHandle log = staticConstructor.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Logger.class, "getLogger", Logger.class, String.class),
                        staticConstructor.load(fillerClassName));
                staticConstructor.writeStaticField(logField.getFieldDescriptor(), log);
                staticConstructor.returnValue(null);

                MethodCreator addHeaders = headerFillerClass
                        .getMethodCreator(
                                MethodDescriptor.ofMethod(HeaderFiller.class, "addHeaders", void.class,
                                        MultivaluedMap.class, ResteasyReactiveClientRequestContext.class));

                for (Map.Entry<String, ParamData> headerEntry : headerFillersByName.entrySet()) {
                    addHeaderParam(method, addHeaders, headerEntry.getValue(), generatedClasses,
                            fillerClassName, index);
                }
                if (!addHeadersEnhancers.isEmpty()) {
                    var context = new AddHeadersEnhancer.Context() {
                        @Override
                        public MethodCreator addHeadersMethodCreator() {
                            return addHeaders;
                        }

                        @Override
                        public ResultHandle headersMapHandle() {
                            return addHeaders.getMethodParam(0);
                        }

                        @Override
                        public ResultHandle requestContextHandle() {
                            return addHeaders.getMethodParam(1);
                        }
                    };
                    for (AddHeadersEnhancer enhancer : addHeadersEnhancers) {
                        enhancer.accept(context);
                    }
                }

                addHeaders.returnValue(null);

                headerFiller = constructor.newInstance(MethodDescriptor.ofConstructor(fillerClassName));
            }
        } else {
            headerFiller = constructor
                    .readStaticField(FieldDescriptor.of(NoOpHeaderFiller.class, "INSTANCE", NoOpHeaderFiller.class));
        }
        constructor.writeInstanceField(headerFillerField, constructor.getThis(), headerFiller);

        ResultHandle headerFillerAsObject = methodCreator.checkCast(
                methodCreator.readInstanceField(headerFillerField, methodCreator.getThis()), Object.class);
        methodCreator.assign(invocationBuilder,
                methodCreator.invokeInterfaceMethod(INVOCATION_BUILDER_PROPERTY_METHOD, invocationBuilder,
                        methodCreator.load(HeaderFiller.class.getName()), headerFillerAsObject));

        ResultHandle parametersList = null;
        if (method.parametersCount() == 0) {
            parametersList = methodCreator.invokeStaticMethod(ofMethod(
                    Collections.class, "emptyList", List.class));
        } else {
            ResultHandle parametersArray = methodCreator.newArray(Object.class,
                    method.parametersCount());
            for (int i = 0; i < method.parametersCount(); i++) {
                methodCreator.writeArrayValue(parametersArray, i, methodCreator.getMethodParam(i));
            }
            parametersList = methodCreator.invokeStaticMethod(
                    ARRAYS_AS_LIST, parametersArray);
        }
        methodCreator.assign(invocationBuilder,
                methodCreator.invokeInterfaceMethod(INVOCATION_BUILDER_PROPERTY_METHOD, invocationBuilder,
                        methodCreator.load(INVOKED_METHOD_PARAMETERS_PROP), parametersList));
    }

    private void collectHeaderFillers(ClassInfo interfaceClass, MethodInfo method,
            Map<String, ParamData> headerFillersByName) {
        AnnotationInstance classLevelHeader = interfaceClass.declaredAnnotation(CLIENT_HEADER_PARAM);
        if (classLevelHeader != null) {
            headerFillersByName.put(classLevelHeader.value("name").asString(),
                    new ParamData(classLevelHeader, interfaceClass));
        }
        putAllHeaderAnnotations(headerFillersByName,
                interfaceClass,
                extractAnnotations(interfaceClass.declaredAnnotation(CLIENT_HEADER_PARAMS)));

        Map<String, ParamData> methodLevelHeadersByName = new HashMap<>();
        AnnotationInstance methodLevelHeader = method.annotation(CLIENT_HEADER_PARAM);
        if (methodLevelHeader != null) {
            methodLevelHeadersByName.put(methodLevelHeader.value("name").asString(),
                    new ParamData(methodLevelHeader, interfaceClass));
        }
        putAllHeaderAnnotations(methodLevelHeadersByName, interfaceClass,
                extractAnnotations(method.annotation(CLIENT_HEADER_PARAMS)));

        headerFillersByName.putAll(methodLevelHeadersByName);
    }

    private String basicAuthImplClassName(ClassInfo interfaceClass) {
        return interfaceClass.name().toString() + "BasicAuthImpl";
    }

    private String basicAuthImplMethodName() {
        return "create";
    }

    /**
     * create a field in the stub class to contain (interface) java.lang.reflect.Method corresponding to this method
     * MP Rest Client spec says it has to be in the request context, keeping it in a field we don't have to
     * initialize it on each call
     *
     * @param javaMethodField method reference in a static class field
     * @param methodCreator method for which we put the java.lang.reflect.Method to context (aka this method)
     * @param invocationBuilder Invocation.Builder in this method
     */
    private void addJavaMethodToContext(FieldDescriptor javaMethodField, MethodCreator methodCreator,
            AssignableResultHandle invocationBuilder) {
        ResultHandle javaMethod = methodCreator.readStaticField(javaMethodField);
        ResultHandle javaMethodAsObject = methodCreator.checkCast(javaMethod, Object.class);
        methodCreator.assign(invocationBuilder,
                methodCreator.invokeInterfaceMethod(INVOCATION_BUILDER_PROPERTY_METHOD, invocationBuilder,
                        methodCreator.load(INVOKED_METHOD_PROP), javaMethodAsObject));
    }

    private void putAllHeaderAnnotations(Map<String, ParamData> headerMap, ClassInfo interfaceClass,
            AnnotationInstance[] annotations) {
        for (AnnotationInstance annotation : annotations) {
            String headerName = annotation.value("name").asString();
            if (headerMap.put(headerName, new ParamData(annotation, interfaceClass)) != null) {
                throw new RestClientDefinitionException("Duplicate ClientHeaderParam annotation for header: " + headerName +
                        " on " + annotation.target());
            }
        }
    }

    // fillHeaders takes `MultivaluedMap<String, String>` as param and modifies it
    private void addHeaderParam(MethodInfo declaringMethod, MethodCreator fillHeadersCreator,
            ParamData paramData,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            String fillerClassName,
            IndexView index) {

        AnnotationInstance annotation = paramData.annotation;
        ClassInfo declaringClass = paramData.definingClass;

        String headerName = annotation.value("name").asString();

        String[] values = annotation.value().asStringArray();

        if (values.length == 0) {
            log.warnv("Ignoring ClientHeaderParam that specifies an empty array of header values for header {0} on {1}",
                    annotation.value("name").asString(), annotation.target());
            return;
        }

        ResultHandle headerMap = fillHeadersCreator.getMethodParam(0);
        ResultHandle requestContext = fillHeadersCreator.getMethodParam(1);

        // if headers are set here, they were set with @HeaderParam, which should take precedence of MP ways
        BytecodeCreator fillHeaders = fillHeadersCreator
                .ifTrue(fillHeadersCreator.invokeStaticMethod(HEADER_FILLER_UTIL_SHOULD_ADD_HEADER,
                        fillHeadersCreator.load(headerName), headerMap, requestContext))
                .trueBranch();

        if (values.length > 1) {
            // TODO: we should probably also get rid of this as some point
            boolean required = annotation.valueWithDefault(index, "required").asBoolean();
            ResultHandle headerList = Gizmo.newArrayList(fillHeaders, 1);
            for (String value : values) {
                if (value.contains("${")) {
                    ResultHandle headerValueFromConfig = fillHeaders.invokeStaticMethod(
                            CONFIG_UTILS_INTERPOLATE,
                            fillHeaders.load(value), fillHeaders.load(required));
                    fillHeaders.ifNotNull(headerValueFromConfig)
                            .trueBranch().invokeInterfaceMethod(LIST_ADD_METHOD, headerList, headerValueFromConfig);
                } else {
                    fillHeaders.invokeInterfaceMethod(LIST_ADD_METHOD, headerList, fillHeaders.load(value));
                }
            }

            fillHeaders.invokeInterfaceMethod(MAP_PUT_METHOD, headerMap, fillHeaders.load(headerName), headerList);
        } else {
            // if there is only one value, we support mixing verbatim values, config params method invocations and method parameter lookups
            // A method call is in the form of {some.package.ClassName.methodName} or {defaultMethodWithinThisInterfaceName}
            // A method parameter lookup is also in the form of {methodParamName} and if there are clashes with a method call, the latter takes precedence
            // An config name is in the form of ${config.name}
            List<Node> nodes = new RestClientAnnotationExpressionParser(values[0],
                    declaringMethod.declaringClass().name().toString() + "#" + declaringMethod.name()).parse();

            // if `!required` an exception on header filling does not fail the invocation:
            boolean required = annotation.valueWithDefault(index, "required").asBoolean();

            BytecodeCreator fillHeader;
            TryBlock tryBlock;
            if (required) {
                tryBlock = null;
                fillHeader = fillHeaders;
            } else {
                tryBlock = fillHeaders.tryBlock();
                fillHeader = tryBlock;
            }

            List<HeaderFillerInfo> headerFillerInfos = nodes.stream().map(n -> {
                if (n instanceof RestClientAnnotationExpressionParser.Verbatim) {

                    return new HeaderFillerInfo(STRING_TYPE, n, new Supplier<ResultHandle>() {
                        @Override
                        public ResultHandle get() {
                            return fillHeader.load(n.getValue());
                        }
                    });

                } else if (n instanceof RestClientAnnotationExpressionParser.ConfigName) {

                    return new HeaderFillerInfo(STRING_TYPE, n, new Supplier<ResultHandle>() {
                        @Override
                        public ResultHandle get() {
                            return fillHeader.invokeStaticMethod(
                                    ofMethod(ConfigUtils.class, "doGetConfigValue", String.class, String.class,
                                            boolean.class, String.class),
                                    fillHeader.load("${" + n.getValue() + "}"), fillHeader.load(required),
                                    fillHeader.load(n.getValue()));
                        }
                    });

                } else if (n instanceof RestClientAnnotationExpressionParser.Accessible) {

                    String accessibleName = n.getValue();
                    MethodInfo headerFillingMethod;
                    AccessibleType accessibleType = accessibleName.contains(".") ? AccessibleType.STATIC_METHOD
                            : AccessibleType.INTERFACE_METHOD;
                    if (accessibleType == AccessibleType.STATIC_METHOD) {
                        // calling a static method
                        int endOfClassName = accessibleName.lastIndexOf('.');
                        String className = accessibleName.substring(0, endOfClassName);
                        String staticMethodName = accessibleName.substring(endOfClassName + 1);

                        ClassInfo clazz = index.getClassByName(DotName.createSimple(className));
                        if (clazz == null) {
                            throw new RestClientDefinitionException(String.format(
                                    "Invalid %s definition, unable to determine class %s. Problematic interface: %s",
                                    CLIENT_HEADER_PARAM, className, declaringClass));
                        }
                        headerFillingMethod = findMethod(clazz, declaringClass, staticMethodName,
                                CLIENT_HEADER_PARAM.toString());
                    } else if (accessibleType == AccessibleType.INTERFACE_METHOD) {
                        headerFillingMethod = findMethod(declaringClass, declaringClass, accessibleName,
                                CLIENT_HEADER_PARAM.toString());
                    } else {
                        throw new IllegalStateException("Unknown type " + accessibleType);
                    }

                    Type valueType = null;
                    AtomicInteger parameterPosition = new AtomicInteger(-1);
                    if (headerFillingMethod == null) {
                        for (MethodParameterInfo parameter : declaringMethod.parameters()) {
                            if (!accessibleName.equals(parameter.name())) {
                                continue;
                            }
                            if (!isString(parameter.type())) {
                                throw new RestClientDefinitionException(String.format(
                                        "Invalid %s definition, method parameter %s is not of String type. Problematic interface: %s",
                                        CLIENT_HEADER_PARAM, accessibleName, declaringClass));
                            }
                            accessibleType = AccessibleType.METHOD_PARAMETER;
                            valueType = parameter.type();
                            parameterPosition.set(parameter.position());
                            break;
                        }
                        if (valueType == null) {
                            throw new RestClientDefinitionException(String.format(
                                    "Invalid %s definition, unable to determine target method '%s'. Problematic interface: %s",
                                    CLIENT_HEADER_PARAM, accessibleName, declaringClass));
                        }
                    } else {
                        valueType = headerFillingMethod.returnType();
                    }

                    Supplier<ResultHandle> supplier;
                    if (accessibleType == AccessibleType.STATIC_METHOD) {
                        supplier = new Supplier<ResultHandle>() {
                            @Override
                            public ResultHandle get() {

                                if (headerFillingMethod.parametersCount() == 0) {
                                    return fillHeader.invokeStaticMethod(headerFillingMethod);
                                } else if (headerFillingMethod.parametersCount() == 1
                                        && isString(headerFillingMethod.parameterType(0))) {
                                    return fillHeader.invokeStaticMethod(headerFillingMethod, fillHeader.load(headerName));
                                } else if (headerFillingMethod.parametersCount() == 1
                                        && isComputedParamContext(headerFillingMethod.parameterType(0))) {
                                    ResultHandle fillerParam = fillHeader
                                            .newInstance(
                                                    COMPUTER_PARAM_CONTEXT_IMPL_CTOR,
                                                    fillHeader.load(headerName), requestContext);
                                    return fillHeader.invokeStaticMethod(headerFillingMethod, fillerParam);
                                } else {
                                    throw new RestClientDefinitionException(
                                            "@ClientHeaderParam method " + headerFillingMethod.declaringClass().toString() + "#"
                                                    + headerFillingMethod.name()
                                                    + " has too many parameters, at most one parameter, header name, expected");
                                }

                            }
                        };
                    } else if (accessibleType == AccessibleType.INTERFACE_METHOD) {
                        supplier = new Supplier<ResultHandle>() {
                            @Override
                            public ResultHandle get() {

                                String mockName = mockInterface(declaringClass, generatedClasses, index);
                                ResultHandle interfaceMock = fillHeader.newInstance(MethodDescriptor.ofConstructor(mockName));

                                if (headerFillingMethod.parametersCount() == 0) {
                                    return fillHeader.invokeInterfaceMethod(headerFillingMethod, interfaceMock);
                                } else if (headerFillingMethod.parametersCount() == 1
                                        && isString(headerFillingMethod.parameterType(0))) {
                                    return fillHeader.invokeInterfaceMethod(headerFillingMethod, interfaceMock,
                                            fillHeader.load(headerName));
                                } else if (headerFillingMethod.parametersCount() == 1
                                        && isComputedParamContext(headerFillingMethod.parameterType(0))) {
                                    ResultHandle fillerParam = fillHeader
                                            .newInstance(
                                                    COMPUTER_PARAM_CONTEXT_IMPL_CTOR,
                                                    fillHeader.load(headerName), requestContext);
                                    return fillHeader.invokeInterfaceMethod(headerFillingMethod, interfaceMock,
                                            fillerParam);
                                } else {
                                    throw new RestClientDefinitionException(
                                            "@ClientHeaderParam method " + headerFillingMethod.declaringClass().toString() + "#"
                                                    + headerFillingMethod.name()
                                                    + " has too many parameters, at most one parameter, header name, expected");
                                }
                            }
                        };
                    } else if (accessibleType == AccessibleType.METHOD_PARAMETER) {
                        supplier = new Supplier<ResultHandle>() {
                            @Override
                            public ResultHandle get() {
                                return fillHeader.invokeStaticMethod(COMPUTER_PARAM_CONTEXT_IMPL_GET_METHOD_PARAM,
                                        requestContext, fillHeader.load(parameterPosition.get()));
                            }
                        };
                    } else {
                        throw new IllegalStateException("Unknown type " + accessibleType);
                    }

                    if (nodes.size() == 1) {
                        if (!isString(valueType) && !isStringArray(
                                valueType)) {
                            throw new RestClientDefinitionException("Method " + headerFillingMethod.declaringClass().toString()
                                    + "#" + headerFillingMethod.name()
                                    + " has an unsupported return type for ClientHeaderParam. " +
                                    "Only String and String[] return types are supported");
                        }
                    } else {
                        if (!isString(valueType)) {
                            throw new RestClientDefinitionException("Method " + headerFillingMethod.declaringClass().toString()
                                    + "#" + headerFillingMethod.name()
                                    + " has an unsupported return type for ClientHeaderParam. " +
                                    "Only String is supported when using complex expressions");
                        }
                    }

                    return new HeaderFillerInfo(valueType, n, supplier);

                } else {
                    throw new IllegalStateException("Unknown node type " + n.getClass().getName());
                }
            }).collect(Collectors.toList());

            AssignableResultHandle headerList = fillHeader.createVariable(List.class);
            fillHeader.assign(headerList, fillHeader.loadNull());
            if (headerFillerInfos.size() == 1) {
                HeaderFillerInfo headerFillerInfo = headerFillerInfos.get(0);
                ResultHandle headerFillerResult = headerFillerInfo.getResultHandleSupplier().get();
                BytecodeCreator notNullBranchTrue = fillHeader.ifNotNull(headerFillerResult).trueBranch();
                Type headerFillerMethodReturnType = headerFillerInfo.getValueType();
                if (isStringArray(headerFillerMethodReturnType)) {
                    // repack array to list
                    ResultHandle asList = notNullBranchTrue.invokeStaticMethod(ARRAYS_AS_LIST, headerFillerResult);
                    notNullBranchTrue.assign(headerList, asList);
                } else if (isString(headerFillerMethodReturnType)) {
                    notNullBranchTrue.assign(headerList, Gizmo.newArrayList(notNullBranchTrue, 1));
                    notNullBranchTrue.invokeInterfaceMethod(LIST_ADD_METHOD, headerList, headerFillerResult);
                } else {
                    throw new IllegalStateException("Unhandled type: " + headerFillerMethodReturnType);
                }
            } else {
                ResultHandle nonNullValuesList = Gizmo.newArrayList(fillHeader, headerFillerInfos.size());
                for (HeaderFillerInfo headerFillerInfo : headerFillerInfos) {
                    if (!isString(headerFillerInfo.getValueType())) {
                        throw new IllegalStateException("Unhandled type: " + headerFillerInfo.getValueType());
                    }
                    ResultHandle value = headerFillerInfo.getResultHandleSupplier().get();
                    BytecodeCreator notNullBranch = fillHeader.ifNotNull(value).trueBranch();
                    notNullBranch.invokeInterfaceMethod(LIST_ADD_METHOD, nonNullValuesList, value);
                }
                ResultHandle sb = fillHeader.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class));
                ForEachLoop loop = fillHeader.forEach(nonNullValuesList);
                BytecodeCreator block = loop.block();
                block.invokeVirtualMethod(STRING_BUILDER_APPEND, sb, loop.element());
                ResultHandle stringValue = Gizmo.toString(fillHeader, sb);

                ResultHandle stringValueLength = fillHeader.invokeVirtualMethod(STRING_LENGTH, stringValue);
                BytecodeCreator notEmptyStringBranchTrue = fillHeader.ifNonZero(stringValueLength).trueBranch();
                notEmptyStringBranchTrue.assign(headerList, Gizmo.newArrayList(notEmptyStringBranchTrue, 1));
                notEmptyStringBranchTrue.invokeInterfaceMethod(LIST_ADD_METHOD, headerList, stringValue);
            }

            BytecodeCreator headerListNotNull = fillHeader.ifNotNull(headerList).trueBranch();
            headerListNotNull.invokeInterfaceMethod(MAP_PUT_METHOD, headerMap, headerListNotNull.load(headerName), headerList);

            if (!required) {
                CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
                ResultHandle log = catchBlock.readStaticField(FieldDescriptor.of(fillerClassName, "log", Logger.class));
                String errorMessage = String.format("Invoking header for header '%s' failed", headerName);
                catchBlock.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Logger.class, "warn", void.class, Object.class, Throwable.class),
                        log,
                        catchBlock.load(errorMessage), catchBlock.getCaughtException());
            }
        }
    }

    enum AccessibleType {
        INTERFACE_METHOD,
        STATIC_METHOD,
        METHOD_PARAMETER
    }

    private static class HeaderFillerInfo {
        private final Type valueType;
        private final Supplier<ResultHandle> resultHandleSupplier;

        private final Node source;

        HeaderFillerInfo(Type valueType, Node source, Supplier<ResultHandle> resultHandleSupplier) {
            this.valueType = valueType;
            this.source = source;
            this.resultHandleSupplier = resultHandleSupplier;
        }

        Type getValueType() {
            return valueType;
        }

        Supplier<ResultHandle> getResultHandleSupplier() {
            return resultHandleSupplier;
        }

        HeaderFillerInfo mapResultHandle(Function<Supplier<ResultHandle>, Supplier<ResultHandle>> mapper) {
            return new HeaderFillerInfo(this.valueType, this.source, mapper.apply(this.resultHandleSupplier));
        }
    }

    private MethodInfo findMethod(ClassInfo declaringClass, ClassInfo restInterface, String methodName,
            String sourceAnnotationName) {
        MethodInfo result = null;
        for (MethodInfo method : declaringClass.methods()) {
            if (method.name().equals(methodName)) {
                if (result != null) {
                    throw new RestClientDefinitionException(String.format(
                            "Ambiguous %s definition, more than one method of name %s found on %s. Problematic interface: %s",
                            sourceAnnotationName, methodName, declaringClass, restInterface));
                } else {
                    result = method;
                }
            }
        }
        return result;
    }

    private static boolean isString(Type type) {
        return type.kind() == Type.Kind.CLASS && type.name().toString().equals(String.class.getName());
    }

    private static boolean isStringArray(Type returnType) {
        return returnType.kind() == Type.Kind.ARRAY && returnType.asArrayType().constituent().name().equals(STRING);
    }

    private static boolean isComputedParamContext(Type type) {
        return type.kind() == Type.Kind.CLASS && type.name().toString().equals(ComputedParamContext.class.getName());
    }

    private String mockInterface(ClassInfo declaringClass, BuildProducer<GeneratedClassBuildItem> generatedClass,
            IndexView index) {
        // we have an interface, we have to call a default method on it, we generate a (very simplistic) implementation:

        return interfaceMocks.computeIfAbsent(declaringClass, classInfo -> {
            String mockName = declaringClass.toString() + HashUtil.sha1(declaringClass.toString());
            ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
            List<DotName> interfaceNames = declaringClass.interfaceNames();
            Set<MethodInfo> methods = new HashSet<>();
            for (DotName interfaceName : interfaceNames) {
                ClassInfo interfaceClass = index.getClassByName(interfaceName);
                methods.addAll(interfaceClass.methods());
            }
            methods.addAll(declaringClass.methods());

            try (ClassCreator classCreator = ClassCreator.builder().className(mockName).interfaces(declaringClass.toString())
                    .classOutput(classOutput)
                    .build()) {

                for (MethodInfo method : methods) {
                    if (Modifier.isAbstract(method.flags())) {
                        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.of(method));
                        methodCreator.throwException(IllegalStateException.class, "This should never be called");
                    }
                }
            }
            return mockName;
        });
    }

    private AnnotationInstance[] extractAnnotations(AnnotationInstance groupAnnotation) {
        if (groupAnnotation != null) {
            AnnotationValue annotationValue = groupAnnotation.value();
            if (annotationValue != null) {
                return annotationValue.asNestedArray();
            }
        }
        return EMPTY_ANNOTATION_INSTANCES;
    }

    /**
     * ClientxxxParam annotations can be defined on a JAX-RS interface or a sub-client (sub-resource).
     * If we're filling parameters for a sub-client, we need to know the defining class of the ClientxxxParam
     * to properly resolve default methods of the "root" client
     */
    private static class ParamData {
        private final AnnotationInstance annotation;
        private final ClassInfo definingClass;

        public ParamData(AnnotationInstance annotation, ClassInfo definingClass) {
            this.annotation = annotation;
            this.definingClass = definingClass;
        }
    }

    /**
     * This class is meant to parse the values in {@link org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam}
     * into a list of supported types
     */
    static class RestClientAnnotationExpressionParser {

        private final String input;
        private final String sourceMethod;

        RestClientAnnotationExpressionParser(String input, String sourceMethod) {
            this.input = Objects.requireNonNull(input);
            this.sourceMethod = sourceMethod;
        }

        // this is a pretty naive implementation, but it suffices for what we are trying to do
        List<Node> parse() {
            int i = 0;
            int configStart = -1;
            int accessibleStart = -1;
            int verbatimStart = -1;
            List<Node> nodes = new ArrayList<>();
            while (i < input.length()) {
                char c = input.charAt(i);
                if (c == '$') {
                    if ((configStart != -1) || (accessibleStart != -1)) {
                        throw new IllegalArgumentException(createEffectiveErrorMessage("Cannot mix expressions"));
                    }
                    if (i == input.length() - 1) {
                        throw new IllegalArgumentException(createEffectiveErrorMessage("Illegal end of expression"));
                    }
                    if (input.charAt(i + 1) != '{') {
                        throw new IllegalArgumentException(createEffectiveErrorMessage("'$' must always be followed by '{'"));
                    }
                    if (verbatimStart != -1) {
                        nodes.add(new Verbatim(input.substring(verbatimStart, i)));
                    }
                    i += 2;
                    configStart = i;
                    verbatimStart = -1;
                } else if (c == '{') {
                    if ((configStart != -1) || (accessibleStart != -1)) {
                        throw new IllegalArgumentException(createEffectiveErrorMessage("Cannot mix expressions"));
                    }
                    if (i == input.length() - 1) {
                        throw new IllegalArgumentException(createEffectiveErrorMessage("Illegal end of expression"));
                    }
                    if (verbatimStart != -1) {
                        nodes.add(new Verbatim(input.substring(verbatimStart, i)));
                    }
                    i++;
                    accessibleStart = i;
                    verbatimStart = -1;
                } else if (c == '}') {
                    if ((configStart == -1) && (accessibleStart == -1)) {
                        throw new IllegalArgumentException(createEffectiveErrorMessage("Illegal end of expression"));
                    }
                    if (configStart != -1) {
                        nodes.add(new ConfigName(input.substring(configStart, i)));
                    } else {
                        nodes.add(new Accessible(input.substring(accessibleStart, i)));
                    }
                    configStart = -1;
                    accessibleStart = -1;
                    i++;
                } else {
                    if ((verbatimStart == -1) && (configStart == -1) && (accessibleStart == -1)) {
                        verbatimStart = i;
                    }
                    i++;
                }
            }
            if (verbatimStart != -1) {
                nodes.add(new Verbatim(input.substring(verbatimStart)));
            }

            return nodes;
        }

        private String createEffectiveErrorMessage(String errorMessage) {
            return "Invalid REST Client annotation value expression '" + input + "'"
                    + (sourceMethod != null ? ("found on method '" + sourceMethod + "'") : "") + ". Error is : '" + errorMessage
                    + "'";
        }

        static abstract class Node {

            protected final String value;

            Node(String value) {
                this.value = Objects.requireNonNull(value);
            }

            String getValue() {
                return value;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof Node)) {
                    return false;
                }
                Node node = (Node) o;
                return Objects.equals(value, node.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(value);
            }
        }

        static class Verbatim extends Node {

            Verbatim(String value) {
                super(value);
            }

            @Override
            public String toString() {
                return "Verbatim{" +
                        "value='" + value + '\'' +
                        '}';
            }
        }

        static class ConfigName extends Node {

            ConfigName(String value) {
                super(value);
            }

            @Override
            public String toString() {
                return "ConfigName{" +
                        "value='" + value + '\'' +
                        '}';
            }
        }

        static class Accessible extends Node {

            Accessible(String value) {
                super(value);
            }

            @Override
            public String toString() {
                return "Accessible{" +
                        "value='" + value + '\'' +
                        '}';
            }
        }
    }
}
