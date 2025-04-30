package io.quarkus.resteasy.reactive.server.deployment;

import static org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_READER;
import static org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_WRITER;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;
import org.jboss.resteasy.reactive.common.processor.scanning.ScannedSerializer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;
import org.jboss.resteasy.reactive.server.spi.EndpointInvokerFactory;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.resteasy.reactive.common.deployment.JsonDefaultProducersHandler;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;

public class QuarkusServerEndpointIndexer
        extends ServerEndpointIndexer {

    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(QuarkusServerEndpointIndexer.class);
    private static final String REST_CLIENT_NOT_BODY_ANNOTATION = "io.quarkus.rest.client.reactive.NotBody";

    private final Capabilities capabilities;
    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final DefaultProducesHandler defaultProducesHandler;
    private final JsonDefaultProducersHandler jsonDefaultProducersHandler;
    private final ResteasyReactiveRecorder resteasyReactiveRecorder;

    QuarkusServerEndpointIndexer(Builder builder) {
        super(builder);
        this.capabilities = builder.capabilities;
        this.generatedClassBuildItemBuildProducer = builder.generatedClassBuildItemBuildProducer;
        this.defaultProducesHandler = builder.defaultProducesHandler;
        this.resteasyReactiveRecorder = builder.resteasyReactiveRecorder;
        this.jsonDefaultProducersHandler = new JsonDefaultProducersHandler();
    }

    private DefaultProducesHandler.Context currentDefaultProducesContext;

    @Override
    protected void setupApplyDefaults(Type nonAsyncReturnType, DotName httpMethod) {
        currentDefaultProducesContext = new DefaultProducesHandler.Context() {
            @Override
            public Type nonAsyncReturnType() {
                return nonAsyncReturnType;
            }

            @Override
            public DotName httpMethod() {
                return httpMethod;
            }

            @Override
            public IndexView index() {
                return applicationIndex;
            }

            @Override
            public ResteasyReactiveConfig config() {
                return config;
            }
        };
    }

    @Override
    protected String[] applyAdditionalDefaults(Type nonAsyncReturnType) {
        List<MediaType> defaultMediaTypes = defaultProducesHandler.handle(currentDefaultProducesContext);
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

    public static final class Builder extends AbstractBuilder<Builder> {

        private final Capabilities capabilities;

        private BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
        private ResteasyReactiveRecorder resteasyReactiveRecorder;
        private DefaultProducesHandler defaultProducesHandler = DefaultProducesHandler.Noop.INSTANCE;
        public Predicate<String> applicationClassPredicate;

        public Builder(Capabilities capabilities) {
            this.capabilities = capabilities;
        }

        @Override
        public QuarkusServerEndpointIndexer build() {
            return new QuarkusServerEndpointIndexer(this);
        }

        public Builder setGeneratedClassBuildItemBuildProducer(
                BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
            this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
            return this;
        }

        public Builder setApplicationClassPredicate(Predicate<String> applicationClassPredicate) {
            this.applicationClassPredicate = applicationClassPredicate;
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

    @Override
    protected void handleAdditionalMethodProcessing(ServerResourceMethod method, ClassInfo currentClassInfo,
            MethodInfo info, AnnotationStore annotationStore) {
        super.handleAdditionalMethodProcessing(method, currentClassInfo, info, annotationStore);

        if (!capabilities.isCapabilityWithPrefixMissing("io.quarkus.resteasy.reactive.json")) {
            return;
        }

        warnAboutMissingJsonProviderIfNeeded(method, info, jsonDefaultProducersHandler, currentDefaultProducesContext);
    }

    @Override
    public boolean additionalRegisterClassForReflectionCheck(ResourceMethodCallbackEntry entry) {
        return checkBodyParameterMessageBodyReader(entry) || checkReturnTypeMessageBodyWriter(entry);
    }

    /**
     * Check whether the Resource Method has a body parameter for which there exists a matching
     * {@link jakarta.ws.rs.ext.MessageBodyReader}
     * that is not a {@link org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader}.
     * In this case the Resource Class needs to be registered for reflection because the
     * {@link jakarta.ws.rs.ext.MessageBodyReader#isReadable(Class, java.lang.reflect.Type, Annotation[], MediaType)}
     * method expects to be passed the method annotations.
     */
    private boolean checkBodyParameterMessageBodyReader(ResourceMethodCallbackEntry entry) {
        MethodParameter[] parameters = entry.getResourceMethod().getParameters();
        if (parameters.length == 0) {
            return false;
        }
        MethodParameter bodyParameter = null;
        for (MethodParameter parameter : parameters) {
            if (parameter.parameterType == ParameterType.BODY) {
                bodyParameter = parameter;
                break;
            }
        }
        if (bodyParameter == null) {
            return false;
        }
        String parameterClassName = bodyParameter.getDeclaredType();
        List<ScannedSerializer> readers = getSerializerScanningResult().getReaders();

        for (ScannedSerializer reader : readers) {
            if (isSubclassOf(parameterClassName, reader.getHandledClassName()) && !isServerMessageBodyReader(
                    reader.getClassInfo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the Resource Method has a return type for which there exists a matching
     * {@link jakarta.ws.rs.ext.MessageBodyWriter}
     * that is not a {@link org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter}.
     * In this case the Resource Class needs to be registered for reflection because the
     * {@link jakarta.ws.rs.ext.MessageBodyWriter#isWriteable(Class, java.lang.reflect.Type, Annotation[], MediaType)}
     * method expects to be passed the method annotations.
     */
    private boolean checkReturnTypeMessageBodyWriter(ResourceMethodCallbackEntry entry) {
        Type returnType = entry.getMethodInfo().returnType();
        String returnTypeName;
        switch (returnType.kind()) {
            case CLASS:
                returnTypeName = returnType.asClassType().name().toString();
                break;
            case PARAMETERIZED_TYPE:
                returnTypeName = returnType.asParameterizedType().name().toString();
                break;
            default:
                returnTypeName = null;
        }
        if (returnTypeName == null) {
            return false;
        }

        List<ScannedSerializer> writers = getSerializerScanningResult().getWriters();

        for (ScannedSerializer writer : writers) {
            if (isSubclassOf(returnTypeName, writer.getHandledClassName())
                    && !isServerMessageBodyWriter(writer.getClassInfo())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSubclassOf(String className, String parentName) {
        if (className.equals(parentName)) {
            return true;
        }
        ClassInfo classByName = index.getClassByName(className);
        if ((classByName == null) || (classByName.superName() == null)) {
            return false;
        }
        try {
            return JandexUtil.isSubclassOf(index, classByName,
                    DotName.createSimple(parentName));
        } catch (BuildException e) {
            return false;
        }
    }

    private boolean isServerMessageBodyReader(ClassInfo classInfo) {
        return index.getAllKnownImplementors(SERVER_MESSAGE_BODY_READER).contains(classInfo);
    }

    private boolean isServerMessageBodyWriter(ClassInfo classInfo) {
        return index.getAllKnownImplementors(SERVER_MESSAGE_BODY_WRITER).contains(classInfo);
    }

    @Override
    protected void logMissingJsonWarning(MethodInfo info) {
        LOGGER.warnf("Quarkus detected the use of JSON in JAX-RS method '" + info.declaringClass().name() + "#"
                + info.name()
                + "' but no JSON extension has been added. Consider adding 'quarkus-rest-jackson' (recommended) or 'quarkus-rest-jsonb'.");
    }

    @Override
    protected void warnAboutMissUsedBodyParameter(DotName httpMethod, MethodInfo methodInfo) {
        // This indexer also picks up REST client methods as well as there is no bulletproof way of distinguishing the two.
        // That is why we check for client specific annotations here
        if (methodInfo.hasAnnotation(REST_CLIENT_NOT_BODY_ANNOTATION)) {
            return;
        }
        super.warnAboutMissUsedBodyParameter(httpMethod, methodInfo);
    }

    /**
     * At this point we know exactly which resources will require field injection and therefore are required to be
     * {@link RequestScoped}.
     * We can't change anything CDI related at this point (because it would create build cycles), so all we can do
     * is fail the build if the resource has not already been handled automatically (by the best effort approach performed
     * elsewhere)
     * or it's not manually set to be {@link RequestScoped}.
     */
    @Override
    protected void verifyClassThatRequiresFieldInjection(ClassInfo classInfo) {
        if (!alreadyHandledRequestScopedResources.contains(classInfo.name())) {
            BuiltinScope scope = BuiltinScope.from(classInfo);
            if (BuiltinScope.REQUEST != scope) {
                throw new DeploymentException(
                        "Resource classes that use field injection for REST parameters can only be @RequestScoped. Offending class is "
                                + classInfo.name());
            }
        }
    }

}
