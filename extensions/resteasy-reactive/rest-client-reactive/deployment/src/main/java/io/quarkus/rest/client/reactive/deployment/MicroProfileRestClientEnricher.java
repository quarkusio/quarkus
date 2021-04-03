package io.quarkus.rest.client.reactive.deployment;

import static io.quarkus.arc.processor.DotNames.STRING;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_HEADER_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_HEADER_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_CLIENT_HEADERS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_PROVIDER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_PROVIDERS;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
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
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.jaxrs.client.reactive.deployment.JaxrsClientReactiveEnricher;
import io.quarkus.rest.client.reactive.BeanGrabber;
import io.quarkus.rest.client.reactive.HeaderFiller;
import io.quarkus.rest.client.reactive.MicroProfileRestClientRequestFilter;
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
    private static final MethodDescriptor MAP_PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class,
            Object.class, Object.class);
    private static final MethodDescriptor MAP_CONTAINS_KEY_METHOD = MethodDescriptor.ofMethod(Map.class, "containsKey",
            boolean.class, Object.class);
    public static final String INVOKED_METHOD = "org.eclipse.microprofile.rest.client.invokedMethod";

    private final Map<ClassInfo, String> interfaceMocks = new HashMap<>();

    @Override
    public void forClass(MethodCreator constructor, AssignableResultHandle webTargetBase,
            ClassInfo interfaceClass, IndexView index) {

        AnnotationInstance annotation = interfaceClass.classAnnotation(REGISTER_PROVIDER);
        AnnotationInstance groupAnnotation = interfaceClass.classAnnotation(REGISTER_PROVIDERS);

        if (annotation != null) {
            addProvider(constructor, webTargetBase, index, annotation);
        }
        for (AnnotationInstance annotationInstance : extractAnnotations(groupAnnotation)) {
            addProvider(constructor, webTargetBase, index, annotationInstance);
        }

        ResultHandle clientHeadersFactory = null;

        AnnotationInstance registerClientHeaders = interfaceClass.classAnnotation(REGISTER_CLIENT_HEADERS);

        boolean useDefaultHeaders = true;
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
                        containerHandle, constructor.loadClass(headersFactoryClass),
                        constructor.newArray(Annotation.class, 0));
                clientHeadersFactory = constructor
                        .invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                                instanceHandle);
                useDefaultHeaders = false;
            }
        }
        if (useDefaultHeaders) {
            clientHeadersFactory = constructor
                    .newInstance(MethodDescriptor.ofConstructor(DEFAULT_HEADERS_FACTORY));
        }

        ResultHandle restClientFilter = constructor.newInstance(
                MethodDescriptor.ofConstructor(MicroProfileRestClientRequestFilter.class, ClientHeadersFactory.class),
                clientHeadersFactory);

        constructor.assign(webTargetBase, constructor.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Configurable.class, "register", Configurable.class, Object.class),
                webTargetBase, restClientFilter));
    }

    @Override
    public void forMethod(ClassCreator classCreator, MethodCreator constructor,
            MethodCreator methodCreator,
            ClassInfo interfaceClass,
            MethodInfo method, AssignableResultHandle invocationBuilder,
            IndexView index, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            int methodIndex) {

        // create a field in the stub class to contain (interface) java.lang.reflect.Method corresponding to this method
        // MP Rest Client spec says it has to be in the request context, keeping it in a field we don't have to
        // initialize it on each call
        ResultHandle interfaceClassHandle = constructor.loadClass(interfaceClass.toString());

        ResultHandle parameterArray = constructor.newArray(Class.class, method.parameters().size());
        for (int i = 0; i < method.parameters().size(); i++) {
            String parameterClass = method.parameters().get(i).name().toString();
            constructor.writeArrayValue(parameterArray, i, constructor.loadClass(parameterClass));
        }

        ResultHandle javaMethodHandle = constructor.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Class.class, "getMethod", Method.class, String.class, Class[].class),
                interfaceClassHandle, constructor.load(method.name()), parameterArray);
        FieldDescriptor javaMethodField = FieldDescriptor.of(classCreator.getClassName(), "javaMethod" + methodIndex,
                Method.class);
        classCreator.getFieldCreator(javaMethodField).setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        constructor.writeInstanceField(javaMethodField, constructor.getThis(), javaMethodHandle);

        // header filler
        Map<String, AnnotationInstance> headerFillersByName = new HashMap<>();

        AnnotationInstance classLevelHeader = interfaceClass.classAnnotation(CLIENT_HEADER_PARAM);
        if (classLevelHeader != null) {
            headerFillersByName.put(classLevelHeader.value("name").asString(), classLevelHeader);
        }
        putAllHeaderAnnotations(headerFillersByName,
                extractAnnotations(interfaceClass.classAnnotation(CLIENT_HEADER_PARAMS)));

        Map<String, AnnotationInstance> methodLevelHeadersByName = new HashMap<>();
        AnnotationInstance methodLevelHeader = method.annotation(CLIENT_HEADER_PARAM);
        if (methodLevelHeader != null) {
            methodLevelHeadersByName.put(methodLevelHeader.value("name").asString(), methodLevelHeader);
        }
        putAllHeaderAnnotations(methodLevelHeadersByName, extractAnnotations(method.annotation(CLIENT_HEADER_PARAMS)));

        headerFillersByName.putAll(methodLevelHeadersByName);

        FieldDescriptor headerFillerField = FieldDescriptor.of(classCreator.getClassName(),
                "headerFiller" + methodIndex, HeaderFiller.class);
        classCreator.getFieldCreator(headerFillerField).setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        ResultHandle headerFiller;
        // create header filler for this method if headerFillersByName is not empty
        if (!headerFillersByName.isEmpty()) {
            String fillerClassName = interfaceClass.toString() + "$$" + method.name() + "$$" + methodIndex;

            GeneratedClassGizmoAdaptor classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
            try (ClassCreator headerFillerClass = ClassCreator.builder().className(fillerClassName)
                    .interfaces(HeaderFiller.class)
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

                MethodCreator fillHeaders = headerFillerClass
                        .getMethodCreator(
                                MethodDescriptor.ofMethod(HeaderFiller.class, "addHeaders", void.class, MultivaluedMap.class));

                for (Map.Entry<String, AnnotationInstance> headerEntry : headerFillersByName.entrySet()) {
                    addHeaderParam(interfaceClass, method, fillHeaders, headerEntry.getValue(), generatedClasses,
                            fillerClassName, index);
                }
                fillHeaders.returnValue(null);

                headerFiller = constructor.newInstance(MethodDescriptor.ofConstructor(fillerClassName));
            }
        } else {
            headerFiller = constructor
                    .readStaticField(FieldDescriptor.of(NoOpHeaderFiller.class, "INSTANCE", NoOpHeaderFiller.class));
        }
        constructor.writeInstanceField(headerFillerField, constructor.getThis(), headerFiller);

        ResultHandle javaMethod = methodCreator.readInstanceField(javaMethodField, methodCreator.getThis());
        ResultHandle javaMethodAsObject = methodCreator.checkCast(javaMethod, Object.class);
        methodCreator.assign(invocationBuilder,
                methodCreator.invokeInterfaceMethod(INVOCATION_BUILDER_PROPERTY_METHOD, invocationBuilder,
                        methodCreator.load(INVOKED_METHOD), javaMethodAsObject));
        ResultHandle headerFillerAsObject = methodCreator.checkCast(
                methodCreator.readInstanceField(headerFillerField, methodCreator.getThis()), Object.class);
        methodCreator.assign(invocationBuilder,
                methodCreator.invokeInterfaceMethod(INVOCATION_BUILDER_PROPERTY_METHOD, invocationBuilder,
                        methodCreator.load(HeaderFiller.class.getName()), headerFillerAsObject));
    }

    private void putAllHeaderAnnotations(Map<String, AnnotationInstance> headerMap, AnnotationInstance[] annotations) {
        for (AnnotationInstance annotation : annotations) {
            String headerName = annotation.value("name").asString();
            if (headerMap.put(headerName, annotation) != null) {
                throw new RestClientDefinitionException("Duplicate ClientHeaderParam annotation for header: " + headerName +
                        " on " + annotation.target());
            }
        }
    }

    // fillHeaders takes `MultivaluedMap<String, String>` as param and modifies it
    private void addHeaderParam(ClassInfo declaringClass,
            MethodInfo declaringMethod, MethodCreator fillHeadersCreator,
            AnnotationInstance annotation,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            String fillerClassName,
            IndexView index) {

        String headerName = annotation.value("name").asString();

        String[] values = annotation.value().asStringArray();

        if (values.length == 0) {
            log.warnv("Ignoring ClientHeaderParam that specifies an empty array of header values for header {} on {}",
                    annotation.value("name").asString(), annotation.target());
            return;
        }

        ResultHandle headerMap = fillHeadersCreator.getMethodParam(0);

        // if headers are set here, they were set with @HeaderParam, which should take precedence of MP ways
        BytecodeCreator fillHeaders = fillHeadersCreator
                .ifFalse(fillHeadersCreator.invokeInterfaceMethod(MAP_CONTAINS_KEY_METHOD, headerMap,
                        fillHeadersCreator.load(headerName)))
                .trueBranch();

        if (values.length > 1 || !(values[0].startsWith("{") && values[0].endsWith("}"))) {
            ResultHandle headerList = fillHeaders.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
            for (String value : values) {
                fillHeaders.invokeInterfaceMethod(LIST_ADD_METHOD, headerList, fillHeaders.load(value));
            }

            fillHeaders.invokeInterfaceMethod(MAP_PUT_METHOD, headerMap, fillHeaders.load(headerName), headerList);
        } else { // method call :O {some.package.ClassName.methodName} or {defaultMethodWithinThisInterfaceName}
            // if `required` an exception on header filling does not fail the invocation:
            boolean required = annotation.valueWithDefault(index, "required").asBoolean();

            BytecodeCreator fillHeader = fillHeaders;
            TryBlock tryBlock = null;

            if (!required) {
                tryBlock = fillHeaders.tryBlock();
                fillHeader = tryBlock;
            }
            String methodName = values[0].substring(1, values[0].length() - 1); // strip curly braces

            MethodInfo headerFillingMethod = null;
            ResultHandle headerValue;
            if (methodName.contains(".")) {
                // calling a static method
                int endOfClassName = methodName.lastIndexOf('.');
                String className = methodName.substring(0, endOfClassName);
                String staticMethodName = methodName.substring(endOfClassName + 1);

                ClassInfo clazz = index.getClassByName(DotName.createSimple(className));
                if (clazz == null) {
                    throw new RestClientDefinitionException(
                            "Class " + className + " used in ClientHeaderParam on " + declaringClass + " not found");
                }
                headerFillingMethod = findMethod(clazz, declaringClass, staticMethodName);

                if (headerFillingMethod.parameters().size() == 0) {
                    headerValue = fillHeader.invokeStaticMethod(headerFillingMethod);
                } else if (headerFillingMethod.parameters().size() == 1 && isString(headerFillingMethod.parameters().get(0))) {
                    headerValue = fillHeader.invokeStaticMethod(headerFillingMethod, fillHeader.load(headerName));
                } else {
                    throw new RestClientDefinitionException(
                            "ClientHeaderParam method " + declaringClass.toString() + "#" + staticMethodName
                                    + " has too many parameters, at most one parameter, header name, expected");
                }
            } else {
                // interface method
                String mockName = mockInterface(declaringClass, generatedClasses, index);
                ResultHandle interfaceMock = fillHeader.newInstance(MethodDescriptor.ofConstructor(mockName));

                headerFillingMethod = findMethod(declaringClass, declaringClass, methodName);

                if (headerFillingMethod == null) {
                    throw new RestClientDefinitionException(
                            "ClientHeaderParam method " + methodName + " not found on " + declaringClass);
                }

                if (headerFillingMethod.parameters().size() == 0) {
                    headerValue = fillHeader.invokeInterfaceMethod(headerFillingMethod, interfaceMock);
                } else if (headerFillingMethod.parameters().size() == 1 && isString(headerFillingMethod.parameters().get(0))) {
                    headerValue = fillHeader.invokeInterfaceMethod(headerFillingMethod, interfaceMock,
                            fillHeader.load(headerName));
                } else {
                    throw new RestClientDefinitionException(
                            "ClientHeaderParam method " + declaringClass.toString() + "#" + methodName
                                    + " has too many parameters, at most one parameter, header name, expected");
                }

            }

            Type returnType = headerFillingMethod.returnType();
            ResultHandle headerList;
            if (returnType.kind() == Type.Kind.ARRAY && returnType.asArrayType().component().name().equals(STRING)) {
                // repack array to list
                headerList = fillHeader.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Arrays.class, "asList", List.class, Object[].class), headerValue);
            } else if (returnType.kind() == Type.Kind.CLASS && returnType.name().equals(STRING)) {
                headerList = fillHeader.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                fillHeader.invokeInterfaceMethod(LIST_ADD_METHOD, headerList, headerValue);
            } else {
                throw new RestClientDefinitionException("Method " + declaringClass.toString() + "#" + methodName
                        + " has an unsupported return type for ClientHeaderParam. " +
                        "Only String and String[] return types are supported");
            }
            fillHeader.invokeInterfaceMethod(MAP_PUT_METHOD, headerMap, fillHeader.load(headerName), headerList);

            if (!required) {
                CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
                ResultHandle log = catchBlock.readStaticField(FieldDescriptor.of(fillerClassName, "log", Logger.class));
                String errorMessage = String.format(
                        "Invoking header generation method '%s' for header '%s' on method '%s#%s' failed",
                        methodName, headerName, declaringClass.name(), declaringMethod.name());
                catchBlock.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Logger.class, "warn", void.class, Object.class, Throwable.class),
                        log,
                        catchBlock.load(errorMessage), catchBlock.getCaughtException());
            }
        }
    }

    private MethodInfo findMethod(ClassInfo declaringClass, ClassInfo restInterface, String methodName) {
        MethodInfo headerFillingMethod = null;
        for (MethodInfo method : declaringClass.methods()) {
            if (method.name().equals(methodName)) {
                if (headerFillingMethod != null) {
                    throw new RestClientDefinitionException("Ambiguous ClientHeaderParam definition, " +
                            "more than one method of name " + methodName + " found on " + declaringClass +
                            ". Problematic interface: " + restInterface);
                } else {
                    headerFillingMethod = method;
                }
            }
        }
        return headerFillingMethod;
    }

    private static boolean isString(Type type) {
        return type.kind() == Type.Kind.CLASS && type.name().toString().equals(String.class.getName());
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
                        methodCreator.returnValue(methodCreator.loadNull());
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

    private void addProvider(MethodCreator ctor, AssignableResultHandle target, IndexView index,
            AnnotationInstance registerProvider) {
        // if a registered provider is a cdi bean, it has to be reused
        // take the name of the provider class from the annotation:
        String providerClass = registerProvider.value().asString();

        // get bean, or null, with BeanGrabber.getBeanIfDefined(providerClass)
        ResultHandle providerBean = ctor.invokeStaticMethod(
                MethodDescriptor.ofMethod(BeanGrabber.class, "getBeanIfDefined", Object.class, Class.class),
                ctor.loadClass(providerClass));

        // if bean != null, register the bean
        BranchResult branchResult = ctor.ifNotNull(providerBean);
        BytecodeCreator beanProviderAvailable = branchResult.trueBranch();

        ResultHandle alteredTarget = beanProviderAvailable.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Configurable.class, "register", Configurable.class, Object.class,
                        int.class),
                target, providerBean,
                beanProviderAvailable.load(registerProvider.valueWithDefault(index, "priority").asInt()));
        beanProviderAvailable.assign(target, alteredTarget);

        // else, create a new instance of the provider class
        BytecodeCreator beanProviderNotAvailable = branchResult.falseBranch();
        ResultHandle provider = beanProviderNotAvailable.newInstance(MethodDescriptor.ofConstructor(providerClass));
        alteredTarget = beanProviderNotAvailable.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Configurable.class, "register", Configurable.class, Object.class,
                        int.class),
                target, provider,
                beanProviderNotAvailable.load(registerProvider.valueWithDefault(index, "priority").asInt()));
        beanProviderNotAvailable.assign(target, alteredTarget);
    }

}
