package io.quarkus.jaxrs.client.reactive.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.processor.scanning.ClientEndpointIndexer;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;

import io.quarkus.deployment.Capabilities;
import io.quarkus.resteasy.reactive.common.deployment.JsonDefaultProducersHandler;

public class QuarkusClientEndpointIndexer extends ClientEndpointIndexer {

    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(QuarkusClientEndpointIndexer.class);

    private final JsonDefaultProducersHandler jsonDefaultProducersHandler;
    private final Capabilities capabilities;

    QuarkusClientEndpointIndexer(Builder builder, String defaultProduces, boolean smartDefaultProduces) {
        super(builder, defaultProduces, smartDefaultProduces);
        capabilities = builder.capabilities;
        jsonDefaultProducersHandler = new JsonDefaultProducersHandler();
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
    protected void handleAdditionalMethodProcessing(ResourceMethod method, ClassInfo currentClassInfo,
            MethodInfo info, AnnotationStore annotationStore) {
        super.handleAdditionalMethodProcessing(method, currentClassInfo, info, annotationStore);
        if (!capabilities.isCapabilityWithPrefixMissing("io.quarkus.rest.client.reactive.json")) {
            return;
        }
        warnAboutMissingJsonProviderIfNeeded(method, info, jsonDefaultProducersHandler, currentDefaultProducesContext);
    }

    @Override
    protected void logMissingJsonWarning(MethodInfo info) {
        LOGGER.warnf("Quarkus detected the use of JSON in REST Client method '" + info.declaringClass().name() + "#"
                + info.name()
                + "' but no JSON extension has been added. Consider adding 'quarkus-rest-client-jackson' (recommended) or 'quarkus-rest-client-jsonb'.");
    }

    @Override
    protected void warnAboutMissUsedBodyParameter(DotName httpMethod, MethodInfo methodInfo) {
        // do nothing in the case of the client
    }

    public static final class Builder extends AbstractBuilder<Builder> {

        private final Capabilities capabilities;

        public Builder(Capabilities capabilities) {
            this.capabilities = capabilities;
        }

    }
}
