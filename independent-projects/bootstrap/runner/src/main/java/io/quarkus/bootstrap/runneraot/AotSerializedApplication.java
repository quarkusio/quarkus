package io.quarkus.bootstrap.runneraot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serialization and deserialization of cached resources for AOT-optimized jar packaging.
 *
 * This class handles the serialization of frequently-accessed resources (like service loader files)
 * to avoid classpath scanning at runtime, while maintaining compatibility
 * with standard Java classloaders for optimal AOT performance.
 *
 * The format is versioned and includes a magic number for validation.
 */
public class AotSerializedApplication {

    private static final int MAGIC = 0xA07CA3E; // AOT CACHE
    private static final int VERSION = 1;

    private final AotRunnerClassLoader runnerClassLoader;
    private final String mainClass;

    public AotSerializedApplication(AotRunnerClassLoader runnerClassLoader, String mainClass) {
        this.runnerClassLoader = runnerClassLoader;
        this.mainClass = mainClass;
    }

    public String getMainClass() {
        return mainClass;
    }

    public AotRunnerClassLoader getRunnerClassLoader() {
        return runnerClassLoader;
    }

    /**
     * Writes cached resources to an output stream in a binary format.
     *
     * Format:
     * - Magic number (int)
     * - Version (int)
     * - Main class name (UTF string)
     * - Fully indexed directory count (int)
     * - For each fully indexed directory:
     * . Directory path (UTF string)
     * - Fully indexed resource count (int)
     * - For each fully indexed resource:
     * . Resource path (UTF string)
     * - Service file count (int)
     * - For each service file:
     * . Resource path (UTF string)
     * . Data length (int)
     * . Data bytes
     *
     * @param out the output stream to write to
     * @param mainClass the main class name
     * @param fullyIndexedDirectories list of fully indexed directory paths
     * @param fullyIndexedResources set of resource paths that are direct children of fully indexed directories
     * @param serviceFiles map of service files to their content
     * @throws IOException if an I/O error occurs
     */
    public static void write(OutputStream out, String mainClass, List<String> fullyIndexedDirectories,
            Set<String> fullyIndexedResources, Map<String, byte[]> serviceFiles) throws IOException {
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeUTF(mainClass);

            // Write tracked directories
            data.writeInt(fullyIndexedDirectories.size());
            for (String directory : fullyIndexedDirectories) {
                data.writeUTF(directory);
            }

            // Write directory contents
            data.writeInt(fullyIndexedResources.size());
            for (String path : fullyIndexedResources) {
                data.writeUTF(path);
            }

            // Write service files
            data.writeInt(serviceFiles.size());
            for (Map.Entry<String, byte[]> entry : serviceFiles.entrySet()) {
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
     * @return an AotSerializedApplication containing the main class and cached resources
     * @throws IOException if an I/O error occurs or the format is invalid
     */
    public static AotSerializedApplication read(InputStream in) throws IOException {
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

            // Read tracked directories
            int fullyIndexedDirectoryCount = data.readInt();
            Set<String> fullyIndexedDirectories = new HashSet<>((int) Math.ceil(fullyIndexedDirectoryCount / 0.75f));
            for (int i = 0; i < fullyIndexedDirectoryCount; i++) {
                fullyIndexedDirectories.add(data.readUTF());
            }

            // Read directory contents
            int fullyIndexedResourceCount = data.readInt();
            Set<String> fullyIndexedResources = new HashSet<>((int) Math.ceil(fullyIndexedResourceCount / 0.75f));
            for (int i = 0; i < fullyIndexedResourceCount; i++) {
                fullyIndexedResources.add(data.readUTF());
            }

            // Read cached resources
            int serviceFileCount = data.readInt();
            Map<String, byte[]> serviceFiles = new HashMap<>((int) Math.ceil(serviceFileCount / 0.75f));

            for (int i = 0; i < serviceFileCount; i++) {
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

                serviceFiles.put(resourcePath, content);
            }

            AotRunnerClassLoader runnerClassLoader = new AotRunnerClassLoader(AotSerializedApplication.class.getClassLoader(),
                    fullyIndexedDirectories, fullyIndexedResources, serviceFiles);

            return new AotSerializedApplication(runnerClassLoader, mainClass);
        }
    }
}
