package org.jboss.protean.gizmo;

import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

public class LoadClassTestCase {

    @Test
    public void testLoadClass() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = new ClassCreator(cl, "com.MyTest", Object.class, Supplier.class)) {
            MethodCreator method = creator.getMethodCreator("get", Object.class);
            ResultHandle stringHandle = method.loadClass(String.class);
            method.returnValue(stringHandle);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Supplier myInterface = (Supplier) clazz.newInstance();
        Assert.assertEquals(String.class, myInterface.get());
    }

}
