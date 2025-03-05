package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveCompressionHandler;
import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;
import io.quarkus.vertx.http.runtime.HttpCompression;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;

public class CompressionScanner implements MethodScanner {

    static final DotName COMPRESSED = DotName.createSimple(Compressed.class.getName());
    static final DotName UNCOMPRESSED = DotName.createSimple(Uncompressed.class.getName());

    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;

    public CompressionScanner(VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        if (!httpBuildTimeConfig.enableCompression()) {
            return Collections.emptyList();
        }

        AnnotationStore annotationStore = (AnnotationStore) methodContext.get(EndpointIndexer.METHOD_CONTEXT_ANNOTATION_STORE);
        HttpCompression compression = HttpCompression.UNDEFINED;
        if (annotationStore.hasAnnotation(method, COMPRESSED)) {
            compression = HttpCompression.ON;
        }
        if (annotationStore.hasAnnotation(method, UNCOMPRESSED)) {
            if (compression == HttpCompression.ON) {
                throw new IllegalStateException(
                        String.format(
                                "@Compressed and @Uncompressed cannot be both declared on resource method %s declared on %s",
                                method, actualEndpointClass));
            } else {
                compression = HttpCompression.OFF;
            }
        }
        if (compression == HttpCompression.OFF) {
            // No action is needed because the "Content-Encoding: identity" header is set for every request if compression is enabled
            return Collections.emptyList();
        }
        ResteasyReactiveCompressionHandler handler = new ResteasyReactiveCompressionHandler(
                Set.copyOf(httpBuildTimeConfig.compressMediaTypes().orElse(Collections.emptyList())));
        handler.setCompression(compression);
        String[] produces = (String[]) methodContext.get(EndpointIndexer.METHOD_PRODUCES);
        if ((produces != null) && (produces.length > 0)) {
            handler.setProduces(produces[0]);
        } else {
            handler.setProduces(null);
        }
        return List.of(new FixedHandlerChainCustomizer(handler, HandlerChainCustomizer.Phase.AFTER_RESPONSE_CREATED));
    }

}
