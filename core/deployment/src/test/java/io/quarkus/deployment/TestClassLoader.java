package io.quarkus.deployment;

public class TestClassLoader extends io.quarkus.gizmo.TestClassLoader {
    public TestClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len);
    }
}
