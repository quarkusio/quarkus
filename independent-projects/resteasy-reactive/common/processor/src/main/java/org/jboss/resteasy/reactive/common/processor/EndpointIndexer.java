package org.jboss.resteasy.reactive.common.processor;

import static java.util.Map.entry;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BEAN_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BIG_DECIMAL;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BIG_INTEGER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BLOCKING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BOOLEAN;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CHARACTER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETABLE_FUTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONFIGURATION;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONSUMES;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DEFAULT_VALUE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DOUBLE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DUMMY_ELEMENT_TYPE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.ENCODED;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FLOAT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.GET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEAD;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HTTP_HEADERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.INSTANT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.INTEGER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LIST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LOCAL_DATE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LOCAL_DATE_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LOCAL_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LONG;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MATRIX_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI_PART_DATA_INPUT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI_PART_FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.NON_BLOCKING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OFFSET_DATE_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OFFSET_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONAL;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH_SEGMENT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRIMITIVE_BOOLEAN;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRIMITIVE_CHAR;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRIMITIVE_DOUBLE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRIMITIVE_FLOAT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRIMITIVE_INTEGER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRIMITIVE_LONG;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRODUCES;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PROVIDERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.QUERY_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_COOKIE_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_HEADER_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MATRIX_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_PATH_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_QUERY_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_SSE_ELEMENT_TYPE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_STREAM_ELEMENT_TYPE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RUN_ON_VIRTUAL_THREAD;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SECURITY_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SERVER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SORTED_SET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SSE_EVENT_SINK;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.STRING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SUSPENDED;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.TRANSACTIONAL;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.YEAR;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.ZONED_DATE_TIME;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.InjectableBean;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.TargetJavaVersion.Status;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ScannedSerializer;
import org.jboss.resteasy.reactive.common.processor.scanning.SerializerScanningResult;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.util.URLUtils;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public abstract class EndpointIndexer<T extends EndpointIndexer<T, PARAM, METHOD>, PARAM extends IndexedParameter<PARAM>, METHOD extends ResourceMethod> {

    public static final Map<String, String> primitiveTypes;
    private static final Map<DotName, Class<?>> supportedReaderJavaTypes;
    // spec
    // NOTE: sync with ContextProducer and ContextParamExtractor
    private static final Set<DotName> DEFAULT_CONTEXT_TYPES = Set.of(URI_INFO, HTTP_HEADERS, REQUEST, SECURITY_CONTEXT,
            PROVIDERS, RESOURCE_CONTEXT, CONFIGURATION, SSE,
            SSE_EVENT_SINK,
            // extras
            SERVER_REQUEST_CONTEXT,
            DotName.createSimple("org.jboss.resteasy.reactive.server.SimpleResourceInfo"), //TODO: fixme
            RESOURCE_INFO);

    protected static final Set<DotName> SUPPORT_TEMPORAL_PARAMS = Set.of(INSTANT, LOCAL_DATE, LOCAL_TIME, LOCAL_DATE_TIME,
            OFFSET_TIME, OFFSET_DATE_TIME, ZONED_DATE_TIME, YEAR);

    protected static final Logger log = Logger.getLogger(EndpointIndexer.class);
    protected static final String[] EMPTY_STRING_ARRAY = new String[] {};
    private static final String[] PRODUCES_PLAIN_TEXT_NEGOTIATED = new String[] { MediaType.TEXT_PLAIN, MediaType.WILDCARD };
    private static final String[] PRODUCES_PLAIN_TEXT = new String[] { MediaType.TEXT_PLAIN };
    public static final String CDI_WRAPPER_SUFFIX = "$$CDIWrapper";

    public static final String METHOD_CONTEXT_CUSTOM_RETURN_TYPE_KEY = "METHOD_CONTEXT_CUSTOM_RETURN_TYPE_KEY";
    public static final String METHOD_CONTEXT_ANNOTATION_STORE = "ANNOTATION_STORE";
    public static final String METHOD_PRODUCES = "METHOD_PRODUCES";

    private static final boolean JDK_SUPPORTS_VIRTUAL_THREADS;

    static {
        primitiveTypes = Map.ofEntries(
                entry(byte.class.getName(), Byte.class.getName()),
                entry(Byte.class.getName(), Byte.class.getName()),
                entry(boolean.class.getName(), Boolean.class.getName()),
                entry(Boolean.class.getName(), Boolean.class.getName()),
                entry(char.class.getName(), Character.class.getName()),
                entry(Character.class.getName(), Character.class.getName()),
                entry(short.class.getName(), Short.class.getName()),
                entry(Short.class.getName(), Short.class.getName()),
                entry(int.class.getName(), Integer.class.getName()),
                entry(Integer.class.getName(), Integer.class.getName()),
                entry(float.class.getName(), Float.class.getName()),
                entry(Float.class.getName(), Float.class.getName()),
                entry(double.class.getName(), Double.class.getName()),
                entry(Double.class.getName(), Double.class.getName()),
                entry(long.class.getName(), Long.class.getName()),
                entry(Long.class.getName(), Long.class.getName()));

        supportedReaderJavaTypes = Map.ofEntries(
                entry(PRIMITIVE_BOOLEAN, boolean.class),
                entry(PRIMITIVE_DOUBLE, double.class),
                entry(PRIMITIVE_FLOAT, float.class),
                entry(PRIMITIVE_LONG, long.class),
                entry(PRIMITIVE_INTEGER, int.class),
                entry(PRIMITIVE_CHAR, char.class),
                entry(BOOLEAN, Boolean.class),
                entry(DOUBLE, Double.class),
                entry(FLOAT, Float.class),
                entry(LONG, Long.class),
                entry(INTEGER, Integer.class),
                entry(CHARACTER, Character.class),
                entry(BIG_DECIMAL, BigDecimal.class),
                entry(BIG_INTEGER, BigInteger.class));

        boolean isJDKCompatible = true;
        try {
            Class.forName("java.lang.ThreadBuilders");
        } catch (ClassNotFoundException e) {
            isJDKCompatible = false;
        }
        JDK_SUPPORTS_VIRTUAL_THREADS = isJDKCompatible;
    }

    protected final IndexView index;
    protected final IndexView applicationIndex;
    protected final Map<String, String> existingConverters;
    protected final Map<String, InjectableBean> injectableBeans;
    protected final boolean hasRuntimeConverters;

    private final Map<DotName, String> scannedResourcePaths;
    protected final ResteasyReactiveConfig config;
    protected final AdditionalReaders additionalReaders;
    private final Map<DotName, String> httpAnnotationToMethod;
    private final AdditionalWriters additionalWriters;
    private final BlockingDefault defaultBlocking;
    private final Map<DotName, Map<String, String>> classLevelExceptionMappers;
    private final Function<String, BeanFactory<Object>> factoryCreator;
    private final Consumer<ResourceMethodCallbackEntry> resourceMethodCallback;
    private final AnnotationStore annotationStore;
    protected final ApplicationScanningResult applicationScanningResult;
    private final Set<DotName> contextTypes;
    private final Set<DotName> parameterContainerTypes;
    private final MultipartReturnTypeIndexerExtension multipartReturnTypeIndexerExtension;
    private final TargetJavaVersion targetJavaVersion;

    private final Function<ClassInfo, Supplier<Boolean>> isDisabledCreator;

    private final Predicate<Map<DotName, AnnotationInstance>> skipMethodParameter;
    private SerializerScanningResult serializerScanningResult;

    protected EndpointIndexer(Builder<T, ?, METHOD> builder) {
        this.index = builder.index;
        this.applicationIndex = builder.applicationIndex;
        this.existingConverters = builder.existingConverters;
        this.scannedResourcePaths = builder.scannedResourcePaths;
        this.config = builder.config;
        this.additionalReaders = builder.additionalReaders;
        this.httpAnnotationToMethod = builder.httpAnnotationToMethod;
        this.injectableBeans = builder.injectableBeans;
        this.additionalWriters = builder.additionalWriters;
        this.hasRuntimeConverters = builder.hasRuntimeConverters;
        this.defaultBlocking = builder.defaultBlocking;
        this.classLevelExceptionMappers = builder.classLevelExceptionMappers;
        this.factoryCreator = builder.factoryCreator;
        this.resourceMethodCallback = builder.resourceMethodCallback;
        this.annotationStore = new AnnotationStore(builder.annotationsTransformers);
        this.applicationScanningResult = builder.applicationScanningResult;
        this.contextTypes = builder.contextTypes;
        this.parameterContainerTypes = builder.parameterContainerTypes;
        this.multipartReturnTypeIndexerExtension = builder.multipartReturnTypeIndexerExtension;
        this.targetJavaVersion = builder.targetJavaVersion;
        this.isDisabledCreator = builder.isDisabledCreator;
        this.skipMethodParameter = builder.skipMethodParameter;
    }

    public Optional<ResourceClass> createEndpoints(ClassInfo classInfo, boolean considerApplication) {
        if (considerApplication && !applicationScanningResult.keepClass(classInfo.name().toString())) {
            return Optional.empty();
        }
        try {
            String path = scannedResourcePaths.get(classInfo.name());
            ResourceClass clazz = new ResourceClass();
            if ((classInfo.enclosingClass() != null) && !Modifier.isStatic(classInfo.flags())) {
                throw new DeploymentException(
                        "Non static nested resources classes are not supported: '" + classInfo.name() + "'");
            }
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                clazz.setPath(sanitizePath(path));
            }
            if (factoryCreator != null) {
                clazz.setFactory(factoryCreator.apply(classInfo.name().toString()));
            }
            Map<String, String> classLevelExceptionMappers = this.classLevelExceptionMappers.get(classInfo.name());
            if (classLevelExceptionMappers != null) {
                clazz.setClassLevelExceptionMappers(classLevelExceptionMappers);
            }
            List<ResourceMethod> methods = createEndpoints(classInfo, classInfo, new HashSet<>(), new HashSet<>(),
                    clazz.getPathParameters(), clazz.getPath(), considerApplication);
            clazz.getMethods().addAll(methods);

            // get an InjectableBean view of our class
            InjectableBean injectableBean = scanInjectableBean(classInfo, classInfo,
                    existingConverters,
                    additionalReaders, injectableBeans, hasRuntimeConverters);

            // at this point we've scanned the class and its bean infos, which can have form params
            if (injectableBean.isFormParamRequired()) {
                clazz.setFormParamRequired(true);
                // we must propagate this to all our methods, regardless of what they thought they required
                for (ResourceMethod method : methods) {
                    method.setFormParamRequired(true);
                }
            }
            if (injectableBean.isInjectionRequired()) {
                clazz.setPerRequestResource(true);
            }

            if (isDisabledCreator != null) {
                clazz.setIsDisabled(isDisabledCreator.apply(classInfo));
            }

            return Optional.of(clazz);
        } catch (Exception e) {
            if (Modifier.isInterface(classInfo.flags()) || Modifier.isAbstract(classInfo.flags())) {
                //kinda bogus, but we just ignore failed interfaces for now
                //they can have methods that are not valid until they are actually extended by a concrete type
                log.debug("Ignoring interface " + classInfo.name(), e);
                return Optional.empty();
            }
            throw new RuntimeException(e);
        }
    }

    private String sanitizePath(String path) {
        // this simply replaces the whitespace characters (not part of a path variable) with %20
        // TODO: this might have to be more complex, URL encoding maybe?
        // zero braces indicates we are outside of a variable
        int bracesCount = 0;
        StringBuilder replaced = null;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c == ' ') && (bracesCount == 0)) {
                if (replaced == null) {
                    replaced = new StringBuilder(path.length() + 2);
                    replaced.append(path, 0, i);
                }
                replaced.append("%20");
                continue;
            }
            if (replaced != null) {
                replaced.append(c);
            }
            if (c == '{') {
                bracesCount++;
            } else if (c == '}') {
                bracesCount--;
            }
        }
        if (replaced == null) {
            return path;
        }
        return replaced.toString();
    }

    protected abstract METHOD createResourceMethod(MethodInfo info, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext);

    protected List<ResourceMethod> createEndpoints(ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo, Set<String> seenMethods, Set<String> existingClassNameBindings,
            Set<String> pathParameters, String resourceClassPath, boolean considerApplication) {

        List<ResourceMethod> ret = new ArrayList<>();

        List<FoundEndpoint> endpoints = collectEndpoints(currentClassInfo, actualEndpointInfo, seenMethods,
                existingClassNameBindings, considerApplication, httpAnnotationToMethod, index, applicationScanningResult,
                getAnnotationStore());
        Map<String, BasicResourceClassInfo> basicResourceClassInfoMap = new HashMap<>();
        for (FoundEndpoint endpoint : endpoints) {

            // assemble method path
            String methodPath = readStringValue(getAnnotationStore().getAnnotation(endpoint.methodInfo, PATH));
            if (endpoint.httpMethod != null) {
                // HTTP annotation method
                validateHttpAnnotations(endpoint.methodInfo);
                if (methodPath != null) {
                    if (!methodPath.startsWith("/")) {
                        methodPath = "/" + methodPath;
                    }
                    if (methodPath.endsWith("/")) {
                        methodPath = handleTrailingSlash(methodPath);
                    }
                } else {
                    methodPath = "";
                }
            } else {
                // resource locator method
                if (methodPath != null) {
                    if (!methodPath.startsWith("/")) {
                        methodPath = "/" + methodPath;
                    }
                    if (methodPath.endsWith("/")) {
                        methodPath = methodPath.substring(0, methodPath.length() - 1);
                    }
                }
            }

            // create BasicResourceClassInfo and remember it
            BasicResourceClassInfo basicResourceClassInfo = basicResourceClassInfoMap.get(endpoint.classInfo.name().toString());
            if (basicResourceClassInfo == null) {
                String[] classProduces = extractProducesConsumesValues(
                        getAnnotationStore().getAnnotation(endpoint.classInfo, PRODUCES));
                String[] classConsumes = extractProducesConsumesValues(
                        getAnnotationStore().getAnnotation(endpoint.classInfo, CONSUMES));
                basicResourceClassInfo = new BasicResourceClassInfo(resourceClassPath, classProduces,
                        classConsumes, pathParameters, getStreamAnnotationValue(endpoint.classInfo));
                basicResourceClassInfoMap.put(endpoint.classInfo.name().toString(), basicResourceClassInfo);
            }

            ResourceMethod method = createResourceMethod(endpoint.classInfo, actualEndpointInfo, basicResourceClassInfo,
                    endpoint.classNameBindings, endpoint.httpMethod, endpoint.methodInfo, methodPath);
            ret.add(method);
        }

        return ret;
    }

    /**
     * Return endpoints defined directly on classInfo.
     *
     * @param classInfo resource class
     * @return classInfo endpoint method info
     */
    public static Collection<MethodInfo> collectClassEndpoints(ClassInfo classInfo,
            Map<DotName, String> httpAnnotationToMethod, IndexView index, ApplicationScanningResult applicationScanningResult) {
        Collection<FoundEndpoint> endpoints = collectEndpoints(classInfo, classInfo, new HashSet<>(), new HashSet<>(), true,
                httpAnnotationToMethod, index, applicationScanningResult, new AnnotationStore(null));
        Collection<MethodInfo> ret = new HashSet<>();
        for (FoundEndpoint endpoint : endpoints) {
            if (endpoint.classInfo.equals(classInfo)) {
                ret.add(endpoint.methodInfo);
            }
        }
        return ret;
    }

    private static List<FoundEndpoint> collectEndpoints(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            Set<String> seenMethods, Set<String> existingClassNameBindings, boolean considerApplication,
            Map<DotName, String> httpAnnotationToMethod, IndexView index, ApplicationScanningResult applicationScanningResult,
            AnnotationStore annotationStore) {

        if (considerApplication && applicationScanningResult != null
                && !applicationScanningResult.keepClass(actualEndpointInfo.name().toString())) {
            return Collections.emptyList();
        }

        // $$CDIWrapper suffix is used to generate CDI beans from interfaces, we don't want to create endpoints for them:
        if (currentClassInfo.name().toString().endsWith(CDI_WRAPPER_SUFFIX)) {
            return Collections.emptyList();
        }

        List<FoundEndpoint> ret = new ArrayList<>();
        Set<String> classNameBindings = NameBindingUtil.nameBindingNames(index, currentClassInfo);
        if (classNameBindings.isEmpty()) {
            classNameBindings = existingClassNameBindings;
        }

        for (DotName httpMethod : httpAnnotationToMethod.keySet()) {
            List<MethodInfo> methods = currentClassInfo.methods();
            for (MethodInfo info : methods) {
                AnnotationInstance annotation = annotationStore.getAnnotation(info, httpMethod);
                if (annotation != null) {
                    if (!hasProperModifiers(info)) {
                        continue;
                    }
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    ret.add(new FoundEndpoint(currentClassInfo, classNameBindings, info, httpMethod));
                }
            }
        }

        //now resource locator methods
        List<MethodInfo> methods = currentClassInfo.methods();
        for (MethodInfo info : methods) {
            AnnotationInstance annotation = annotationStore.getAnnotation(info, PATH);
            if (annotation != null) {
                if (!hasProperModifiers(info)) {
                    continue;
                }
                String descriptor = methodDescriptor(info);
                if (seenMethods.contains(descriptor)) {
                    continue;
                }
                seenMethods.add(descriptor);
                ret.add(new FoundEndpoint(currentClassInfo, classNameBindings, info, null));
            }
        }

        DotName superClassName = currentClassInfo.superName();
        if (superClassName != null && !superClassName.equals(OBJECT)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                ret.addAll(collectEndpoints(superClass, actualEndpointInfo, seenMethods, classNameBindings, considerApplication,
                        httpAnnotationToMethod, index, applicationScanningResult, annotationStore));
            }
        }
        List<DotName> interfaces = currentClassInfo.interfaceNames();
        for (DotName i : interfaces) {
            ClassInfo superClass = index.getClassByName(i);
            if (superClass != null) {
                ret.addAll(collectEndpoints(superClass, actualEndpointInfo, seenMethods, classNameBindings, considerApplication,
                        httpAnnotationToMethod, index, applicationScanningResult, annotationStore));
            }
        }
        return ret;
    }

    protected String handleTrailingSlash(String path) {
        return path;
    }

    private void validateHttpAnnotations(MethodInfo info) {
        List<AnnotationInstance> annotationInstances = info.annotations();
        Set<DotName> allMethodAnnotations = new HashSet<>(annotationInstances.size());
        for (AnnotationInstance instance : annotationInstances) {
            allMethodAnnotations.add(instance.name());
        }
        int httpAnnotationCount = 0;
        for (DotName dotName : allMethodAnnotations) {
            if (httpAnnotationToMethod.containsKey(dotName)) {
                httpAnnotationCount++;
            }
            if (httpAnnotationCount > 1) {
                throw new DeploymentException("Method '" + info.name() + "' of class '" + info.declaringClass().name()
                        + "' contains multiple HTTP method annotations.");
            }
        }
    }

    private static boolean hasProperModifiers(MethodInfo info) {
        if (isSynthetic(info.flags())) {
            log.debug("Method '" + info.name() + " of Resource class '" + info.declaringClass().name()
                    + "' is a synthetic method and will therefore be ignored");
            return false;
        }
        if ((info.flags() & Modifier.PUBLIC) == 0) {
            log.warn("Method '" + info.name() + " of Resource class '" + info.declaringClass().name()
                    + "' is not public and will therefore be ignored");
            return false;
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            log.warn("Method '" + info.name() + " of Resource class '" + info.declaringClass().name()
                    + "' is static and will therefore be ignored");
            return false;
        }
        return true;
    }

    private static boolean isSynthetic(int mod) {
        return (mod & 0x1000) != 0; //0x1000 == SYNTHETIC
    }

    private ResourceMethod createResourceMethod(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            BasicResourceClassInfo basicResourceClassInfo, Set<String> classNameBindings, DotName httpMethod,
            MethodInfo currentMethodInfo, String methodPath) {
        try {
            Map<String, Object> methodContext = new HashMap<>();
            methodContext.put(METHOD_CONTEXT_ANNOTATION_STORE, getAnnotationStore());
            Set<String> pathParameters = new HashSet<>(basicResourceClassInfo.getPathParameters());
            URLUtils.parsePathParameters(methodPath, pathParameters);
            Map<DotName, AnnotationInstance>[] parameterAnnotations = new Map[currentMethodInfo.parametersCount()];
            MethodParameter[] methodParameters = new MethodParameter[currentMethodInfo.parametersCount()];
            for (int paramPos = 0; paramPos < currentMethodInfo.parametersCount(); ++paramPos) {
                parameterAnnotations[paramPos] = new HashMap<>();
            }
            for (AnnotationInstance i : getAnnotationStore().getAnnotations(currentMethodInfo)) {
                if (i.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    parameterAnnotations[i.target().asMethodParameter().position()].put(i.name(), i);
                }
            }
            String[] consumes = extractProducesConsumesValues(getAnnotationStore().getAnnotation(currentMethodInfo, CONSUMES),
                    basicResourceClassInfo.getConsumes());
            boolean suspended = false;
            boolean sse = false;
            boolean formParamRequired = getAnnotationStore().getAnnotation(currentMethodInfo,
                    ResteasyReactiveDotNames.WITH_FORM_READ) != null;
            Set<String> fileFormNames = new HashSet<>();
            Type bodyParamType = null;
            TypeArgMapper typeArgMapper = new TypeArgMapper(currentMethodInfo.declaringClass(), index);
            for (int i = 0; i < methodParameters.length; ++i) {
                Map<DotName, AnnotationInstance> anns = parameterAnnotations[i];
                PARAM parameterResult = null;
                boolean encoded = anns.containsKey(ENCODED);
                Type paramType = currentMethodInfo.parameterType(i);
                String errorLocation = "method " + currentMethodInfo + " on class " + currentMethodInfo.declaringClass();

                if (skipParameter(anns)) {
                    parameterResult = createIndexedParam()
                            .setCurrentClassInfo(currentClassInfo)
                            .setActualEndpointInfo(actualEndpointInfo)
                            .setExistingConverters(existingConverters)
                            .setAdditionalReaders(additionalReaders)
                            .setAnns(anns)
                            .setParamType(paramType)
                            .setErrorLocation(errorLocation)
                            .setField(false)
                            .setHasRuntimeConverters(hasRuntimeConverters)
                            .setPathParameters(pathParameters)
                            .setSourceName(currentMethodInfo.parameterName(i))
                            .setType(ParameterType.SKIPPED);
                } else {
                    parameterResult = extractParameterInfo(currentClassInfo, actualEndpointInfo, currentMethodInfo,
                            existingConverters, additionalReaders,
                            anns, paramType, errorLocation, false, hasRuntimeConverters, pathParameters,
                            currentMethodInfo.parameterName(i),
                            consumes,
                            methodContext);
                }

                suspended |= parameterResult.isSuspended();
                sse |= parameterResult.isSse();
                String name = parameterResult.getName();
                String defaultValue = parameterResult.getDefaultValue();
                ParameterType type = parameterResult.getType();
                if (type == ParameterType.BODY) {
                    if (bodyParamType != null)
                        throw new RuntimeException(
                                "Resource method " + currentMethodInfo + " can only have a single body parameter: "
                                        + currentMethodInfo.parameterName(i));
                    bodyParamType = paramType;
                    if (GET.equals(httpMethod) || HEAD.equals(httpMethod) || OPTIONS.equals(httpMethod)) {
                        log.warn("Using a body parameter with " + httpMethod + " is strongly discouraged. Offending method is '"
                                + currentMethodInfo.declaringClass().name() + "#" + currentMethodInfo + "'");
                    }
                }
                String elementType = parameterResult.getElementType();
                boolean single = parameterResult.isSingle();
                if (defaultValue == null && paramType.kind() == Type.Kind.PRIMITIVE) {
                    defaultValue = "0";
                }
                methodParameters[i] = createMethodParameter(currentClassInfo, actualEndpointInfo, encoded, paramType,
                        parameterResult, name, defaultValue, type, elementType, single,
                        AsmUtil.getSignature(paramType, typeArgMapper), fileFormNames);

                if (type == ParameterType.BEAN
                        || type == ParameterType.MULTI_PART_FORM) {
                    // transform the bean param
                    formParamRequired |= handleBeanParam(actualEndpointInfo, paramType, methodParameters, i, fileFormNames);
                } else if (type == ParameterType.FORM || type == ParameterType.MULTI_PART_DATA_INPUT) {
                    formParamRequired = true;
                }
            }

            if (formParamRequired) {
                if (bodyParamType != null
                        && !bodyParamType.name().equals(ResteasyReactiveDotNames.MULTI_VALUED_MAP)
                        && !bodyParamType.name().equals(ResteasyReactiveDotNames.STRING)) {
                    throw new RuntimeException(
                            "'@FormParam' and '@RestForm' cannot be used in a resource method that contains a body parameter. Offending method is '"
                                    + currentMethodInfo.declaringClass().name() + "#" + currentMethodInfo + "'");
                }
                boolean validConsumes = false;
                if (consumes != null && consumes.length > 0) {
                    for (String c : consumes) {
                        if (c.startsWith(MediaType.MULTIPART_FORM_DATA)
                                || c.startsWith(MediaType.APPLICATION_FORM_URLENCODED)) {
                            validConsumes = true;
                            break;
                        }
                    }
                    // TODO: does it make sense to default to MediaType.MULTIPART_FORM_DATA when no consumes is set?
                    if (!validConsumes) {
                        throw new RuntimeException(
                                "'@FormParam' and '@RestForm' can only be used on methods annotated with '@Consumes(MediaType.MULTIPART_FORM_DATA)' '@Consumes(MediaType.APPLICATION_FORM_URLENCODED)'. Offending method is '"
                                        + currentMethodInfo.declaringClass().name() + "#" + currentMethodInfo + "'");
                    }
                }
            }

            Type methodContextReturnTypeOrReturnType = methodContext.containsKey(METHOD_CONTEXT_CUSTOM_RETURN_TYPE_KEY)
                    ? (Type) methodContext.get(METHOD_CONTEXT_CUSTOM_RETURN_TYPE_KEY)
                    : currentMethodInfo.returnType();
            if (REST_RESPONSE.equals(methodContextReturnTypeOrReturnType.name())
                    && (methodContextReturnTypeOrReturnType.kind() == Kind.CLASS)) {
                log.warn("Method '" + currentMethodInfo.name() + " of Resource class '"
                        + currentMethodInfo.declaringClass().name()
                        + "' returns RestResponse but does not declare a generic type. It is strongly advised to define the generic type otherwise the behavior could be unpredictable");
            }
            Type nonAsyncReturnType = getNonAsyncReturnType(methodContextReturnTypeOrReturnType);
            addWriterForType(additionalWriters, nonAsyncReturnType);

            String streamElementType = basicResourceClassInfo.getStreamElementType();
            String streamElementTypeInMethod = getStreamAnnotationValue(currentMethodInfo);
            if (streamElementTypeInMethod != null) {
                streamElementType = streamElementTypeInMethod;
            }

            String[] produces = extractProducesConsumesValues(getAnnotationStore().getAnnotation(currentMethodInfo, PRODUCES));
            if (((produces == null) || (produces.length == 0)) && (streamElementType != null)) {
                // when @RestStreamElementType is used, we automatically determine SSE as the @Produces MediaType
                produces = new String[] { MediaType.SERVER_SENT_EVENTS };
            } else if (produces == null || produces.length == 0) {
                // use the @Produces annotation at class level if exists
                produces = basicResourceClassInfo.getProduces();
            }

            produces = applyDefaultProducesAndAddCharsets(httpMethod, nonAsyncReturnType, produces);

            boolean returnsMultipart = false;
            if (produces != null && produces.length == 1) {
                if (streamElementType == null && MediaType.SERVER_SENT_EVENTS.equals(produces[0])) {
                    // Handle server sent events responses
                    String[] defaultProducesForType = applyAdditionalDefaults(nonAsyncReturnType);
                    if (defaultProducesForType.length == 1) {
                        streamElementType = defaultProducesForType[0];
                    }
                } else if (MediaType.MULTIPART_FORM_DATA.equals(produces[0])) {
                    if (RESPONSE.equals(nonAsyncReturnType.name())) {
                        throw new DeploymentException(
                                String.format(
                                        "Endpoints that produce a Multipart result cannot return '%s' - consider returning '%s' instead. Offending method is '%s'",
                                        RESPONSE,
                                        REST_RESPONSE,
                                        currentMethodInfo.declaringClass().name() + "#" + currentMethodInfo));
                    }

                    // Handle multipart form data responses
                    ClassInfo multipartClassInfo = index.getClassByName(nonAsyncReturnType.name());
                    returnsMultipart = multipartReturnTypeIndexerExtension.handleMultipartForReturnType(additionalWriters,
                            multipartClassInfo, index);
                }
            }
            Set<String> nameBindingNames = nameBindingNames(currentMethodInfo, classNameBindings);
            boolean blocking = isBlocking(currentMethodInfo, defaultBlocking);
            boolean runOnVirtualThread = isRunOnVirtualThread(currentMethodInfo, defaultBlocking);
            // we want to allow "overriding" the blocking/non-blocking setting from an implementation class
            // when the class defining the annotations is an interface
            if (!actualEndpointInfo.equals(currentClassInfo) && Modifier.isInterface(currentClassInfo.flags())) {
                MethodInfo actualMethodInfo = actualEndpointInfo.method(currentMethodInfo.name(),
                        currentMethodInfo.parameterTypes().toArray(new Type[0]));
                if (actualMethodInfo != null) {
                    //we don't pass AUTOMATIC here, as the method signature would be the same, so the same determination
                    //would be reached for a default
                    blocking = isBlocking(actualMethodInfo,
                            blocking ? BlockingDefault.BLOCKING : BlockingDefault.NON_BLOCKING);
                    runOnVirtualThread = isRunOnVirtualThread(actualMethodInfo,
                            blocking ? BlockingDefault.BLOCKING : BlockingDefault.NON_BLOCKING);
                }
            }

            if (returnsMultipart && !blocking) {
                throw new DeploymentException(
                        "Endpoints that produce a Multipart result can only be used on blocking methods. Offending method is '"
                                + currentMethodInfo.declaringClass().name() + "#" + currentMethodInfo + "'");
            }

            methodContext.put(METHOD_PRODUCES, produces);
            ResourceMethod method = createResourceMethod(currentMethodInfo, actualEndpointInfo, methodContext)
                    .setHttpMethod(httpMethod == null ? null : httpAnnotationToMethod.get(httpMethod))
                    .setPath(sanitizePath(methodPath))
                    .setConsumes(consumes)
                    .setProduces(produces)
                    .setNameBindingNames(nameBindingNames)
                    .setName(currentMethodInfo.name())
                    .setBlocking(blocking)
                    .setRunOnVirtualThread(runOnVirtualThread)
                    .setSuspended(suspended)
                    .setSse(sse)
                    .setEncoded(currentMethodInfo.hasDeclaredAnnotation(ENCODED))
                    .setStreamElementType(streamElementType)
                    .setFormParamRequired(formParamRequired)
                    .setFileFormNames(fileFormNames)
                    .setParameters(methodParameters)
                    .setSimpleReturnType(
                            toClassName(currentMethodInfo.returnType(), currentClassInfo, actualEndpointInfo, index))
                    // FIXME: resolved arguments ?
                    .setReturnType(
                            determineReturnType(methodContextReturnTypeOrReturnType, typeArgMapper, currentClassInfo,
                                    actualEndpointInfo, index));

            if (httpMethod == null) {
                handleClientSubResource(method, currentMethodInfo, index);
            }

            handleAdditionalMethodProcessing((METHOD) method, currentClassInfo, currentMethodInfo, getAnnotationStore());
            if (resourceMethodCallback != null) {
                resourceMethodCallback.accept(
                        new ResourceMethodCallbackEntry(this, index, basicResourceClassInfo, actualEndpointInfo,
                                currentMethodInfo,
                                method));
            }
            return method;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process method '" + currentMethodInfo.declaringClass().name() + "#"
                    + currentMethodInfo.name() + "'", e);
        }
    }

    protected boolean skipParameter(Map<DotName, AnnotationInstance> anns) {
        return skipMethodParameter != null && skipMethodParameter.test(anns);
    }

    private String[] applyDefaultProducesAndAddCharsets(DotName httpMethod, Type nonAsyncReturnType, String[] produces) {
        return addDefaultCharsets(applyDefaultProduces(produces, nonAsyncReturnType, httpMethod));
    }

    protected void handleClientSubResource(ResourceMethod resourceMethod, MethodInfo method, IndexView index) {

    }

    private String getStreamAnnotationValue(AnnotationTarget target) {
        String value = getAnnotationValueAsString(target, REST_STREAM_ELEMENT_TYPE);
        if (value == null) {
            value = getAnnotationValueAsString(target, REST_SSE_ELEMENT_TYPE);
        }

        return value;
    }

    private String getAnnotationValueAsString(AnnotationTarget target, DotName annotationType) {
        String value = null;
        AnnotationInstance annotation = getAnnotationStore().getAnnotation(target, annotationType);
        if (annotation != null) {
            value = annotation.value().asString();
        }

        return value;
    }

    private boolean isRunOnVirtualThread(MethodInfo info, BlockingDefault defaultValue) {
        boolean isRunOnVirtualThread = false;
        Map.Entry<AnnotationTarget, AnnotationInstance> runOnVirtualThreadAnnotation = getInheritableAnnotation(info,
                RUN_ON_VIRTUAL_THREAD);

        if (runOnVirtualThreadAnnotation != null) {
            if (!JDK_SUPPORTS_VIRTUAL_THREADS) {
                throw new DeploymentException("Method '" + info.name() + "' of class '" + info.declaringClass().name()
                        + "' uses @RunOnVirtualThread but the JDK version '" + Runtime.version() +
                        "' and doesn't support virtual threads");
            }
            if (targetJavaVersion.isJava19OrHigher() == Status.FALSE) {
                throw new DeploymentException("Method '" + info.name() + "' of class '" + info.declaringClass().name()
                        + "' uses @RunOnVirtualThread but the target JDK version doesn't support virtual threads. Please configure your build tool to target Java 19 or above");
            }
            isRunOnVirtualThread = true;
        }

        //BlockingDefault.BLOCKING should mean "block a platform thread" ? here it does
        if (defaultValue == BlockingDefault.BLOCKING) {
            return false;
        } else if (defaultValue == BlockingDefault.RUN_ON_VIRTUAL_THREAD) {
            isRunOnVirtualThread = true;
        } else if (defaultValue == BlockingDefault.NON_BLOCKING) {
            return false;
        }

        if (isRunOnVirtualThread && !isBlocking(info, defaultValue)) {
            throw new DeploymentException(
                    "Method '" + info.name() + "' of class '" + info.declaringClass().name()
                            + "' is considered a non blocking method. @RunOnVirtualThread can only be used on " +
                            " methods considered blocking");
        } else if (isRunOnVirtualThread) {
            return true;
        }

        return false;
    }

    private boolean isBlocking(MethodInfo info, BlockingDefault defaultValue) {
        Map.Entry<AnnotationTarget, AnnotationInstance> blockingAnnotation = getInheritableAnnotation(info, BLOCKING);
        Map.Entry<AnnotationTarget, AnnotationInstance> runOnVirtualThreadAnnotation = getInheritableAnnotation(info,
                RUN_ON_VIRTUAL_THREAD);
        Map.Entry<AnnotationTarget, AnnotationInstance> nonBlockingAnnotation = getInheritableAnnotation(info,
                NON_BLOCKING);

        if ((blockingAnnotation != null) && (nonBlockingAnnotation != null)) {
            if (blockingAnnotation.getKey().kind() == nonBlockingAnnotation.getKey().kind()) {
                if (blockingAnnotation.getKey().kind() == AnnotationTarget.Kind.METHOD) {
                    throw new DeploymentException(
                            "Method '" + info.name() + "' of class '" + info.declaringClass().name()
                                    + "' contains both @Blocking and @NonBlocking annotations.");
                } else {
                    throw new DeploymentException("Class '" + info.declaringClass().name()
                            + "' contains both @Blocking and @NonBlocking annotations.");
                }
            }
            if (blockingAnnotation.getKey().kind() == AnnotationTarget.Kind.METHOD) {
                // the most specific annotation was the @Blocking annotation on the method
                return true;
            } else {
                // the most specific annotation was the @NonBlocking annotation on the method
                return false;
            }
        } else if ((blockingAnnotation != null)) {
            return true;
        } else if ((nonBlockingAnnotation != null)) {
            return false;
        }
        Map.Entry<AnnotationTarget, AnnotationInstance> transactional = getInheritableAnnotation(info, TRANSACTIONAL); //we treat this the same as blocking, as JTA is blocking, but it is lower priority
        if (defaultValue == BlockingDefault.BLOCKING) {
            return true;
        } else if (defaultValue == BlockingDefault.RUN_ON_VIRTUAL_THREAD) {
            return false;
        } else if (defaultValue == BlockingDefault.NON_BLOCKING) {
            return false;
        } else if (transactional != null) {
            return true;
        }
        return doesMethodHaveBlockingSignature(info);
    }

    protected boolean doesMethodHaveBlockingSignature(MethodInfo info) {
        return true;
    }

    private String determineReturnType(Type returnType, TypeArgMapper typeArgMapper, ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo, IndexView index) {
        if (returnType.kind() == Type.Kind.TYPE_VARIABLE) {
            returnType = resolveTypeVariable(returnType.asTypeVariable(), currentClassInfo, actualEndpointInfo, index);
        }
        return AsmUtil.getSignature(returnType, typeArgMapper);
    }

    protected abstract boolean handleBeanParam(ClassInfo actualEndpointInfo, Type paramType, MethodParameter[] methodParameters,
            int i, Set<String> fileFormNames);

    protected void handleAdditionalMethodProcessing(METHOD method, ClassInfo currentClassInfo, MethodInfo info,
            AnnotationStore annotationStore) {

    }

    public boolean additionalRegisterClassForReflectionCheck(ResourceMethodCallbackEntry entry) {
        return false;
    }

    protected abstract InjectableBean scanInjectableBean(ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo,
            Map<String, String> existingConverters,
            AdditionalReaders additionalReaders,
            Map<String, InjectableBean> injectableBeans,
            boolean hasRuntimeConverters);

    protected abstract MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            boolean encoded, Type paramType, PARAM parameterResult, String name, String defaultValue,
            ParameterType type, String elementType, boolean single, String signature,
            Set<String> fileFormNames);

    private String[] applyDefaultProduces(String[] produces, Type nonAsyncReturnType,
            DotName httpMethod) {
        setupApplyDefaults(nonAsyncReturnType, httpMethod);
        if (produces != null && produces.length != 0)
            return produces;
        return applyAdditionalDefaults(nonAsyncReturnType);
    }

    // see https://github.com/quarkusio/quarkus/issues/19535
    private String[] addDefaultCharsets(String[] produces) {
        if ((produces == null) || (produces.length == 0)) {
            return produces;
        }
        List<String> result = new ArrayList<>(produces.length);
        for (String p : produces) {
            if (p.equals(MediaType.TEXT_PLAIN)) {
                result.add(MediaType.TEXT_PLAIN + ";charset=" + StandardCharsets.UTF_8.name());
            } else {
                result.add(p);
            }
        }
        return result.toArray(EMPTY_STRING_ARRAY);
    }

    protected void setupApplyDefaults(Type nonAsyncReturnType, DotName httpMethod) {

    }

    protected String[] applyAdditionalDefaults(Type nonAsyncReturnType) {
        // FIXME: primitives
        if (STRING.equals(nonAsyncReturnType.name()))
            return config.isSingleDefaultProduces() ? PRODUCES_PLAIN_TEXT : PRODUCES_PLAIN_TEXT_NEGOTIATED;
        return EMPTY_STRING_ARRAY;
    }

    private Type getNonAsyncReturnType(Type returnType) {
        switch (returnType.kind()) {
            case ARRAY:
            case CLASS:
            case PRIMITIVE:
            case VOID:
                return returnType;
            case PARAMETERIZED_TYPE:
                // NOTE: same code in RuntimeResourceDeployment.getNonAsyncReturnType
                ParameterizedType parameterizedType = returnType.asParameterizedType();
                if (COMPLETION_STAGE.equals(parameterizedType.name())
                        || COMPLETABLE_FUTURE.equals(parameterizedType.name())
                        || UNI.equals(parameterizedType.name())
                        || MULTI.equals(parameterizedType.name())
                        || REST_MULTI.equals(parameterizedType.name())
                        || REST_RESPONSE.equals(parameterizedType.name())) {
                    return parameterizedType.arguments().get(0);
                }
                return returnType;
            default:
        }
        return returnType;
        // FIXME: should be an exception, but we have incomplete support for generics ATM, so we still
        // have some unresolved type vars and they do pass _some_ tests
        //        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + returnType);
    }

    protected abstract void addWriterForType(AdditionalWriters additionalWriters, Type paramType);

    protected abstract void addReaderForType(AdditionalReaders additionalReaders, Type paramType);

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getSupportedReaderJavaClass(Type paramType) {
        Class<T> result = (Class<T>) supportedReaderJavaTypes.get(paramType.name());
        return Objects.requireNonNull(result);
    }

    private Map.Entry<AnnotationTarget, AnnotationInstance> getInheritableAnnotation(MethodInfo info, DotName name) {
        // try method first, class second
        AnnotationInstance annotation = getAnnotationStore().getAnnotation(info, name);
        AnnotationTarget target = info;
        if (annotation == null) {
            annotation = getAnnotationStore().getAnnotation(info.declaringClass(), name);
            target = info.declaringClass();
        }
        return annotation != null ? new AbstractMap.SimpleEntry<>(target, annotation) : null;
    }

    private static String methodDescriptor(MethodInfo info) {
        return info.name() + ":" + info.descriptor();
    }

    private static boolean moreThanOne(AnnotationInstance... annotations) {
        boolean oneNonNull = false;
        for (AnnotationInstance annotation : annotations) {
            if (annotation != null) {
                if (oneNonNull)
                    return true;
                oneNonNull = true;
            }
        }
        return false;
    }

    public static String[] extractProducesConsumesValues(AnnotationInstance annotation, String[] defaultValue) {
        String[] read = extractProducesConsumesValues(annotation);
        if (read == null) {
            return defaultValue;
        }
        return read;
    }

    private static String[] extractProducesConsumesValues(AnnotationInstance annotation) {
        if (annotation == null) {
            return null;
        }

        String[] originalStrings;
        AnnotationValue value = annotation.value();
        if (value == null) {
            originalStrings = new String[] { MediaType.WILDCARD };
        } else {
            originalStrings = value.asStringArray();
        }

        if (originalStrings.length > 0) {
            List<String> result = new ArrayList<>(originalStrings.length);
            for (String s : originalStrings) {
                String[] trimmed = s.split(","); // spec says that the value can be a comma separated list...
                for (String t : trimmed) {
                    result.add(t.trim());
                }
            }
            return result.toArray(EMPTY_STRING_ARRAY);
        } else {
            return originalStrings;
        }

    }

    private static String readStringValue(AnnotationInstance annotationInstance) {
        if (annotationInstance != null) {
            return annotationInstance.value().asString();
        }
        return null;
    }

    protected static String toClassName(Type indexType, ClassInfo currentClass, ClassInfo actualEndpointClass,
            IndexView indexView) {
        switch (indexType.kind()) {
            case VOID:
                return "void";
            case CLASS:
                return indexType.asClassType().name().toString();
            case PRIMITIVE:
                return indexType.asPrimitiveType().primitive().name().toLowerCase(Locale.ENGLISH);
            case PARAMETERIZED_TYPE:
                return indexType.asParameterizedType().name().toString();
            case ARRAY:
                return indexType.asArrayType().name().toString();
            case WILDCARD_TYPE:
                WildcardType wildcardType = indexType.asWildcardType();
                Type extendsBound = wildcardType.extendsBound();
                if (extendsBound.name().equals(OBJECT)) {
                    // this is a super bound type that we don't support
                    throw new RuntimeException("Cannot handle wildcard type " + indexType);
                }
                // this is an extend bound type, so we just user the bound
                return wildcardType.name().toString();
            case TYPE_VARIABLE:
                TypeVariable typeVariable = indexType.asTypeVariable();
                if (typeVariable.bounds().isEmpty()) {
                    return Object.class.getName();
                }

                return toClassName(resolveTypeVariable(typeVariable, currentClass, actualEndpointClass, indexView),
                        currentClass, actualEndpointClass, indexView);
            default:
                throw new RuntimeException("Unknown parameter type " + indexType);
        }
    }

    private static Type resolveTypeVariable(TypeVariable typeVariable, ClassInfo currentClass, ClassInfo actualEndpointClass,
            IndexView indexView) {
        if (typeVariable.bounds().isEmpty()) {
            return Type.create(DotName.createSimple(Object.class.getName()), Kind.CLASS);
        }
        int pos = -1;
        List<TypeVariable> typeVariables = currentClass.typeParameters();
        for (int i = 0; i < typeVariables.size(); ++i) {
            if (typeVariables.get(i).identifier().equals(typeVariable.identifier())) {
                pos = i;
                break;
            }
        }
        if (pos != -1) {
            Type resolved = resolveTypeParameters(pos, actualEndpointClass, currentClass, indexView);
            if (resolved != null
                    && (resolved.kind() != Type.Kind.TYPE_VARIABLE
                            || !resolved.asTypeVariable().identifier().equals(typeVariable.identifier()))) {
                return resolved;
            }
        }

        return typeVariable.bounds().get(0);
    }

    private static Type resolveTypeParameters(int position, ClassInfo actualEndpointClass, ClassInfo currentClass,
            IndexView indexView) {
        List<Type> params = JandexUtil.resolveTypeParameters(actualEndpointClass.name(), currentClass.name(),
                indexView);

        if (position < params.size()) {
            return params.get(position);
        }

        // if not found, try to find the type from the methods
        for (MethodInfo method : actualEndpointClass.methods()) {
            for (int paramIndex = 0; paramIndex < method.parametersCount(); paramIndex++) {
                Type paramType = method.parameterType(paramIndex);
                if (paramType.kind() == Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType parameterizedType = paramType.asParameterizedType();
                    if (parameterizedType.name().equals(currentClass.name())
                            && position < parameterizedType.arguments().size()) {
                        return parameterizedType.arguments().get(position);
                    }
                }
            }
        }

        return null;
    }

    private static String appendPath(String prefix, String suffix) {
        if (prefix == null) {
            return suffix;
        } else if (suffix == null) {
            return prefix;
        }
        if ((prefix.endsWith("/") && !suffix.startsWith("/")) ||
                (!prefix.endsWith("/") && suffix.startsWith("/"))) {
            return prefix + suffix;
        } else if (prefix.endsWith("/")) {
            return prefix.substring(0, prefix.length() - 1) + suffix;
        } else {
            return prefix + "/" + suffix;
        }
    }

    protected abstract PARAM createIndexedParam();

    public PARAM extractParameterInfo(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            MethodInfo currentMethodInfo, Map<String, String> existingConverters, AdditionalReaders additionalReaders,
            Map<DotName, AnnotationInstance> anns, Type paramType, String errorLocation, boolean field,
            boolean hasRuntimeConverters, Set<String> pathParameters, String sourceName,
            String[] declaredConsumes,
            Map<String, Object> methodContext) {
        PARAM builder = createIndexedParam()
                .setCurrentClassInfo(currentClassInfo)
                .setActualEndpointInfo(actualEndpointInfo)
                .setExistingConverters(existingConverters)
                .setAdditionalReaders(additionalReaders)
                .setAnns(anns)
                .setParamType(paramType)
                .setErrorLocation(errorLocation)
                .setField(field)
                .setHasRuntimeConverters(hasRuntimeConverters)
                .setPathParameters(pathParameters)
                .setSourceName(sourceName);

        AnnotationInstance beanParam = anns.get(BEAN_PARAM);
        AnnotationInstance multiPartFormParam = anns.get(MULTI_PART_FORM_PARAM);
        AnnotationInstance pathParam = anns.get(PATH_PARAM);
        AnnotationInstance queryParam = anns.get(QUERY_PARAM);
        AnnotationInstance headerParam = anns.get(HEADER_PARAM);
        AnnotationInstance formParam = anns.get(FORM_PARAM);
        AnnotationInstance matrixParam = anns.get(MATRIX_PARAM);
        AnnotationInstance cookieParam = anns.get(COOKIE_PARAM);
        AnnotationInstance restPathParam = anns.get(REST_PATH_PARAM);
        AnnotationInstance restQueryParam = anns.get(REST_QUERY_PARAM);
        AnnotationInstance restHeaderParam = anns.get(REST_HEADER_PARAM);
        AnnotationInstance restFormParam = anns.get(REST_FORM_PARAM);
        AnnotationInstance restMatrixParam = anns.get(REST_MATRIX_PARAM);
        AnnotationInstance restCookieParam = anns.get(REST_COOKIE_PARAM);
        AnnotationInstance contextParam = anns.get(CONTEXT);
        AnnotationInstance defaultValueAnnotation = anns.get(DEFAULT_VALUE);
        AnnotationInstance suspendedAnnotation = anns.get(SUSPENDED);
        boolean convertible = false;
        if (defaultValueAnnotation != null) {
            builder.setDefaultValue(defaultValueAnnotation.value().asString());
        }
        if (handleCustomParameter(anns, builder, paramType, field, methodContext)) {
            return builder;
        } else if (moreThanOne(pathParam, queryParam, headerParam, formParam, cookieParam, contextParam, beanParam,
                restPathParam, restQueryParam, restHeaderParam, restFormParam, restCookieParam)) {
            throw new RuntimeException(
                    "Cannot have more than one of @PathParam, @QueryParam, @HeaderParam, @FormParam, @CookieParam, @BeanParam, @Context on "
                            + errorLocation);
        } else if (pathParam != null) {
            builder.setName(pathParam.value().asString());
            builder.setType(ParameterType.PATH);
            convertible = true;
        } else if (restPathParam != null) {
            builder.setName(valueOrDefault(restPathParam.value(), sourceName));
            builder.setType(ParameterType.PATH);
            convertible = true;
        } else if (queryParam != null) {
            builder.setName(queryParam.value().asString());
            builder.setType(ParameterType.QUERY);
            builder.setSeparator(getSeparator(anns));
            convertible = true;
        } else if (restQueryParam != null) {
            builder.setName(valueOrDefault(restQueryParam.value(), sourceName));
            builder.setType(ParameterType.QUERY);
            builder.setSeparator(getSeparator(anns));
            convertible = true;
        } else if (cookieParam != null) {
            builder.setName(cookieParam.value().asString());
            builder.setType(ParameterType.COOKIE);
            convertible = true;
        } else if (restCookieParam != null) {
            builder.setName(valueOrDefault(restCookieParam.value(), sourceName));
            builder.setType(ParameterType.COOKIE);
            convertible = true;
        } else if (headerParam != null) {
            builder.setName(headerParam.value().asString());
            builder.setType(ParameterType.HEADER);
            convertible = true;
        } else if (restHeaderParam != null) {
            if (restHeaderParam.value() == null || restHeaderParam.value().asString().isEmpty()) {
                builder.setName(StringUtil.hyphenateWithCapitalFirstLetter(sourceName));
            } else {
                builder.setName(restHeaderParam.value().asString());
            }
            builder.setType(ParameterType.HEADER);
            convertible = true;
        } else if (formParam != null) {
            builder.setName(formParam.value().asString());
            builder.setType(ParameterType.FORM);
            convertible = isFormParamConvertible(paramType);
        } else if (restFormParam != null) {
            builder.setName(valueOrDefault(restFormParam.value(), sourceName));
            builder.setType(ParameterType.FORM);
            convertible = isFormParamConvertible(paramType);
        } else if (matrixParam != null) {
            builder.setName(matrixParam.value().asString());
            builder.setType(ParameterType.MATRIX);
            convertible = true;
        } else if (restMatrixParam != null) {
            builder.setName(valueOrDefault(restMatrixParam.value(), sourceName));
            builder.setType(ParameterType.MATRIX);
            convertible = true;
        } else if (contextParam != null) {
            //this is handled by CDI
            if (field) {
                return builder;
            }
            // no name required
            builder.setType(ParameterType.CONTEXT);
        } else if (beanParam != null) {
            // no name required
            builder.setType(ParameterType.BEAN);
        } else if (multiPartFormParam != null) {
            builder.setType(ParameterType.MULTI_PART_FORM);
        } else if (suspendedAnnotation != null) {
            // no name required
            builder.setType(ParameterType.ASYNC_RESPONSE);
            builder.setSuspended(true);
        } else {
            // auto context parameters
            if (!field
                    && paramType.kind() == Kind.CLASS
                    && isContextType(paramType.asClassType())) {
                // no name required
                builder.setType(ParameterType.CONTEXT);
            } else if (!field
                    && paramType.kind() == Kind.CLASS
                    && isParameterContainerType(paramType.asClassType())) {
                // auto @BeanParam/@MultipartForm parameters
                builder.setType(ParameterType.BEAN);
            } else if (!field && pathParameters.contains(sourceName)) {
                builder.setName(sourceName);
                builder.setType(ParameterType.PATH);
                builder.setErrorLocation(builder.getErrorLocation()
                        + " (this parameter name matches the @Path parameter name, so it has been implicitly assumed to be an @PathParam and not the request body)");
                convertible = true;
            } else if (!field && paramType.name().equals(MULTI_PART_DATA_INPUT)) {
                builder.setType(ParameterType.MULTI_PART_DATA_INPUT);
                builder.setSingle(true);
                return builder;
            } else {
                //un-annotated field
                //just ignore it
                if (field) {
                    return builder;
                }
                builder.setName(sourceName);
                builder.setType(ParameterType.BODY);
            }
        }
        builder.setSingle(true);
        boolean typeHandled = false;
        String elementType = null;
        final ParameterType type = builder.getType();
        if (paramType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType pt = paramType.asParameterizedType();
            if (pt.name().equals(LIST)) {
                typeHandled = true;
                builder.setSingle(false);
                elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                if (convertible) {
                    handleListParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
                }
            } else if (pt.name().equals(SET)) {
                typeHandled = true;
                builder.setSingle(false);
                elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                if (convertible) {
                    handleSetParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
                }
            } else if (pt.name().equals(SORTED_SET)) {
                typeHandled = true;
                builder.setSingle(false);
                elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                if (convertible) {
                    handleSortedSetParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
                }
            } else if (pt.name().equals(OPTIONAL)) {
                typeHandled = true;
                elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                if (convertible) {
                    String genericElementType = null;
                    if (pt.arguments().get(0).kind() == Kind.PARAMETERIZED_TYPE) {
                        genericElementType = toClassName(pt.arguments().get(0).asParameterizedType().arguments().get(0),
                                currentClassInfo, actualEndpointInfo, index);
                    }

                    handleOptionalParam(existingConverters, anns, errorLocation, hasRuntimeConverters, builder, elementType,
                            genericElementType, currentMethodInfo);
                }
                builder.setOptional(true);
            } else if (convertible) {
                typeHandled = true;
                elementType = toClassName(pt, currentClassInfo, actualEndpointInfo, index);
                handleOtherParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
            } else if (builder.getType() != ParameterType.BEAN) {
                // the "element" type is not of importance as in this case the signature is used at runtime to determine the proper types
                elementType = DUMMY_ELEMENT_TYPE.toString();
                addReaderForType(additionalReaders, pt);
                typeHandled = true;
            }
        } else if ((paramType.name().equals(PATH_SEGMENT)) && (type == ParameterType.PATH)) {
            elementType = paramType.name().toString();
            handlePathSegmentParam(builder);
            typeHandled = true;
        } else if (SUPPORT_TEMPORAL_PARAMS.contains(paramType.name())
                && (type == ParameterType.PATH || type == ParameterType.QUERY || type == ParameterType.FORM
                        || type == ParameterType.COOKIE || type == ParameterType.HEADER)) {
            elementType = paramType.name().toString();
            handleTemporalParam(builder, paramType.name(), anns, currentMethodInfo);
            typeHandled = true;
        } else if (paramType.name().equals(LIST) && (type == ParameterType.QUERY
                || type == ParameterType.HEADER)) { // RESTEasy Classic handles the non-generic List type
            elementType = String.class.getName();
            typeHandled = true;
            builder.setSingle(false);
            if (convertible) {
                handleListParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
            }
        } else if (paramType.name().equals(SET) && type == ParameterType.HEADER) { // RESTEasy Classic handles the non-generic Set type
            elementType = String.class.getName();
            typeHandled = true;
            builder.setSingle(false);
            if (convertible) {
                handleSetParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
            }
        } else if (paramType.name().equals(SORTED_SET) && type == ParameterType.HEADER) { // RESTEasy Classic handles the non-generic SortedSet type
            elementType = String.class.getName();
            typeHandled = true;
            builder.setSingle(false);
            if (convertible) {
                handleSortedSetParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
            }
        } else if (paramType.kind() == Kind.ARRAY) {
            ArrayType at = paramType.asArrayType();
            typeHandled = true;
            // special case do not treat byte[] for multipart form values
            if (builder.type != ParameterType.FORM
                    || convertible) {
                builder.setSingle(false);
            }
            elementType = toClassName(at.constituent(), currentClassInfo, actualEndpointInfo, index);
            if (convertible) {
                handleArrayParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
            }
        }

        if (!typeHandled) {
            elementType = toClassName(paramType, currentClassInfo, actualEndpointInfo, index);
            addReaderForType(additionalReaders, paramType);

            if (type != ParameterType.CONTEXT && type != ParameterType.BEAN && type != ParameterType.BODY
                    && type != ParameterType.ASYNC_RESPONSE && type != ParameterType.MULTI_PART_FORM) {
                handleOtherParam(existingConverters, errorLocation, hasRuntimeConverters, builder, elementType);
            }
            if (type == ParameterType.CONTEXT && elementType.equals(SseEventSink.class.getName())) {
                builder.setSse(true);
            }
        }
        if (suspendedAnnotation != null && !elementType.equals(AsyncResponse.class.getName())) {
            throw new RuntimeException("Can only inject AsyncResponse on methods marked @Suspended");
        }
        if (builder.isSingle() && builder.getSeparator() != null) {
            throw new DeploymentException("Single parameters should not be marked with @Separator");
        }
        builder.setElementType(elementType);
        return builder;
    }

    private boolean isFormParamConvertible(Type paramType) {
        // let's not call the array converter for byte[] for multipart
        if (paramType.kind() == Kind.ARRAY
                && paramType.asArrayType().constituent().kind() == Kind.PRIMITIVE
                && paramType.asArrayType().constituent().asPrimitiveType().primitive() == Primitive.BYTE) {
            return false;
        } else {
            return true;
        }
    }

    protected boolean handleCustomParameter(Map<DotName, AnnotationInstance> anns, PARAM builder, Type paramType, boolean field,
            Map<String, Object> methodContext) {
        return false;
    }

    protected void handlePathSegmentParam(PARAM builder) {
    }

    protected void handleTemporalParam(PARAM builder, DotName name, Map<DotName, AnnotationInstance> parameterAnnotations,
            MethodInfo currentMethodInfo) {
    }

    protected DeclaredTypes getDeclaredTypes(Type paramType, ClassInfo currentClassInfo, ClassInfo actualEndpointInfo) {
        String declaredType = toClassName(paramType, currentClassInfo, actualEndpointInfo, index);
        String declaredUnresolvedType;
        if (paramType.kind() == Kind.TYPE_VARIABLE) {
            // we need to handle this specially since we want the actual declared type here and not the resolved type
            // that toClassName(...) gives us
            TypeVariable typeVariable = paramType.asTypeVariable();
            if (typeVariable.bounds().isEmpty()) {
                declaredUnresolvedType = Object.class.getName();
            } else {
                declaredUnresolvedType = typeVariable.bounds().get(0).name().toString();
            }

        } else {
            declaredUnresolvedType = declaredType;
        }

        return new DeclaredTypes(declaredType, declaredUnresolvedType);
    }

    protected void handleOtherParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            PARAM builder, String elementType) {
    }

    protected void handleSortedSetParam(Map<String, String> existingConverters, String errorLocation,
            boolean hasRuntimeConverters, PARAM builder, String elementType) {
    }

    protected void handleOptionalParam(Map<String, String> existingConverters,
            Map<DotName, AnnotationInstance> parameterAnnotations,
            String errorLocation,
            boolean hasRuntimeConverters, PARAM builder, String elementType, String genericElementType,
            MethodInfo currentMethodInfo) {
    }

    protected void handleSetParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            PARAM builder, String elementType) {
    }

    protected void handleListParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            PARAM builder, String elementType) {
    }

    protected void handleArrayParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            PARAM builder, String elementType) {
    }

    final boolean isContextType(ClassType klass) {
        return contextTypes.contains(klass.name());
    }

    final boolean isParameterContainerType(ClassType klass) {
        return parameterContainerTypes.contains(klass.name());
    }

    private String valueOrDefault(AnnotationValue annotation, String defaultValue) {
        if (annotation == null)
            return defaultValue;
        String val = annotation.asString();
        return val != null && !val.isEmpty() ? val : defaultValue;
    }

    public Set<String> nameBindingNames(ClassInfo selectedAppClass) {
        return NameBindingUtil.nameBindingNames(index, selectedAppClass);
    }

    public Set<String> nameBindingNames(MethodInfo methodInfo, Set<String> forClass) {
        return NameBindingUtil.nameBindingNames(index, methodInfo, forClass);
    }

    protected AnnotationStore getAnnotationStore() {
        return annotationStore;
    }

    protected String getPartMime(Map<DotName, AnnotationInstance> annotations) {
        AnnotationInstance partType = annotations.get(ResteasyReactiveDotNames.PART_TYPE_NAME);
        String mimeType = null;
        if (partType != null && partType.value() != null) {
            mimeType = partType.value().asString();
            // remove what ends up being the default
            if (MediaType.TEXT_PLAIN.equals(mimeType)) {
                mimeType = null;
            }
        }
        return mimeType;
    }

    protected String getSeparator(Map<DotName, AnnotationInstance> annotations) {
        AnnotationInstance separator = annotations.get(ResteasyReactiveDotNames.SEPARATOR);
        String result = null;
        if (separator != null && separator.value() != null) {
            result = separator.value().asString();
        }
        return result;
    }

    protected boolean hasJson(String[] types) {
        if (types == null) {
            return false;
        }
        for (String type : types) {
            if (isJson(type)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isJson(String type) {
        if (type == null) {
            return false;
        }
        return type.startsWith(MediaType.APPLICATION_JSON);
    }

    protected boolean hasJson(ResourceMethod method) {
        return hasJson(method.getProduces()) || hasJson(method.getConsumes()) || isJson(method.getStreamElementType());
    }

    protected boolean hasNoTypesDefined(ResourceMethod method) {
        return (method.getProduces() == null || method.getProduces().length == 0) &&
                (method.getConsumes() == null || method.getConsumes().length == 0) &&
                (method.getStreamElementType() == null);
    }

    protected boolean isDefaultJson(DefaultProducesHandler jsonDefaultProducersHandler,
            DefaultProducesHandler.Context context) {
        List<MediaType> mediaTypes = jsonDefaultProducersHandler.handle(context);
        for (MediaType mediaType : mediaTypes) {
            if (isJson(mediaType.toString())) {
                return true;
            }
        }
        return false;
    }

    protected SerializerScanningResult getSerializerScanningResult() {
        if (serializerScanningResult == null) {
            serializerScanningResult = ResteasyReactiveScanner.scanForSerializers(index, applicationScanningResult);
        }
        return serializerScanningResult;
    }

    protected void warnAboutMissingJsonProviderIfNeeded(ResourceMethod method, MethodInfo info,
            DefaultProducesHandler jsonDefaultProducersHandler,
            DefaultProducesHandler.Context context) {
        if (hasJson(method) || (hasNoTypesDefined(method) && isDefaultJson(jsonDefaultProducersHandler, context))) {
            if (STRING.toString().equals(method.getSimpleReturnType())) { // when returning string, we assume that the method implementation is actually handling to conversion
                return;
            }
            boolean appProvidedJsonReaderExists = appProvidedJsonProviderExists(getSerializerScanningResult().getReaders());
            boolean appProvidedJsonWriterExists = appProvidedJsonProviderExists(getSerializerScanningResult().getWriters());
            if (!appProvidedJsonReaderExists || !appProvidedJsonWriterExists) {
                logMissingJsonWarning(info);
            }
        }
    }

    private boolean appProvidedJsonProviderExists(List<ScannedSerializer> providers) {
        boolean appProvidedJsonReaderExists = false;
        for (ScannedSerializer provider : providers) {
            for (String mt : provider.getMediaTypeStrings()) {
                if (isJson(mt)) {
                    appProvidedJsonReaderExists = true;
                    break;
                }
            }
            if (appProvidedJsonReaderExists) {
                break;
            }
        }
        return appProvidedJsonReaderExists;
    }

    protected void logMissingJsonWarning(MethodInfo info) {

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static abstract class Builder<T extends EndpointIndexer<T, ?, METHOD>, B extends Builder<T, B, METHOD>, METHOD extends ResourceMethod> {
        private Function<String, BeanFactory<Object>> factoryCreator;
        private BlockingDefault defaultBlocking = BlockingDefault.AUTOMATIC;
        private IndexView index;
        private IndexView applicationIndex;
        private Map<String, String> existingConverters = new HashMap<>();
        private Map<DotName, String> scannedResourcePaths;
        private ResteasyReactiveConfig config;
        private AdditionalReaders additionalReaders;
        private Map<DotName, String> httpAnnotationToMethod;
        private Map<String, InjectableBean> injectableBeans;
        private AdditionalWriters additionalWriters;
        private boolean hasRuntimeConverters;
        private Map<DotName, Map<String, String>> classLevelExceptionMappers;
        private Consumer<ResourceMethodCallbackEntry> resourceMethodCallback;
        private Collection<AnnotationsTransformer> annotationsTransformers;
        private ApplicationScanningResult applicationScanningResult;
        private final Set<DotName> contextTypes = new HashSet<>(DEFAULT_CONTEXT_TYPES);
        private final Set<DotName> parameterContainerTypes = new HashSet<>();
        private MultipartReturnTypeIndexerExtension multipartReturnTypeIndexerExtension = new MultipartReturnTypeIndexerExtension() {
            @Override
            public boolean handleMultipartForReturnType(AdditionalWriters additionalWriters, ClassInfo multipartClassInfo,
                    IndexView indexView) {
                return false;
            }
        };
        private TargetJavaVersion targetJavaVersion = new TargetJavaVersion.Unknown();

        private Function<ClassInfo, Supplier<Boolean>> isDisabledCreator = null;

        private Predicate<Map<DotName, AnnotationInstance>> skipMethodParameter = null;

        public B setMultipartReturnTypeIndexerExtension(MultipartReturnTypeIndexerExtension multipartReturnTypeHandler) {
            this.multipartReturnTypeIndexerExtension = multipartReturnTypeHandler;
            return (B) this;
        }

        public B setDefaultBlocking(BlockingDefault defaultBlocking) {
            this.defaultBlocking = defaultBlocking;
            return (B) this;
        }

        public B setHasRuntimeConverters(boolean hasRuntimeConverters) {
            this.hasRuntimeConverters = hasRuntimeConverters;
            return (B) this;
        }

        public B setIndex(IndexView index) {
            this.index = index;
            return (B) this;
        }

        public B setApplicationIndex(IndexView index) {
            this.applicationIndex = index;
            return (B) this;
        }

        public B addContextType(DotName contextType) {
            this.contextTypes.add(contextType);
            return (B) this;
        }

        public B addContextTypes(Collection<DotName> contextTypes) {
            this.contextTypes.addAll(contextTypes);
            return (B) this;
        }

        public B addParameterContainerType(DotName parameterContainerType) {
            this.parameterContainerTypes.add(parameterContainerType);
            return (B) this;
        }

        public B addParameterContainerTypes(Collection<DotName> parameterContainerTypes) {
            this.parameterContainerTypes.addAll(parameterContainerTypes);
            return (B) this;
        }

        public B setExistingConverters(Map<String, String> existingConverters) {
            this.existingConverters = existingConverters;
            return (B) this;
        }

        public B setScannedResourcePaths(Map<DotName, String> scannedResourcePaths) {
            this.scannedResourcePaths = scannedResourcePaths;
            return (B) this;
        }

        public B setFactoryCreator(Function<String, BeanFactory<Object>> factoryCreator) {
            this.factoryCreator = factoryCreator;
            return (B) this;
        }

        public B setConfig(ResteasyReactiveConfig config) {
            this.config = config;
            return (B) this;
        }

        public B setAdditionalReaders(AdditionalReaders additionalReaders) {
            this.additionalReaders = additionalReaders;
            return (B) this;
        }

        public B setHttpAnnotationToMethod(Map<DotName, String> httpAnnotationToMethod) {
            this.httpAnnotationToMethod = httpAnnotationToMethod;
            return (B) this;
        }

        public B setInjectableBeans(Map<String, InjectableBean> injectableBeans) {
            this.injectableBeans = injectableBeans;
            return (B) this;
        }

        public B setAdditionalWriters(AdditionalWriters additionalWriters) {
            this.additionalWriters = additionalWriters;
            return (B) this;
        }

        public B setClassLevelExceptionMappers(Map<DotName, Map<String, String>> classLevelExceptionMappers) {
            this.classLevelExceptionMappers = classLevelExceptionMappers;
            return (B) this;
        }

        public B setResourceMethodCallback(Consumer<ResourceMethodCallbackEntry> resourceMethodCallback) {
            this.resourceMethodCallback = resourceMethodCallback;
            return (B) this;
        }

        public B setAnnotationsTransformers(Collection<AnnotationsTransformer> annotationsTransformers) {
            this.annotationsTransformers = annotationsTransformers;
            return (B) this;
        }

        public B setApplicationScanningResult(ApplicationScanningResult applicationScanningResult) {
            this.applicationScanningResult = applicationScanningResult;
            return (B) this;
        }

        public B setTargetJavaVersion(TargetJavaVersion targetJavaVersion) {
            this.targetJavaVersion = targetJavaVersion;
            return (B) this;
        }

        public B setIsDisabledCreator(
                Function<ClassInfo, Supplier<Boolean>> isDisabledCreator) {
            this.isDisabledCreator = isDisabledCreator;
            return (B) this;
        }

        public B setSkipMethodParameter(
                Predicate<Map<DotName, AnnotationInstance>> skipMethodParameter) {
            this.skipMethodParameter = skipMethodParameter;
            return (B) this;
        }

        public abstract T build();
    }

    public static class BasicResourceClassInfo {
        private final String path;
        private final String[] produces;
        private final String[] consumes;
        private final Set<String> pathParameters;
        private final String streamElementType;

        public BasicResourceClassInfo(String path, String[] produces, String[] consumes, Set<String> pathParameters,
                String streamElementType) {
            this.path = path;
            this.produces = produces;
            this.consumes = consumes;
            this.pathParameters = pathParameters;
            this.streamElementType = streamElementType;
        }

        public String getPath() {
            return path;
        }

        public String[] getProduces() {
            return produces;
        }

        public String[] getConsumes() {
            return consumes;
        }

        public Set<String> getPathParameters() {
            return pathParameters;
        }

        public String getStreamElementType() {
            return streamElementType;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class ResourceMethodCallbackEntry {

        private final EndpointIndexer indexer;
        private final IndexView index;
        private final BasicResourceClassInfo basicResourceClassInfo;
        private final ClassInfo actualEndpointInfo;
        private final MethodInfo methodInfo;
        private final ResourceMethod resourceMethod;

        public ResourceMethodCallbackEntry(EndpointIndexer indexer, IndexView index,
                BasicResourceClassInfo basicResourceClassInfo,
                ClassInfo actualEndpointInfo,
                MethodInfo methodInfo, ResourceMethod resourceMethod) {
            this.indexer = indexer;
            this.index = index;
            this.basicResourceClassInfo = basicResourceClassInfo;
            this.methodInfo = methodInfo;
            this.actualEndpointInfo = actualEndpointInfo;
            this.resourceMethod = resourceMethod;
        }

        public BasicResourceClassInfo getBasicResourceClassInfo() {
            return basicResourceClassInfo;
        }

        public MethodInfo getMethodInfo() {
            return methodInfo;
        }

        public ClassInfo getActualEndpointInfo() {
            return actualEndpointInfo;
        }

        public ResourceMethod getResourceMethod() {
            return resourceMethod;
        }

        public boolean additionalRegisterClassForReflectionCheck() {
            return indexer.additionalRegisterClassForReflectionCheck(this);
        }
    }

    public static class DeclaredTypes {
        private final String declaredType;
        private final String declaredUnresolvedType;

        public DeclaredTypes(String declaredType, String declaredUnresolvedType) {
            this.declaredType = declaredType;
            this.declaredUnresolvedType = declaredUnresolvedType;
        }

        public String getDeclaredType() {
            return declaredType;
        }

        public String getDeclaredUnresolvedType() {
            return declaredUnresolvedType;
        }
    }

    /**
     * @return true if return type is compatible to handle multipart types.
     */
    public interface MultipartReturnTypeIndexerExtension {
        boolean handleMultipartForReturnType(AdditionalWriters additionalWriters, ClassInfo multipartClassInfo,
                IndexView index);
    }

    /**
     * @return true if return type is compatible to handle multipart types.
     */
    public interface MultipartParameterIndexerExtension {
        void handleMultipartParameter(ClassInfo multipartClassInfo, IndexView index);
    }

    private static final class FoundEndpoint {
        private final ClassInfo classInfo;
        private final Set<String> classNameBindings;
        private final MethodInfo methodInfo;
        private final DotName httpMethod;

        private FoundEndpoint(ClassInfo classInfo, Set<String> classNameBindings, MethodInfo methodInfo, DotName httpMethod) {
            this.classInfo = classInfo;
            this.classNameBindings = classNameBindings;
            this.methodInfo = methodInfo;
            this.httpMethod = httpMethod;
        }
    }
}
