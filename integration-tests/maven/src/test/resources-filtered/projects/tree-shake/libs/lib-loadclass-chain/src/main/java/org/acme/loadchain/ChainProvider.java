package org.acme.loadchain;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates the BouncyCastle BouncyCastleProvider pattern:
 * constructor dynamically loads classes by constructing names at runtime.
 */
public class ChainProvider {
    private static final String PACKAGE = "org.acme.loadchain.";
    private static final String[] TARGETS = { "Alpha", "Beta" };
    private static final String[] SILENT_TARGETS = { "ChainHolder$Gamma" };

    private final List<String> loaded = new ArrayList<>();

    public ChainProvider() {
        for (String name : TARGETS) {
            try {
                Class<?> clazz = Class.forName(PACKAGE + name + "Target");
                loaded.add(clazz.getSimpleName());
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        // Loaded without reflecting on the class, so the recording run never loads the enclosing class.
        // The enclosing class is kept only because the tree-shaker keeps the enclosing class of a
        // reachable member class, whichever analysis pass reached it.
        for (String name : SILENT_TARGETS) {
            try {
                Class.forName(PACKAGE + name + "Target");
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

    public List<String> getLoaded() {
        return loaded;
    }
}
