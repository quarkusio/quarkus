package io.quarkus.resteasy.reactive.links.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.quarkus.resteasy.reactive.links.RestLinksHandler;

public class LinksMethodScanner implements MethodScanner {

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        AnnotationStore annotationStore = (AnnotationStore) methodContext.get(EndpointIndexer.METHOD_CONTEXT_ANNOTATION_STORE);
        AnnotationInstance injectRestLinksInstance = getInjectRestLinksAnnotation(method, actualEndpointClass, annotationStore);
        if (injectRestLinksInstance == null) {
            return Collections.emptyList();
        }

        RestLinkType restLinkType = RestLinkType.TYPE;
        AnnotationValue injectRestLinksValue = injectRestLinksInstance.value();
        if (injectRestLinksValue != null) {
            restLinkType = RestLinkType.valueOf(injectRestLinksValue.asEnum());
        }

        AnnotationInstance restLinkInstance = annotationStore.getAnnotation(method, DotNames.REST_LINK_ANNOTATION);
        String entityType = null;
        if (restLinkInstance != null) {
            AnnotationValue restInstanceValue = restLinkInstance.value("entityType");
            if (restInstanceValue != null) {
                entityType = restInstanceValue.asClass().name().toString();
            }
        }

        RestLinksHandler handler = new RestLinksHandler();
        handler.setRestLinkData(new RestLinksHandler.RestLinkData(restLinkType, entityType));
        return Collections.singletonList(new FixedHandlerChainCustomizer(handler,
                HandlerChainCustomizer.Phase.AFTER_RESPONSE_CREATED));
    }

    private AnnotationInstance getInjectRestLinksAnnotation(MethodInfo method, ClassInfo actualEndpointClass,
            AnnotationStore annotationStore) {
        AnnotationInstance annotationInstance = annotationStore.getAnnotation(method, DotNames.INJECT_REST_LINKS_ANNOTATION);
        if (annotationInstance == null) {
            annotationInstance = annotationStore.getAnnotation(method.declaringClass(), DotNames.INJECT_REST_LINKS_ANNOTATION);
            if ((annotationInstance == null) && !actualEndpointClass.equals(method.declaringClass())) {
                annotationInstance = annotationStore.getAnnotation(actualEndpointClass, DotNames.INJECT_REST_LINKS_ANNOTATION);
            }
        }
        return annotationInstance;
    }
}
