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

import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

public class ArrayTestCase {

    @Test
    public void testNewArray() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(Supplier.class).build()) {
            MethodCreator method = creator.getMethodCreator("get", Object.class);
            ResultHandle arrayHandle = method.newArray(String.class, method.load(10));
            method.returnValue(arrayHandle);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Supplier myInterface = (Supplier) clazz.getDeclaredConstructor().newInstance();
        Object o = myInterface.get();
        Assert.assertEquals(String[].class, o.getClass());
        String[] res = (String[]) o;
        Assert.assertEquals(10, res.length);

    }

    @Test
    public void testWriteArray() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").interfaces(Supplier.class).build()) {
            MethodCreator method = creator.getMethodCreator("get", Object.class);
            ResultHandle arrayHandle = method.newArray(String.class, method.load(1));
            method.writeArrayValue(arrayHandle, method.load(0), method.load("hello"));
            method.returnValue(arrayHandle);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        Supplier myInterface = (Supplier) clazz.getDeclaredConstructor().newInstance();
        Object o = myInterface.get();
        Assert.assertEquals(String[].class, o.getClass());
        String[] res = (String[]) o;
        Assert.assertEquals(1, res.length);
        Assert.assertEquals("hello", res[0]);

    }
}
