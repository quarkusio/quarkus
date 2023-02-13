package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.types.PrimitiveType;

class PrimitiveTypeImpl extends TypeImpl<org.jboss.jandex.PrimitiveType> implements PrimitiveType {
    PrimitiveTypeImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.PrimitiveType jandexType) {
        super(jandexIndex, annotationOverlays, jandexType);
    }

    @Override
    public String name() {
        return jandexType.name().toString();
    }

    @Override
    public PrimitiveKind primitiveKind() {
        org.jboss.jandex.PrimitiveType.Primitive primitive = jandexType.primitive();
        switch (primitive) {
            case BOOLEAN:
                return PrimitiveKind.BOOLEAN;
            case BYTE:
                return PrimitiveKind.BYTE;
            case SHORT:
                return PrimitiveKind.SHORT;
            case INT:
                return PrimitiveKind.INT;
            case LONG:
                return PrimitiveKind.LONG;
            case FLOAT:
                return PrimitiveKind.FLOAT;
            case DOUBLE:
                return PrimitiveKind.DOUBLE;
            case CHAR:
                return PrimitiveKind.CHAR;
            default:
                throw new IllegalStateException("Unknown primitive type " + primitive);
        }
    }
}
