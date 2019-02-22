/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.deployment;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * ClassWriter which tries to get around ClassNotFoundExceptions related to reflection usage in
 * getCommonSuperClass.
 *
 * @author Stéphane Épardaud
 */
public class QuarkusClassWriter extends ClassWriter {

    public QuarkusClassWriter(final ClassReader classReader, final int flags) {
        super(classReader, flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        ClassLoader cl = getClassLoader();
        Class<?> c1 = null, c2 = null;
        try {
            c1 = cl.loadClass(type1.replace('/', '.'));
        } catch (ClassNotFoundException e) {
        }
        try {
            c2 = cl.loadClass(type2.replace('/', '.'));
        } catch (ClassNotFoundException e) {
        }
        if (c1 != null && c2 != null) {
            return super.getCommonSuperClass(type1, type2);
        }
        return Object.class.getName().replace('.', '/');
    }

}
