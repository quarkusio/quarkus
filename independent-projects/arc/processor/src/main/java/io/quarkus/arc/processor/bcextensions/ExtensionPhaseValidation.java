package io.quarkus.arc.processor.bcextensions;

import java.util.List;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.MutableAnnotationOverlay;

import io.quarkus.arc.processor.AsyncHandlerInfo;

class ExtensionPhaseValidation extends ExtensionPhaseBase {
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final List<AsyncHandlerInfo> asyncHandlers;

    ExtensionPhaseValidation(ExtensionInvoker invoker, IndexView beanArchiveIndex, SharedErrors errors,
            MutableAnnotationOverlay annotationOverlay, List<AsyncHandlerInfo> asyncHandlers) {
        super(ExtensionPhase.VALIDATION, invoker, beanArchiveIndex, errors);
        this.annotationOverlay = annotationOverlay;
        this.asyncHandlers = asyncHandlers;
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        return switch (type) {
            case INVOKER_VALIDATION -> new InvokerValidationImpl(errors, asyncHandlers);
            case TYPES -> new TypesImpl(index, annotationOverlay);
            default -> super.argumentForExtensionMethod(type, method);
        };
    }
}
