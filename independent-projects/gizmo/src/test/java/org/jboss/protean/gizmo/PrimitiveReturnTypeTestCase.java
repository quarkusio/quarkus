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

import org.junit.Test;
import org.objectweb.asm.Opcodes;

public class PrimitiveReturnTypeTestCase {

    @Test
    public void testPrimitiveReturnType() throws Exception {
        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        try (ClassCreator creator = ClassCreator.builder().classOutput(cl).className("com.MyTest").build()) {
            MethodCreator method = creator.getMethodCreator("transform", Object.class).setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle ret = method.readStaticField(FieldDescriptor.of(Integer.class, "MAX_VALUE", int.class));
            method.returnValue(ret);
        }
        Class<?> clazz = cl.loadClass("com.MyTest");
        clazz.getMethod("transform").invoke(null);
    }
}