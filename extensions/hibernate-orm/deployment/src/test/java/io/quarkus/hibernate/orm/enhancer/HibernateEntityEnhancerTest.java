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

package io.quarkus.hibernate.orm.enhancer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.jboss.protean.gizmo.TestClassLoader;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.hibernate.orm.HibernateEntityEnhancer;

/**
 * Verifies the HibernateEntityEnhancer actually does enhance the entity class
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class HibernateEntityEnhancerTest {

    private static final String TEST_CLASSNAME = Address.class.getName();

    @Test
    public void testBytecodeEnhancement() throws IOException, ClassNotFoundException {

        Assert.assertFalse(isEnhanced(Address.class));

        ClassReader classReader = new ClassReader(TEST_CLASSNAME);
        ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = writer;
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        visitor = hibernateEntityEnhancer.apply(TEST_CLASSNAME, visitor);
        classReader.accept(visitor, 0);
        final byte[] modifiedBytecode = writer.toByteArray();

        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        cl.write(TEST_CLASSNAME, modifiedBytecode);
        final Class<?> modifiedClass = cl.loadClass(TEST_CLASSNAME);
        Assert.assertTrue(isEnhanced(modifiedClass));
    }

    private boolean isEnhanced(final Class<?> modifiedClass) {
        Set<Class> interfaces = new HashSet<Class>(Arrays.asList(modifiedClass.getInterfaces()));
        //Assert it now implements these three interfaces:
        return interfaces.contains(ManagedEntity.class) &&
                interfaces.contains(PersistentAttributeInterceptable.class);// &&
        //interfaces.contains(SelfDirtinessTracker.class);
    }

}
