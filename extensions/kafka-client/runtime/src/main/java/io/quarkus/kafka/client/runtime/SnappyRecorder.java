package io.quarkus.kafka.client.runtime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

import org.xerial.snappy.OSInfo;

import io.quarkus.runtime.Application;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SnappyRecorder {

    public void loadSnappy(boolean loadFromSharedClassLoader) {
        if (loadFromSharedClassLoader) {
            try {
                Application.class.getClassLoader().loadClass(SnappyLoader.class.getName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            File out = getLibraryFile();
            try {
                System.load(out.getAbsolutePath());
            } catch (UnsatisfiedLinkError e) {
                // Try to load the library from the system library path
                throw new RuntimeException("Failed to load Snappy native library", e);
            }
        }
    }

    static File getLibraryFile() {
        // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
        String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
        String snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        boolean hasNativeLib = hasResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName);

        if (!hasNativeLib) {
            String errorMessage = String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(),
                    OSInfo.getArchName());
            throw new RuntimeException(errorMessage);
        }

        return extractLibraryFile(
                SnappyLoader.class.getResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName),
                snappyNativeLibraryName);
    }

    static boolean hasResource(String path) {
        return SnappyLoader.class.getResource(path) != null;
    }

    static File extractLibraryFile(URL library, String name) {
        String tmp = System.getProperty("java.io.tmpdir");
        File extractedLibFile = new File(tmp, name);

        try (BufferedInputStream inputStream = new BufferedInputStream(library.openStream());
                FileOutputStream fileOS = new FileOutputStream(extractedLibFile)) {
            byte[] data = new byte[8192];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 8192)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to extract native library " + name + " to " + extractedLibFile.getAbsolutePath(), e);
        }

        extractedLibFile.deleteOnExit();

        return extractedLibFile;
    }
}
