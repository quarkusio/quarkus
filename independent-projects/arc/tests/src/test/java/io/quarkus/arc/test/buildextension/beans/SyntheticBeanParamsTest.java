package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class SyntheticBeanParamsTest {
    public enum SimpleEnum {
        FOO,
        BAR,
        BAZ,
    }

    public enum AnotherEnum {
        INSTANCE
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimpleAnnotation {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnotherAnnotation {
        int value();
    }

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .additionalClasses(SimpleEnum.class, AnotherEnum.class, SimpleAnnotation.class, AnotherAnnotation.class,
                    Verification.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    ClassInfo enumClass = context.get(Key.INDEX).getClassByName(
                            DotName.createSimple(SimpleEnum.class.getName()));
                    ClassInfo annClass = context.get(Key.INDEX).getClassByName(
                            DotName.createSimple(SimpleAnnotation.class.getName()));

                    context.configure(Verification.class)
                            .addType(Verification.class)
                            .param("bool", true)
                            .param("b", (byte) 1)
                            .param("s", (short) 2)
                            .param("i", 3)
                            .param("l", 4L)
                            .param("f", 5.0F)
                            .param("d", 6.0)
                            .param("ch", 'a')
                            .param("str", "bc")
                            .param("en", SimpleEnum.FOO)
                            .param("cls", Object.class)
                            .param("clsJandex", enumClass)
                            .param("ann", simpleAnnotation("one"))
                            .param("boolArray", new boolean[] { true, false })
                            .param("bArray", new byte[] { (byte) 7, (byte) 8 })
                            .param("sArray", new short[] { (short) 9, (short) 10 })
                            .param("iArray", new int[] { 11, 12 })
                            .param("lArray", new long[] { 13L, 14L })
                            .param("fArray", new float[] { 15.0F, 16.0F })
                            .param("dArray", new double[] { 17.0, 18.0 })
                            .param("chArray", new char[] { 'd', 'e' })
                            .param("strArray", new String[] { "fg", "hi" })
                            .param("enArray", new SimpleEnum[] { SimpleEnum.BAR, SimpleEnum.BAZ })
                            .param("enMixedArray", new Enum[] { SimpleEnum.FOO, AnotherEnum.INSTANCE })
                            .param("clsArray", new Class<?>[] { String.class, Number.class })
                            .param("clsJandexArray", new ClassInfo[] { enumClass, annClass })
                            .param("annArray", new AnnotationInstance[] { simpleAnnotation("two"),
                                    simpleAnnotation("three") })
                            .param("annMixedArray", new AnnotationInstance[] { simpleAnnotation("four"),
                                    anotherAnnotation(42) })
                            .creator(mc -> {
                                ResultHandle params = mc.readInstanceField(
                                        FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                                        mc.getThis());
                                mc.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(Verification.class, "invoke", void.class, Map.class),
                                        params);
                                ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(Verification.class));
                                mc.returnValue(instance);
                            })
                            .done();
                }
            })
            .build();

    @Test
    public void test() {
        Verification verification = Arc.container().select(Verification.class).get();
        assertNotNull(verification);
        assertTrue(Verification.invoked);
    }

    static class Verification {
        static boolean invoked = false;

        static void invoke(Map<String, Object> params) {
            assertEquals(true, params.get("bool"));
            assertEquals((byte) 1, (byte) params.get("b"));
            assertEquals((short) 2, (short) params.get("s"));
            assertEquals(3, (int) params.get("i"));
            assertEquals(4L, (long) params.get("l"));
            assertEquals(5.0F, (float) params.get("f"));
            assertEquals(6.0, (double) params.get("d"));
            assertEquals('a', (char) params.get("ch"));
            assertEquals("bc", params.get("str"));
            assertEquals(SimpleEnum.FOO, params.get("en"));
            assertEquals(Object.class, params.get("cls"));
            assertEquals(SimpleEnum.class, params.get("clsJandex"));
            assertEquals("one", ((SimpleAnnotation) params.get("ann")).value());

            assertEquals(2, ((boolean[]) params.get("boolArray")).length);
            assertEquals(true, ((boolean[]) params.get("boolArray"))[0]);
            assertEquals(false, ((boolean[]) params.get("boolArray"))[1]);
            assertEquals(2, ((byte[]) params.get("bArray")).length);
            assertEquals((byte) 7, ((byte[]) params.get("bArray"))[0]);
            assertEquals((byte) 8, ((byte[]) params.get("bArray"))[1]);
            assertEquals(2, ((short[]) params.get("sArray")).length);
            assertEquals((short) 9, ((short[]) params.get("sArray"))[0]);
            assertEquals((short) 10, ((short[]) params.get("sArray"))[1]);
            assertEquals(2, ((int[]) params.get("iArray")).length);
            assertEquals(11, ((int[]) params.get("iArray"))[0]);
            assertEquals(12, ((int[]) params.get("iArray"))[1]);
            assertEquals(2, ((long[]) params.get("lArray")).length);
            assertEquals(13L, ((long[]) params.get("lArray"))[0]);
            assertEquals(14L, ((long[]) params.get("lArray"))[1]);
            assertEquals(2, ((float[]) params.get("fArray")).length);
            assertEquals(15.0F, ((float[]) params.get("fArray"))[0]);
            assertEquals(16.0F, ((float[]) params.get("fArray"))[1]);
            assertEquals(2, ((double[]) params.get("dArray")).length);
            assertEquals(17.0, ((double[]) params.get("dArray"))[0]);
            assertEquals(18.0, ((double[]) params.get("dArray"))[1]);
            assertEquals(2, ((char[]) params.get("chArray")).length);
            assertEquals('d', ((char[]) params.get("chArray"))[0]);
            assertEquals('e', ((char[]) params.get("chArray"))[1]);
            assertEquals(2, ((String[]) params.get("strArray")).length);
            assertEquals("fg", ((String[]) params.get("strArray"))[0]);
            assertEquals("hi", ((String[]) params.get("strArray"))[1]);
            assertEquals(2, ((SimpleEnum[]) params.get("enArray")).length);
            assertEquals(SimpleEnum.BAR, ((SimpleEnum[]) params.get("enArray"))[0]);
            assertEquals(SimpleEnum.BAZ, ((SimpleEnum[]) params.get("enArray"))[1]);
            assertEquals(2, ((Enum<?>[]) params.get("enMixedArray")).length);
            assertEquals(SimpleEnum.FOO, ((Enum<?>[]) params.get("enMixedArray"))[0]);
            assertEquals(AnotherEnum.INSTANCE, ((Enum<?>[]) params.get("enMixedArray"))[1]);
            assertEquals(2, ((Class<?>[]) params.get("clsArray")).length);
            assertEquals(String.class, ((Class<?>[]) params.get("clsArray"))[0]);
            assertEquals(Number.class, ((Class<?>[]) params.get("clsArray"))[1]);
            assertEquals(2, ((Class<?>[]) params.get("clsJandexArray")).length);
            assertEquals(SimpleEnum.class, ((Class<?>[]) params.get("clsJandexArray"))[0]);
            assertEquals(SimpleAnnotation.class, ((Class<?>[]) params.get("clsJandexArray"))[1]);
            assertEquals(2, ((SimpleAnnotation[]) params.get("annArray")).length);
            assertEquals("two", ((SimpleAnnotation[]) params.get("annArray"))[0].value());
            assertEquals("three", ((SimpleAnnotation[]) params.get("annArray"))[1].value());
            assertEquals(2, ((Annotation[]) params.get("annMixedArray")).length);
            assertEquals("four", ((SimpleAnnotation) ((Annotation[]) params.get("annMixedArray"))[0]).value());
            assertEquals(42, ((AnotherAnnotation) ((Annotation[]) params.get("annMixedArray"))[1]).value());

            invoked = true;
        }
    }

    private static AnnotationInstance simpleAnnotation(String value) {
        return AnnotationInstance.create(DotName.createSimple(SimpleAnnotation.class.getName()), null,
                List.of(AnnotationValue.createStringValue("value", value)));
    }

    private static AnnotationInstance anotherAnnotation(int value) {
        return AnnotationInstance.create(DotName.createSimple(AnotherAnnotation.class.getName()), null,
                List.of(AnnotationValue.createIntegerValue("value", value)));
    }
}
