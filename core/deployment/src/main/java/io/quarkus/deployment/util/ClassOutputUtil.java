package io.quarkus.deployment.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility that dumps bytes from a class to a file - useful for debugging generated classes
 */
public final class ClassOutputUtil {

    private ClassOutputUtil() {
    }

    public static void dumpClass(String name, byte[] data) {
        try {
            File dir = new File("target/test-classes/", name.substring(0, name.lastIndexOf("/")));
            dir.mkdirs();
            File output = new File("target/test-classes/", name + ".class");
            Files.write(output.toPath(), data);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot dump the class: " + name, e);
        }
    }
}
