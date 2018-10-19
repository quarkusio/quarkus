package org.jboss.shamrock.jpa.enhancer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.jboss.shamrock.jpa.HibernateEntityEnhancer;

import org.jboss.shamrock.jpa.KnownDomainObjects;
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

    private static final String TEST_CLASSNAME = Address.class.getName();

    @Test
    public void testBytecodeEnhancement() throws IOException, ClassNotFoundException {

        Assert.assertFalse(isEnhanced(Address.class));

        ClassReader classReader = new ClassReader(TEST_CLASSNAME);
        ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = writer;
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer(new TestingKnownDomainObjects());
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
              interfaces.contains(PersistentAttributeInterceptable.class) &&
              interfaces.contains(SelfDirtinessTracker.class);
    }

    private static class TestingKnownDomainObjects implements KnownDomainObjects {

        @Override
        public boolean contains(final String className) {
            return TEST_CLASSNAME.equals(className);
        }

        @Override
        public Set<String> getClassNames() {
            return Collections.singleton(TEST_CLASSNAME);
        }
    }

}
