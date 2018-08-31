package org.jboss.protean.gizmo;

import org.junit.Assert;
import org.junit.Test;

public class TryCatchTestCase {

    @Test
    public void testTryCatch() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);
            ExceptionTable table = method.addTryCatch();
            BytecodeCreator illegalState = table.addCatchClause(IllegalStateException.class);
            illegalState.returnValue(illegalState.load("caught-exception"));
            method.invokeStaticMethod(MethodDescriptor.ofMethod(ExceptionClass.class.getName(), "throwIllegalState", "V"));
            method.returnValue(method.load("complete"));
            table.complete();
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
            ExceptionTable table = method.addTryCatch();
            table.addCatchClause(IllegalStateException.class).load(34);
            method.invokeStaticMethod(MethodDescriptor.ofMethod(ExceptionClass.class.getName(), "throwIllegalState", "V"));
            table.complete();
            method.returnValue(existingVal);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        Assert.assertEquals("complete", myInterface.transform("ignored"));
    }
}
