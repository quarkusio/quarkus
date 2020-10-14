package io.quarkus.rest.deployment.framework;

import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BEAN_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BIG_DECIMAL;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BIG_INTEGER;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BLOCKING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BOOLEAN;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.CHARACTER;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.COMPLETION_STAGE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.CONSUMES;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.CONTEXT;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.COOKIE_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.DEFAULT_VALUE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.DOUBLE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.FLOAT;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.FORM_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.HEADER_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.INTEGER;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.JSONP_JSON_ARRAY;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.JSONP_JSON_NUMBER;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.JSONP_JSON_OBJECT;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.JSONP_JSON_STRING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.JSONP_JSON_STRUCTURE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.JSONP_JSON_VALUE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.LIST;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.LONG;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.MATRIX_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.MULTI;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.MULTI_VALUED_MAP;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.NAME_BINDING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PATH;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PATH_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PATH_SEGMENT;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRIMITIVE_BOOLEAN;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRIMITIVE_CHAR;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRIMITIVE_DOUBLE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRIMITIVE_FLOAT;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRIMITIVE_INTEGER;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRIMITIVE_LONG;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRODUCES;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.QUERY_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.REQUIRE_CDI_REQUEST_SCOPE;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SET;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SORTED_SET;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.STRING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SUSPENDED;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.UNI;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.runtime.QuarkusRestConfig;
import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.core.parameters.converters.GeneratedParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ListConverter;
import io.quarkus.rest.runtime.core.parameters.converters.NoopParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverterSupplier;
import io.quarkus.rest.runtime.core.parameters.converters.PathSegmentParamConverter;
import io.quarkus.rest.runtime.core.parameters.converters.RuntimeResolvedConverter;
import io.quarkus.rest.runtime.core.parameters.converters.SetConverter;
import io.quarkus.rest.runtime.core.parameters.converters.SortedSetConverter;
import io.quarkus.rest.runtime.model.BeanParamInfo;
import io.quarkus.rest.runtime.model.InjectableBean;
import io.quarkus.rest.runtime.model.MethodParameter;
import io.quarkus.rest.runtime.model.ParameterType;
import io.quarkus.rest.runtime.model.ResourceClass;
import io.quarkus.rest.runtime.model.ResourceMethod;
import io.quarkus.rest.runtime.model.RestClientInterface;
import io.quarkus.rest.runtime.providers.serialisers.FormUrlEncodedProvider;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonArrayHandler;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonObjectHandler;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonStructureHandler;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonValueHandler;
import io.quarkus.rest.runtime.spi.EndpointInvoker;
import io.quarkus.runtime.util.HashUtil;

public class EndpointIndexer {

    private static final Map<String, String> primitiveTypes;
    private static final Map<DotName, Class<?>> supportedReaderJavaTypes;
    private static final Set<DotName> SUPPORTED_TEXT_PLAIN_READER_TYPES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(PRIMITIVE_INTEGER, PRIMITIVE_LONG, PRIMITIVE_FLOAT, PRIMITIVE_DOUBLE,
                    PRIMITIVE_BOOLEAN, PRIMITIVE_CHAR, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, CHARACTER, BIG_DECIMAL,
                    BIG_INTEGER)));

    private static final Logger log = Logger.getLogger(EndpointInvoker.class);
    private static final String[] PRODUCES_PLAIN_TEXT_NEGOTIATED = new String[] { MediaType.TEXT_PLAIN, MediaType.WILDCARD };
    private static final String[] PRODUCES_PLAIN_TEXT = new String[] { MediaType.TEXT_PLAIN };

    static {
        Map<String, String> prims = new HashMap<>();
        prims.put(byte.class.getName(), Byte.class.getName());
        prims.put(Byte.class.getName(), Byte.class.getName());
        prims.put(boolean.class.getName(), Boolean.class.getName());
        prims.put(Boolean.class.getName(), Boolean.class.getName());
        prims.put(char.class.getName(), Character.class.getName());
        prims.put(Character.class.getName(), Character.class.getName());
        prims.put(short.class.getName(), Short.class.getName());
        prims.put(Short.class.getName(), Short.class.getName());
        prims.put(int.class.getName(), Integer.class.getName());
        prims.put(Integer.class.getName(), Integer.class.getName());
        prims.put(float.class.getName(), Float.class.getName());
        prims.put(Float.class.getName(), Float.class.getName());
        prims.put(double.class.getName(), Double.class.getName());
        prims.put(Double.class.getName(), Double.class.getName());
        prims.put(long.class.getName(), Long.class.getName());
        prims.put(Long.class.getName(), Long.class.getName());
        primitiveTypes = Collections.unmodifiableMap(prims);

        Map<DotName, Class<?>> supportedReaderJavaTps = new HashMap<>();
        supportedReaderJavaTps.put(PRIMITIVE_BOOLEAN, boolean.class);
        supportedReaderJavaTps.put(PRIMITIVE_DOUBLE, double.class);
        supportedReaderJavaTps.put(PRIMITIVE_FLOAT, float.class);
        supportedReaderJavaTps.put(PRIMITIVE_LONG, long.class);
        supportedReaderJavaTps.put(PRIMITIVE_INTEGER, int.class);
        supportedReaderJavaTps.put(PRIMITIVE_CHAR, char.class);
        supportedReaderJavaTps.put(BOOLEAN, Boolean.class);
        supportedReaderJavaTps.put(DOUBLE, Double.class);
        supportedReaderJavaTps.put(FLOAT, Float.class);
        supportedReaderJavaTps.put(LONG, Long.class);
        supportedReaderJavaTps.put(INTEGER, Integer.class);
        supportedReaderJavaTps.put(CHARACTER, Character.class);
        supportedReaderJavaTps.put(BIG_DECIMAL, BigDecimal.class);
        supportedReaderJavaTps.put(BIG_INTEGER, BigInteger.class);
        supportedReaderJavaTypes = Collections.unmodifiableMap(supportedReaderJavaTps);
    }

    private final IndexView index;
    private final BeanContainer beanContainer;
    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer;
    private final QuarkusRestRecorder recorder;
    private final Map<String, String> existingConverters;
    private final Map<DotName, String> scannedResourcePaths;
    private final QuarkusRestConfig config;
    private final AdditionalReaders additionalReaders;
    private final Map<DotName, String> httpAnnotationToMethod;
    private final Map<String, InjectableBean> injectableBeans;
    private final AdditionalWriters additionalWriters;
    private final MethodCreator initConverters;
    private final boolean hasRuntimeConverters;

    EndpointIndexer(Builder builder) {
        this.index = builder.index;
        this.beanContainer = builder.beanContainer;
        this.generatedClassBuildItemBuildProducer = builder.generatedClassBuildItemBuildProducer;
        this.bytecodeTransformerBuildItemBuildProducer = builder.bytecodeTransformerBuildItemBuildProducer;
        this.recorder = builder.recorder;
        this.existingConverters = builder.existingConverters;
        this.scannedResourcePaths = builder.scannedResourcePaths;
        this.config = builder.config;
        this.additionalReaders = builder.additionalReaders;
        this.httpAnnotationToMethod = builder.httpAnnotationToMethod;
        this.injectableBeans = builder.injectableBeans;
        this.additionalWriters = builder.additionalWriters;
        this.initConverters = builder.initConverters;
        this.hasRuntimeConverters = builder.hasRuntimeConverters;
    }

    public ResourceClass createEndpoints(ClassInfo classInfo) {
        try {
            String path = scannedResourcePaths.get(classInfo.name());
            List<ResourceMethod> methods = createEndpoints(classInfo, classInfo, new HashSet<>(),
                    generatedClassBuildItemBuildProducer, bytecodeTransformerBuildItemBuildProducer,
                    recorder, existingConverters, config, additionalReaders,
                    additionalWriters, injectableBeans, httpAnnotationToMethod, initConverters, hasRuntimeConverters);
            ResourceClass clazz = new ResourceClass();
            clazz.getMethods().addAll(methods);
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                clazz.setPath(path);
            }
            clazz.setFactory(recorder.factory(clazz.getClassName(), beanContainer));

            // get an InjectableBean view of our class
            InjectableBean injectableBean = scanInjectableBean(classInfo, classInfo,
                    generatedClassBuildItemBuildProducer,
                    bytecodeTransformerBuildItemBuildProducer, existingConverters,
                    additionalReaders, injectableBeans, initConverters, hasRuntimeConverters);

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

            return clazz;
        } catch (Exception e) {
            if (Modifier.isInterface(classInfo.flags()) || Modifier.isAbstract(classInfo.flags())) {
                //kinda bogus, but we just ignore failed interfaces for now
                //they can have methods that are not valid until they are actually extended by a concrete type
                log.debug("Ignoring interface " + classInfo.name(), e);
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    private InjectableBean scanInjectableBean(ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer,
            Map<String, String> existingConverters,
            AdditionalReaders additionalReaders,
            Map<String, InjectableBean> injectableBeans,
            MethodCreator initConverters,
            boolean hasRuntimeConverters) {

        // do not scan a bean twice
        String currentTypeName = currentClassInfo.name().toString();
        InjectableBean currentInjectableBean = injectableBeans.get(currentTypeName);
        if (currentInjectableBean != null) {
            return currentInjectableBean;
        }
        currentInjectableBean = new BeanParamInfo();
        injectableBeans.put(currentTypeName, currentInjectableBean);

        // LinkedHashMap the TCK expects that fields annotated with @BeanParam are handled last
        Map<FieldInfo, ParameterExtractor> fieldExtractors = new LinkedHashMap<>();
        Map<FieldInfo, ParameterExtractor> beanParamFields = new LinkedHashMap<>();
        for (FieldInfo field : currentClassInfo.fields()) {
            Map<DotName, AnnotationInstance> annotations = new HashMap<>();
            for (AnnotationInstance i : field.annotations()) {
                annotations.put(i.name(), i);
            }
            ParameterExtractor extractor = new ParameterExtractor(currentClassInfo, actualEndpointInfo,
                    generatedClassBuildItemBuildProducer, existingConverters, additionalReaders, false, false,
                    annotations, field.type(), field.toString(), true, hasRuntimeConverters);
            ParameterExtractor result = extractor.invoke();
            if ((result.getType() != null) && (result.getType() != ParameterType.BEAN)) {
                //BODY means no annotation, so for fields not injectable
                fieldExtractors.put(field, extractor);
            }
            if (result.getConverter() != null) {
                initConverters.invokeStaticMethod(MethodDescriptor.ofMethod(currentTypeName,
                        ClassInjectorTransformer.INIT_CONVERTER_METHOD_NAME + field.name(),
                        void.class, QuarkusRestDeployment.class),
                        initConverters.getMethodParam(0));
            }
            if (result.getType() == ParameterType.BEAN) {
                beanParamFields.put(field, extractor);
                // transform the bean param
                // FIXME: pretty sure this doesn't work with generics
                ClassInfo beanParamClassInfo = index.getClassByName(field.type().name());
                InjectableBean injectableBean = scanInjectableBean(beanParamClassInfo, actualEndpointInfo,
                        generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                        existingConverters, additionalReaders, injectableBeans, initConverters, hasRuntimeConverters);
                // inherit form param requirement from field
                if (injectableBean.isFormParamRequired()) {
                    currentInjectableBean.setFormParamRequired(true);
                }
            } else if (result.getType() == ParameterType.FORM) {
                // direct form param requirement
                currentInjectableBean.setFormParamRequired(true);
            }
        }
        // the TCK expects that fields annotated with @BeanParam are handled last
        fieldExtractors.putAll(beanParamFields);

        DotName superClassName = currentClassInfo.superName();
        boolean superTypeIsInjectable = false;
        if (superClassName != null && !superClassName.equals(DotNames.OBJECT)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                InjectableBean superInjectableBean = scanInjectableBean(superClass, actualEndpointInfo,
                        generatedClassBuildItemBuildProducer,
                        bytecodeTransformerBuildProducer,
                        existingConverters, additionalReaders, injectableBeans, initConverters, hasRuntimeConverters);
                superTypeIsInjectable = superInjectableBean.isInjectionRequired();
                // inherit form param requirement from supertype
                if (superInjectableBean.isFormParamRequired()) {
                    currentInjectableBean.setFormParamRequired(true);
                }
            }
        }

        if (!fieldExtractors.isEmpty()) {
            bytecodeTransformerBuildProducer.produce(new BytecodeTransformerBuildItem(currentTypeName,
                    new ClassInjectorTransformer(fieldExtractors, superTypeIsInjectable)));
        }
        currentInjectableBean.setInjectionRequired(!fieldExtractors.isEmpty() || superTypeIsInjectable);
        return currentInjectableBean;
    }

    public RestClientInterface createClientProxy(ClassInfo classInfo,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer,
            String path) {
        try {
            List<ResourceMethod> methods = createEndpoints(classInfo, classInfo, new HashSet<>(),
                    generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                    recorder, existingConverters, config, additionalReaders,
                    additionalWriters, injectableBeans, httpAnnotationToMethod, initConverters, hasRuntimeConverters);
            RestClientInterface clazz = new RestClientInterface();
            clazz.getMethods().addAll(methods);
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                clazz.setPath(path);
            }
            return clazz;
        } catch (Exception e) {
            //kinda bogus, but we just ignore failed interfaces for now
            //they can have methods that are not valid until they are actually extended by a concrete type
            log.debug("Ignoring interface for creating client proxy" + classInfo.name(), e);
            return null;
        }
    }

    private List<ResourceMethod> createEndpoints(ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo, Set<String> seenMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer,
            QuarkusRestRecorder recorder,
            Map<String, String> existingConverters, QuarkusRestConfig config, AdditionalReaders additionalReaders,
            AdditionalWriters additionalWriters, Map<String, InjectableBean> injectableBeans,
            Map<DotName, String> httpAnnotationToMethod, MethodCreator initConverters, boolean hasRuntimeConverters) {
        List<ResourceMethod> ret = new ArrayList<>();
        String[] classProduces = extractProducesConsumesValues(currentClassInfo.classAnnotation(PRODUCES));
        String[] classConsumes = extractProducesConsumesValues(currentClassInfo.classAnnotation(CONSUMES));
        Set<String> classNameBindings = nameBindingNames(currentClassInfo);

        for (DotName httpMethod : httpAnnotationToMethod.keySet()) {
            List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(httpMethod);
            if (foundMethods != null) {
                for (AnnotationInstance annotation : foundMethods) {
                    MethodInfo info = annotation.target().asMethod();
                    if (!hasProperModifiers(info)) {
                        continue;
                    }
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(info.annotation(PATH));
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    } else {
                        methodPath = "/";
                    }
                    ResourceMethod method = createResourceMethod(currentClassInfo, actualEndpointInfo,
                            generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                            recorder, classProduces, classConsumes, classNameBindings, httpMethod, info, methodPath,
                            existingConverters,
                            config, additionalReaders, additionalWriters, httpAnnotationToMethod, injectableBeans,
                            initConverters, hasRuntimeConverters);

                    ret.add(method);
                }
            }
        }
        //now resource locator methods
        List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(PATH);
        if (foundMethods != null) {
            for (AnnotationInstance annotation : foundMethods) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo info = annotation.target().asMethod();
                    if (!hasProperModifiers(info)) {
                        continue;
                    }
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(annotation);
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    }
                    ResourceMethod method = createResourceMethod(currentClassInfo, actualEndpointInfo,
                            generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                            recorder, classProduces, classConsumes, classNameBindings, null, info, methodPath,
                            existingConverters, config, additionalReaders, additionalWriters, httpAnnotationToMethod,
                            injectableBeans, initConverters, hasRuntimeConverters);
                    ret.add(method);
                }
            }
        }

        DotName superClassName = currentClassInfo.superName();
        if (superClassName != null && !superClassName.equals(DotNames.OBJECT)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                ret.addAll(createEndpoints(superClass, actualEndpointInfo, seenMethods,
                        generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                        recorder, existingConverters, config, additionalReaders,
                        additionalWriters, injectableBeans, httpAnnotationToMethod, initConverters, hasRuntimeConverters));
            }
        }
        List<DotName> interfaces = currentClassInfo.interfaceNames();
        for (DotName i : interfaces) {
            ClassInfo superClass = index.getClassByName(i);
            if (superClass != null) {
                ret.addAll(createEndpoints(superClass, actualEndpointInfo, seenMethods,
                        generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                        recorder, existingConverters, config, additionalReaders,
                        additionalWriters, injectableBeans, httpAnnotationToMethod, initConverters, hasRuntimeConverters));
            }
        }
        return ret;
    }

    private boolean hasProperModifiers(MethodInfo info) {
        if ((info.flags() & Modifier.PUBLIC) == 0) {
            log.warn("Method '" + info.name() + " of Resource class '" + info.declaringClass().name()
                    + "' it not public and will therefore be ignored");
            return false;
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            log.warn("Method '" + info.name() + " of Resource class '" + info.declaringClass().name()
                    + "' it static and will therefore be ignored");
            return false;
        }
        return true;
    }

    private ResourceMethod createResourceMethod(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer,
            QuarkusRestRecorder recorder,
            String[] classProduces, String[] classConsumes, Set<String> classNameBindings, DotName httpMethod, MethodInfo info,
            String methodPath, Map<String, String> existingConverters, QuarkusRestConfig config,
            AdditionalReaders additionalReaders, AdditionalWriters additionalWriters,
            Map<DotName, String> httpAnnotationToMethod,
            Map<String, InjectableBean> injectableBeans, MethodCreator initConverters, boolean hasRuntimeConverters) {
        try {
            Map<DotName, AnnotationInstance>[] parameterAnnotations = new Map[info.parameters().size()];
            MethodParameter[] methodParameters = new MethodParameter[info.parameters()
                    .size()];
            for (int paramPos = 0; paramPos < info.parameters().size(); ++paramPos) {
                parameterAnnotations[paramPos] = new HashMap<>();
            }
            for (AnnotationInstance i : info.annotations()) {
                if (i.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    parameterAnnotations[i.target().asMethodParameter().position()].put(i.name(), i);
                }
            }
            String[] consumes = extractProducesConsumesValues(info.annotation(CONSUMES), classConsumes);
            boolean suspended = false;
            boolean sse = false;
            boolean formParamRequired = false;
            for (int i = 0; i < methodParameters.length; ++i) {
                Map<DotName, AnnotationInstance> anns = parameterAnnotations[i];
                boolean encoded = anns.containsKey(QuarkusRestDotNames.ENCODED);
                Type paramType = info.parameters().get(i);
                String errorLocation = "method " + info + " on class " + info.declaringClass();

                ParameterExtractor parameterExtractor = new ParameterExtractor(currentClassInfo, actualEndpointInfo,
                        generatedClassBuildItemBuildProducer, existingConverters, additionalReaders, suspended, sse,
                        anns, paramType, errorLocation, false, hasRuntimeConverters).invoke();
                suspended |= parameterExtractor.isSuspended();
                sse |= parameterExtractor.isSse();
                String name = parameterExtractor.getName();
                String defaultValue = parameterExtractor.getDefaultValue();
                ParameterType type = parameterExtractor.getType();
                String elementType = parameterExtractor.getElementType();
                boolean single = parameterExtractor.isSingle();
                ParameterConverterSupplier converter = parameterExtractor.getConverter();
                if (defaultValue == null && paramType.kind() == Type.Kind.PRIMITIVE) {
                    defaultValue = "0";
                }
                methodParameters[i] = new MethodParameter(name,
                        elementType, toClassName(paramType, currentClassInfo, actualEndpointInfo, index), type, single,
                        converter, defaultValue, parameterExtractor.isObtainedAsCollection(), encoded);

                if (type == ParameterType.BEAN) {
                    // transform the bean param
                    ClassInfo beanParamClassInfo = index.getClassByName(paramType.name());
                    InjectableBean injectableBean = scanInjectableBean(beanParamClassInfo,
                            actualEndpointInfo,
                            generatedClassBuildItemBuildProducer, bytecodeTransformerBuildProducer,
                            existingConverters, additionalReaders, injectableBeans, initConverters, hasRuntimeConverters);
                    if (injectableBean.isFormParamRequired()) {
                        formParamRequired = true;
                    }
                } else if (type == ParameterType.FORM) {
                    formParamRequired = true;
                }
            }
            Type nonAsyncReturnType = getNonAsyncReturnType(info.returnType());
            addWriterForType(additionalWriters, nonAsyncReturnType);

            String[] produces = extractProducesConsumesValues(info.annotation(PRODUCES), classProduces);
            produces = applyDefaultProduces(produces, nonAsyncReturnType);
            Set<String> nameBindingNames = nameBindingNames(info, classNameBindings);
            boolean blocking = config.blocking;
            AnnotationInstance blockingAnnotation = getInheritableAnnotation(info, BLOCKING);
            if (blockingAnnotation != null) {
                AnnotationValue value = blockingAnnotation.value();
                if (value != null) {
                    blocking = value.asBoolean();
                } else {
                    blocking = true;
                }
            }
            boolean cdiRequestScopeRequired = true;
            AnnotationInstance cdiRequestScopeRequiredAnnotation = getInheritableAnnotation(info, REQUIRE_CDI_REQUEST_SCOPE);
            if (cdiRequestScopeRequiredAnnotation != null) {
                AnnotationValue value = cdiRequestScopeRequiredAnnotation.value();
                if (value != null) {
                    cdiRequestScopeRequired = value.asBoolean();
                } else {
                    cdiRequestScopeRequired = true;
                }
            }

            ResourceMethod method = new ResourceMethod()
                    .setHttpMethod(httpMethod == null ? null : httpAnnotationToMethod.get(httpMethod))
                    .setPath(methodPath)
                    .setConsumes(consumes)
                    .setProduces(produces)
                    .setNameBindingNames(nameBindingNames)
                    .setName(info.name())
                    .setBlocking(blocking)
                    .setSuspended(suspended)
                    .setSse(sse)
                    .setFormParamRequired(formParamRequired)
                    .setCDIRequestScopeRequired(cdiRequestScopeRequired)
                    .setParameters(methodParameters)
                    .setSimpleReturnType(toClassName(info.returnType(), currentClassInfo, actualEndpointInfo, index))
                    // FIXME: resolved arguments ?
                    .setReturnType(AsmUtil.getSignature(info.returnType(), new Function<String, String>() {
                        @Override
                        public String apply(String v) {
                            //we attempt to resolve type variables
                            ClassInfo declarer = info.declaringClass();
                            int pos = -1;
                            for (;;) {
                                if (declarer == null) {
                                    return null;
                                }
                                List<TypeVariable> typeParameters = declarer.typeParameters();
                                for (int i = 0; i < typeParameters.size(); i++) {
                                    TypeVariable tv = typeParameters.get(i);
                                    if (tv.identifier().equals(v)) {
                                        pos = i;
                                    }
                                }
                                if (pos != -1) {
                                    break;
                                }
                                declarer = index.getClassByName(declarer.superName());
                            }
                            Type type = JandexUtil
                                    .resolveTypeParameters(info.declaringClass().name(), declarer.name(), index)
                                    .get(pos);
                            if (type.kind() == Type.Kind.TYPE_VARIABLE && type.asTypeVariable().identifier().equals(v)) {
                                List<Type> bounds = type.asTypeVariable().bounds();
                                if (bounds.isEmpty()) {
                                    return "Ljava/lang/Object;";
                                }
                                return AsmUtil.getSignature(bounds.get(0), this);
                            } else {
                                return AsmUtil.getSignature(type, this);
                            }
                        }
                    }));

            StringBuilder sigBuilder = new StringBuilder();
            sigBuilder.append(method.getName())
                    .append(method.getReturnType());
            for (MethodParameter t : method.getParameters()) {
                sigBuilder.append(t);
            }
            String baseName = currentClassInfo.name() + "$quarkusrestinvoker$" + method.getName() + "_"
                    + HashUtil.sha1(sigBuilder.toString());
            try (ClassCreator classCreator = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                    Object.class.getName(), EndpointInvoker.class.getName())) {
                MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
                ResultHandle[] args = new ResultHandle[method.getParameters().length];
                ResultHandle array = mc.getMethodParam(1);
                for (int i = 0; i < method.getParameters().length; ++i) {
                    args[i] = mc.readArrayValue(array, i);
                }
                ResultHandle res;
                if (Modifier.isInterface(currentClassInfo.flags())) {
                    res = mc.invokeInterfaceMethod(info, mc.getMethodParam(0), args);
                } else {
                    res = mc.invokeVirtualMethod(info, mc.getMethodParam(0), args);
                }
                if (info.returnType().kind() == Type.Kind.VOID) {
                    mc.returnValue(mc.loadNull());
                } else {
                    mc.returnValue(res);
                }
            }
            method.setInvoker(recorder.invoker(baseName));
            return method;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process method " + info.declaringClass().name() + "#" + info.toString(), e);
        }
    }

    private String[] applyDefaultProduces(String[] produces, Type nonAsyncReturnType) {
        if (produces != null && produces.length != 0)
            return produces;
        // FIXME: primitives
        if (STRING.equals(nonAsyncReturnType.name()))
            return config.singleDefaultProduces ? PRODUCES_PLAIN_TEXT : PRODUCES_PLAIN_TEXT_NEGOTIATED;
        // FIXME: JSON
        return produces;
    }

    private Type getNonAsyncReturnType(Type returnType) {
        switch (returnType.kind()) {
            case ARRAY:
            case CLASS:
            case PRIMITIVE:
            case VOID:
                return returnType;
            case PARAMETERIZED_TYPE:
                // NOTE: same code in QuarkusRestRecorder.getNonAsyncReturnType
                ParameterizedType parameterizedType = returnType.asParameterizedType();
                if (COMPLETION_STAGE.equals(parameterizedType.name())
                        || UNI.equals(parameterizedType.name())
                        || MULTI.equals(parameterizedType.name())) {
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

    private static void addWriterForType(AdditionalWriters additionalWriters, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalWriters.add(JsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalWriters.add(JsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalWriters.add(JsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalWriters.add(JsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
        }
    }

    private static void addReaderForType(AdditionalReaders additionalReaders, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalReaders.add(JsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalReaders.add(JsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalReaders.add(JsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalReaders.add(JsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getSupportedReaderJavaClass(Type paramType) {
        Class<T> result = (Class<T>) supportedReaderJavaTypes.get(paramType.name());
        return Objects.requireNonNull(result);
    }

    private static AnnotationInstance getInheritableAnnotation(MethodInfo info, DotName name) {
        // try method first, class second
        AnnotationInstance annotation = info.annotation(name);
        if (annotation == null) {
            annotation = info.declaringClass().classAnnotation(name);
        }
        return annotation;
    }

    /**
     * Returns the class names of the {@code @NameBinding} annotations or null if non are present
     */
    public Set<String> nameBindingNames(ClassInfo classInfo) {
        return nameBindingNames(instanceDotNames(classInfo.classAnnotations()));
    }

    private Set<String> nameBindingNames(MethodInfo methodInfo, Set<String> forClass) {
        Set<String> fromMethod = nameBindingNames(instanceDotNames(methodInfo.annotations()));
        if (fromMethod.isEmpty()) {
            return forClass;
        }
        fromMethod.addAll(forClass);
        return fromMethod;
    }

    private static List<DotName> instanceDotNames(Collection<AnnotationInstance> instances) {
        List<DotName> result = new ArrayList<>(instances.size());
        for (AnnotationInstance instance : instances) {
            result.add(instance.name());
        }
        return result;
    }

    private Set<String> nameBindingNames(Collection<DotName> annotations) {
        Set<String> result = new HashSet<>();
        for (DotName classAnnotationDotName : annotations) {
            if (classAnnotationDotName.equals(PATH) || classAnnotationDotName.equals(CONSUMES)
                    || classAnnotationDotName.equals(PRODUCES)) {
                continue;
            }
            ClassInfo classAnnotation = index.getClassByName(classAnnotationDotName);
            if (classAnnotation == null) {
                return result;
            }
            if (classAnnotation.classAnnotation(NAME_BINDING) != null) {
                result.add(classAnnotation.name().toString());
            }
        }
        return result;
    }

    private static String methodDescriptor(MethodInfo info) {
        return info.name() + ":" + AsmUtil.getDescriptor(info, s -> null);
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

    private static String[] extractProducesConsumesValues(AnnotationInstance annotation, String[] defaultValue) {
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
        String[] originalStrings = annotation.value().asStringArray();
        if (originalStrings.length > 0) {
            List<String> result = new ArrayList<>(originalStrings.length);
            for (String s : originalStrings) {
                String[] trimmed = s.split(","); // spec says that the value can be a comma separated list...
                for (String t : trimmed) {
                    result.add(t.trim());
                }
            }
            return result.toArray(new String[0]);
        } else {
            return originalStrings;
        }

    }

    public static String readStringValue(AnnotationInstance annotationInstance) {
        String classProduces = null;
        if (annotationInstance != null) {
            classProduces = annotationInstance.value().asString();
        }
        return classProduces;
    }

    private static String toClassName(Type indexType, ClassInfo currentClass, ClassInfo actualEndpointClass,
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
            case TYPE_VARIABLE:
                TypeVariable typeVariable = indexType.asTypeVariable();
                if (typeVariable.bounds().isEmpty()) {
                    return Object.class.getName();
                }
                int pos = -1;
                for (int i = 0; i < currentClass.typeParameters().size(); ++i) {
                    if (currentClass.typeParameters().get(i).identifier().equals(typeVariable.identifier())) {
                        pos = i;
                        break;
                    }
                }
                if (pos != -1) {
                    List<Type> params = JandexUtil.resolveTypeParameters(actualEndpointClass.name(), currentClass.name(),
                            indexView);

                    Type resolved = params.get(pos);
                    if (resolved.kind() != Type.Kind.TYPE_VARIABLE
                            || !resolved.asTypeVariable().identifier().equals(typeVariable.identifier())) {
                        return toClassName(resolved, currentClass, actualEndpointClass, indexView);
                    }
                }
                return toClassName(typeVariable.bounds().get(0), currentClass, actualEndpointClass, indexView);
            default:
                throw new RuntimeException("Unknown parameter type " + indexType);
        }
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

    class ParameterExtractor {
        private ClassInfo currentClassInfo;
        private ClassInfo actualEndpointInfo;
        private BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
        private Map<String, String> existingEndpoints;
        private AdditionalReaders additionalReaders;
        private boolean suspended;
        private boolean sse;
        private Map<DotName, AnnotationInstance> anns;
        private Type paramType;
        private String errorLocation;
        private String name;
        private String defaultValue;
        private ParameterType type;
        private String elementType;
        private boolean single;
        private ParameterConverterSupplier converter;
        private final boolean field;
        private boolean hasRuntimeConverters;

        public ParameterExtractor(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
                BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
                Map<String, String> existingConverters, AdditionalReaders additionalReaders, boolean suspended, boolean sse,
                Map<DotName, AnnotationInstance> anns, Type paramType, String errorLocation, boolean field,
                boolean hasRuntimeConverters) {
            this.currentClassInfo = currentClassInfo;
            this.actualEndpointInfo = actualEndpointInfo;
            this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
            this.existingEndpoints = existingConverters;
            this.additionalReaders = additionalReaders;
            this.suspended = suspended;
            this.sse = sse;
            this.anns = anns;
            this.paramType = paramType;
            this.errorLocation = errorLocation;
            this.field = field;
            this.hasRuntimeConverters = hasRuntimeConverters;
        }

        public boolean isObtainedAsCollection() {
            return !single
                    && (type == ParameterType.HEADER
                            || type == ParameterType.MATRIX
                            || type == ParameterType.FORM
                            || type == ParameterType.QUERY);
        }

        public boolean isSuspended() {
            return suspended;
        }

        public boolean isSse() {
            return sse;
        }

        public String getName() {
            return name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public ParameterType getType() {
            return type;
        }

        public String getElementType() {
            return elementType;
        }

        public boolean isSingle() {
            return single;
        }

        public ParameterConverterSupplier getConverter() {
            return converter;
        }

        public ParameterExtractor invoke() {
            name = null;
            AnnotationInstance beanParam = anns.get(BEAN_PARAM);
            AnnotationInstance pathParam = anns.get(PATH_PARAM);
            AnnotationInstance queryParam = anns.get(QUERY_PARAM);
            AnnotationInstance headerParam = anns.get(HEADER_PARAM);
            AnnotationInstance formParam = anns.get(FORM_PARAM);
            AnnotationInstance contextParam = anns.get(CONTEXT);
            AnnotationInstance matrixParam = anns.get(MATRIX_PARAM);
            AnnotationInstance cookieParam = anns.get(COOKIE_PARAM);
            AnnotationInstance defaultValueAnnotation = anns.get(DEFAULT_VALUE);
            AnnotationInstance suspendedAnnotation = anns.get(SUSPENDED);
            defaultValue = null;
            boolean convertable = false;
            if (defaultValueAnnotation != null) {
                defaultValue = defaultValueAnnotation.value().asString();
            }
            if (moreThanOne(pathParam, queryParam, headerParam, formParam, contextParam, cookieParam)) {
                throw new RuntimeException(
                        "Cannot have more than one of @PathParam, @QueryParam, @HeaderParam, @FormParam, @CookieParam, @BeanParam, @Context on "
                                + errorLocation);
            } else if (pathParam != null) {
                name = pathParam.value().asString();
                type = ParameterType.PATH;
                convertable = true;
            } else if (queryParam != null) {
                name = queryParam.value().asString();
                type = ParameterType.QUERY;
                convertable = true;
            } else if (cookieParam != null) {
                name = cookieParam.value().asString();
                type = ParameterType.COOKIE;
                convertable = true;
            } else if (headerParam != null) {
                name = headerParam.value().asString();
                type = ParameterType.HEADER;
                convertable = true;
            } else if (formParam != null) {
                name = formParam.value().asString();
                type = ParameterType.FORM;
                convertable = true;
            } else if (contextParam != null) {
                //this is handled by CDI
                if (field) {
                    return this;
                }
                // no name required
                type = ParameterType.CONTEXT;
            } else if (beanParam != null) {
                // no name required
                type = ParameterType.BEAN;
            } else if (suspendedAnnotation != null) {
                // no name required
                type = ParameterType.ASYNC_RESPONSE;
                suspended = true;
            } else if (matrixParam != null) {
                // no name required
                name = matrixParam.value().asString();
                type = ParameterType.MATRIX;
                convertable = true;
            } else {
                //unannoated field
                //just ignore it
                if (field) {
                    return this;
                }
                type = ParameterType.BODY;
            }
            single = true;
            converter = null;
            boolean typeHandled = false;
            if (paramType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                typeHandled = true;
                ParameterizedType pt = paramType.asParameterizedType();
                if (pt.name().equals(LIST)) {
                    single = false;
                    elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                    converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                            existingEndpoints, errorLocation, hasRuntimeConverters);
                    converter = new ListConverter.ListSupplier(converter);
                } else if (pt.name().equals(SET)) {
                    single = false;
                    elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                    converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                            existingEndpoints, errorLocation, hasRuntimeConverters);
                    converter = new SetConverter.SetSupplier(converter);
                } else if (pt.name().equals(SORTED_SET)) {
                    single = false;
                    elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, index);
                    converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                            existingEndpoints, errorLocation, hasRuntimeConverters);
                    converter = new SortedSetConverter.SortedSetSupplier(converter);
                } else if ((pt.name().equals(MULTI_VALUED_MAP)) && (type == ParameterType.BODY)) {
                    elementType = pt.name().toString();
                    single = true;
                    converter = null;
                    additionalReaders.add(FormUrlEncodedProvider.class, APPLICATION_FORM_URLENCODED, MultivaluedMap.class);
                } else if (convertable) {
                    throw new RuntimeException("Invalid parameter type '" + pt + "' used on method " + errorLocation);
                } else {
                    typeHandled = false;
                }
            } else if ((paramType.name().equals(PATH_SEGMENT)) && (type == ParameterType.PATH)) {
                elementType = paramType.name().toString();
                single = true;
                converter = new PathSegmentParamConverter.Supplier();
                typeHandled = true;
            }
            if (!typeHandled) {
                elementType = toClassName(paramType, currentClassInfo, actualEndpointInfo, index);
                addReaderForType(additionalReaders, paramType);

                if (type != ParameterType.CONTEXT && type != ParameterType.BEAN && type != ParameterType.BODY
                        && type != ParameterType.ASYNC_RESPONSE) {
                    converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                            existingEndpoints, errorLocation, hasRuntimeConverters);
                }
                if (type == ParameterType.CONTEXT && elementType.equals(SseEventSink.class.getName())) {
                    sse = true;
                }
            }
            if (suspendedAnnotation != null && !elementType.equals(AsyncResponse.class.getName())) {
                throw new RuntimeException("Can only inject AsyncResponse on methods marked @Suspended");
            }
            return this;
        }

        private ParameterConverterSupplier extractConverter(String elementType, IndexView indexView,
                BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
                Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters) {
            if (elementType.equals(String.class.getName())) {
                if (hasRuntimeConverters)
                    return new RuntimeResolvedConverter.Supplier().setDelegate(new NoopParameterConverter.Supplier());
                // String needs no conversion
                return null;
            } else if (existingConverters.containsKey(elementType)) {
                String className = existingConverters.get(elementType);
                ParameterConverterSupplier delegate;
                if (className == null)
                    delegate = null;
                else
                    delegate = new GeneratedParameterConverter().setClassName(className);
                if (hasRuntimeConverters)
                    return new RuntimeResolvedConverter.Supplier().setDelegate(delegate);
                if (delegate == null)
                    throw new RuntimeException("Failed to find converter for " + elementType);
                return delegate;
            }

            MethodDescriptor fromString = null;
            MethodDescriptor valueOf = null;
            MethodInfo stringCtor = null;
            String primitiveWrapperType = primitiveTypes.get(elementType);
            String prefix = "";
            if (primitiveWrapperType != null) {
                valueOf = MethodDescriptor.ofMethod(primitiveWrapperType, "valueOf", primitiveWrapperType, String.class);
                prefix = "io.quarkus.generated.";
            } else {
                ClassInfo type = indexView.getClassByName(DotName.createSimple(elementType));
                if (type != null) {
                    for (MethodInfo i : type.methods()) {
                        if (i.parameters().size() == 1) {
                            if (i.parameters().get(0).name().equals(STRING)) {
                                if (i.name().equals("<init>")) {
                                    stringCtor = i;
                                } else if (i.name().equals("valueOf")) {
                                    valueOf = MethodDescriptor.of(i);
                                } else if (i.name().equals("fromString")) {
                                    fromString = MethodDescriptor.of(i);
                                }
                            }
                        }
                    }
                    if (type.isEnum()) {
                        //spec weirdness, enums order is different
                        if (fromString != null) {
                            valueOf = null;
                        }
                    }
                }
            }

            String baseName;
            ParameterConverterSupplier delegate;
            if (stringCtor != null || valueOf != null || fromString != null) {
                baseName = prefix + elementType + "$quarkusrestparamConverter$";
                try (ClassCreator classCreator = new ClassCreator(
                        new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                        Object.class.getName(), ParameterConverter.class.getName())) {
                    MethodCreator mc = classCreator.getMethodCreator("convert", Object.class, Object.class);
                    if (stringCtor != null) {
                        ResultHandle ret = mc.newInstance(stringCtor, mc.getMethodParam(0));
                        mc.returnValue(ret);
                    } else if (valueOf != null) {
                        ResultHandle ret = mc.invokeStaticMethod(valueOf, mc.getMethodParam(0));
                        mc.returnValue(ret);
                    } else if (fromString != null) {
                        ResultHandle ret = mc.invokeStaticMethod(fromString, mc.getMethodParam(0));
                        mc.returnValue(ret);
                    }
                }
                delegate = new GeneratedParameterConverter().setClassName(baseName);
            } else {
                // let's not try this again
                baseName = null;
                delegate = null;
            }
            existingConverters.put(elementType, baseName);
            if (hasRuntimeConverters)
                return new RuntimeResolvedConverter.Supplier().setDelegate(delegate);
            if (delegate == null)
                throw new RuntimeException("Failed to find converter for " + elementType);
            return delegate;
        }

    }

    public static class Builder {
        private IndexView index;
        private BeanContainer beanContainer;
        private BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
        private BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer;
        private QuarkusRestRecorder recorder;
        private Map<String, String> existingConverters;
        private Map<DotName, String> scannedResourcePaths;
        private QuarkusRestConfig config;
        private AdditionalReaders additionalReaders;
        private Map<DotName, String> httpAnnotationToMethod;
        private Map<String, InjectableBean> injectableBeans;
        private AdditionalWriters additionalWriters;
        private MethodCreator initConverters;
        private boolean hasRuntimeConverters;

        public Builder setHasRuntimeConverters(boolean hasRuntimeConverters) {
            this.hasRuntimeConverters = hasRuntimeConverters;
            return this;
        }

        public Builder setIndex(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder setBeanContainer(BeanContainer beanContainer) {
            this.beanContainer = beanContainer;
            return this;
        }

        public Builder setGeneratedClassBuildItemBuildProducer(
                BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
            this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
            return this;
        }

        public Builder setBytecodeTransformerBuildItemBuildProducer(
                BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer) {
            this.bytecodeTransformerBuildItemBuildProducer = bytecodeTransformerBuildItemBuildProducer;
            return this;
        }

        public Builder setRecorder(QuarkusRestRecorder recorder) {
            this.recorder = recorder;
            return this;
        }

        public Builder setExistingConverters(Map<String, String> existingConverters) {
            this.existingConverters = existingConverters;
            return this;
        }

        public Builder setScannedResourcePaths(Map<DotName, String> scannedResourcePaths) {
            this.scannedResourcePaths = scannedResourcePaths;
            return this;
        }

        public Builder setConfig(QuarkusRestConfig config) {
            this.config = config;
            return this;
        }

        public Builder setAdditionalReaders(AdditionalReaders additionalReaders) {
            this.additionalReaders = additionalReaders;
            return this;
        }

        public Builder setHttpAnnotationToMethod(Map<DotName, String> httpAnnotationToMethod) {
            this.httpAnnotationToMethod = httpAnnotationToMethod;
            return this;
        }

        public Builder setInjectableBeans(Map<String, InjectableBean> injectableBeans) {
            this.injectableBeans = injectableBeans;
            return this;
        }

        public Builder setAdditionalWriters(AdditionalWriters additionalWriters) {
            this.additionalWriters = additionalWriters;
            return this;
        }

        public Builder setInitConverters(MethodCreator initConverters) {
            this.initConverters = initConverters;
            return this;
        }

        public EndpointIndexer build() {
            return new EndpointIndexer(this);
        }
    }
}
