package org.jboss.resteasy.reactive.server.processor;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DATE_FORMAT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.INSTANT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JAX_RS_ANNOTATIONS_FOR_FIELDS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_ARRAY;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_NUMBER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRUCTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_VALUE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LIST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LOCAL_DATE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LOCAL_DATE_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LOCAL_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI_VALUED_MAP;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OFFSET_DATE_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OFFSET_TIME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SORTED_SET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.YEAR;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.YEAR_MONTH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.ZONED_DATE_TIME;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.PatternSyntaxException;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.BeanParamInfo;
import org.jboss.resteasy.reactive.common.model.InjectableBean;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ArrayConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.CharParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.CharacterParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.InstantParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ListConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.LoadedParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.LocalDateParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.LocalDateTimeParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.LocalTimeParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.NoopParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.OffsetDateTimeParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.OffsetTimeParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.OptionalConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.PathSegmentParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.SetConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.SortedSetConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.YearMonthParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.YearParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ZonedDateTimeParamConverter;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerMethodParameter;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.processor.reflection.ReflectionConverterIndexerExtension;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.providers.serialisers.ServerFormUrlEncodedProvider;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonArrayHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonObjectHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonStructureHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonValueHandler;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

public class ServerEndpointIndexer
        extends EndpointIndexer<ServerEndpointIndexer, ServerIndexedParameter, ServerResourceMethod> {

    private static final DotName FILE_DOT_NAME = DotName.createSimple(File.class.getName());
    private static final DotName PATH_DOT_NAME = DotName.createSimple(Path.class.getName());
    private static final DotName FILEUPLOAD_DOT_NAME = DotName.createSimple(FileUpload.class.getName());

    private static final Set<DotName> SUPPORTED_MULTIPART_FILE_TYPES = Set.of(FILE_DOT_NAME, PATH_DOT_NAME,
            FILEUPLOAD_DOT_NAME);
    protected final EndpointInvokerFactory endpointInvokerFactory;
    protected final List<MethodScanner> methodScanners;
    protected final FieldInjectionIndexerExtension fieldInjectionHandler;
    protected final ConverterSupplierIndexerExtension converterSupplierIndexerExtension;
    protected final boolean removesTrailingSlash;

    protected ServerEndpointIndexer(AbstractBuilder builder) {
        super(builder);
        this.endpointInvokerFactory = builder.endpointInvokerFactory;
        this.methodScanners = new ArrayList<>(builder.methodScanners);
        this.fieldInjectionHandler = builder.fieldInjectionIndexerExtension;
        this.converterSupplierIndexerExtension = builder.converterSupplierIndexerExtension;
        this.removesTrailingSlash = builder.removesTrailingSlash;
    }

    @Override
    protected void addWriterForType(AdditionalWriters additionalWriters, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalWriters.add(ServerJsonValueHandler.class, APPLICATION_JSON, jakarta.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalWriters.add(ServerJsonArrayHandler.class, APPLICATION_JSON, jakarta.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalWriters.add(ServerJsonObjectHandler.class, APPLICATION_JSON, jakarta.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalWriters.add(ServerJsonStructureHandler.class, APPLICATION_JSON, jakarta.json.JsonStructure.class);
        }
    }

    @Override
    protected void addReaderForType(AdditionalReaders additionalReaders, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalReaders.add(ServerJsonValueHandler.class, APPLICATION_JSON, jakarta.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalReaders.add(ServerJsonArrayHandler.class, APPLICATION_JSON, jakarta.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalReaders.add(ServerJsonObjectHandler.class, APPLICATION_JSON, jakarta.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalReaders.add(ServerJsonStructureHandler.class, APPLICATION_JSON, jakarta.json.JsonStructure.class);
        } else if (dotName.equals(MULTI_VALUED_MAP)) {
            additionalReaders.add(ServerFormUrlEncodedProvider.class, APPLICATION_FORM_URLENCODED,
                    MultivaluedMap.class);
        }
    }

    @Override
    protected ServerIndexedParameter createIndexedParam() {
        return new ServerIndexedParameter();
    }

    @Override
    protected boolean handleCustomParameter(Map<DotName, AnnotationInstance> anns, ServerIndexedParameter builder,
            Type paramType, boolean field, Map<String, Object> methodContext) {
        for (MethodScanner i : methodScanners) {
            ParameterExtractor res = i.handleCustomParameter(paramType, anns, field, methodContext);
            if (res != null) {
                builder.setType(ParameterType.CUSTOM);
                builder.setCustomParameterExtractor(res);
                return true;
            }
        }
        return false;
    }

    @Override
    protected ServerResourceMethod createResourceMethod(MethodInfo methodInfo, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        ServerResourceMethod serverResourceMethod = new ServerResourceMethod();
        List<HandlerChainCustomizer> methodCustomizers = new ArrayList<>();
        for (MethodScanner i : methodScanners) {
            List<HandlerChainCustomizer> scanned = i.scan(methodInfo, actualEndpointClass, methodContext);
            if (scanned != null) {
                methodCustomizers.addAll(scanned);
            }
        }
        serverResourceMethod.setHandlerChainCustomizers(methodCustomizers);

        var actualDeclaringClassName = findActualDeclaringClassName(methodInfo, actualEndpointClass);
        serverResourceMethod.setActualDeclaringClassName(actualDeclaringClassName);
        var classDeclMethodThatHasJaxRsEndpointDefiningAnn = methodInfo.declaringClass().name().toString();
        if (!actualDeclaringClassName.equals(classDeclMethodThatHasJaxRsEndpointDefiningAnn)) {
            serverResourceMethod
                    .setClassDeclMethodThatHasJaxRsEndpointDefiningAnn(classDeclMethodThatHasJaxRsEndpointDefiningAnn);
        }

        return serverResourceMethod;
    }

    private String findActualDeclaringClassName(MethodInfo methodInfo, ClassInfo actualEndpointClass) {
        return findEndpointImplementation(methodInfo, actualEndpointClass, index).declaringClass().name().toString();
    }

    /**
     * Aim here is to find a method that actually returns endpoint response.
     * We can receive method with similar signature several times here, only differing in the modifiers (abstract etc.).
     * However, {@code actualEndpointClass} will change.
     * For example once from the interface with JAX-RS endpoint defining annotations and also from implementors.
     *
     * @return method that returns endpoint response
     */
    public static MethodInfo findEndpointImplementation(MethodInfo methodInfo, ClassInfo actualEndpointClass, IndexView index) {
        // provided that 'actualEndpointClass' is requested from CDI via InstanceHandler factory
        // we know that this class resolution must be unambiguous:
        // 1. go down - find exactly one non-abstract class
        ClassInfo clazz = null;
        if (actualEndpointClass.isInterface()) {
            for (var implementor : index.getAllKnownImplementors(actualEndpointClass.name())) {
                if (!implementor.isInterface() && !implementor.isAbstract()) {
                    if (clazz == null) {
                        clazz = implementor;
                        // keep going to recognize if there is more than one non-abstract implementor
                    } else {
                        // resolution is not unambiguous, this at least make behavior deterministic
                        clazz = actualEndpointClass;
                        break;
                    }
                }
            }
        } else {
            for (var subClass : index.getAllKnownSubclasses(actualEndpointClass.name())) {
                if (!subClass.isAbstract()) {
                    if (clazz == null) {
                        clazz = subClass;
                        // keep going to recognize if there is more than one non-abstract subclass
                    } else {
                        // resolution is not unambiguous, this at least make behavior deterministic
                        clazz = actualEndpointClass;
                        break;
                    }
                }
            }
        }
        if (clazz == null) {
            clazz = actualEndpointClass;
        }

        // 2. go up - first impl. going up is the one invoked on the endpoint instance
        Queue<MethodInfo> defaultInterfaceMethods = new ArrayDeque<>();
        do {
            // is non-abstract method declared on this class?
            var method = clazz.method(methodInfo.name(), methodInfo.parameterTypes());
            if (method != null && !Modifier.isAbstract(method.flags())) {
                return method;
            }

            var interfaceWithImplMethod = findInterfaceDefaultMethod(clazz, methodInfo, index);
            if (interfaceWithImplMethod != null) {
                // class methods override default interface methods -> check parent first
                defaultInterfaceMethods.add(interfaceWithImplMethod);
            }

            if (clazz.superName() != null && !clazz.superName().equals(ResteasyReactiveDotNames.OBJECT)) {
                clazz = index.getClassByName(clazz.superName());
            } else {
                break;
            }
        } while (clazz != null);
        if (!defaultInterfaceMethods.isEmpty()) {
            return defaultInterfaceMethods.peek();
        }

        // 3. fallback to original behavior
        return methodInfo;
    }

    private static MethodInfo findInterfaceDefaultMethod(ClassInfo clazz, MethodInfo methodInfo, IndexView index) {
        for (DotName interfaceName : clazz.interfaceNames()) {
            var interfaceClass = index.getClassByName(interfaceName);
            if (interfaceClass != null) {
                var intMethod = interfaceClass.method(methodInfo.name(), methodInfo.parameterTypes());
                if (intMethod != null && intMethod.isDefault() && Modifier.isPublic(intMethod.flags())) {
                    return intMethod;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean handleBeanParam(ClassInfo actualEndpointInfo, Type paramType, MethodParameter[] methodParameters, int i,
            Set<String> fileFormNames) {
        ClassInfo beanParamClassInfo = index.getClassByName(paramType.name());
        InjectableBean injectableBean = scanInjectableBean(beanParamClassInfo,
                actualEndpointInfo,
                existingConverters, additionalReaders, injectableBeans, hasRuntimeConverters);
        if ((injectableBean.getFieldExtractorsCount() == 0) && !injectableBean.isInjectionRequired()) {
            long declaredMethodsCount = beanParamClassInfo.methods().stream()
                    .filter(m -> !m.name().equals("<init>") && !m.name().equals("<clinit>")).count();
            if (declaredMethodsCount == 0) {
                throw new DeploymentException(String.format(
                        "Class %s has no fields. Parameters containers are only supported if they have at least one annotated field.",
                        beanParamClassInfo.name()));
            } else {
                throw new DeploymentException(String.format("No annotations found on fields at '%s'. "
                        + "Annotations like `@QueryParam` should be used in fields, not in methods.",
                        beanParamClassInfo.name()));
            }

        }
        fileFormNames.addAll(injectableBean.getFileFormNames());
        return injectableBean.isFormParamRequired();
    }

    @Override
    protected boolean doesMethodHaveBlockingSignature(MethodInfo info) {
        for (var i : methodScanners) {
            if (i.isMethodSignatureAsync(info)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void handleAdditionalMethodProcessing(ServerResourceMethod method, ClassInfo currentClassInfo, MethodInfo info,
            AnnotationStore annotationStore) {
        Supplier<EndpointInvoker> invokerSupplier = null;
        for (HandlerChainCustomizer i : method.getHandlerChainCustomizers()) {
            invokerSupplier = i.alternateInvoker(method);
            if (invokerSupplier != null) {
                break;
            }
        }
        if (invokerSupplier == null) {
            invokerSupplier = endpointInvokerFactory.create(method, currentClassInfo, info);
        }
        method.setInvoker(invokerSupplier);
        Set<String> methodAnnotationNames = new HashSet<>();
        Collection<AnnotationInstance> instances = annotationStore.getAnnotations(info);
        for (AnnotationInstance instance : instances) {
            methodAnnotationNames.add(instance.name().toString());
        }
        method.setMethodAnnotationNames(methodAnnotationNames);

        // validate the path
        validateMethodPath(method, currentClassInfo, info);
    }

    private void validateMethodPath(ServerResourceMethod method, ClassInfo currentClassInfo, MethodInfo info) {
        try {
            new URITemplate(method.getPath(), false);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Path '" + method.getPath() + "' of method '" + currentClassInfo.name() + "#"
                    + info.name() + "' is not a valid expression", e);
        }
    }

    @Override
    protected InjectableBean scanInjectableBean(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            Map<String, String> existingConverters, AdditionalReaders additionalReaders,
            Map<String, InjectableBean> injectableBeans, boolean hasRuntimeConverters) {

        // do not scan a bean twice
        String currentTypeName = currentClassInfo.name().toString();
        InjectableBean currentInjectableBean = injectableBeans.get(currentTypeName);
        if (currentInjectableBean != null) {
            return currentInjectableBean;
        }
        currentInjectableBean = new BeanParamInfo();
        injectableBeans.put(currentTypeName, currentInjectableBean);

        // validate methods
        validateMethodsForInjectableBean(currentClassInfo);

        // LinkedHashMap the TCK expects that fields annotated with @BeanParam are handled last
        Map<FieldInfo, ServerIndexedParameter> fieldExtractors = new LinkedHashMap<>();
        Map<FieldInfo, ServerIndexedParameter> beanParamFields = new LinkedHashMap<>();
        // records do not have field injection, we use their constructor, so field rules do not apply
        boolean applyFieldRules = !currentClassInfo.isRecord();
        for (FieldInfo field : currentClassInfo.fields()) {
            Map<DotName, AnnotationInstance> annotations = new HashMap<>();
            for (AnnotationInstance i : field.annotations()) {
                annotations.put(i.name(), i);
            }
            ServerIndexedParameter result = extractParameterInfo(currentClassInfo, actualEndpointInfo, null, existingConverters,
                    additionalReaders,
                    annotations, field.type(), "%s", new Object[] { field }, applyFieldRules, hasRuntimeConverters,
                    // We don't support annotation-less path params in injectable beans: only annotations
                    Collections.emptySet(), field.name(), EMPTY_STRING_ARRAY, new HashMap<>());
            if ((result.getType() != null) && (result.getType() != ParameterType.BEAN)) {
                //BODY means no annotation, so for fields not injectable
                fieldExtractors.put(field, result);
            }
            if (result.getType() == ParameterType.BEAN) {
                beanParamFields.put(field, result);
                // transform the bean param
                // FIXME: pretty sure this doesn't work with generics
                ClassInfo beanParamClassInfo = index.getClassByName(field.type().name());
                InjectableBean injectableBean = scanInjectableBean(beanParamClassInfo, actualEndpointInfo,
                        existingConverters, additionalReaders, injectableBeans, hasRuntimeConverters);
                // inherit form param requirement from field
                if (injectableBean.isFormParamRequired()) {
                    currentInjectableBean.setFormParamRequired(true);
                }
            } else if (result.getType() == ParameterType.FORM) {
                // direct form param requirement
                currentInjectableBean.setFormParamRequired(true);

                if (SUPPORTED_MULTIPART_FILE_TYPES.contains(field.type().name())) {
                    String name = field.name();
                    AnnotationInstance restForm = field.annotation(ResteasyReactiveDotNames.REST_FORM_PARAM);
                    AnnotationInstance formParam = field.annotation(ResteasyReactiveDotNames.FORM_PARAM);
                    if (restForm != null) {
                        AnnotationValue value = restForm.value();
                        if (value != null) {
                            name = value.asString();
                        }
                    } else if (formParam != null) {
                        AnnotationValue value = formParam.value();
                        if (value != null) {
                            name = value.asString();
                        }
                    }
                    currentInjectableBean.getFileFormNames().add(name);
                }
            }
        }
        // the TCK expects that fields annotated with @BeanParam are handled last
        fieldExtractors.putAll(beanParamFields);

        DotName superClassName = currentClassInfo.superName();
        boolean superTypeIsInjectable = false;
        if (superClassName != null
                && !superClassName.equals(ResteasyReactiveDotNames.OBJECT)
                && !superClassName.equals(ResteasyReactiveDotNames.RECORD)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                InjectableBean superInjectableBean = scanInjectableBean(superClass, actualEndpointInfo,
                        existingConverters, additionalReaders, injectableBeans, hasRuntimeConverters);
                superTypeIsInjectable = superInjectableBean.isInjectionRequired();
                // inherit form param requirement from supertype
                if (superInjectableBean.isFormParamRequired()) {
                    currentInjectableBean.setFormParamRequired(true);
                }
            }
        }

        currentInjectableBean.setFieldExtractorsCount(fieldExtractors.size());

        if ((fieldInjectionHandler != null) && (!fieldExtractors.isEmpty() || superTypeIsInjectable)) {
            fieldInjectionHandler.handleFieldInjection(currentTypeName, fieldExtractors, superTypeIsInjectable, this.index);
        }
        currentInjectableBean.setInjectionRequired(!fieldExtractors.isEmpty() || superTypeIsInjectable);
        return currentInjectableBean;
    }

    @Override
    protected MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo, boolean encoded,
            Type paramType, ServerIndexedParameter parameterResult, String name, String defaultValue, ParameterType type,
            String elementType, boolean single, String signature,
            Set<String> fileFormNames) {
        ParameterConverterSupplier converter = parameterResult.getConverter();
        DeclaredTypes declaredTypes = getDeclaredTypes(paramType, currentClassInfo, actualEndpointInfo);
        String mimeType = getPartMime(parameterResult.getAnns());
        String declaredType = declaredTypes.getDeclaredType();

        if (SUPPORTED_MULTIPART_FILE_TYPES.contains(DotName.createSimple(declaredType))) {
            fileFormNames.add(name);
        }
        return new ServerMethodParameter(name,
                elementType, declaredType, declaredTypes.getDeclaredUnresolvedType(),
                type, single, signature,
                converter, defaultValue, parameterResult.isObtainedAsCollection(), parameterResult.isOptional(), encoded,
                parameterResult.getCustomParameterExtractor(), mimeType, parameterResult.getSeparator());
    }

    @Override
    protected void handleOtherParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType, MethodInfo currentMethodInfo) {
        try {
            builder.setConverter(extractConverter(elementType, index,
                    existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns(), currentMethodInfo));
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not create converter for " + elementType + " for " + builder.getErrorLocation()
                    + " of type " + builder.getType(), throwable);
        }
    }

    @Override
    protected void handleSortedSetParam(Map<String, String> existingConverters, String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType, MethodInfo currentMethodInfo) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns(), currentMethodInfo);
        builder.setConverter(new SortedSetConverter.SortedSetSupplier(converter));
    }

    @Override
    protected void handleOptionalParam(Map<String, String> existingConverters,
            Map<DotName, AnnotationInstance> parameterAnnotations,
            String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType, String genericElementType,
            MethodInfo currentMethodInfo) {
        ParameterConverterSupplier converter = null;

        if (genericElementType != null) {
            ParameterConverterSupplier genericTypeConverter = extractConverter(genericElementType, index, existingConverters,
                    errorLocation, hasRuntimeConverters, builder.getAnns(), currentMethodInfo);
            if (LIST.toString().equals(elementType)) {
                converter = new ListConverter.ListSupplier(genericTypeConverter);
                builder.setSingle(false);
            } else if (SET.toString().equals(elementType)) {
                converter = new SetConverter.SetSupplier(genericTypeConverter);
                builder.setSingle(false);
            } else if (SORTED_SET.toString().equals(elementType)) {
                converter = new SortedSetConverter.SortedSetSupplier(genericTypeConverter);
                builder.setSingle(false);
            }
        } else if (SUPPORT_TEMPORAL_PARAMS.contains(DotName.createSimple(elementType))) {
            converter = determineTemporalConverter(DotName.createSimple(elementType), parameterAnnotations,
                    currentMethodInfo);
        }

        if (converter == null) {
            // If no generic type provided or element type is not supported, then we try to use a custom runtime converter:
            converter = extractConverter(elementType, index, existingConverters, errorLocation, hasRuntimeConverters,
                    builder.getAnns(), currentMethodInfo);
        }

        builder.setConverter(new OptionalConverter.OptionalSupplier(converter));
    }

    @Override
    protected void handleSetParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType, MethodInfo currentMethodInfo) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns(), currentMethodInfo);
        builder.setConverter(new SetConverter.SetSupplier(converter));
    }

    @Override
    protected void handleListParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType, MethodInfo currentMethodInfo) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns(), currentMethodInfo);
        builder.setConverter(new ListConverter.ListSupplier(converter));
    }

    @Override
    protected void handleArrayParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType, MethodInfo currentMethodInfo) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns(), currentMethodInfo);
        builder.setConverter(new ArrayConverter.ArraySupplier(converter, elementType));
    }

    @Override
    protected void handlePathSegmentParam(ServerIndexedParameter builder) {
        builder.setConverter(new PathSegmentParamConverter.Supplier());
    }

    /**
     * For the server side, by default, we are removing the trailing slash unless is not configured otherwise.
     */
    @Override
    protected String handleTrailingSlash(String path) {
        if (removesTrailingSlash) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }

    @Override
    protected void handleTemporalParam(ServerIndexedParameter builder, DotName paramType,
            Map<DotName, AnnotationInstance> parameterAnnotations,
            MethodInfo currentMethodInfo) {
        builder.setConverter(determineTemporalConverter(paramType, parameterAnnotations, currentMethodInfo));
    }

    private ParameterConverterSupplier determineTemporalConverter(DotName paramType,
            Map<DotName, AnnotationInstance> parameterAnnotations, MethodInfo currentMethodInfo) {
        String format = null;
        String dateTimeFormatterProviderClassName = null;

        AnnotationInstance dateFormatInstance = parameterAnnotations.get(DATE_FORMAT);

        if (dateFormatInstance != null) {
            AnnotationValue formatValue = dateFormatInstance.value("pattern");
            if (formatValue != null) {
                format = formatValue.asString();
            }

            AnnotationValue dateTimeFormatterProviderValue = dateFormatInstance.value("dateTimeFormatterProvider");
            if (dateTimeFormatterProviderValue != null) {
                dateTimeFormatterProviderClassName = dateTimeFormatterProviderValue.asClass().name().toString();
            }
        }

        if (INSTANT.equals(paramType)) {
            if (dateFormatInstance != null) {
                throw new RuntimeException(contextualizeErrorMessage(
                        "'java.time.Instant' types must not be annotated with '@DateFormat'",
                        currentMethodInfo));
            }
            return new InstantParamConverter.Supplier();
        }

        if ((format != null) && (dateTimeFormatterProviderClassName != null)) {
            throw new RuntimeException(contextualizeErrorMessage(
                    "Using both 'format' and 'dateTimeFormatterProvider' is not allowed when using '@DateFormat'",
                    currentMethodInfo));
        } else if ((format == null) && (dateTimeFormatterProviderClassName == null) && (dateFormatInstance != null)) {
            throw new RuntimeException(contextualizeErrorMessage(
                    "One of 'format' or 'dateTimeFormatterProvider' must be set when using '@DateFormat'", currentMethodInfo));
        }

        if (LOCAL_DATE.equals(paramType)) {
            return new LocalDateParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (LOCAL_DATE_TIME.equals(paramType)) {
            return new LocalDateTimeParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (LOCAL_TIME.equals(paramType)) {
            return new LocalTimeParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (OFFSET_DATE_TIME.equals(paramType)) {
            return new OffsetDateTimeParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (OFFSET_TIME.equals(paramType)) {
            return new OffsetTimeParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (ZONED_DATE_TIME.equals(paramType)) {
            return new ZonedDateTimeParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (YEAR.equals(paramType)) {
            return new YearParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        } else if (YEAR_MONTH.equals(paramType)) {
            return new YearMonthParamConverter.Supplier(format, dateTimeFormatterProviderClassName);
        }

        throw new RuntimeException(
                contextualizeErrorMessage("Unable to handle temporal type '" + paramType + "'", currentMethodInfo));
    }

    private void validateMethodsForInjectableBean(ClassInfo currentClassInfo) {
        // do not check methods of records, they get the annotations from their record components, but that's automatic:
        // they are actually placed on the constructor parameters and also end up on the fields and methods
        if (currentClassInfo.isRecord()) {
            return;
        }
        for (MethodInfo method : currentClassInfo.methods()) {
            for (AnnotationInstance annotation : method.annotations()) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    for (DotName annotationForField : JAX_RS_ANNOTATIONS_FOR_FIELDS) {
                        if (annotation.name().equals(annotationForField)) {
                            throw new DeploymentException(String.format(
                                    "Method '%s' of class '%s' is annotated with @%s annotation which is prohibited. "
                                            + "Classes used as @BeanParam parameters must have a JAX-RS parameter annotation on "
                                            + "fields only.",
                                    method.name(), currentClassInfo.name().toString(),
                                    annotation.name().withoutPackagePrefix()));
                        }
                    }
                }
            }
        }
    }

    private String contextualizeErrorMessage(String errorMessage, MethodInfo currentMethodInfo) {
        errorMessage += ". Offending method if '" + currentMethodInfo.name() + "' of class '"
                + currentMethodInfo.declaringClass().name() + "'";
        return errorMessage;
    }

    private ParameterConverterSupplier extractConverter(String elementType, IndexView indexView,
            Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            Map<DotName, AnnotationInstance> annotations, MethodInfo currentMethodInfo) {
        // no converter if we have a RestForm mime type: this goes via message body readers in MultipartFormParamExtractor
        if (getPartMime(annotations) != null)
            return null;
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
                delegate = new LoadedParameterConverter().setClassName(className);
            if (hasRuntimeConverters)
                return new RuntimeResolvedConverter.Supplier().setDelegate(delegate);
            if (delegate == null)
                throw new RuntimeException("Failed to find converter for " + elementType);
            return delegate;
        } else if (elementType.equals(PathSegment.class.getName())) {
            return new PathSegmentParamConverter.Supplier();
        } else if (elementType.equals("char")) {
            return new CharParamConverter.Supplier();
        } else if (elementType.equals(Character.class.getName())) {
            return new CharacterParamConverter.Supplier();
        } else if (elementType.equals(FileUpload.class.getName())
                || elementType.equals(Path.class.getName())
                || elementType.equals(File.class.getName())
                || elementType.equals(InputStream.class.getName())) {
            // this is handled by MultipartFormParamExtractor
            return null;
        } else {
            DotName typeName = DotName.createSimple(elementType);
            if (SUPPORT_TEMPORAL_PARAMS.contains(typeName)) {
                //It might be a LocalDate[Time] object
                return determineTemporalConverter(typeName, annotations, currentMethodInfo);
            }
        }

        return converterSupplierIndexerExtension.extractConverterImpl(elementType, indexView, existingConverters, errorLocation,
                hasRuntimeConverters);
    }

    @SuppressWarnings("unchecked")
    public static class AbstractBuilder<B extends EndpointIndexer.Builder<ServerEndpointIndexer, B, ServerResourceMethod>>
            extends EndpointIndexer.Builder<ServerEndpointIndexer, B, ServerResourceMethod> {

        private EndpointInvokerFactory endpointInvokerFactory = new ReflectionEndpointInvokerFactory();
        private List<MethodScanner> methodScanners = new ArrayList<>();
        private FieldInjectionIndexerExtension fieldInjectionIndexerExtension;
        private ConverterSupplierIndexerExtension converterSupplierIndexerExtension = new ReflectionConverterIndexerExtension();
        private boolean removesTrailingSlash = true;

        public EndpointInvokerFactory getEndpointInvokerFactory() {
            return endpointInvokerFactory;
        }

        public B setEndpointInvokerFactory(EndpointInvokerFactory endpointInvokerFactory) {
            this.endpointInvokerFactory = endpointInvokerFactory;
            return (B) this;
        }

        public B addMethodScanner(MethodScanner methodScanner) {
            this.methodScanners.add(methodScanner);
            return (B) this;
        }

        public B setConverterSupplierIndexerExtension(ConverterSupplierIndexerExtension converterSupplierIndexerExtension) {
            this.converterSupplierIndexerExtension = converterSupplierIndexerExtension;
            return (B) this;
        }

        public B addMethodScanners(Collection<MethodScanner> methodScanners) {
            this.methodScanners.addAll(methodScanners);
            return (B) this;
        }

        public B setFieldInjectionIndexerExtension(FieldInjectionIndexerExtension fieldInjectionHandler) {
            this.fieldInjectionIndexerExtension = fieldInjectionHandler;
            return (B) this;
        }

        public B setRemovesTrailingSlash(boolean removesTrailingSlash) {
            this.removesTrailingSlash = removesTrailingSlash;
            return (B) this;
        }

        @Override
        public ServerEndpointIndexer build() {
            return new ServerEndpointIndexer(this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder> {

    }

    public interface FieldInjectionIndexerExtension {

        void handleFieldInjection(String currentTypeName, Map<FieldInfo, ServerIndexedParameter> fieldExtractors,
                boolean superTypeIsInjectable, IndexView indexView);
    }

    public interface ConverterSupplierIndexerExtension {
        ParameterConverterSupplier extractConverterImpl(String elementType, IndexView indexView,
                Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters);
    }

}
