/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
