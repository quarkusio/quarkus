package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedResponseBuilderAndStreamingResponseCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public class ResponseHeaderMethodScanner implements MethodScanner {
    private static final DotName RESPONSE_HEADER = DotName.createSimple(ResponseHeader.class.getName());
    private static final DotName RESPONSE_HEADER_LIST = DotName.createSimple(ResponseHeader.List.class.getName());

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        AnnotationStore annotationStore = (AnnotationStore) methodContext
                .get(EndpointIndexer.METHOD_CONTEXT_ANNOTATION_STORE);
        AnnotationInstance responseHeaderInstance = annotationStore.getAnnotation(method, RESPONSE_HEADER);
        AnnotationInstance responseHeadersInstance = annotationStore.getAnnotation(method, RESPONSE_HEADER_LIST);
        if ((responseHeaderInstance == null) && (responseHeadersInstance == null)) {
            return Collections.emptyList();
        }
        List<AnnotationInstance> instances = new ArrayList<>();
        if (responseHeaderInstance != null) {
            instances.add(responseHeaderInstance);
        }
        if (responseHeadersInstance != null) {
            AnnotationValue value = responseHeadersInstance.value();
            if (value != null) {
                instances.addAll(Arrays.asList(value.asNestedArray()));
            }
        }
        Map<String, List<String>> headers = new HashMap<>();
        for (AnnotationInstance headerInstance : instances) {
            headers.put(headerInstance.value("name").asString(),
                    Arrays.asList(headerInstance.value("value").asStringArray()));
        }
        ResponseHandler.ResponseBuilderCustomizer.AddHeadersCustomizer responseBuilderCustomizer = new ResponseHandler.ResponseBuilderCustomizer.AddHeadersCustomizer();
        responseBuilderCustomizer.setHeaders(headers);
        PublisherResponseHandler.StreamingResponseCustomizer.AddHeadersCustomizer streamingResponseCustomizer = new PublisherResponseHandler.StreamingResponseCustomizer.AddHeadersCustomizer();
        streamingResponseCustomizer.setHeaders(headers);
        return Collections.singletonList(new FixedResponseBuilderAndStreamingResponseCustomizer(
                responseBuilderCustomizer, streamingResponseCustomizer));
    }
}
