package org.jboss.protean.gizmo;

import org.junit.Assert;
import org.junit.Test;

public class ExceptionThrowingTestCase {


    @Test
    public void testThrowException() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, "java.lang.String");
            method.throwException(IllegalStateException.class, "ERROR");
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        try {
            myInterface.transform("ignored");
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("ERROR", e.getMessage());
        }
    }

}
