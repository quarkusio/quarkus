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
    }

    public List<String> getLoaded() {
        return loaded;
    }
}
