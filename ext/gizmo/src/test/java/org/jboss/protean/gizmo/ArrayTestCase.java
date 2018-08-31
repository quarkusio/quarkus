package org.jboss.protean.gizmo;

import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

public class ArrayTestCase {

    @Test
    public void testNewArray() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(Supplier.class).build()) {
            MethodCreator method = creator.getMethodCreator("get", Object.class);
            ResultHandle arrayHandle = method.newArray(String.class, method.load(10));
            method.returnValue(arrayHandle);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Supplier myInterface = (Supplier) clazz.newInstance();
        Object o = myInterface.get();
        Assert.assertEquals(String[].class, o.getClass());
        String[] res = (String[]) o;
        Assert.assertEquals(10, res.length);

    }

    @Test
    public void testWriteArray() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(Supplier.class).build()) {
            MethodCreator method = creator.getMethodCreator("get", Object.class);
            ResultHandle arrayHandle = method.newArray(String.class, method.load(1));
            method.writeArrayValue(arrayHandle, method.load(0), method.load("hello"));
            method.returnValue(arrayHandle);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Supplier myInterface = (Supplier) clazz.newInstance();
        Object o = myInterface.get();
        Assert.assertEquals(String[].class, o.getClass());
        String[] res = (String[]) o;
        Assert.assertEquals(1, res.length);
        Assert.assertEquals("hello", res[0]);

    }
}
