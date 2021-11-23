package org.jboss.resteasy.reactive.server.vertx.test.framework;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClass;

public class ResteasyReactiveTestClassLoader extends URLClassLoader {

    final Map<String, byte[]> generatedClasses;

    public ResteasyReactiveTestClassLoader(URL[] urls, ClassLoader parent, List<GeneratedClass> generatedClasses) {
        super(urls, parent);
        this.generatedClasses = new HashMap<>();
        for (var i : generatedClasses) {
            this.generatedClasses.put(i.getName(), i.getData());
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (generatedClasses.containsKey(name)) {
            byte[] data = generatedClasses.get(name);
            return defineClass(name, data, 0, data.length);
        }
        return super.findClass(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
        }
    }
}
