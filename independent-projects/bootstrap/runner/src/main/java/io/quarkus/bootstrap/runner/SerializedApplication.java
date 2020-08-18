package io.quarkus.bootstrap.runner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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

    public static final String META_INF_VERSIONS = "META-INF/versions/";

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

    public static void write(OutputStream outputStream, String mainClass, Path applicationRoot, List<Path> classPath,
            List<Path> parentFirst)
            throws IOException {
        try (DataOutputStream data = new DataOutputStream(outputStream)) {
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeUTF(mainClass);
            data.writeInt(classPath.size());
            for (Path jar : classPath) {
                String relativePath = applicationRoot.relativize(jar).toString().replace("\\", "/");
                data.writeUTF(relativePath);
                writeJar(data, jar);
            }
            Set<String> parentFirstPackages = new HashSet<>();

            for (Path jar : parentFirst) {
                collectPackages(jar, parentFirstPackages);
            }
            data.writeInt(parentFirstPackages.size());
            for (String p : parentFirstPackages) {
                data.writeUTF(p.replace("/", ".").replace("\\", "."));
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
            Set<String> parentFirstPackages = new HashSet<>();
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
            int packages = in.readInt();
            for (int i = 0; i < packages; ++i) {
                parentFirstPackages.add(in.readUTF());
            }
            return new SerializedApplication(
                    new RunnerClassLoader(ClassLoader.getSystemClassLoader(), resourceDirectoryMap, parentFirstPackages),
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

                    if (entry.getName().startsWith(META_INF_VERSIONS)) {
                        //multi release jar
                        //we add all packages here
                        //they may no be relevant for some versions, but that is fine
                        String part = entry.getName().substring(META_INF_VERSIONS.length());
                        int slash = part.indexOf("/");
                        if (slash != -1) {
                            final int subIndex = part.lastIndexOf('/');
                            if (subIndex != slash) {
                                dirs.add(part.substring(slash + 1, subIndex));
                            }
                        }
                    }
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

    private static void collectPackages(Path jar, Set<String> dirs) throws IOException {
        if (Files.isDirectory(jar)) {
            //this can only really happen when testing quarkus itself
            //but is included for completeness
            Files.walkFileTree(jar, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    dirs.add(jar.relativize(dir).toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            try (JarFile zip = new JarFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        //some jars don't have correct  directory entries
                        //so we look at the file paths instead
                        //looking at you h2
                        final int index = entry.getName().lastIndexOf('/');
                        if (index > 0) {
                            dirs.add(entry.getName().substring(0, index));
                        }
                    }
                }
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

}
