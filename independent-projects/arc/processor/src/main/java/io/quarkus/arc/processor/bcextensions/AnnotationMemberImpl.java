package io.quarkus.arc.processor.bcextensions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;

class AnnotationMemberImpl implements AnnotationMember {
    final org.jboss.jandex.IndexView jandexIndex;
    final AllAnnotationOverlays annotationOverlays;

    final Kind kind;
    final org.jboss.jandex.AnnotationValue jandexAnnotationMember;

    AnnotationMemberImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.AnnotationValue jandexAnnotationMember) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlays = annotationOverlays;
        this.kind = determineKind(jandexAnnotationMember);
        this.jandexAnnotationMember = jandexAnnotationMember;
    }

    private static Kind determineKind(org.jboss.jandex.AnnotationValue value) {
        switch (value.kind()) {
            case BOOLEAN:
                return Kind.BOOLEAN;
            case BYTE:
                return Kind.BYTE;
            case SHORT:
                return Kind.SHORT;
            case INTEGER:
                return Kind.INT;
            case LONG:
                return Kind.LONG;
            case FLOAT:
                return Kind.FLOAT;
            case DOUBLE:
                return Kind.DOUBLE;
            case CHARACTER:
                return Kind.CHAR;
            case STRING:
                return Kind.STRING;
            case ENUM:
                return Kind.ENUM;
            case CLASS:
                return Kind.CLASS;
            case NESTED:
                return Kind.NESTED_ANNOTATION;
            case ARRAY:
                return Kind.ARRAY;
            default:
                throw new IllegalArgumentException("Unknown annotation member " + value);
        }
    }

    private void checkKind(Kind kind) {
        if (this.kind != kind) {
            throw new IllegalStateException("Not " + kind + ": " + jandexAnnotationMember);
        }
    }

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public boolean asBoolean() {
        checkKind(Kind.BOOLEAN);
        return jandexAnnotationMember.asBoolean();
    }

    @Override
    public byte asByte() {
        checkKind(Kind.BYTE);
        return jandexAnnotationMember.asByte();
    }

    @Override
    public short asShort() {
        checkKind(Kind.SHORT);
        return jandexAnnotationMember.asShort();
    }

    @Override
    public int asInt() {
        checkKind(Kind.INT);
        return jandexAnnotationMember.asInt();
    }

    @Override
    public long asLong() {
        checkKind(Kind.LONG);
        return jandexAnnotationMember.asLong();
    }

    @Override
    public float asFloat() {
        checkKind(Kind.FLOAT);
        return jandexAnnotationMember.asFloat();
    }

    @Override
    public double asDouble() {
        checkKind(Kind.DOUBLE);
        return jandexAnnotationMember.asDouble();
    }

    @Override
    public char asChar() {
        checkKind(Kind.CHAR);
        return jandexAnnotationMember.asChar();
    }

    @Override
    public String asString() {
        checkKind(Kind.STRING);
        return jandexAnnotationMember.asString();
    }

    @Override
    public <E extends Enum<E>> E asEnum(Class<E> enumType) {
        checkKind(Kind.ENUM);
        return Enum.valueOf(enumType, jandexAnnotationMember.asEnum());
    }

    @Override
    public String asEnumConstant() {
        checkKind(Kind.ENUM);
        return jandexAnnotationMember.asEnum();
    }

    @Override
    public ClassInfo asEnumClass() {
        checkKind(Kind.ENUM);
        return new ClassInfoImpl(jandexIndex, annotationOverlays,
                jandexIndex.getClassByName(jandexAnnotationMember.asEnumType()));
    }

    @Override
    public Type asType() {
        checkKind(Kind.CLASS);
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlays, jandexAnnotationMember.asClass());
    }

    @Override
    public AnnotationInfo asNestedAnnotation() {
        checkKind(Kind.NESTED_ANNOTATION);
        return new AnnotationInfoImpl(jandexIndex, annotationOverlays, jandexAnnotationMember.asNested());
    }

    @Override
    public List<AnnotationMember> asArray() {
        checkKind(Kind.ARRAY);
        return jandexAnnotationMember.asArrayList()
                .stream()
                .map(it -> new AnnotationMemberImpl(jandexIndex, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnnotationMemberImpl that = (AnnotationMemberImpl) o;
        return Objects.equals(jandexAnnotationMember.value(), that.jandexAnnotationMember.value());
    }

    @Override
    public int hashCode() {
        return Objects.hash(jandexAnnotationMember.value());
    }

    @Override
    public String toString() {
        return "" + jandexAnnotationMember.value();
    }
}
