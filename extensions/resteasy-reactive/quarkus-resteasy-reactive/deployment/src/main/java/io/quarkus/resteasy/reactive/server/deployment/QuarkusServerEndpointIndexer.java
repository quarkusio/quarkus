package io.quarkus.resteasy.reactive.server.deployment;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;
import org.jboss.resteasy.reactive.server.spi.EndpointInvokerFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;

public class QuarkusServerEndpointIndexer
        extends ServerEndpointIndexer {
    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
    private final DefaultProducesHandler defaultProducesHandler;
    private final ResteasyReactiveRecorder resteasyReactiveRecorder;

    private final Predicate<String> applicationClassPredicate;

    QuarkusServerEndpointIndexer(Builder builder) {
        super(builder);
        this.generatedClassBuildItemBuildProducer = builder.generatedClassBuildItemBuildProducer;
        this.bytecodeTransformerBuildProducer = builder.bytecodeTransformerBuildProducer;
        this.reflectiveClassProducer = builder.reflectiveClassProducer;
        this.defaultProducesHandler = builder.defaultProducesHandler;
        this.applicationClassPredicate = builder.applicationClassPredicate;
        this.resteasyReactiveRecorder = builder.resteasyReactiveRecorder;
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

    public static final class Builder extends AbstractBuilder<Builder> {

        private BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
        private BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;
        private BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
        private ResteasyReactiveRecorder resteasyReactiveRecorder;
        private DefaultProducesHandler defaultProducesHandler = DefaultProducesHandler.Noop.INSTANCE;
        public Predicate<String> applicationClassPredicate;

        @Override
        public QuarkusServerEndpointIndexer build() {
            return new QuarkusServerEndpointIndexer(this);
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
