package io.quarkus.arc.test.invoker.basic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class InvokerAssignabilityTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                for (MethodInfo method : bean.getImplClazz().methods()) {
                    if (method.isConstructor()) {
                        continue;
                    }
                    invokers.put(method.name(), factory.createInvoker(bean, method).build());
                }
            }))
            .build();

    @Test
    public void testNonNull() {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        MyService service = Arc.container().instance(MyService.class).get();

        assertOK(helper, service, "helloBoolean", array(true));
        assertOK(helper, service, "helloByte", array((byte) 1));
        assertOK(helper, service, "helloShort", array((short) 1));
        assertOK(helper, service, "helloInt", array(1));
        assertOK(helper, service, "helloLong", array(1L));
        assertOK(helper, service, "helloFloat", array(1.0F));
        assertOK(helper, service, "helloDouble", array(1.0));
        assertOK(helper, service, "helloChar", array('a'));

        assertOK(helper, service, "helloShort", array((byte) 1));
        assertOK(helper, service, "helloInt", array((byte) 1));
        assertOK(helper, service, "helloInt", array((short) 1));
        assertOK(helper, service, "helloLong", array((byte) 1));
        assertOK(helper, service, "helloLong", array((short) 1));
        assertOK(helper, service, "helloLong", array(1));
        assertOK(helper, service, "helloFloat", array(1));
        assertOK(helper, service, "helloFloat", array(1L));
        assertOK(helper, service, "helloDouble", array(1));
        assertOK(helper, service, "helloDouble", array(1L));
        assertOK(helper, service, "helloDouble", array(1.0F));

        assertOK(helper, service, "helloBooleanWrapper", array(true));
        assertOK(helper, service, "helloByteWrapper", array((byte) 1));
        assertOK(helper, service, "helloShortWrapper", array((short) 1));
        assertOK(helper, service, "helloIntWrapper", array(1));
        assertOK(helper, service, "helloLongWrapper", array(1L));
        assertOK(helper, service, "helloFloatWrapper", array(1.0F));
        assertOK(helper, service, "helloDoubleWrapper", array(1.0));
        assertOK(helper, service, "helloCharWrapper", array('a'));
        assertOK(helper, service, "helloString", array(""));
        assertOK(helper, service, "helloCollection", array(List.of()));
        assertOK(helper, service, "helloCollection", array(Set.of()));
        assertOK(helper, service, "helloSerializable", array(1)); // Number is Serializable
        assertOK(helper, service, "helloSerializable", array(new Object[] {}));
        assertOK(helper, service, "helloSerializable", array(new String[] {}));
        assertOK(helper, service, "helloObject", array(""));
        assertOK(helper, service, "helloObject", array(List.of()));
        assertOK(helper, service, "helloObject", array(new Object()));
        assertOK(helper, service, "helloObject", array(new int[] {}));
        assertOK(helper, service, "helloObject", array(new String[] {}));
        assertOK(helper, service, "helloObject", array(new List<?>[] {}));
        assertOK(helper, service, "helloObject", array(new Object[] {}));

        assertOK(helper, service, "helloBooleanArray", array(new boolean[] { true }));
        assertOK(helper, service, "helloByteArray", array(new byte[] { (byte) 1 }));
        assertOK(helper, service, "helloShortArray", array(new short[] { (short) 1 }));
        assertOK(helper, service, "helloIntArray", array(new int[] { 1 }));
        assertOK(helper, service, "helloLongArray", array(new long[] { 1L }));
        assertOK(helper, service, "helloFloatArray", array(new float[] { 1.0F }));
        assertOK(helper, service, "helloDoubleArray", array(new double[] { 1.0 }));
        assertOK(helper, service, "helloCharArray", array(new char[] { 'a' }));
        assertOK(helper, service, "helloStringArray", array(new String[] {}));
        assertOK(helper, service, "helloObjectArray", array(new String[] {}));
        assertOK(helper, service, "helloObjectArray", array(new Object[] {}));

        assertOK(helper, service, "helloCollectionArrayArray", array(new List<?>[][] {}));
        assertOK(helper, service, "helloCollectionArrayArray", array(new Set<?>[][] {}));
        assertOK(helper, service, "helloObjectArrayArray", array(new List<?>[][] {}));
        assertOK(helper, service, "helloObjectArrayArray", array(new Set<?>[][] {}));
        assertOK(helper, service, "helloObjectArrayArray", array(new String[][] {}));
        assertOK(helper, service, "helloObjectArrayArray", array(new Object[][] {}));
        assertOK(helper, service, "helloObjectArrayArray", array(new Object[][][] {}));

        assertFail(helper, service, "helloBoolean", array(1));
        assertFail(helper, service, "helloByte", array(1));
        assertFail(helper, service, "helloShort", array(1));
        assertFail(helper, service, "helloInt", array(1L));
        assertFail(helper, service, "helloInt", array(1.0));
        assertFail(helper, service, "helloLong", array(1.0F));
        assertFail(helper, service, "helloLong", array(1.0));
        assertFail(helper, service, "helloFloat", array(1.0));
        assertFail(helper, service, "helloChar", array(false));
        assertFail(helper, service, "helloChar", array((byte) 1));
        assertFail(helper, service, "helloChar", array((short) 1));
        assertFail(helper, service, "helloChar", array(1));
        assertFail(helper, service, "helloChar", array(1L));
        assertFail(helper, service, "helloChar", array(1.0F));
        assertFail(helper, service, "helloChar", array(1.0));
        assertFail(helper, service, "helloLong", array(new BigInteger("1")));
        assertFail(helper, service, "helloDouble", array(new BigInteger("1")));
        assertFail(helper, service, "helloDouble", array(new BigDecimal("1.0")));

        assertFail(helper, service, "helloBooleanWrapper", array(1));
        assertFail(helper, service, "helloByteWrapper", array(1));
        assertFail(helper, service, "helloShortWrapper", array((byte) 1));
        assertFail(helper, service, "helloShortWrapper", array(1));
        assertFail(helper, service, "helloIntWrapper", array((short) 1));
        assertFail(helper, service, "helloIntWrapper", array(1L));
        assertFail(helper, service, "helloLongWrapper", array(1));
        assertFail(helper, service, "helloLongWrapper", array(1.0));
        assertFail(helper, service, "helloFloatWrapper", array(1));
        assertFail(helper, service, "helloFloatWrapper", array(1L));
        assertFail(helper, service, "helloFloatWrapper", array(1.0));
        assertFail(helper, service, "helloDoubleWrapper", array(1));
        assertFail(helper, service, "helloDoubleWrapper", array(1L));
        assertFail(helper, service, "helloDoubleWrapper", array(1.0F));
        assertFail(helper, service, "helloCharWrapper", array((byte) 1));
        assertFail(helper, service, "helloCharWrapper", array(1));
        assertFail(helper, service, "helloCharWrapper", array(1L));
        assertFail(helper, service, "helloCharWrapper", array(1.0F));
        assertFail(helper, service, "helloCharWrapper", array(1.0));
        assertFail(helper, service, "helloLongWrapper", array(new BigInteger("1")));
        assertFail(helper, service, "helloDoubleWrapper", array(new BigInteger("1")));
        assertFail(helper, service, "helloDoubleWrapper", array(new BigDecimal("1.0")));
        assertFail(helper, service, "helloString", array(new Object()));
        assertFail(helper, service, "helloCollection", array(1));
        assertFail(helper, service, "helloCollection", array(new Object()));
        assertFail(helper, service, "helloSerializable", array(new Object()));

        assertFail(helper, service, "helloBooleanArray", array(new int[] { 1 }));
        assertFail(helper, service, "helloByteArray", array(new int[] { 1 }));
        assertFail(helper, service, "helloShortArray", array(new int[] { 1 }));
        assertFail(helper, service, "helloIntArray", array(new long[] { 1L }));
        assertFail(helper, service, "helloLongArray", array(new int[] { 1 }));
        assertFail(helper, service, "helloFloatArray", array(new double[] { 1.0 }));
        assertFail(helper, service, "helloDoubleArray", array(new float[] { 1.0F }));
        assertFail(helper, service, "helloCharArray", array(new int[] { 1 }));
        assertFail(helper, service, "helloStringArray", array(""));
        assertFail(helper, service, "helloStringArray", array(new Object()));
        assertFail(helper, service, "helloStringArray", array(new String[][] {}));
        assertFail(helper, service, "helloStringArray", array(new Object[] {}));
        assertFail(helper, service, "helloObjectArray", array(""));
        assertFail(helper, service, "helloObjectArray", array(new Object()));

        assertFail(helper, service, "helloCollectionArrayArray", array(new List<?>[] {}));
        assertFail(helper, service, "helloCollectionArrayArray", array(new Set<?>[] {}));
        assertFail(helper, service, "helloCollectionArrayArray", array(new List<?>[][][] {}));
        assertFail(helper, service, "helloCollectionArrayArray", array(new Set<?>[][][] {}));
        assertFail(helper, service, "helloCollectionArrayArray", array(new Object[][] {}));
        assertFail(helper, service, "helloObjectArrayArray", array(new Object[] {}));
        assertFail(helper, service, "helloObjectArrayArray", array(new Object()));
    }

    @Test
    public void testNull() {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        MyService service = Arc.container().instance(MyService.class).get();

        assertOK(helper, service, "helloBooleanWrapper", array(null));
        assertOK(helper, service, "helloByteWrapper", array(null));
        assertOK(helper, service, "helloShortWrapper", array(null));
        assertOK(helper, service, "helloIntWrapper", array(null));
        assertOK(helper, service, "helloLongWrapper", array(null));
        assertOK(helper, service, "helloFloatWrapper", array(null));
        assertOK(helper, service, "helloDoubleWrapper", array(null));
        assertOK(helper, service, "helloCharWrapper", array(null));
        assertOK(helper, service, "helloString", array(null));
        assertOK(helper, service, "helloCollection", array(null));
        assertOK(helper, service, "helloSerializable", array(null));
        assertOK(helper, service, "helloObject", array(null));

        assertOK(helper, service, "helloBooleanArray", array(null));
        assertOK(helper, service, "helloByteArray", array(null));
        assertOK(helper, service, "helloShortArray", array(null));
        assertOK(helper, service, "helloIntArray", array(null));
        assertOK(helper, service, "helloLongArray", array(null));
        assertOK(helper, service, "helloFloatArray", array(null));
        assertOK(helper, service, "helloDoubleArray", array(null));
        assertOK(helper, service, "helloCharArray", array(null));
        assertOK(helper, service, "helloStringArray", array(null));
        assertOK(helper, service, "helloObjectArray", array(null));

        assertOK(helper, service, "helloCollectionArrayArray", array(null));
        assertOK(helper, service, "helloObjectArrayArray", array(null));

        assertFail(helper, service, "helloBoolean", array(null));
        assertFail(helper, service, "helloByte", array(null));
        assertFail(helper, service, "helloShort", array(null));
        assertFail(helper, service, "helloInt", array(null));
        assertFail(helper, service, "helloLong", array(null));
        assertFail(helper, service, "helloFloat", array(null));
        assertFail(helper, service, "helloDouble", array(null));
        assertFail(helper, service, "helloChar", array(null));
    }

    // produces a single-element array whose only element is `obj`
    private static Object[] array(Object obj) {
        return new Object[] { obj };
    }

    private static void assertOK(InvokerHelper helper, MyService instance, String methodName, Object... arguments) {
        Invoker<MyService, String> invoker = helper.getInvoker(methodName);
        String result = assertDoesNotThrow(() -> invoker.invoke(instance, arguments));
        assertEquals("OK", result);
    }

    private static void assertFail(InvokerHelper helper, MyService instance, String methodName, Object... arguments) {
        Invoker<MyService, String> invoker = helper.getInvoker(methodName);
        assertThrows(RuntimeException.class, () -> invoker.invoke(instance, arguments));
    }

    @Singleton
    static class MyService {
        public String helloBoolean(boolean b) {
            return "OK";
        }

        public String helloByte(byte b) {
            return "OK";
        }

        public String helloShort(short s) {
            return "OK";
        }

        public String helloInt(int i) {
            return "OK";
        }

        public String helloLong(long l) {
            return "OK";
        }

        public String helloFloat(float f) {
            return "OK";
        }

        public String helloDouble(double d) {
            return "OK";
        }

        public String helloChar(char ch) {
            return "OK";
        }

        public String helloBooleanWrapper(Boolean b) {
            return "OK";
        }

        public String helloByteWrapper(Byte b) {
            return "OK";
        }

        public String helloShortWrapper(Short s) {
            return "OK";
        }

        public String helloIntWrapper(Integer i) {
            return "OK";
        }

        public String helloLongWrapper(Long l) {
            return "OK";
        }

        public String helloFloatWrapper(Float f) {
            return "OK";
        }

        public String helloDoubleWrapper(Double d) {
            return "OK";
        }

        public String helloCharWrapper(Character ch) {
            return "OK";
        }

        public String helloString(String s) {
            return "OK";
        }

        public String helloCollection(Collection<String> c) {
            return "OK";
        }

        public String helloSerializable(Serializable s) {
            return "OK";
        }

        public String helloObject(Object o) {
            return "OK";
        }

        public String helloBooleanArray(boolean[] b) {
            return "OK";
        }

        public String helloByteArray(byte[] b) {
            return "OK";
        }

        public String helloShortArray(short[] s) {
            return "OK";
        }

        public String helloIntArray(int[] i) {
            return "OK";
        }

        public String helloLongArray(long[] l) {
            return "OK";
        }

        public String helloFloatArray(float[] f) {
            return "OK";
        }

        public String helloDoubleArray(double[] d) {
            return "OK";
        }

        public String helloCharArray(char[] ch) {
            return "OK";
        }

        public String helloStringArray(String[] s) {
            return "OK";
        }

        public String helloObjectArray(Object[] o) {
            return "OK";
        }

        public String helloCollectionArrayArray(Collection<?>[][] c) {
            return "OK";
        }

        public String helloObjectArrayArray(Object[][] o) {
            return "OK";
        }
    }
}
