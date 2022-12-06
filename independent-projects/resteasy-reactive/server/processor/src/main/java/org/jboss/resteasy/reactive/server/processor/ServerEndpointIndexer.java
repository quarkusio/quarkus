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
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.ZONED_DATE_TIME;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    protected final EndpointInvokerFactory endpointInvokerFactory;
    protected final List<MethodScanner> methodScanners;
    protected final FieldInjectionIndexerExtension fieldInjectionHandler;
    protected final ConverterSupplierIndexerExtension converterSupplierIndexerExtension;

    protected ServerEndpointIndexer(AbstractBuilder builder) {
        super(builder);
        this.endpointInvokerFactory = builder.endpointInvokerFactory;
        this.methodScanners = new ArrayList<>(builder.methodScanners);
        this.fieldInjectionHandler = builder.fieldInjectionIndexerExtension;
        this.converterSupplierIndexerExtension = builder.converterSupplierIndexerExtension;
    }

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
        return serverResourceMethod;
    }

    @Override
    protected boolean handleBeanParam(ClassInfo actualEndpointInfo, Type paramType, MethodParameter[] methodParameters, int i) {
        ClassInfo beanParamClassInfo = index.getClassByName(paramType.name());
        InjectableBean injectableBean = scanInjectableBean(beanParamClassInfo,
                actualEndpointInfo,
                existingConverters, additionalReaders, injectableBeans, hasRuntimeConverters);
        if ((injectableBean.getFieldExtractorsCount() == 0) && !injectableBean.isInjectionRequired()) {
            throw new DeploymentException(String.format("No annotations found on fields at '%s'. "
                    + "Annotations like `@QueryParam` should be used in fields, not in methods.",
                    beanParamClassInfo.name()));
        }

        return injectableBean.isFormParamRequired();
    }

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
        for (FieldInfo field : currentClassInfo.fields()) {
            Map<DotName, AnnotationInstance> annotations = new HashMap<>();
            for (AnnotationInstance i : field.annotations()) {
                annotations.put(i.name(), i);
            }
            ServerIndexedParameter result = extractParameterInfo(currentClassInfo, actualEndpointInfo, null, existingConverters,
                    additionalReaders,
                    annotations, field.type(), field.toString(), true, hasRuntimeConverters,
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
            }
        }
        // the TCK expects that fields annotated with @BeanParam are handled last
        fieldExtractors.putAll(beanParamFields);

        DotName superClassName = currentClassInfo.superName();
        boolean superTypeIsInjectable = false;
        if (superClassName != null && !superClassName.equals(ResteasyReactiveDotNames.OBJECT)) {
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

    protected MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo, boolean encoded,
            Type paramType, ServerIndexedParameter parameterResult, String name, String defaultValue, ParameterType type,
            String elementType, boolean single, String signature) {
        ParameterConverterSupplier converter = parameterResult.getConverter();
        DeclaredTypes declaredTypes = getDeclaredTypes(paramType, currentClassInfo, actualEndpointInfo);
        String mimeType = getPartMime(parameterResult.getAnns());
        return new ServerMethodParameter(name,
                elementType, declaredTypes.getDeclaredType(), declaredTypes.getDeclaredUnresolvedType(),
                type, single, signature,
                converter, defaultValue, parameterResult.isObtainedAsCollection(), parameterResult.isOptional(), encoded,
                parameterResult.getCustomParameterExtractor(), mimeType);
    }

    protected void handleOtherParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        try {
            builder.setConverter(extractConverter(elementType, index,
                    existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns()));
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not create converter for " + elementType + " for " + builder.getErrorLocation()
                    + " of type " + builder.getType(), throwable);
        }
    }

    protected void handleSortedSetParam(Map<String, String> existingConverters, String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns());
        builder.setConverter(new SortedSetConverter.SortedSetSupplier(converter));
    }

    protected void handleOptionalParam(Map<String, String> existingConverters,
            Map<DotName, AnnotationInstance> parameterAnnotations,
            String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType, String genericElementType,
            MethodInfo currentMethodInfo) {
        ParameterConverterSupplier converter = null;

        if (genericElementType != null) {
            ParameterConverterSupplier genericTypeConverter = extractConverter(genericElementType, index, existingConverters,
                    errorLocation, hasRuntimeConverters, builder.getAnns());
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
                    builder.getAnns());
        }

        builder.setConverter(new OptionalConverter.OptionalSupplier(converter));
    }

    protected void handleSetParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns());
        builder.setConverter(new SetConverter.SetSupplier(converter));
    }

    protected void handleListParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns());
        builder.setConverter(new ListConverter.ListSupplier(converter));
    }

    protected void handleArrayParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters, builder.getAnns());
        builder.setConverter(new ArrayConverter.ArraySupplier(converter, elementType));
    }

    protected void handlePathSegmentParam(ServerIndexedParameter builder) {
        builder.setConverter(new PathSegmentParamConverter.Supplier());
    }

    @Override
    protected String handleTrailingSlash(String path) {
        return path.substring(0, path.length() - 1);
    }

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
        }

        throw new RuntimeException(
                contextualizeErrorMessage("Unable to handle temporal type '" + paramType + "'", currentMethodInfo));
    }

    private void validateMethodsForInjectableBean(ClassInfo currentClassInfo) {
        for (MethodInfo method : currentClassInfo.methods()) {
            for (AnnotationInstance annotation : method.annotations()) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    for (DotName annotationForField : JAX_RS_ANNOTATIONS_FOR_FIELDS) {
                        if (annotation.name().equals(annotationForField)) {
                            throw new DeploymentException(String.format(
                                    "Method '%s' of class '%s' is annotated with @%s annotation which is prohibited. "
                                            + "Classes uses as @BeanParam parameters must have a JAX-RS parameter annotation on "
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
            Map<DotName, AnnotationInstance> annotations) {
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
