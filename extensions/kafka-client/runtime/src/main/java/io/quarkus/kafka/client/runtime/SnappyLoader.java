package io.quarkus.kafka.client.runtime;

import java.io.File;

public class SnappyLoader {

    /*
     * This class is intended to be loaded from a shared classloader (e.g., the system classloader) to avoid
     * unsatisfied link errors when the native library is loaded from a different classloader.
     * See https://github.com/quarkusio/quarkus/issues/39767.
     *
     * This class is only used in tests if the `quarkus.kafka.snappy.load-from-shared-classloader=true` is set.
     */
    static {
        File out = SnappyRecorder.getLibraryFile();
        System.load(out.getAbsolutePath());
    }
}
