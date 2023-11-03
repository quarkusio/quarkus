package io.quarkus.arc.processor.bcextensions;

class AllAnnotationTransformations {
    final AllAnnotationOverlays annotationOverlays;
    final AnnotationsTransformation.Classes classes;
    final AnnotationsTransformation.Methods methods;
    final AnnotationsTransformation.Parameters parameters;
    final AnnotationsTransformation.Fields fields;

    AllAnnotationTransformations(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays) {
        this.annotationOverlays = annotationOverlays;
        classes = new AnnotationsTransformation.Classes(jandexIndex, annotationOverlays);
        methods = new AnnotationsTransformation.Methods(jandexIndex, annotationOverlays);
        parameters = new AnnotationsTransformation.Parameters(jandexIndex, annotationOverlays);
        fields = new AnnotationsTransformation.Fields(jandexIndex, annotationOverlays);
    }

    void freeze() {
        classes.freeze();
        methods.freeze();
        parameters.freeze();
        fields.freeze();
    }
}
