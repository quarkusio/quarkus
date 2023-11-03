package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;

class ExtensionPhaseValidation extends ExtensionPhaseBase {
    private final AllAnnotationOverlays annotationOverlays;
    private final Collection<io.quarkus.arc.processor.BeanInfo> allBeans;
    private final Collection<io.quarkus.arc.processor.ObserverInfo> allObservers;

    ExtensionPhaseValidation(ExtensionInvoker invoker, org.jboss.jandex.IndexView beanArchiveIndex, SharedErrors errors,
            AllAnnotationOverlays annotationOverlays, Collection<io.quarkus.arc.processor.BeanInfo> allBeans,
            Collection<io.quarkus.arc.processor.ObserverInfo> allObservers) {
        super(ExtensionPhase.VALIDATION, invoker, beanArchiveIndex, errors);
        this.annotationOverlays = annotationOverlays;
        this.allBeans = allBeans;
        this.allObservers = allObservers;
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        if (type == ExtensionMethodParameter.TYPES) {
            return new TypesImpl(index, annotationOverlays);
        }

        return super.argumentForExtensionMethod(type, method);
    }
}
