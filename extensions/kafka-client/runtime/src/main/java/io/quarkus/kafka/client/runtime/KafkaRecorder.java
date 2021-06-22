package io.quarkus.kafka.client.runtime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.xerial.snappy.OSInfo;
import org.xerial.snappy.SnappyError;
import org.xerial.snappy.SnappyErrorCode;
import org.xerial.snappy.SnappyLoader;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KafkaRecorder {

    public void loadSnappy() {
        // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
        String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
        String snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        boolean hasNativeLib = hasResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName);
        if (!hasNativeLib) {
            if (OSInfo.getOSName().equals("Mac")) {
                // Fix for openjdk7 for Mac
                String altName = "libsnappyjava.jnilib";
                if (hasResource(snappyNativeLibraryPath + "/" + altName)) {
                    snappyNativeLibraryName = altName;
                    hasNativeLib = true;
                }
            }
        }

        if (!hasNativeLib) {
            String errorMessage = String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(),
                    OSInfo.getArchName());
            throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, errorMessage);
        }

        File out = extractLibraryFile(
                SnappyLoader.class.getResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName),
                snappyNativeLibraryName);

        System.load(out.getAbsolutePath());
    }

    private static boolean hasResource(String path) {
        return SnappyLoader.class.getResource(path) != null;
    }

    private static File extractLibraryFile(URL library, String name) {
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

    public void checkBoostrapServers() {
        Config config = ConfigProvider.getConfig();
        Boolean serviceBindingEnabled = config.getValue("quarkus.kubernetes-service-binding.enabled", Boolean.class);
        if (!serviceBindingEnabled) {
            return;
        }
        Optional<String> boostrapServersOptional = config.getOptionalValue("kafka.bootstrap.servers", String.class);
        if (boostrapServersOptional.isEmpty()) {
            throw new IllegalStateException(
                    "The property 'kafka.bootstrap.servers' must be set when 'quarkus.kubernetes-service-binding.enabled' has been set to 'true'");
        }
    }
}
