package io.quarkus.container.image.deployment.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;

public final class NativeBinaryUtil {

    private NativeBinaryUtil() {
    }

    /**
     * Checks if the file is a linux binary by checking the first bytes of the file against the ELF magic number
     */
    public static boolean nativeIsLinuxBinary(NativeImageBuildItem nativeImageBuildItem) {
        File file = nativeImageBuildItem.getPath().toFile();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] firstBytes = new byte[4];
            int readBytes = fileInputStream.read(firstBytes);
            if (readBytes != 4) {
                return false;
            }
            return (firstBytes[0] == 0x7f && firstBytes[1] == 0x45 && firstBytes[2] == 0x4c && firstBytes[3] == 0x46);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to determine type of native binary " + nativeImageBuildItem.getPath(), e);
        }
    }
}
