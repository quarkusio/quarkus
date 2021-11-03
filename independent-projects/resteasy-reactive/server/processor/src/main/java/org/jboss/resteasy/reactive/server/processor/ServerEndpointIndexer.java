package org.jboss.resteasy.reactive.server.processor;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_ARRAY;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_NUMBER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRUCTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_VALUE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI_VALUED_MAP;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.STRING;

import java.lang.reflect.Modifier;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import org.jboss.jandex.AnnotationInstance;
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
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ListConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.LoadedParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.LocalDateParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.NoopParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.OptionalConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.PathSegmentParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.SetConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.SortedSetConverter;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectionConstructorParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectionValueOfParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerMethodParameter;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
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

    protected ServerEndpointIndexer(AbstractBuilder builder) {
        super(builder);
        this.endpointInvokerFactory = builder.endpointInvokerFactory;
        this.methodScanners = new ArrayList<>(builder.methodScanners);
    }

    protected void addWriterForType(AdditionalWriters additionalWriters, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalWriters.add(ServerJsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalWriters.add(ServerJsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalWriters.add(ServerJsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalWriters.add(ServerJsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
        }
    }

    protected void addReaderForType(AdditionalReaders additionalReaders, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalReaders.add(ServerJsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalReaders.add(ServerJsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalReaders.add(ServerJsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalReaders.add(ServerJsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
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
                builder.setCustomerParameterExtractor(res);
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

        // LinkedHashMap the TCK expects that fields annotated with @BeanParam are handled last
        Map<FieldInfo, ServerIndexedParameter> fieldExtractors = new LinkedHashMap<>();
        Map<FieldInfo, ServerIndexedParameter> beanParamFields = new LinkedHashMap<>();
        for (FieldInfo field : currentClassInfo.fields()) {
            Map<DotName, AnnotationInstance> annotations = new HashMap<>();
            for (AnnotationInstance i : field.annotations()) {
                annotations.put(i.name(), i);
            }
            ServerIndexedParameter result = extractParameterInfo(currentClassInfo, actualEndpointInfo, existingConverters,
                    additionalReaders,
                    annotations, field.type(), field.toString(), true, hasRuntimeConverters,
                    // We don't support annotation-less path params in injectable beans: only annotations
                    Collections.emptySet(), field.name(), EMPTY_STRING_ARRAY, new HashMap<>());
            if ((result.getType() != null) && (result.getType() != ParameterType.BEAN)) {
                //BODY means no annotation, so for fields not injectable
                fieldExtractors.put(field, result);
            }
            if (result.getConverter() != null) {
                handleConverter(currentTypeName, field);
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

        if (!fieldExtractors.isEmpty()) {
            handleFieldExtractors(currentTypeName, fieldExtractors, superTypeIsInjectable);
        }
        currentInjectableBean.setInjectionRequired(!fieldExtractors.isEmpty() || superTypeIsInjectable);
        return currentInjectableBean;
    }

    protected void handleFieldExtractors(String currentTypeName, Map<FieldInfo, ServerIndexedParameter> fieldExtractors,
            boolean superTypeIsInjectable) {
    }

    protected void handleConverter(String currentTypeName, FieldInfo field) {
    }

    protected MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo, boolean encoded,
            Type paramType, ServerIndexedParameter parameterResult, String name, String defaultValue, ParameterType type,
            String elementType, boolean single, String signature) {
        ParameterConverterSupplier converter = parameterResult.getConverter();
        DeclaredTypes declaredTypes = getDeclaredTypes(paramType, currentClassInfo, actualEndpointInfo);
        return new ServerMethodParameter(name,
                elementType, declaredTypes.getDeclaredType(), declaredTypes.getDeclaredUnresolvedType(),
                type, single, signature,
                converter, defaultValue, parameterResult.isObtainedAsCollection(), parameterResult.isOptional(), encoded,
                parameterResult.getCustomerParameterExtractor());
    }

    protected void handleOtherParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        try {
            builder.setConverter(extractConverter(elementType, index,
                    existingConverters, errorLocation, hasRuntimeConverters));
        } catch (Throwable throwable) {
            throw new RuntimeException("Could not create converter for " + elementType + " for " + builder.getErrorLocation()
                    + " of type " + builder.getType(), throwable);
        }
    }

    protected void handleSortedSetParam(Map<String, String> existingConverters, String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new SortedSetConverter.SortedSetSupplier(converter));
    }

    protected void handleOptionalParam(Map<String, String> existingConverters, String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new OptionalConverter.OptionalSupplier(converter));
    }

    protected void handleSetParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new SetConverter.SetSupplier(converter));
    }

    protected void handleListParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new ListConverter.ListSupplier(converter));
    }

    protected void handlePathSegmentParam(ServerIndexedParameter builder) {
        builder.setConverter(new PathSegmentParamConverter.Supplier());
    }

    protected void handleLocalDateParam(ServerIndexedParameter builder) {
        builder.setConverter(new LocalDateParamConverter.Supplier());
    }

    private ParameterConverterSupplier extractConverter(String elementType, IndexView indexView,
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
                delegate = new LoadedParameterConverter().setClassName(className);
            if (hasRuntimeConverters)
                return new RuntimeResolvedConverter.Supplier().setDelegate(delegate);
            if (delegate == null)
                throw new RuntimeException("Failed to find converter for " + elementType);
            return delegate;
        } else if (elementType.equals(PathSegment.class.getName())) {
            return new PathSegmentParamConverter.Supplier();
        }
        return extractConverterImpl(elementType, indexView, existingConverters, errorLocation, hasRuntimeConverters);
    }

    protected ParameterConverterSupplier extractConverterImpl(String elementType, IndexView indexView,
            Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters) {
        MethodInfo fromString = null;
        MethodInfo valueOf = null;
        MethodInfo stringCtor = null;
        String primitiveWrapperType = primitiveTypes.get(elementType);
        String prefix = "";
        if (primitiveWrapperType != null) {
            return new ReflectionValueOfParameterConverterSupplier(primitiveWrapperType);
        } else {
            ClassInfo type = indexView.getClassByName(DotName.createSimple(elementType));
            if (type != null) {
                for (MethodInfo i : type.methods()) {
                    boolean isStatic = ((i.flags() & Modifier.STATIC) != 0);
                    boolean isNotPrivate = (i.flags() & Modifier.PRIVATE) == 0;
                    if ((i.parameters().size() == 1) && isNotPrivate) {
                        if (i.parameters().get(0).name().equals(STRING)) {
                            if (i.name().equals("<init>")) {
                                stringCtor = i;
                            } else if (i.name().equals("valueOf") && isStatic) {
                                valueOf = i;
                            } else if (i.name().equals("fromString") && isStatic) {
                                fromString = i;
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
        ParameterConverterSupplier delegate = null;
        if (stringCtor != null) {
            delegate = new ReflectionConstructorParameterConverterSupplier(stringCtor.declaringClass().name().toString());
        } else if (valueOf != null) {
            delegate = new ReflectionValueOfParameterConverterSupplier(valueOf.declaringClass().name().toString());
        } else if (fromString != null) {
            delegate = new ReflectionValueOfParameterConverterSupplier(fromString.declaringClass().name().toString(),
                    "fromString");
        }
        if (hasRuntimeConverters)
            return new RuntimeResolvedConverter.Supplier().setDelegate(delegate);
        if (delegate == null)
            throw new RuntimeException("Failed to find converter for " + elementType);
        return delegate;
    }

    @SuppressWarnings("unchecked")
    public static class AbstractBuilder<B extends EndpointIndexer.Builder<ServerEndpointIndexer, B, ServerResourceMethod>>
            extends EndpointIndexer.Builder<ServerEndpointIndexer, B, ServerResourceMethod> {

        private EndpointInvokerFactory endpointInvokerFactory = new ReflectionEndpointInvokerFactory();
        private List<MethodScanner> methodScanners = new ArrayList<>();

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

        public B addMethodScanners(Collection<MethodScanner> methodScanners) {
            this.methodScanners.addAll(methodScanners);
            return (B) this;
        }

        @Override
        public ServerEndpointIndexer build() {
            return new ServerEndpointIndexer(this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder> {

    }
}
