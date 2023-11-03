package io.quarkus.arc.processor.bcextensions;

import java.util.List;

import org.jboss.jandex.DotName;

class ExtensionPhaseSynthesis extends ExtensionPhaseBase {
    private final AllAnnotationOverlays annotationOverlays;

    private final List<SyntheticBeanBuilderImpl<?>> syntheticBeans;
    private final List<SyntheticObserverBuilderImpl<?>> syntheticObservers;

    ExtensionPhaseSynthesis(ExtensionInvoker invoker, org.jboss.jandex.IndexView beanArchiveIndex, SharedErrors errors,
            AllAnnotationOverlays annotationOverlays, List<SyntheticBeanBuilderImpl<?>> syntheticBeans,
            List<SyntheticObserverBuilderImpl<?>> syntheticObservers) {
        super(ExtensionPhase.SYNTHESIS, invoker, beanArchiveIndex, errors);
        this.annotationOverlays = annotationOverlays;
        this.syntheticBeans = syntheticBeans;
        this.syntheticObservers = syntheticObservers;
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        switch (type) {
            case SYNTHETIC_COMPONENTS:
                DotName extensionClass = method.extensionClass.name();
                return new SyntheticComponentsImpl(syntheticBeans, syntheticObservers, extensionClass);
            case TYPES:
                return new TypesImpl(index, annotationOverlays);

            default:
                return super.argumentForExtensionMethod(type, method);
        }
    }
}
