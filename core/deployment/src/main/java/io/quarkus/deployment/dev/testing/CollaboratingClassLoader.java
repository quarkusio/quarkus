package io.quarkus.deployment.dev.testing;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Gives JUnitTestRunner visibility of the FacadeClassLoader if it's in a different test-framework module.
 * Unfortunately, this technique isn't working for multimodule projects.
 * TODO reinstate this, and get DevMojoIT tests passing with it
 */
public abstract class CollaboratingClassLoader extends ClassLoader implements Closeable {
    private static Map<ClassLoader, CollaboratingClassLoader> cls = new HashMap<>();

    public CollaboratingClassLoader(ClassLoader parent) {

        super(parent);
    }

    public static CollaboratingClassLoader construct(ClassLoader parent) {
        // TODO what happens when it's not available, becuse there's no JUnit 5? Callers need to just not set a TCCL, I guess?
        // TODO tidy this up
        if (cls.get(parent) == null) {
            System.out.println("HOLLY constryucting collaborating classloader ");
            try {
                System.out.println(
                        "CollaboratingClassLoader.construct using class " + CollaboratingClassLoader.class.getClassLoader());

                CollaboratingClassLoader cl = (CollaboratingClassLoader) Class
                        .forName("io.quarkus.test.junit.classloading.FacadeClassLoader")
                        .getConstructor(ClassLoader.class)
                        .newInstance(parent);
                cls.put(parent, cl);
                return cl;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return cls.get(parent);
        }
    }

    public abstract void setAuxiliaryApplication(boolean b);

    public void setProfiles(Map<String, String> profiles) {
    }

    public void setClassPath(String classesPath) {
    }

    public void setQuarkusTestClasses(Set<String> quarkusTestClasses) {
    }

    public void setQuarkusMainTestClasses(Set<String> quarkusMainTestClasses) {
    }
}
