package org.acme.handles;

import java.lang.invoke.MethodHandles;

public class HandleTarget {
    public static void doWork() {
        try {
            // This findClass call triggers the tree-shaker to preserve ALL classes
            // in this dependency (lib-method-handles)
            MethodHandles.lookup().findClass("org.acme.handles.HandleTarget");
        } catch (Exception e) {
            // ignore — the point is the bytecode pattern, not runtime behavior
        }
    }
}
