package io.quarkus.resteasy.reactive.server.deployment;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_ARRAY;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_NUMBER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRUCTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_VALUE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.STRING;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
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
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.parameters.converters.GeneratedParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ListConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.NoopParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.PathSegmentParamConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.SetConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.SortedSetConverter;
import org.jboss.resteasy.reactive.server.model.ServerMethodParameter;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;
import org.jboss.resteasy.reactive.server.providers.serialisers.ServerFormUrlEncodedProvider;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonArrayHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonObjectHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonStructureHandler;
import org.jboss.resteasy.reactive.server.providers.serialisers.jsonp.ServerJsonValueHandler;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class ServerEndpointIndexer extends EndpointIndexer<ServerEndpointIndexer, ServerIndexedParameter> {
    private final MethodCreator initConverters;
    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;
    private static final Set<DotName> CONTEXT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DotName.createSimple(HttpServerRequest.class.getName()),
            DotName.createSimple(HttpServerResponse.class.getName()),
            DotName.createSimple(RoutingContext.class.getName()))));

    ServerEndpointIndexer(Builder builder) {
        super(builder);
        this.initConverters = builder.initConverters;
        this.generatedClassBuildItemBuildProducer = builder.generatedClassBuildItemBuildProducer;
        this.bytecodeTransformerBuildProducer = builder.bytecodeTransformerBuildProducer;
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

    protected boolean isContextType(ClassType klass) {
        return super.isContextType(klass) || CONTEXT_TYPES.contains(klass.name());
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
        }
    }

    @Override
    protected ServerIndexedParameter createIndexedParam() {
        return new ServerIndexedParameter();
    }

    protected MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo, boolean encoded,
            Type paramType, ServerIndexedParameter parameterResult, String name, String defaultValue, ParameterType type,
            String elementType, boolean single) {
        ParameterConverterSupplier converter = parameterResult.getConverter();
        return new ServerMethodParameter(name,
                elementType, toClassName(paramType, currentClassInfo, actualEndpointInfo, index), type, single,
                converter, defaultValue, parameterResult.isObtainedAsCollection(), encoded);
    }

    protected void handleOtherParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        builder.setConverter(extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                existingConverters, errorLocation, hasRuntimeConverters));
    }

    protected void handleMultiMapParam(AdditionalReaders additionalReaders, ServerIndexedParameter builder) {
        additionalReaders.add(ServerFormUrlEncodedProvider.class, APPLICATION_FORM_URLENCODED,
                MultivaluedMap.class);
    }

    protected void handleSortedSetParam(Map<String, String> existingConverters, String errorLocation,
            boolean hasRuntimeConverters, ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new SortedSetConverter.SortedSetSupplier(converter));
    }

    protected void handleSetParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new SetConverter.SetSupplier(converter));
    }

    protected void handleListParam(Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters,
            ServerIndexedParameter builder, String elementType) {
        ParameterConverterSupplier converter = extractConverter(elementType, index, generatedClassBuildItemBuildProducer,
                existingConverters, errorLocation, hasRuntimeConverters);
        builder.setConverter(new ListConverter.ListSupplier(converter));
    }

    protected void handlePathSegmentParam(ServerIndexedParameter builder) {
        builder.setConverter(new PathSegmentParamConverter.Supplier());
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

    protected InjectableBean scanInjectableBean(ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo,
            Map<String, String> existingConverters,
            AdditionalReaders additionalReaders,
            Map<String, InjectableBean> injectableBeans,
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
                    Collections.emptySet(), field.name());
            if ((result.getType() != null) && (result.getType() != ParameterType.BEAN)) {
                //BODY means no annotation, so for fields not injectable
                fieldExtractors.put(field, result);
            }
            if (result.getConverter() != null) {
                initConverters.invokeStaticMethod(MethodDescriptor.ofMethod(currentTypeName,
                        ClassInjectorTransformer.INIT_CONVERTER_METHOD_NAME + field.name(),
                        void.class, Deployment.class),
                        initConverters.getMethodParam(0));
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
        if (superClassName != null && !superClassName.equals(DotNames.OBJECT)) {
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
            bytecodeTransformerBuildProducer.produce(new BytecodeTransformerBuildItem(currentTypeName,
                    new ClassInjectorTransformer(fieldExtractors, superTypeIsInjectable)));
        }
        currentInjectableBean.setInjectionRequired(!fieldExtractors.isEmpty() || superTypeIsInjectable);
        return currentInjectableBean;
    }

    public static final class Builder extends EndpointIndexer.Builder<ServerEndpointIndexer, Builder> {

        private BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
        private BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;
        private MethodCreator initConverters;

        @Override
        public ServerEndpointIndexer build() {
            return new ServerEndpointIndexer(this);
        }

        public MethodCreator getInitConverters() {
            return initConverters;
        }

        public Builder setBytecodeTransformerBuildProducer(
                BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer) {
            this.bytecodeTransformerBuildProducer = bytecodeTransformerBuildProducer;
            return this;
        }

        public Builder setGeneratedClassBuildItemBuildProducer(
                BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
            this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
            return this;
        }

        public Builder setInitConverters(MethodCreator initConverters) {
            this.initConverters = initConverters;
            return this;
        }
    }
}
