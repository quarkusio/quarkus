package io.quarkus.arc.processor.bcextensions;

import java.util.List;

class ExtensionPhaseSynthesis extends ExtensionPhaseBase {
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    private final List<SyntheticBeanBuilderImpl<?>> syntheticBeans;
    private final List<SyntheticObserverBuilderImpl<?>> syntheticObservers;

    ExtensionPhaseSynthesis(ExtensionInvoker invoker, org.jboss.jandex.IndexView beanArchiveIndex, SharedErrors errors,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay, List<SyntheticBeanBuilderImpl<?>> syntheticBeans,
            List<SyntheticObserverBuilderImpl<?>> syntheticObservers) {
        super(ExtensionPhase.SYNTHESIS, invoker, beanArchiveIndex, errors);
        this.annotationOverlay = annotationOverlay;
        this.syntheticBeans = syntheticBeans;
        this.syntheticObservers = syntheticObservers;
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        return switch (type) {
            case SYNTHETIC_COMPONENTS -> new SyntheticComponentsImpl(syntheticBeans, syntheticObservers,
                    method.extensionClass.name());
            case TYPES -> new TypesImpl(index, annotationOverlay);
            default -> super.argumentForExtensionMethod(type, method);
        };
    }
}
