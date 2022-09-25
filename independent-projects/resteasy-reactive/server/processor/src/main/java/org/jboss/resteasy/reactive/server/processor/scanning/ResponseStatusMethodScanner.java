package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedResponseBuilderAndStreamingResponseCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public class ResponseStatusMethodScanner implements MethodScanner {
    private static final DotName RESPONSE_STATUS = DotName.createSimple(ResponseStatus.class.getName());

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        AnnotationStore annotationStore = (AnnotationStore) methodContext
                .get(EndpointIndexer.METHOD_CONTEXT_ANNOTATION_STORE);
        AnnotationInstance annotationInstance = annotationStore.getAnnotation(method, RESPONSE_STATUS);
        if (annotationInstance == null) {
            return Collections.emptyList();
        }
        AnnotationValue responseStatusValue = annotationInstance.value();
        if (responseStatusValue == null) {
            return Collections.emptyList();
        }
        int statusCode = responseStatusValue.asInt();
        ResponseHandler.ResponseBuilderCustomizer.StatusCustomizer responseBuilderCustomizer = new ResponseHandler.ResponseBuilderCustomizer.StatusCustomizer();
        responseBuilderCustomizer.setStatus(statusCode);
        PublisherResponseHandler.StreamingResponseCustomizer.StatusCustomizer streamingResponseCustomizer = new PublisherResponseHandler.StreamingResponseCustomizer.StatusCustomizer();
        streamingResponseCustomizer.setStatus(statusCode);
        return Collections.singletonList(new FixedResponseBuilderAndStreamingResponseCustomizer(
                responseBuilderCustomizer, streamingResponseCustomizer));
    }
}
