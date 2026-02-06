package io.quarkus.bootstrap.runner;

import static io.quarkus.bootstrap.runner.JarVisitor.visitJar;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import io.quarkus.bootstrap.runner.AotRunnerClassLoader.ApplicationConfigEntry;

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
    private static final int VERSION = 2;
    // the files immediately (i.e. not recursively) under these paths should all be indexed
    private static final List<String> FULLY_INDEXED_DIRECTORIES = List.of("", "META-INF", "META-INF/services");

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
     * - Application config file name count (int) [version >= 2]
     * - For each application config file name:
     * . Config file name (UTF string, e.g., "application.properties")
     * . Jar entry count for this file (int)
     * . For each jar entry:
     * .. "URL" = jar!/file (UTF string)
     * .. Data length (int)
     * .. Data bytes
     *
     * @param out the output stream to write to
     * @param mainClass the main class name
     * @param classPath the full class path of the application
     * @throws IOException if an I/O error occurs
     */
    public static void write(OutputStream out, String mainClass, List<Path> classPath) throws IOException {
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeUTF(mainClass);

            // Write tracked directories
            data.writeInt(FULLY_INDEXED_DIRECTORIES.size());
            for (String directory : FULLY_INDEXED_DIRECTORIES) {
                data.writeUTF(directory);
            }

            FullyIndexedJarVisitor fullyIndexedVisitor = new FullyIndexedJarVisitor(FULLY_INDEXED_DIRECTORIES);
            ServiceLoaderFileJarVisitor serviceLoaderFileJarVisitor = new ServiceLoaderFileJarVisitor();
            ApplicationConfigFileJarVisitor applicationConfigFileJarVisitor = new ApplicationConfigFileJarVisitor();
            for (Path classPathElement : classPath) {
                visitJar(classPathElement, fullyIndexedVisitor, serviceLoaderFileJarVisitor, applicationConfigFileJarVisitor);
            }

            // Write directory contents
            data.writeInt(fullyIndexedVisitor.getFullyIndexedResources().size());
            for (String path : fullyIndexedVisitor.getFullyIndexedResources()) {
                data.writeUTF(path);
            }

            Map<String, byte[]> serviceFiles = concatenateServiceFiles(serviceLoaderFileJarVisitor.getServiceFiles());

            // Write service files
            data.writeInt(serviceFiles.size());
            for (Map.Entry<String, byte[]> entry : serviceFiles.entrySet()) {
                data.writeUTF(entry.getKey());
                byte[] content = entry.getValue();
                data.writeInt(content.length);
                data.write(content);
            }

            // Write application config files
            Map<String, List<ApplicationConfigEntry>> applicationConfigFiles = applicationConfigFileJarVisitor
                    .getApplicationConfigFiles();
            data.writeInt(applicationConfigFiles.size());
            for (Map.Entry<String, List<ApplicationConfigEntry>> entry : applicationConfigFiles.entrySet()) {
                data.writeUTF(entry.getKey());
                List<ApplicationConfigEntry> entries = entry.getValue();
                data.writeInt(entries.size());
                for (ApplicationConfigEntry configEntry : entries) {
                    data.writeUTF(configEntry.url());
                    data.writeInt(configEntry.content().length);
                    data.write(configEntry.content());
                }
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
                byte[] content = readBytes(data, dataLength, resourcePath);
                serviceFiles.put(resourcePath, content);
            }

            Map<String, List<ApplicationConfigEntry>> applicationConfigFiles;
            int configFileNameCount = data.readInt();

            if (configFileNameCount == 0) {
                applicationConfigFiles = Map.of();
            } else if (configFileNameCount == 1) {
                String configFileName = data.readUTF();
                int jarEntryCount = data.readInt();
                List<ApplicationConfigEntry> entries = readConfigEntries(data, jarEntryCount, configFileName);
                applicationConfigFiles = Map.of(configFileName, entries);
            } else {
                applicationConfigFiles = new HashMap<>((int) Math.ceil(configFileNameCount / 0.75f));
                for (int i = 0; i < configFileNameCount; i++) {
                    String configFileName = data.readUTF();
                    int jarEntryCount = data.readInt();
                    List<ApplicationConfigEntry> entries = readConfigEntries(data, jarEntryCount, configFileName);
                    applicationConfigFiles.put(configFileName, entries);
                }
            }

            AotRunnerClassLoader runnerClassLoader = new AotRunnerClassLoader(AotSerializedApplication.class.getClassLoader(),
                    fullyIndexedDirectories, fullyIndexedResources, serviceFiles, applicationConfigFiles);

            return new AotSerializedApplication(runnerClassLoader, mainClass);
        }
    }

    private static class ServiceLoaderFileJarVisitor implements JarVisitor {

        private final Map<String, List<byte[]>> serviceFiles = new LinkedHashMap<>();

        public Map<String, List<byte[]>> getServiceFiles() {
            return serviceFiles;
        }

        @Override
        public void visitJarFileEntry(JarFile jarFile, ZipEntry fileEntry) {
            if (!isServiceFile(fileEntry.getName())) {
                return;
            }

            try (var is = jarFile.getInputStream(fileEntry)) {
                serviceFiles.computeIfAbsent(fileEntry.getName(), k -> new ArrayList<>())
                        .add(is.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read entry: " + fileEntry.getName() + " from jar: " + jarFile, e);
            }
        }

        @Override
        public void visitRegularFile(Path jar, Path file, String relativePath) {
            if (!isServiceFile(relativePath)) {
                return;
            }

            try {
                serviceFiles.computeIfAbsent(relativePath, k -> new ArrayList<>())
                        .add(Files.readAllBytes(file));
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read file: " + relativePath, e);
            }
        }

        private static boolean isServiceFile(String resourcePath) {
            return resourcePath.startsWith("META-INF/services/") && resourcePath.length() > 18;
        }
    }

    private static class ApplicationConfigFileJarVisitor implements JarVisitor {

        private final Map<String, List<ApplicationConfigEntry>> applicationConfigFiles = new LinkedHashMap<>();

        public Map<String, List<ApplicationConfigEntry>> getApplicationConfigFiles() {
            return applicationConfigFiles;
        }

        @Override
        public void visitJarFileEntry(JarFile jarFile, ZipEntry fileEntry) {
            if (!AotRunnerClassLoader.isApplicationConfigFile(fileEntry.getName())) {
                return;
            }

            String configFileName = fileEntry.getName();

            try (var is = jarFile.getInputStream(fileEntry)) {
                String jarName = Paths.get(jarFile.getName()).getFileName().toString();
                applicationConfigFiles.computeIfAbsent(configFileName, k -> new ArrayList<>())
                        .add(new ApplicationConfigEntry(jarName + "!/" + configFileName, is.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read entry: " + configFileName + " from jar: " + jarFile, e);
            }
        }

        @Override
        public void visitRegularFile(Path jar, Path file, String relativePath) {
            if (!AotRunnerClassLoader.isApplicationConfigFile(relativePath)) {
                return;
            }

            try {
                String jarName = jar.getFileName().toString();
                applicationConfigFiles.computeIfAbsent(relativePath, k -> new ArrayList<>())
                        .add(new ApplicationConfigEntry(jarName + "!/" + relativePath, Files.readAllBytes(file)));
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read file: " + relativePath, e);
            }
        }
    }

    private static Map<String, byte[]> concatenateServiceFiles(Map<String, List<byte[]>> files) throws IOException {
        Map<String, byte[]> concatenatedServiceFiles = new TreeMap<>();

        for (Entry<String, List<byte[]>> entry : files.entrySet()) {
            if (entry.getValue().size() == 1) {
                concatenatedServiceFiles.put(entry.getKey(), entry.getValue().get(0));
                continue;
            }

            // Concatenate with newlines between files
            ByteArrayOutputStream concatenatedEntry = new ByteArrayOutputStream();
            for (int i = 0; i < entry.getValue().size(); i++) {
                concatenatedEntry.write(entry.getValue().get(i));
                if (i < entry.getValue().size() - 1) {
                    concatenatedEntry.write('\n');
                }
            }
            concatenatedServiceFiles.put(entry.getKey(), concatenatedEntry.toByteArray());
        }

        return concatenatedServiceFiles;
    }

    private static List<ApplicationConfigEntry> readConfigEntries(DataInputStream data, int jarEntryCount,
            String configFileName) throws IOException {
        if (jarEntryCount == 1) {
            String url = data.readUTF();
            int dataLength = data.readInt();
            byte[] content = readBytes(data, dataLength, configFileName);
            return List.of(new ApplicationConfigEntry(url, content));
        } else if (jarEntryCount == 2) {
            String url1 = data.readUTF();
            int dataLength1 = data.readInt();
            byte[] content1 = readBytes(data, dataLength1, configFileName);

            String url2 = data.readUTF();
            int dataLength2 = data.readInt();
            byte[] content2 = readBytes(data, dataLength2, configFileName);

            return List.of(
                    new ApplicationConfigEntry(url1, content1),
                    new ApplicationConfigEntry(url2, content2));
        } else {
            List<ApplicationConfigEntry> entries = new ArrayList<>(jarEntryCount);
            for (int j = 0; j < jarEntryCount; j++) {
                String url = data.readUTF();
                int dataLength = data.readInt();
                byte[] content = readBytes(data, dataLength, configFileName);
                entries.add(new ApplicationConfigEntry(url, content));
            }
            return entries;
        }
    }

    private static byte[] readBytes(DataInputStream data, int length, String resourceName) throws IOException {
        byte[] content = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int read = data.read(content, totalRead, length - totalRead);
            if (read == -1) {
                throw new IOException(
                        "Unexpected end of stream while reading application config file: " + resourceName);
            }
            totalRead += read;
        }
        return content;
    }
}
