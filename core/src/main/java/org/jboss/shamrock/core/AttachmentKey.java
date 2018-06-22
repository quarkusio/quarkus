package org.jboss.shamrock.core;

public class AttachmentKey<T> {

    private final String name;

    public AttachmentKey(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AttachmentKey{" +
                "name='" + name + '\'' +
                '}';
    }
}
