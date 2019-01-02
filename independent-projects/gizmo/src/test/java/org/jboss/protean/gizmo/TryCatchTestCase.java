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
        MyInterface myInterface = (MyInterface) clazz.getDeclaredConstructor().newInstance();
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
        MyInterface myInterface = (MyInterface) clazz.getDeclaredConstructor().newInstance();
        Assert.assertEquals("complete", myInterface.transform("ignored"));
    }
}
