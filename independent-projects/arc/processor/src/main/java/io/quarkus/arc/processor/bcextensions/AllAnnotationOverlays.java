package io.quarkus.arc.processor.bcextensions;

class AllAnnotationOverlays {
    final AnnotationsOverlay.Classes classes;
    final AnnotationsOverlay.Methods methods;
    final AnnotationsOverlay.Parameters parameters;
    final AnnotationsOverlay.Fields fields;

    AllAnnotationOverlays() {
        classes = new AnnotationsOverlay.Classes();
        methods = new AnnotationsOverlay.Methods();
        parameters = new AnnotationsOverlay.Parameters();
        fields = new AnnotationsOverlay.Fields();
    }

    void invalidate() {
        classes.invalidate();
        methods.invalidate();
        parameters.invalidate();
        fields.invalidate();
    }
}
