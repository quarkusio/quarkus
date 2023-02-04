package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;

import jakarta.enterprise.util.AnnotationLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.AbstractAnnotationLiteral;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;

public class AnnotationLiteralProcessorTest {
    public enum SimpleEnum {
        FOO,
        BAR,
        BAZ,
    }

    @Retention(RetentionPolicy.CLASS)
    public @interface ClassRetainedAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimpleAnnotation {
        String value();

        class Literal extends AnnotationLiteral<SimpleAnnotation> implements SimpleAnnotation {
            private final String value;

            public Literal(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
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

        class Literal extends AnnotationLiteral<ComplexAnnotation> implements ComplexAnnotation {
            private final boolean bool;
            private final byte b;
            private final short s;
            private final int i;
            private final long l;
            private final float f;
            private final double d;
            private final char ch;
            private final String str;
            private final SimpleEnum en;
            private final Class<?> cls;
            private final SimpleAnnotation nested;

            private final boolean[] boolArray;
            private final byte[] bArray;
            private final short[] sArray;
            private final int[] iArray;
            private final long[] lArray;
            private final float[] fArray;
            private final double[] dArray;
            private final char[] chArray;
            private final String[] strArray;
            private final SimpleEnum[] enArray;
            private final Class<?>[] clsArray;
            private final SimpleAnnotation[] nestedArray;

            public Literal(boolean bool, byte b, short s, int i, long l, float f, double d, char ch, String str, SimpleEnum en,
                    Class<?> cls, SimpleAnnotation nested, boolean[] boolArray, byte[] bArray, short[] sArray, int[] iArray,
                    long[] lArray, float[] fArray, double[] dArray, char[] chArray, String[] strArray, SimpleEnum[] enArray,
                    Class<?>[] clsArray, SimpleAnnotation[] nestedArray) {
                this.bool = bool;
                this.b = b;
                this.s = s;
                this.i = i;
                this.l = l;
                this.f = f;
                this.d = d;
                this.ch = ch;
                this.str = str;
                this.en = en;
                this.cls = cls;
                this.nested = nested;
                this.boolArray = boolArray;
                this.bArray = bArray;
                this.sArray = sArray;
                this.iArray = iArray;
                this.lArray = lArray;
                this.fArray = fArray;
                this.dArray = dArray;
                this.chArray = chArray;
                this.strArray = strArray;
                this.enArray = enArray;
                this.clsArray = clsArray;
                this.nestedArray = nestedArray;
            }

            @Override
            public boolean bool() {
                return bool;
            }

            @Override
            public byte b() {
                return b;
            }

            @Override
            public short s() {
                return s;
            }

            @Override
            public int i() {
                return i;
            }

            @Override
            public long l() {
                return l;
            }

            @Override
            public float f() {
                return f;
            }

            @Override
            public double d() {
                return d;
            }

            @Override
            public char ch() {
                return ch;
            }

            @Override
            public String str() {
                return str;
            }

            @Override
            public SimpleEnum en() {
                return en;
            }

            @Override
            public Class<?> cls() {
                return cls;
            }

            @Override
            public SimpleAnnotation nested() {
                return nested;
            }

            @Override
            public boolean[] boolArray() {
                return boolArray;
            }

            @Override
            public byte[] bArray() {
                return bArray;
            }

            @Override
            public short[] sArray() {
                return sArray;
            }

            @Override
            public int[] iArray() {
                return iArray;
            }

            @Override
            public long[] lArray() {
                return lArray;
            }

            @Override
            public float[] fArray() {
                return fArray;
            }

            @Override
            public double[] dArray() {
                return dArray;
            }

            @Override
            public char[] chArray() {
                return chArray;
            }

            @Override
            public String[] strArray() {
                return strArray;
            }

            @Override
            public SimpleEnum[] enArray() {
                return enArray;
            }

            @Override
            public Class<?>[] clsArray() {
                return clsArray;
            }

            @Override
            public SimpleAnnotation[] nestedArray() {
                return nestedArray;
            }
        }
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
                    index.getClassByName(DotName.createSimple(ComplexAnnotation.class.getName())), complexAnnotationJandex());
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

        assertTrue(annotation instanceof AbstractAnnotationLiteral);
        AbstractAnnotationLiteral annotationLiteral = (AbstractAnnotationLiteral) annotation;
        assertEquals(annotation.annotationType(), annotationLiteral.annotationType());

        // verify both ways, to ensure our generated classes interop correctly with `AnnotationLiteral`
        assertEquals(complexAnnotationRuntime(), annotation);
        assertEquals(annotation, complexAnnotationRuntime());

        assertEquals(complexAnnotationRuntime().hashCode(), annotation.hashCode());

        assertEquals(
                "@io.quarkus.arc.processor.AnnotationLiteralProcessorTest$ComplexAnnotation(bool=true, b=1, s=2, i=3, l=4, f=5.0, d=6.0, ch=a, str=bc, en=FOO, cls=class java.lang.Object, nested=@io.quarkus.arc.processor.AnnotationLiteralProcessorTest$SimpleAnnotation(value=one), boolArray=[true, false], bArray=[7, 8], sArray=[9, 10], iArray=[11, 12], lArray=[13, 14], fArray=[15.0, 16.0], dArray=[17.0, 18.0], chArray=[d, e], strArray=[fg, hi], enArray=[BAR, BAZ], clsArray=[class java.lang.String, class java.lang.Number], nestedArray=[@io.quarkus.arc.processor.AnnotationLiteralProcessorTest$SimpleAnnotation(value=two), @io.quarkus.arc.processor.AnnotationLiteralProcessorTest$SimpleAnnotation(value=three)])",
                annotation.toString());
    }

    @Test
    public void missingAnnotationClass() {
        AnnotationLiteralProcessor literals = new AnnotationLiteralProcessor(index, ignored -> true);

        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className(generatedClass).build()) {
            MethodCreator method = creator.getMethodCreator("hello", void.class);

            assertThrows(NullPointerException.class, () -> {
                literals.create(method, null, simpleAnnotationJandex("foobar"));
            });
        }
    }

    @Test
    public void classRetainedAnnotation() {
        AnnotationLiteralProcessor literals = new AnnotationLiteralProcessor(index, ignored -> true);

        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className(generatedClass).build()) {
            MethodCreator method = creator.getMethodCreator("hello", void.class);

            assertThrows(IllegalArgumentException.class, () -> {
                literals.create(method, Index.singleClass(ClassRetainedAnnotation.class),
                        AnnotationInstance.builder(ClassRetainedAnnotation.class).build());
            });
        }
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

    private static AnnotationInstance complexAnnotationJandex() {
        return AnnotationInstance.builder(ComplexAnnotation.class)
                .add("bool", true)
                .add("b", (byte) 1)
                .add("s", (short) 2)
                .add("i", 3)
                .add("l", 4L)
                .add("f", 5.0F)
                .add("d", 6.0)
                .add("ch", 'a')
                .add("str", "bc")
                .add("en", SimpleEnum.FOO)
                .add("cls", Object.class)
                .add("nested", simpleAnnotationJandex("one"))
                .add("boolArray", new boolean[] { true, false })
                .add("bArray", new byte[] { (byte) 7, (byte) 8 })
                .add("sArray", new short[] { (short) 9, (short) 10 })
                .add("iArray", new int[] { 11, 12 })
                .add("lArray", new long[] { 13L, 14L })
                .add("fArray", new float[] { 15.0F, 16.0F })
                .add("dArray", new double[] { 17.0, 18.0 })
                .add("chArray", new char[] { 'd', 'e' })
                .add("strArray", new String[] { "fg", "hi" })
                .add("enArray", new SimpleEnum[] { SimpleEnum.BAR, SimpleEnum.BAZ })
                .add("clsArray", new Class[] { String.class, Number.class })
                .add("nestedArray", new AnnotationInstance[] { simpleAnnotationJandex("two"), simpleAnnotationJandex("three") })
                .build();
    }

    private static AnnotationInstance simpleAnnotationJandex(String value) {
        return AnnotationInstance.builder(SimpleAnnotation.class)
                .add("value", value)
                .build();
    }

    private static ComplexAnnotation complexAnnotationRuntime() {
        return new ComplexAnnotation.Literal(
                true,
                (byte) 1,
                (short) 2,
                3,
                4L,
                5.0F,
                6.0,
                'a',
                "bc",
                SimpleEnum.FOO,
                Object.class,
                simpleAnnotationRuntime("one"),
                new boolean[] { true, false },
                new byte[] { (byte) 7, (byte) 8 },
                new short[] { (short) 9, (short) 10 },
                new int[] { 11, 12 },
                new long[] { 13L, 14L },
                new float[] { 15.0F, 16.0F },
                new double[] { 17.0, 18.0 },
                new char[] { 'd', 'e' },
                new String[] { "fg", "hi" },
                new SimpleEnum[] { SimpleEnum.BAR, SimpleEnum.BAZ },
                new Class[] { String.class, Number.class },
                new SimpleAnnotation[] { simpleAnnotationRuntime("two"), simpleAnnotationRuntime("three") });
    }

    private static SimpleAnnotation simpleAnnotationRuntime(String value) {
        return new SimpleAnnotation.Literal(value);
    }
}
