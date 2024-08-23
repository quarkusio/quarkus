package io.quarkus.arc.processor.bcextensions;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;

class AnnotationMemberImpl implements AnnotationMember {
    final org.jboss.jandex.IndexView jandexIndex;
    final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    final Kind kind;
    final org.jboss.jandex.AnnotationValue jandexAnnotationMember;

    AnnotationMemberImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.AnnotationValue jandexAnnotationMember) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.kind = determineKind(jandexAnnotationMember);
        this.jandexAnnotationMember = jandexAnnotationMember;
    }

    private static Kind determineKind(org.jboss.jandex.AnnotationValue value) {
        return switch (value.kind()) {
            case BOOLEAN -> Kind.BOOLEAN;
            case BYTE -> Kind.BYTE;
            case SHORT -> Kind.SHORT;
            case INTEGER -> Kind.INT;
            case LONG -> Kind.LONG;
            case FLOAT -> Kind.FLOAT;
            case DOUBLE -> Kind.DOUBLE;
            case CHARACTER -> Kind.CHAR;
            case STRING -> Kind.STRING;
            case ENUM -> Kind.ENUM;
            case CLASS -> Kind.CLASS;
            case NESTED -> Kind.NESTED_ANNOTATION;
            case ARRAY -> Kind.ARRAY;
            default -> throw new IllegalArgumentException("Unknown annotation member " + value);
        };
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
        return new ClassInfoImpl(jandexIndex, annotationOverlay,
                jandexIndex.getClassByName(jandexAnnotationMember.asEnumType()));
    }

    @Override
    public Type asType() {
        checkKind(Kind.CLASS);
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, jandexAnnotationMember.asClass());
    }

    @Override
    public AnnotationInfo asNestedAnnotation() {
        checkKind(Kind.NESTED_ANNOTATION);
        return new AnnotationInfoImpl(jandexIndex, annotationOverlay, jandexAnnotationMember.asNested());
    }

    @Override
    public List<AnnotationMember> asArray() {
        checkKind(Kind.ARRAY);
        return jandexAnnotationMember.asArrayList()
                .stream()
                .map(it -> (AnnotationMember) new AnnotationMemberImpl(jandexIndex, annotationOverlay, it))
                .toList();
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
