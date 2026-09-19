package io.quarkus.resteasy.reactive.links.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.quarkus.resteasy.reactive.links.RestLinksHandler;

public class LinksMethodScanner implements MethodScanner {

    private final IndexView index;

    public LinksMethodScanner(IndexView index) {
        this.index = index;
    }

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        AnnotationStore annotationStore = (AnnotationStore) methodContext.get(EndpointIndexer.METHOD_CONTEXT_ANNOTATION_STORE);
        MethodInfo endpointImplementation = ServerEndpointIndexer.findEndpointImplementation(method, actualEndpointClass,
                index);
        AnnotationInstance injectRestLinksInstance = getInjectRestLinksAnnotation(method, endpointImplementation,
                actualEndpointClass, annotationStore);
        if (injectRestLinksInstance == null) {
            return Collections.emptyList();
        }

        RestLinkType restLinkType = RestLinkType.TYPE;
        AnnotationValue injectRestLinksValue = injectRestLinksInstance.value();
        if (injectRestLinksValue != null) {
            restLinkType = RestLinkType.valueOf(injectRestLinksValue.asEnum());
        }

        AnnotationInstance restLinkInstance = getMethodAnnotation(method, endpointImplementation,
                DotNames.REST_LINK_ANNOTATION, annotationStore);
        String entityType = null;
        if (restLinkInstance != null) {
            AnnotationValue restInstanceValue = restLinkInstance.value("entityType");
            if (restInstanceValue != null) {
                entityType = restInstanceValue.asClass().name().toString();
            }
        }
        // If not explicitly set on @RestLink, deduce the entity type from the method return type.
        // This ensures collection-returning endpoints (e.g., List<Foo>) inject links for the element type (Foo),
        // instead of the runtime collection class.
        if (entityType == null) {
            Type nonAsync = RestLinksTypeUtil.getNonAsyncReturnType(method.returnType());
            entityType = RestLinksTypeUtil.deductEntityType(nonAsync);
        }

        RestLinksHandler handler = new RestLinksHandler();
        handler.setRestLinkData(new RestLinksHandler.RestLinkData(restLinkType, entityType));
        return Collections.singletonList(new FixedHandlerChainCustomizer(handler,
                HandlerChainCustomizer.Phase.AFTER_RESPONSE_CREATED));
    }

    private AnnotationInstance getInjectRestLinksAnnotation(MethodInfo method, MethodInfo endpointImplementation,
            ClassInfo actualEndpointClass, AnnotationStore annotationStore) {
        AnnotationInstance annotationInstance = getMethodAnnotation(method, endpointImplementation,
                DotNames.INJECT_REST_LINKS_ANNOTATION, annotationStore);
        if (annotationInstance == null) {
            annotationInstance = annotationStore.getAnnotation(method.declaringClass(), DotNames.INJECT_REST_LINKS_ANNOTATION);
            if ((annotationInstance == null) && !actualEndpointClass.equals(method.declaringClass())) {
                annotationInstance = annotationStore.getAnnotation(actualEndpointClass, DotNames.INJECT_REST_LINKS_ANNOTATION);
            }
        }
        return annotationInstance;
    }

    /**
     * An annotation on the overriding method that is actually invoked wins over the one on the declaring method.
     */
    private static AnnotationInstance getMethodAnnotation(MethodInfo method, MethodInfo endpointImplementation,
            DotName annotation, AnnotationStore annotationStore) {
        AnnotationInstance annotationInstance = null;
        if (!endpointImplementation.equals(method)) {
            annotationInstance = annotationStore.getAnnotation(endpointImplementation, annotation);
        }
        if (annotationInstance == null) {
            annotationInstance = annotationStore.getAnnotation(method, annotation);
        }
        return annotationInstance;
    }
}
