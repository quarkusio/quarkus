package io.quarkus.rest.server.runtime.mapping;

public interface Dumpable {
    public void dump(int level);

    public default void indent(int level) {
        for (int i = 0; i < level; i++) {
            System.err.print(' ');
        }
    }
}
