package org.jboss.protean.gizmo;

import static org.junit.Assert.assertNull;

import java.util.function.Supplier;

import org.junit.Test;

public class LoadNullTestCase {

    @Test
    public void testLoadNull() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(Supplier.class).build()) {
            MethodCreator method = creator.getMethodCreator("get", Object.class);
            method.returnValue(method.loadNull());
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Supplier myInterface = (Supplier) clazz.newInstance();
        assertNull(myInterface.get());
    }

}
