package io.quarkus.resteasy.reactive.server.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.STRING;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.parameters.converters.GeneratedParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.NoopParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.server.common.runtime.EndpointInvokerFactory;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class QuarkusServerEndpointIndexer
        extends ServerEndpointIndexer {
    private final MethodCreator initConverters;
    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
    private final DefaultProducesHandler defaultProducesHandler;
    private final ResteasyReactiveRecorder resteasyReactiveRecorder;

    private final Map<String, String> multipartGeneratedPopulators = new HashMap<>();
    private final Predicate<String> applicationClassPredicate;

    private static final Set<DotName> CONTEXT_TYPES = Set.of(
            DotName.createSimple(HttpServerRequest.class.getName()),
            DotName.createSimple(HttpServerResponse.class.getName()),
            DotName.createSimple(RoutingContext.class.getName()));

    QuarkusServerEndpointIndexer(Builder builder) {
        super(builder);
        this.initConverters = builder.initConverters;
        this.generatedClassBuildItemBuildProducer = builder.generatedClassBuildItemBuildProducer;
        this.bytecodeTransformerBuildProducer = builder.bytecodeTransformerBuildProducer;
        this.reflectiveClassProducer = builder.reflectiveClassProducer;
        this.defaultProducesHandler = builder.defaultProducesHandler;
        this.applicationClassPredicate = builder.applicationClassPredicate;
        this.resteasyReactiveRecorder = builder.resteasyReactiveRecorder;
    }

    protected boolean isContextType(ClassType klass) {
        return super.isContextType(klass) || CONTEXT_TYPES.contains(klass.name());
    }

    @Override
    protected String[] applyAdditionalDefaults(Type nonAsyncReturnType) {
        List<MediaType> defaultMediaTypes = defaultProducesHandler.handle(new DefaultProducesHandler.Context() {
            @Override
            public Type nonAsyncReturnType() {
                return nonAsyncReturnType;
            }

            @Override
            public IndexView index() {
                return index;
            }

            @Override
            public ResteasyReactiveConfig config() {
                return config;
            }
        });
        if ((defaultMediaTypes != null) && !defaultMediaTypes.isEmpty()) {
            String[] result = new String[defaultMediaTypes.size()];
            for (int i = 0; i < defaultMediaTypes.size(); i++) {
                result[i] = defaultMediaTypes.get(i).toString();
            }
            return result;
        }
        return super.applyAdditionalDefaults(nonAsyncReturnType);
    }

    @Override
    protected boolean handleCustomParameter(Map<DotName, AnnotationInstance> anns, ServerIndexedParameter builder,
            Type paramType, boolean field, Map<String, Object> methodContext) {
        methodContext.put(GeneratedClassBuildItem.class.getName(), generatedClassBuildItemBuildProducer);
        methodContext.put(EndpointInvokerFactory.class.getName(), resteasyReactiveRecorder);
        return super.handleCustomParameter(anns, builder, paramType, field, methodContext);
    }

    @Override
    protected ParameterConverterSupplier extractConverter(String elementType, IndexView indexView,
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
                    boolean isStatic = ((i.flags() & Modifier.STATIC) != 0);
                    boolean isNotPrivate = (i.flags() & Modifier.PRIVATE) == 0;
                    if ((i.parameters().size() == 1) && isNotPrivate) {
                        if (i.parameters().get(0).name().equals(STRING)) {
                            if (i.name().equals("<init>")) {
                                stringCtor = i;
                            } else if (i.name().equals("valueOf") && isStatic) {
                                valueOf = MethodDescriptor.of(i);
                            } else if (i.name().equals("fromString") && isStatic) {
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
            String effectivePrefix = prefix + elementType;
            if (effectivePrefix.startsWith("java")) {
                effectivePrefix = effectivePrefix.replace("java", "javaq"); // generated classes can't start with the java package
            }
            baseName = effectivePrefix + "$quarkusrestparamConverter$";
            try (ClassCreator classCreator = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer,
                            applicationClassPredicate.test(elementType)),
                    baseName, null,
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

    protected void handleFieldExtractors(String currentTypeName, Map<FieldInfo, ServerIndexedParameter> fieldExtractors,
            boolean superTypeIsInjectable) {
        bytecodeTransformerBuildProducer.produce(new BytecodeTransformerBuildItem(currentTypeName,
                new ClassInjectorTransformer(fieldExtractors, superTypeIsInjectable)));
    }

    protected void handleConverter(String currentTypeName, FieldInfo field) {
        initConverters.invokeStaticMethod(MethodDescriptor.ofMethod(currentTypeName,
                ClassInjectorTransformer.INIT_CONVERTER_METHOD_NAME + field.name(),
                void.class, Deployment.class),
                initConverters.getMethodParam(0));
    }

    protected void handleMultipart(ClassInfo multipartClassInfo) {
        String className = multipartClassInfo.name().toString();
        if (multipartGeneratedPopulators.containsKey(className)) {
            // we've already seen this class before and have done all we need to make it work
            return;
        }
        reflectiveClassProducer.produce(new ReflectiveClassBuildItem(false, false, className));
        String populatorClassName = MultipartPopulatorGenerator.generate(multipartClassInfo,
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), index);
        multipartGeneratedPopulators.put(className, populatorClassName);

        // transform the multipart pojo (and any super-classes) so we can access its fields no matter what
        ClassInfo currentClassInHierarchy = multipartClassInfo;
        while (true) {
            bytecodeTransformerBuildProducer
                    .produce(new BytecodeTransformerBuildItem(currentClassInHierarchy.name().toString(),
                            new MultipartTransformer(populatorClassName)));

            DotName superClassDotName = currentClassInHierarchy.superName();
            if (superClassDotName.equals(DotNames.OBJECT_NAME)) {
                break;
            }
            ClassInfo newCurrentClassInHierarchy = index.getClassByName(superClassDotName);
            if (newCurrentClassInHierarchy == null) {
                break;
            }
            currentClassInHierarchy = newCurrentClassInHierarchy;
        }

    }

    public static final class Builder extends AbstractBuilder<Builder> {

        private BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
        private BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;
        private BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
        private ResteasyReactiveRecorder resteasyReactiveRecorder;
        private MethodCreator initConverters;
        private DefaultProducesHandler defaultProducesHandler = DefaultProducesHandler.Noop.INSTANCE;
        public Predicate<String> applicationClassPredicate;

        @Override
        public QuarkusServerEndpointIndexer build() {
            return new QuarkusServerEndpointIndexer(this);
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

        public Builder setReflectiveClassProducer(
                BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
            this.reflectiveClassProducer = reflectiveClassProducer;
            return this;
        }

        public Builder setApplicationClassPredicate(Predicate<String> applicationClassPredicate) {
            this.applicationClassPredicate = applicationClassPredicate;
            return this;
        }

        public Builder setInitConverters(MethodCreator initConverters) {
            this.initConverters = initConverters;
            return this;
        }

        public Builder setResteasyReactiveRecorder(ResteasyReactiveRecorder resteasyReactiveRecorder) {
            this.resteasyReactiveRecorder = resteasyReactiveRecorder;
            return this;
        }

        public Builder setDefaultProducesHandler(DefaultProducesHandler defaultProducesHandler) {
            this.defaultProducesHandler = defaultProducesHandler;
            return this;
        }
    }
}
