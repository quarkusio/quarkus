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

    public QuarkusClassWriter(final int flags) {
        super(flags);
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
