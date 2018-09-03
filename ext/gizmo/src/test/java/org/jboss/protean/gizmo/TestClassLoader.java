package org.jboss.protean.gizmo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TestClassLoader extends ClassLoader implements ClassOutput {

    private final Map<String, byte[]> appClasses = new HashMap<>();

    public TestClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> ex = findLoadedClass(name);
        if (ex != null) {
            return ex;
        }
        if (appClasses.containsKey(name)) {
            return findClass(name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = appClasses.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException();
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public void write(String name, byte[] data) {
        if (System.getProperty("dumpClass") != null) {
            try {
                File dir = new File("target/test-classes/", name.substring(0, name.lastIndexOf("/")));
                dir.mkdirs();
                File output = new File("target/test-classes/", name + ".class");
                Files.write(output.toPath(), data);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot dump the class: " + name, e);
            }
        }
        appClasses.put(name.replace('/', '.'), data);
    }
}
