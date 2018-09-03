package org.jboss.protean.gizmo;

import org.junit.Assert;
import org.junit.Test;

public class SampleTest {

    @Test
    public void testSimpleGetMessage() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);
            ResultHandle message = method.invokeStaticMethod(MethodDescriptor.ofMethod(MessageClass.class.getName(), "getMessage", "Ljava/lang/String;"));
            method.returnValue(message);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        Assert.assertEquals("MESSAGE", myInterface.transform("ignored"));
    }

    @Test
    public void testStringTransform() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);
            ResultHandle message = method.invokeStaticMethod(MethodDescriptor.ofMethod(MessageClass.class.getName(), "getMessage", "Ljava/lang/String;"));
            ResultHandle constant = method.load(":CONST:");
            message = method.invokeVirtualMethod(MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"), message, constant);
            message = method.invokeVirtualMethod(MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"), message, method.getMethodParam(0));


            method.returnValue(message);
        }
        MyInterface myInterface = (MyInterface) cl.loadClass("com.MyTest").newInstance();
        Assert.assertEquals("MESSAGE:CONST:PARAM", myInterface.transform("PARAM"));
    }


    @Test
    public void testIfStatement() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);
            ResultHandle equalsResult = method.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "equals", boolean.class, Object.class), method.getMethodParam(0), method.load("TEST"));
            BranchResult branch = method.ifNonZero(equalsResult);
            branch.trueBranch().returnValue(branch.trueBranch().load("TRUE BRANCH"));
            branch.falseBranch().returnValue(method.getMethodParam(0));

        }
        MyInterface myInterface = (MyInterface) cl.loadClass("com.MyTest").newInstance();
        Assert.assertEquals("PARAM", myInterface.transform("PARAM"));
        Assert.assertEquals("TRUE BRANCH", myInterface.transform("TEST"));
    }
}
