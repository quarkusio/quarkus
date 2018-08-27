package org.jboss.protean.gizmo;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class FieldModifiersTest {

    @Test
    public void testFieldModifiers() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").build()) {
            creator.getFieldCreator("foo", DescriptorUtils.classToStringRepresentation(String.class));
            creator.getFieldCreator("list", DescriptorUtils.classToStringRepresentation(List.class)).setModifiers(ACC_FINAL | ACC_PROTECTED);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Field foo = clazz.getDeclaredField("foo");
        Assert.assertTrue(Modifier.isPrivate(foo.getModifiers()));
        Assert.assertEquals(String.class, foo.getType());
        Field list = clazz.getDeclaredField("list");
        Assert.assertTrue(Modifier.isFinal(list.getModifiers()));
        Assert.assertTrue(Modifier.isProtected(list.getModifiers()));
        Assert.assertEquals(List.class, list.getType());
    }

}
