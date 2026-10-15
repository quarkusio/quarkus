package io.quarkus.arc.processor.bcextensions;

record TypeAndQualifiers(org.jboss.jandex.Type type, org.jboss.jandex.AnnotationInstance... qualifiers) {
}
