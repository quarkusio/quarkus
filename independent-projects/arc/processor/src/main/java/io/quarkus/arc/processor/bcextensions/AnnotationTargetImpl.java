package io.quarkus.arc.processor.bcextensions;

abstract class AnnotationTargetImpl {
    final org.jboss.jandex.IndexView jandexIndex;
    final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final org.jboss.jandex.EquivalenceKey key; // for equals/hashCode

    AnnotationTargetImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.EquivalenceKey key) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.key = key;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnnotationTargetImpl && key.equals(((AnnotationTargetImpl) obj).key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
