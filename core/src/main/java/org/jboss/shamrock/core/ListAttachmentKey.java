package org.jboss.shamrock.core;

public class ListAttachmentKey<T> {

    private final String name;

    public ListAttachmentKey(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CollectionKey{" +
                "name='" + name + '\'' +
                '}';
    }
}
