package org.jboss.protean.gizmo;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

public class FunctionTestCase {

    @Test
    public void testSimpleFunction() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);

            //create a function that appends '-func' to its input
            FunctionCreator functionCreator = method.createFunction(Function.class);
            BytecodeCreator fbc = functionCreator.getBytecode();
            ResultHandle functionReturn = fbc.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "concat", String.class, String.class), fbc.getMethodParam(0), fbc.load("-func"));
            fbc.returnValue(functionReturn);

            ResultHandle ret = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(Function.class, "apply", Object.class, Object.class), functionCreator.getInstance(), method.getMethodParam(0));
            method.returnValue(ret);

        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        Assert.assertEquals("input-func", myInterface.transform("input"));
    }

    @Test
    public void testSimpleFunctionWithCapture() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(MyInterface.class).build()) {
            MethodCreator method = creator.getMethodCreator("transform", String.class, String.class);

            //create a function that appends '-func' to its input
            FunctionCreator functionCreator = method.createFunction(Supplier.class);
            BytecodeCreator fbc = functionCreator.getBytecode();
            ResultHandle functionReturn = fbc.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "concat", String.class, String.class), method.getMethodParam(0), fbc.load("-func"));
            fbc.returnValue(functionReturn);

            ResultHandle ret = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(Supplier.class, "get", Object.class), functionCreator.getInstance());
            method.returnValue(ret);

        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        MyInterface myInterface = (MyInterface) clazz.newInstance();
        Assert.assertEquals("input-func", myInterface.transform("input"));
    }

    @Test
    public void testInvokeSuperMethodFromFunction() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").superClass(Superclass.class).build()) {
            MethodCreator method = creator.getMethodCreator("getMessage", String.class);

            //create a function that calls super appends '-func' to its input
            FunctionCreator functionCreator = method.createFunction(Supplier.class);
            BytecodeCreator fbc = functionCreator.getBytecode();
            ResultHandle superResult = fbc.invokeSpecialMethod(MethodDescriptor.ofMethod(Superclass.class, "getMessage", String.class), method.getThis());
            ResultHandle functionReturn = fbc.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "concat", String.class, String.class), superResult, fbc.load("-func"));
            fbc.returnValue(functionReturn);

            ResultHandle ret = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(Supplier.class, "get", Object.class), functionCreator.getInstance());
            method.returnValue(ret);

        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Assert.assertTrue(clazz.isSynthetic());
        Superclass superclass = (Superclass) clazz.newInstance();
        Assert.assertEquals("Superclass-func", superclass.getMessage());
    }
}
