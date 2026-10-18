package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.types.PrimitiveType;

class PrimitiveTypeImpl extends TypeImpl<org.jboss.jandex.PrimitiveType> implements PrimitiveType {
    PrimitiveTypeImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.PrimitiveType jandexType) {
        super(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public String name() {
        return jandexType.name().toString();
    }

    @Override
    public PrimitiveKind primitiveKind() {
        org.jboss.jandex.PrimitiveType.Primitive primitive = jandexType.primitive();
        return switch (primitive) {
            case BOOLEAN -> PrimitiveKind.BOOLEAN;
            case BYTE -> PrimitiveKind.BYTE;
            case SHORT -> PrimitiveKind.SHORT;
            case INT -> PrimitiveKind.INT;
            case LONG -> PrimitiveKind.LONG;
            case FLOAT -> PrimitiveKind.FLOAT;
            case DOUBLE -> PrimitiveKind.DOUBLE;
            case CHAR -> PrimitiveKind.CHAR;
            default -> throw new IllegalStateException("Unknown primitive type " + primitive);
        };
    }
}
