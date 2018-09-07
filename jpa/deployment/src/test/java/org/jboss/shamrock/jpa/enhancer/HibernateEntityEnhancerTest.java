package org.jboss.shamrock.jpa.enhancer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.jboss.shamrock.jpa.HibernateEntityEnhancer;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import org.jboss.protean.gizmo.TestClassLoader;

/**
 * Verifies the HibernateEntityEnhancer actually does enhance the entity class
 *
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public class HibernateEntityEnhancerTest {

    @Test
    public void testBytecodeEnhancement() throws IOException, ClassNotFoundException {

        Assert.assertFalse(isEnhanced(Address.class));

        final String className = Address.class.getName();

        ClassReader classReader = new ClassReader(className);
        ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = writer;
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        visitor = hibernateEntityEnhancer.apply(className).apply(visitor);
        classReader.accept(visitor, 0);
        final byte[] modifiedBytecode = writer.toByteArray();

        TestClassLoader cl = new TestClassLoader(getClass().getClassLoader());
        cl.write(className, modifiedBytecode);
        final Class<?> modifiedClass = cl.loadClass(className);
        Assert.assertTrue(isEnhanced(modifiedClass));
    }

    private boolean isEnhanced(final Class<?> modifiedClass) {
        Set<Class> interfaces = new HashSet<Class>(Arrays.asList(modifiedClass.getInterfaces()));
        //Assert it now implements these three interfaces:
        return interfaces.contains(ManagedEntity.class) &&
              interfaces.contains(PersistentAttributeInterceptable.class) &&
              interfaces.contains(SelfDirtinessTracker.class);
    }

}
