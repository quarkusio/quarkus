package io.quarkus.hibernate.orm.enhancer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.gizmo.TestClassLoader;
import io.quarkus.hibernate.orm.deployment.HibernateEntityEnhancer;

/**
 * Verifies the HibernateEntityEnhancer actually does enhance the entity class
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class HibernateEntityEnhancerTest {

    private static final String TEST_CLASSNAME = Address.class.getName();

    @Test
    public void testBytecodeEnhancement() throws IOException, ClassNotFoundException {

        assertFalse(isEnhanced(Address.class));

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
        assertTrue(isEnhanced(modifiedClass));
    }

    private boolean isEnhanced(final Class<?> modifiedClass) {
        Set<Class> interfaces = new HashSet<Class>(Arrays.asList(modifiedClass.getInterfaces()));
        //Assert it now implements these three interfaces:
        return interfaces.contains(ManagedEntity.class) &&
                interfaces.contains(PersistentAttributeInterceptable.class) &&
                interfaces.contains(SelfDirtinessTracker.class);
    }

}
