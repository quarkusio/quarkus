package io.quarkus.arc.test.devconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.deployment.devconsole.Name;

public class NameTest {

    @Test
    public void testFromDotName() {
        assertName(Name.from(DotName.createSimple("org.acme.Foo")), "org.acme.Foo", "Foo");
        assertName(Name.from(DotName.createSimple("org.acme.Foo$Lu")), "org.acme.Foo$Lu", "Foo$Lu");
        assertName(Name.from(PrimitiveType.BYTE.name()), "byte", "byte");
    }

    @Test
    public void testFromType() {
        assertName(Name.from(Type.create(DotName.createSimple("org.acme.Foo"), Kind.CLASS)), "org.acme.Foo", "Foo");
        assertName(Name.from(PrimitiveType.BOOLEAN), "boolean", "boolean");
        assertName(Name.from(ArrayType.create(PrimitiveType.LONG, 1)), "long[]", "long[]");
        assertName(Name.from(ArrayType.create(Type.create(DotName.createSimple("org.acme.Foo"), Kind.CLASS), 1)),
                "org.acme.Foo[]", "Foo[]");
        assertName(
                Name.from(ParameterizedType.create(DotName.createSimple("org.acme.Foo"),
                        new Type[] { Type.create(DotName.createSimple("java.lang.String"), Kind.CLASS) }, null)),
                "org.acme.Foo<java.lang.String>", "Foo<String>");
        assertName(
                Name.from(ParameterizedType.create(DotName.createSimple("org.acme.Foo"),
                        new Type[] { ParameterizedType.create(DotName.createSimple("java.util.List"),
                                new Type[] { Type.create(DotName.createSimple("java.lang.String"), Kind.CLASS) }, null) },
                        null)),
                "org.acme.Foo<java.util.List<java.lang.String>>", "Foo<List<String>>");
    }

    @Test
    public void testFromAnnotation() {
        assertName(
                Name.from(AnnotationInstance.create(DotName.createSimple("org.acme.Bar"), null,
                        new AnnotationValue[] {})),
                "@org.acme.Bar", "@Bar");
        assertName(
                Name.from(AnnotationInstance.create(DotName.createSimple("org.acme.Bar"), null,
                        new AnnotationValue[] { AnnotationValue.createBooleanValue("checked", false) })),
                "@org.acme.Bar(checked = false)", "@Bar(checked = false)");
        assertName(
                Name.from(AnnotationInstance.create(DotName.createSimple("org.acme.Bar"), null,
                        new AnnotationValue[] { AnnotationValue.createClassValue("impl",
                                Type.create(DotName.createSimple("org.acme.Baz"), Kind.CLASS)) })),
                "@org.acme.Bar(impl = org.acme.Baz)", "@Bar(impl = org.acme.Baz)");
    }

    private void assertName(Name name, String expectedName, String expectedSimpleName) {
        assertEquals(expectedName, name.toString());
        assertEquals(expectedSimpleName, name.getSimpleName());
    }

}
