package io.quarkus.test.component;

class ComponentClassLoader extends ClassLoader {

    private final QuarkusComponentFacadeClassLoaderProvider cls = new QuarkusComponentFacadeClassLoaderProvider();

    ComponentClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = cls.getClassLoader(name, getParent());
        if (cl != null) {
            return cl.loadClass(name);
        }
        return getParent().loadClass(name);
    }

}
