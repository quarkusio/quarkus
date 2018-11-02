package org.jboss.protean.gizmo;

import java.util.function.Function;
import java.util.function.IntSupplier;
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

    @Test
    public void testNestedFunction() throws Exception {
        MethodDescriptor getAsInt = MethodDescriptor.ofMethod(IntSupplier.class, "getAsInt", int.class);
        MethodDescriptor addExact = MethodDescriptor.ofMethod(Math.class, "addExact", int.class, int.class, int.class);

        final TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(IntSupplier.class).build()) {
            MethodCreator bc = creator.getMethodCreator("getAsInt", int.class);
            ResultHandle seven = bc.invokeStaticMethod(addExact, bc.load(2), bc.load(5));
            FunctionCreator f1 = bc.createFunction(IntSupplier.class);
            BytecodeCreator f1bc = f1.getBytecode();
            ResultHandle four = f1bc.invokeStaticMethod(addExact, seven, f1bc.load(- 3));
            FunctionCreator f2 = f1bc.createFunction(IntSupplier.class);
            BytecodeCreator f2bc = f2.getBytecode();
            f2bc.returnValue(f2bc.invokeStaticMethod(addExact, seven, four));
            f1bc.returnValue(f1bc.invokeInterfaceMethod(getAsInt, f2.getInstance()));
            bc.returnValue(bc.invokeInterfaceMethod(getAsInt, f1.getInstance()));
        }
        Class<? extends IntSupplier> clazz = cl.loadClass("com.MyTest").asSubclass(IntSupplier.class);
        IntSupplier supplier = clazz.newInstance();
        Assert.assertEquals(11, supplier.getAsInt());
    }
}
