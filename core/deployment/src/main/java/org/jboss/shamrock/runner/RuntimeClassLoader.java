package org.jboss.shamrock.runner;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shamrock.deployment.ClassOutput;

public class RuntimeClassLoader extends ClassLoader implements ClassOutput {

    private final Map<String, byte[]> classes = new HashMap<>();

    public RuntimeClassLoader(ClassLoader parent) {
        super(parent);
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (classes.containsKey(name)) {
            Class<?> existing = findLoadedClass(name);
            if(existing != null) {
                return existing;
            }
            return findClass(name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name);
        if(bytes == null) {
            throw new ClassNotFoundException();
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public void writeClass(String className, byte[] data) {
        classes.put(className.replace('/', '.'), data);
    }
}
