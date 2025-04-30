package org.jboss.resteasy.reactive.server.processor.scanning;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETABLE_FUTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LEGACY_PUBLISHER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUBLISHER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.server.Cancellable;
import org.jboss.resteasy.reactive.server.handlers.CompletionStageResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.UniResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public class AsyncReturnTypeScanner implements MethodScanner {

    private static final DotName CANCELLABLE = DotName.createSimple(Cancellable.class.getName());

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        DotName returnTypeName = method.returnType().name();
        AnnotationStore annotationStore = (AnnotationStore) methodContext
                .get(EndpointIndexer.METHOD_CONTEXT_ANNOTATION_STORE);
        boolean isCancelable = determineCancelable(method, actualEndpointClass, annotationStore);
        if (returnTypeName.equals(COMPLETION_STAGE) || returnTypeName.equals(COMPLETABLE_FUTURE)) {
            CompletionStageResponseHandler handler = new CompletionStageResponseHandler();
            handler.setCancellable(isCancelable);
            return Collections.singletonList(new FixedHandlerChainCustomizer(handler,
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        } else if (returnTypeName.equals(UNI)) {
            UniResponseHandler handler = new UniResponseHandler();
            handler.setCancellable(isCancelable);
            return Collections.singletonList(new FixedHandlerChainCustomizer(handler,
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        }
        if (returnTypeName.equals(MULTI) || returnTypeName.equals(REST_MULTI) || returnTypeName.equals(PUBLISHER)
                || returnTypeName.equals(LEGACY_PUBLISHER)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new PublisherResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        }
        return Collections.emptyList();
    }

    private boolean determineCancelable(MethodInfo method, ClassInfo clazz, AnnotationStore annotationStore) {
        AnnotationInstance instance = annotationStore.getAnnotation(method, CANCELLABLE);
        if (instance == null) {
            instance = annotationStore.getAnnotation(method.declaringClass(), CANCELLABLE);
            if ((instance == null) && !clazz.equals(method.declaringClass())) {
                instance = annotationStore.getAnnotation(clazz, CANCELLABLE);
            }
        }
        if (instance != null) {
            AnnotationValue value = instance.value();
            if (value != null) {
                return value.asBoolean();
            }
        }
        return true;
    }

    @Override
    public boolean isMethodSignatureAsync(MethodInfo method) {
        DotName returnTypeName = method.returnType().name();
        return returnTypeName.equals(COMPLETION_STAGE) || returnTypeName.equals(COMPLETABLE_FUTURE) ||
                returnTypeName.equals(UNI) || returnTypeName.equals(MULTI) || returnTypeName.equals(REST_MULTI) ||
                returnTypeName.equals(PUBLISHER) || returnTypeName.equals(LEGACY_PUBLISHER);
    }
}
