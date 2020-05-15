package io.quarkus.bootstrap.runner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Data that reads serialized Class Path info
 *
 * This format is subject to change, and gives no compatibility guarantees, it is only intended to be used
 * with the same version of Quarkus that created it.
 */
public class SerializedApplication {

    private static final int MAGIC = 0XF0315432;
    private static final int VERSION = 1;

    private final RunnerClassLoader runnerClassLoader;
    private final String mainClass;

    public SerializedApplication(RunnerClassLoader runnerClassLoader, String mainClass) {
        this.runnerClassLoader = runnerClassLoader;
        this.mainClass = mainClass;
    }

    public RunnerClassLoader getRunnerClassLoader() {
        return runnerClassLoader;
    }

    public String getMainClass() {
        return mainClass;
    }

    public static void write(OutputStream outputStream, String mainClass, Path applicationRoot, List<Path> classPath)
            throws IOException {
        try (DataOutputStream data = new DataOutputStream(outputStream)) {
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeUTF(mainClass);
            data.writeInt(classPath.size());
            for (Path jar : classPath) {
                String relativePath = relativize(applicationRoot, jar);
                data.writeUTF(relativePath);
                writeJar(data, jar);
            }
            data.flush();
        }
    }

    public static SerializedApplication read(InputStream inputStream, Path appRoot) throws IOException {
        try (DataInputStream in = new DataInputStream(inputStream)) {
            if (in.readInt() != MAGIC) {
                throw new RuntimeException("Wrong magic number");
            }
            if (in.readInt() != VERSION) {
                throw new RuntimeException("Wrong class path version");
            }
            String mainClass = in.readUTF();
            Map<String, ClassLoadingResource[]> resourceDirectoryMap = new HashMap<>();
            int numPaths = in.readInt();
            for (int pathCount = 0; pathCount < numPaths; ++pathCount) {
                String path = in.readUTF();
                boolean hasManifest = in.readBoolean();
                ManifestInfo info = null;
                if (hasManifest) {
                    info = new ManifestInfo(readNullableString(in), readNullableString(in), readNullableString(in),
                            readNullableString(in), readNullableString(in), readNullableString(in));
                }
                JarResource resource = new JarResource(info, appRoot.resolve(path));
                int numDirs = in.readInt();
                for (int i = 0; i < numDirs; ++i) {
                    String dir = in.readUTF();
                    ClassLoadingResource[] existing = resourceDirectoryMap.get(dir);
                    if (existing == null) {
                        resourceDirectoryMap.put(dir, new ClassLoadingResource[] { resource });
                    } else {
                        ClassLoadingResource[] newResources = new ClassLoadingResource[existing.length + 1];
                        System.arraycopy(existing, 0, newResources, 0, existing.length);
                        newResources[existing.length] = resource;
                        resourceDirectoryMap.put(dir, newResources);
                    }
                }
            }
            return new SerializedApplication(new RunnerClassLoader(ClassLoader.getSystemClassLoader(), resourceDirectoryMap),
                    mainClass);
        }
    }

    private static String readNullableString(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            return in.readUTF();
        }
        return null;
    }

    private static void writeJar(DataOutputStream out, Path jar) throws IOException {
        try (JarFile zip = new JarFile(jar.toFile())) {
            Manifest manifest = zip.getManifest();
            if (manifest == null) {
                out.writeBoolean(false);
            } else {
                //write the manifest
                Attributes ma = manifest.getMainAttributes();
                if (ma == null) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    writeNullableString(out, ma.getValue(Attributes.Name.SPECIFICATION_TITLE));
                    writeNullableString(out, ma.getValue(Attributes.Name.SPECIFICATION_VERSION));
                    writeNullableString(out, ma.getValue(Attributes.Name.SPECIFICATION_VENDOR));
                    writeNullableString(out, ma.getValue(Attributes.Name.IMPLEMENTATION_TITLE));
                    writeNullableString(out, ma.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
                    writeNullableString(out, ma.getValue(Attributes.Name.IMPLEMENTATION_VENDOR));
                }
            }

            Set<String> dirs = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            boolean hasDefaultPackge = false;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().contains("/")) {
                    hasDefaultPackge = true;
                } else if (!entry.isDirectory()) {
                    //some jars don't have correct directory entries
                    //so we look at the file paths instead
                    //looking at you h2
                    final int index = entry.getName().lastIndexOf('/');
                    dirs.add(entry.getName().substring(0, index));
                }
            }
            if (hasDefaultPackge) {
                dirs.add("");
            }
            out.writeInt(dirs.size());
            for (String i : dirs) {
                out.writeUTF(i);
            }
        }
    }

    private static void writeNullableString(DataOutputStream out, String string) throws IOException {
        if (string == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeUTF(string);
        }
    }

    private static String relativize(Path applicationRoot, Path jar) {
        Path relative = applicationRoot.relativize(jar);
        if (relative.getName(0).toString().equals("..")) {
            throw new RuntimeException(jar + " was not present in application " + applicationRoot);
        }
        return relative.toString();
    }

}
