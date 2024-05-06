package io.quarkus.arc.processor.bcextensions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;

import org.jboss.jandex.DotName;

class ExtensionPhaseDiscovery extends ExtensionPhaseBase {
    private final Set<String> additionalClasses;

    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final Map<DotName, ClassConfig> qualifiers;
    private final Map<DotName, ClassConfig> interceptorBindings;
    private final Map<DotName, ClassConfig> stereotypes;
    private final List<MetaAnnotationsImpl.ContextData> contexts;

    ExtensionPhaseDiscovery(ExtensionInvoker invoker, org.jboss.jandex.IndexView applicationIndex, SharedErrors errors,
            Set<String> additionalClasses, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            Map<DotName, ClassConfig> qualifiers, Map<DotName, ClassConfig> interceptorBindings,
            Map<DotName, ClassConfig> stereotypes, List<MetaAnnotationsImpl.ContextData> contexts) {
        super(ExtensionPhase.DISCOVERY, invoker, applicationIndex, errors);
        this.additionalClasses = additionalClasses;
        this.annotationOverlay = annotationOverlay;
        this.qualifiers = qualifiers;
        this.interceptorBindings = interceptorBindings;
        this.stereotypes = stereotypes;
        this.contexts = contexts;
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        return switch (type) {
            case META_ANNOTATIONS -> new MetaAnnotationsImpl(index, annotationOverlay, qualifiers, interceptorBindings,
                    stereotypes, contexts);
            case SCANNED_CLASSES -> new ScannedClassesImpl(additionalClasses);
            default -> super.argumentForExtensionMethod(type, method);
        };
    }
}
