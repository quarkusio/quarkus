package io.quarkus.it.nativeannotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.quarkus.runtime.annotations.RuntimeInitialized;

//class that opens a file in static init, this is not allowed
//this is an implicit test, there is no corresponding test case
//but native image would fail without the @RuntimeInitialized annotation being present
@RuntimeInitialized
public class NativeFileClass {
    private static OutputStream outputStream;
    static {
        try {
            File file = File.createTempFile("test-file", "quarkus-test");
            file.deleteOnExit();
            outputStream = new FileOutputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void doClose() throws IOException {
        outputStream.close();
    }

}
