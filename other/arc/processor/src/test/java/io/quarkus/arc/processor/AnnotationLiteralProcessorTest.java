package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

public class AnnotationLiteralProcessorTest {
    public enum SimpleEnum {
        FOO,
        BAR,
        BAZ,
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimpleAnnotation {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ComplexAnnotation {
        boolean bool();

        byte b();

        short s();

        int i();

        long l();

        float f();

        double d();

        char ch();

        String str();

        SimpleEnum en();

        Class<?> cls();

        SimpleAnnotation nested();

        boolean[] boolArray();

        byte[] bArray();

        short[] sArray();

        int[] iArray();

        long[] lArray();

        float[] fArray();

        double[] dArray();

        char[] chArray();

        String[] strArray();

        SimpleEnum[] enArray();

        Class<?>[] clsArray();

        SimpleAnnotation[] nestedArray();
    }

    private final String generatedClass = "io.quarkus.arc.processor.test.GeneratedClass";
    private final IndexView index;

    public AnnotationLiteralProcessorTest() throws IOException {
        index = Index.of(SimpleEnum.class, SimpleAnnotation.class, ComplexAnnotation.class);
    }

    @Test
    public void test() throws ReflectiveOperationException {
        AnnotationLiteralProcessor literals = new AnnotationLiteralProcessor(index, ignored -> true);

        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className(generatedClass).build()) {
            MethodCreator method = creator.getMethodCreator("get", ComplexAnnotation.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle annotation = literals.create(method,
                    index.getClassByName(DotName.createSimple(ComplexAnnotation.class.getName())), complexAnnotation());
            method.returnValue(annotation);
        }

        Collection<ResourceOutput.Resource> resources = new AnnotationLiteralGenerator(false)
                .generate(literals.getCache(), Collections.emptySet());
        for (ResourceOutput.Resource resource : resources) {
            if (resource.getType() == ResourceOutput.Resource.Type.JAVA_CLASS) {
                cl.write(resource.getName(), resource.getData());
            } else {
                throw new IllegalStateException("Unexpected " + resource.getType() + " " + resource.getName());
            }
        }

        Class<?> clazz = cl.loadClass(generatedClass);
        ComplexAnnotation annotation = (ComplexAnnotation) clazz.getMethod("get").invoke(null);
        verify(annotation);
    }

    private static void verify(ComplexAnnotation ann) {
        assertEquals(true, ann.bool());
        assertEquals((byte) 1, ann.b());
        assertEquals((short) 2, ann.s());
        assertEquals(3, ann.i());
        assertEquals(4L, ann.l());
        assertEquals(5.0F, ann.f());
        assertEquals(6.0, ann.d());
        assertEquals('a', ann.ch());
        assertEquals("bc", ann.str());
        assertEquals(SimpleEnum.FOO, ann.en());
        assertEquals(Object.class, ann.cls());
        assertEquals("one", ann.nested().value());

        assertEquals(2, ann.boolArray().length);
        assertEquals(true, ann.boolArray()[0]);
        assertEquals(false, ann.boolArray()[1]);
        assertEquals(2, ann.bArray().length);
        assertEquals((byte) 7, ann.bArray()[0]);
        assertEquals((byte) 8, ann.bArray()[1]);
        assertEquals(2, ann.sArray().length);
        assertEquals((short) 9, ann.sArray()[0]);
        assertEquals((short) 10, ann.sArray()[1]);
        assertEquals(2, ann.iArray().length);
        assertEquals(11, ann.iArray()[0]);
        assertEquals(12, ann.iArray()[1]);
        assertEquals(2, ann.lArray().length);
        assertEquals(13L, ann.lArray()[0]);
        assertEquals(14L, ann.lArray()[1]);
        assertEquals(2, ann.fArray().length);
        assertEquals(15.0F, ann.fArray()[0]);
        assertEquals(16.0F, ann.fArray()[1]);
        assertEquals(2, ann.dArray().length);
        assertEquals(17.0, ann.dArray()[0]);
        assertEquals(18.0, ann.dArray()[1]);
        assertEquals(2, ann.chArray().length);
        assertEquals('d', ann.chArray()[0]);
        assertEquals('e', ann.chArray()[1]);
        assertEquals(2, ann.strArray().length);
        assertEquals("fg", ann.strArray()[0]);
        assertEquals("hi", ann.strArray()[1]);
        assertEquals(2, ann.enArray().length);
        assertEquals(SimpleEnum.BAR, ann.enArray()[0]);
        assertEquals(SimpleEnum.BAZ, ann.enArray()[1]);
        assertEquals(2, ann.clsArray().length);
        assertEquals(String.class, ann.clsArray()[0]);
        assertEquals(Number.class, ann.clsArray()[1]);
        assertEquals(2, ann.nestedArray().length);
        assertEquals("two", ann.nestedArray()[0].value());
        assertEquals("three", ann.nestedArray()[1].value());
    }

    private static AnnotationInstance complexAnnotation() {
        return AnnotationInstance.create(DotName.createSimple(ComplexAnnotation.class.getName()), null, List.of(
                AnnotationValue.createBooleanValue("bool", true),
                AnnotationValue.createByteValue("b", (byte) 1),
                AnnotationValue.createShortValue("s", (short) 2),
                AnnotationValue.createIntegerValue("i", 3),
                AnnotationValue.createLongValue("l", 4L),
                AnnotationValue.createFloatValue("f", 5.0F),
                AnnotationValue.createDoubleValue("d", 6.0),
                AnnotationValue.createCharacterValue("ch", 'a'),
                AnnotationValue.createStringValue("str", "bc"),
                AnnotationValue.createEnumValue("en", DotName.createSimple(SimpleEnum.class.getName()), "FOO"),
                AnnotationValue.createClassValue("cls", Type.create(DotName.createSimple("java.lang.Object"), Type.Kind.CLASS)),
                AnnotationValue.createNestedAnnotationValue("nested", simpleAnnotation("one")),

                AnnotationValue.createArrayValue("boolArray", new AnnotationValue[] {
                        AnnotationValue.createBooleanValue("", true),
                        AnnotationValue.createBooleanValue("", false),
                }),
                AnnotationValue.createArrayValue("bArray", new AnnotationValue[] {
                        AnnotationValue.createByteValue("", (byte) 7),
                        AnnotationValue.createByteValue("", (byte) 8),
                }),
                AnnotationValue.createArrayValue("sArray", new AnnotationValue[] {
                        AnnotationValue.createShortValue("", (short) 9),
                        AnnotationValue.createShortValue("", (short) 10),
                }),
                AnnotationValue.createArrayValue("iArray", new AnnotationValue[] {
                        AnnotationValue.createIntegerValue("", 11),
                        AnnotationValue.createIntegerValue("", 12),
                }),
                AnnotationValue.createArrayValue("lArray", new AnnotationValue[] {
                        AnnotationValue.createLongValue("", 13L),
                        AnnotationValue.createLongValue("", 14L),
                }),
                AnnotationValue.createArrayValue("fArray", new AnnotationValue[] {
                        AnnotationValue.createFloatValue("", 15.0F),
                        AnnotationValue.createFloatValue("", 16.0F),
                }),
                AnnotationValue.createArrayValue("dArray", new AnnotationValue[] {
                        AnnotationValue.createDoubleValue("", 17.0),
                        AnnotationValue.createDoubleValue("", 18.0),
                }),
                AnnotationValue.createArrayValue("chArray", new AnnotationValue[] {
                        AnnotationValue.createCharacterValue("", 'd'),
                        AnnotationValue.createCharacterValue("", 'e'),
                }),
                AnnotationValue.createArrayValue("strArray", new AnnotationValue[] {
                        AnnotationValue.createStringValue("", "fg"),
                        AnnotationValue.createStringValue("", "hi"),
                }),
                AnnotationValue.createArrayValue("enArray", new AnnotationValue[] {
                        AnnotationValue.createEnumValue("", DotName.createSimple(SimpleEnum.class.getName()), "BAR"),
                        AnnotationValue.createEnumValue("", DotName.createSimple(SimpleEnum.class.getName()), "BAZ"),
                }),
                AnnotationValue.createArrayValue("clsArray", new AnnotationValue[] {
                        AnnotationValue.createClassValue("",
                                Type.create(DotName.createSimple("java.lang.String"), Type.Kind.CLASS)),
                        AnnotationValue.createClassValue("",
                                Type.create(DotName.createSimple("java.lang.Number"), Type.Kind.CLASS)),
                }),
                AnnotationValue.createArrayValue("nestedArray", new AnnotationValue[] {
                        AnnotationValue.createNestedAnnotationValue("", simpleAnnotation("two")),
                        AnnotationValue.createNestedAnnotationValue("", simpleAnnotation("three")),
                })));
    }

    private static AnnotationInstance simpleAnnotation(String value) {
        return AnnotationInstance.create(DotName.createSimple(SimpleAnnotation.class.getName()), null,
                List.of(AnnotationValue.createStringValue("value", value)));
    }
}
