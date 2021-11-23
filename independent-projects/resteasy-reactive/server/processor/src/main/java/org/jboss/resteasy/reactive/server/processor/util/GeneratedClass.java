package org.jboss.resteasy.reactive.server.processor.util;

public class GeneratedClass {
    final String name;
    final byte[] data;

    public GeneratedClass(String name, byte[] data) {
        if (name.endsWith(".class")) {
            throw new RuntimeException("resource name specified instead of class name: " + name);
        }
        this.name = name.replace("/", ".");
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
