package io.quarkus.deployment.dev.testing;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

public abstract class CollaboratingClassLoader extends ClassLoader implements Closeable {
    public CollaboratingClassLoader(ClassLoader parent) {

        super(parent);
    }

    public static CollaboratingClassLoader construct(ClassLoader parent) {
        try {
            return (CollaboratingClassLoader) Class.forName("io.quarkus.test.junit.classloading.FacadeClassLoader")
                    .getConstructor(ClassLoader.class)
                    .newInstance(parent);
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
