package io.quarkus.bootstrap.runneraot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialization and deserialization of cached resources for AOT-optimized jar packaging.
 *
 * This class handles the serialization of frequently-accessed resources (like service loader files
 * and configuration files) to avoid classpath scanning at runtime, while maintaining compatibility
 * with standard Java classloaders for optimal AOT performance.
 *
 * The format is versioned and includes a magic number for validation.
 */
public class AotSerializedCache {

    private static final int MAGIC = 0xA07CA3E; // AOT CACHE
    private static final int VERSION = 1;

    private final String mainClass;
    private final Map<String, byte[]> cachedResources;

    public AotSerializedCache(String mainClass, Map<String, byte[]> cachedResources) {
        this.mainClass = mainClass;
        this.cachedResources = cachedResources;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Map<String, byte[]> getCachedResources() {
        return cachedResources;
    }

    /**
     * Writes cached resources to an output stream in a binary format.
     *
     * Format:
     * - Magic number (int)
     * - Version (int)
     * - Main class name (UTF string)
     * - Resource count (int)
     * - For each resource:
     * - Resource path (UTF string)
     * - Data length (int)
     * - Data bytes
     *
     * @param out the output stream to write to
     * @param mainClass the main class name
     * @param cachedResources map of resource paths to their content
     * @throws IOException if an I/O error occurs
     */
    public static void write(OutputStream out, String mainClass, Map<String, byte[]> cachedResources) throws IOException {
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeUTF(mainClass);
            data.writeInt(cachedResources.size());

            for (Map.Entry<String, byte[]> entry : cachedResources.entrySet()) {
                data.writeUTF(entry.getKey());
                byte[] content = entry.getValue();
                data.writeInt(content.length);
                data.write(content);
            }
            data.flush();
        }
    }

    /**
     * Reads cached resources from an input stream.
     *
     * @param in the input stream to read from
     * @return an AotSerializedCache containing the main class and cached resources
     * @throws IOException if an I/O error occurs or the format is invalid
     */
    public static AotSerializedCache read(InputStream in) throws IOException {
        try (DataInputStream data = new DataInputStream(in)) {
            int magic = data.readInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid magic number in AOT cache file: expected 0x"
                        + Integer.toHexString(MAGIC) + " but got 0x" + Integer.toHexString(magic));
            }

            int version = data.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported AOT cache version: expected " + VERSION + " but got " + version);
            }

            String mainClass = data.readUTF();
            int resourceCount = data.readInt();
            Map<String, byte[]> cachedResources = new HashMap<>((int) Math.ceil(resourceCount / 0.75f));

            for (int i = 0; i < resourceCount; i++) {
                String resourcePath = data.readUTF();
                int dataLength = data.readInt();
                byte[] content = new byte[dataLength];

                int totalRead = 0;
                while (totalRead < dataLength) {
                    int read = data.read(content, totalRead, dataLength - totalRead);
                    if (read == -1) {
                        throw new IOException("Unexpected end of stream while reading resource: " + resourcePath);
                    }
                    totalRead += read;
                }

                cachedResources.put(resourcePath, content);
            }

            return new AotSerializedCache(mainClass, cachedResources);
        }
    }
}
