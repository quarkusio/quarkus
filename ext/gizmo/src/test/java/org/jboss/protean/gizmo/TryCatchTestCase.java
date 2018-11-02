package org.jboss.protean.gizmo;

import org.junit.Assert;
import org.junit.Test;

public class TryCatchTestCase {

    @Test
    public void testTryCatch() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);
            TryBlock tb = method.tryBlock();
            BytecodeCreator illegalState = tb.addCatch(IllegalStateException.class);
            illegalState.returnValue(illegalState.load("caught-exception"));
            tb.invokeStaticMethod(MethodDescriptor.ofMethod(ExceptionClass.class.getName(), "throwIllegalState", "V"));
            tb.returnValue(method.load("complete"));
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        Assert.assertEquals("caught-exception", myInterface.transform("ignored"));
    }

    @Test
    public void testTryCatchEmptyCatchBlock() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);
            ResultHandle existingVal = method.load("complete");
            TryBlock tb = method.tryBlock();
            tb.addCatch(IllegalStateException.class).load(34);
            tb.invokeStaticMethod(MethodDescriptor.ofMethod(ExceptionClass.class.getName(), "throwIllegalState", "V"));
            method.returnValue(existingVal);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        Assert.assertEquals("complete", myInterface.transform("ignored"));
    }
}
