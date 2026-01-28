package io.quarkus.bootstrap.runner;

import static io.quarkus.bootstrap.runner.JarVisitor.visitJar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    public static final String META_INF = "META-INF/";
    public static final String META_INF_VERSIONS = "META-INF/versions/";
    // the files immediately (i.e. not recursively) under these paths should all be indexed
    private static final List<String> FULLY_INDEXED_DIRECTORIES = List.of("", "META-INF", "META-INF/services");

    private static final int MAGIC = 0XF0315432;
    private static final int VERSION = 3;

    private static final ClassLoadingResource[] EMPTY_ARRAY = new ClassLoadingResource[0];
    private static final JarResource SENTINEL = new JarResource(null, Path.of("wqxehxivam"));

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
            List<Path> parentFirst) throws IOException {
        try (DataOutputStream data = new DataOutputStream(outputStream)) {
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeUTF(mainClass);
            data.writeShort(classPath.size());

            Map<String, List<Integer>> fullyIndexedResourcesToCPJarIndex = new LinkedHashMap<>();
            for (int i = 0; i < classPath.size(); i++) {
                Path jar = classPath.get(i);

                FullyIndexedJarVisitor fullyIndexedVisitor = new FullyIndexedJarVisitor(FULLY_INDEXED_DIRECTORIES);
                JarInspectorVisitor jarInspectorVisitor = new JarInspectorVisitor();

                visitJar(jar, fullyIndexedVisitor, jarInspectorVisitor);

                String relativePath = applicationRoot.relativize(jar).toString().replace('\\', '/');
                data.writeUTF(relativePath);

                Attributes manifestAttributes = jarInspectorVisitor.getManifestAttributes();
                if (manifestAttributes == null) {
                    data.writeBoolean(false);
                } else {
                    data.writeBoolean(true);
                    writeNullableString(data, manifestAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE));
                    writeNullableString(data, manifestAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION));
                    writeNullableString(data, manifestAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR));
                    writeNullableString(data, manifestAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE));
                    writeNullableString(data, manifestAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
                    writeNullableString(data, manifestAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR));
                }

                data.writeBoolean(jarInspectorVisitor.isGeneratedBytecode());
                data.writeBoolean(jarInspectorVisitor.isTransformedBytecode());

                data.writeShort(jarInspectorVisitor.getDirectories().size());
                for (String directory : jarInspectorVisitor.getDirectories()) {
                    data.writeUTF(directory);
                }
                if (jarInspectorVisitor.isWriteAllEntries()) {
                    data.writeInt(jarInspectorVisitor.getAllEntries().size());
                    for (String entry : jarInspectorVisitor.getAllEntries()) {
                        data.writeUTF(entry);
                    }
                }

                for (String resource : fullyIndexedVisitor.getFullyIndexedResources()) {
                    fullyIndexedResourcesToCPJarIndex.computeIfAbsent(resource, s -> new ArrayList<>()).add(i);
                }
            }

            ParentFirstPackageVisitor parentFirstPackageVisitor = new ParentFirstPackageVisitor();
            for (Path jar : parentFirst) {
                visitJar(jar, parentFirstPackageVisitor);
            }
            data.writeShort(parentFirstPackageVisitor.getParentFirstPackages().size());
            for (String p : parentFirstPackageVisitor.getParentFirstPackages()) {
                data.writeUTF(p.replace('/', '.').replace('\\', '.'));
            }

            data.writeShort(fullyIndexedResourcesToCPJarIndex.size());
            for (Map.Entry<String, List<Integer>> entry : fullyIndexedResourcesToCPJarIndex.entrySet()) {
                data.writeUTF(entry.getKey());
                data.writeShort(entry.getValue().size());
                for (Integer index : entry.getValue()) {
                    data.writeShort(index);
                }
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
            ResourceDirectoryTracker resourceDirectoryTracker = new ResourceDirectoryTracker();
            int numPaths = in.readUnsignedShort();
            ClassLoadingResource[] allClassLoadingResources = new ClassLoadingResource[numPaths];
            ClassLoadingResource generatedBytecodeClassLoadingResource = null;
            Set<String> generatedBytecode = Set.of();
            ClassLoadingResource transformedBytecodeClassLoadingResource = null;
            Set<String> transformedBytecode = Set.of();
            for (int pathCount = 0; pathCount < numPaths; pathCount++) {
                String path = in.readUTF();
                boolean hasManifest = in.readBoolean();
                ManifestInfo info = null;
                if (hasManifest) {
                    info = new ManifestInfo(readNullableString(in), readNullableString(in), readNullableString(in),
                            readNullableString(in), readNullableString(in), readNullableString(in));
                }
                JarResource resource = new JarResource(info, appRoot.resolve(path));
                boolean generatedBytecodeJar = in.readBoolean();
                boolean transformedBytecodeJar = in.readBoolean();
                if (generatedBytecodeJar) {
                    generatedBytecodeClassLoadingResource = resource;
                } else if (transformedBytecodeJar) {
                    transformedBytecodeClassLoadingResource = resource;
                }
                allClassLoadingResources[pathCount] = resource;
                int numDirs = in.readUnsignedShort();
                for (int i = 0; i < numDirs; ++i) {
                    String dir = in.readUTF();
                    int j = dir.indexOf('/');
                    while (j >= 0) {
                        resourceDirectoryTracker.addResourceDir(dir.substring(0, j), resource);
                        j = dir.indexOf('/', j + 1);
                    }
                    resourceDirectoryTracker.addResourceDir(dir, resource);
                }
                if (generatedBytecodeJar || transformedBytecodeJar) {
                    int numEntries = in.readInt();
                    // let's make the Set as compact as we can
                    Set<String> entries = new HashSet<>((int) Math.ceil(numEntries / 0.75f));
                    for (int i = 0; i < numEntries; ++i) {
                        entries.add(in.readUTF());
                    }
                    if (generatedBytecodeJar) {
                        generatedBytecode = entries;
                    } else if (transformedBytecodeJar) {
                        transformedBytecode = entries;
                    }
                }
            }
            int packages = in.readUnsignedShort();
            Set<String> parentFirstPackages = new HashSet<>((int) Math.ceil(packages / 0.75f));
            for (int i = 0; i < packages; ++i) {
                parentFirstPackages.add(in.readUTF());
            }
            // this map is populated correctly because the JarResource entries are added to allClassLoadingResources
            // in the same order as the classpath was written during the writing of the index
            int directlyIndexedSize = in.readUnsignedShort();
            Map<String, ClassLoadingResource[]> fullyIndexedResourcesIndexMap = new HashMap<>(
                    (int) Math.ceil(directlyIndexedSize / 0.75f));
            for (int i = 0; i < directlyIndexedSize; i++) {
                String resource = in.readUTF();
                int indexesSize = in.readUnsignedShort();
                ClassLoadingResource[] matchingResources = new ClassLoadingResource[indexesSize];
                for (int j = 0; j < indexesSize; j++) {
                    matchingResources[j] = allClassLoadingResources[in.readUnsignedShort()];
                }
                fullyIndexedResourcesIndexMap.put(resource, matchingResources);
            }
            RunnerClassLoader runnerClassLoader = new RunnerClassLoader(ClassLoader.getSystemClassLoader(),
                    resourceDirectoryTracker.getResult(), parentFirstPackages,
                    FULLY_INDEXED_DIRECTORIES, fullyIndexedResourcesIndexMap,
                    generatedBytecodeClassLoadingResource, generatedBytecode,
                    transformedBytecodeClassLoadingResource, transformedBytecode);
            for (ClassLoadingResource classLoadingResource : allClassLoadingResources) {
                classLoadingResource.init();
            }
            return new SerializedApplication(runnerClassLoader, mainClass);
        }
    }

    private static String readNullableString(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            return in.readUTF();
        }
        return null;
    }

    private static class JarInspectorVisitor implements JarVisitor {

        private Attributes manifestAttributes;
        private boolean generatedBytecode;
        private boolean transformedBytecode;
        private final Set<String> directories = new LinkedHashSet<>();
        private final Set<String> allEntries = new LinkedHashSet<>();

        public Attributes getManifestAttributes() {
            return manifestAttributes;
        }

        public boolean isGeneratedBytecode() {
            return generatedBytecode;
        }

        public boolean isTransformedBytecode() {
            return transformedBytecode;
        }

        public Set<String> getDirectories() {
            return directories;
        }

        public boolean isWriteAllEntries() {
            return generatedBytecode || transformedBytecode;
        }

        public Set<String> getAllEntries() {
            return allEntries;
        }

        @Override
        public void preVisit(Path jar) {
            generatedBytecode = jar.endsWith("generated-bytecode.jar");
            transformedBytecode = jar.endsWith("transformed-bytecode.jar");
        }

        @Override
        public void visitJarManifest(Path jar, Manifest manifest) {
            manifestAttributes = manifest.getMainAttributes();
        }

        @Override
        public void visitJarFileEntry(JarFile jarFile, ZipEntry entry) {
            if (isWriteAllEntries()) {
                allEntries.add(entry.getName());
            }

            if (!entry.getName().contains("/")) {
                // we add the default package
                directories.add("");
            } else {
                // some jars don't have correct directory entries
                // so we look at the file paths instead
                // looking at you h2
                final int index = entry.getName().lastIndexOf('/');
                directories.add(entry.getName().substring(0, index));

                if (entry.getName().startsWith(META_INF_VERSIONS)) {
                    // multi release jar
                    // we add all packages here
                    // they may not be relevant for some versions, but that is fine
                    String part = entry.getName().substring(META_INF_VERSIONS.length());
                    int slash = part.indexOf("/");
                    if (slash != -1) {
                        final int subIndex = part.lastIndexOf('/');
                        if (subIndex != slash) {
                            directories.add(part.substring(slash + 1, subIndex));
                        }
                    }
                }
            }
        }
    }

    private static class ParentFirstPackageVisitor implements JarVisitor {

        private final Set<String> parentFirstPackages = new HashSet<>();

        @Override
        public void visitRegularDirectory(Path jar, Path directory, String relativePath) {
            if (relativePath.startsWith(META_INF)) {
                return;
            }

            parentFirstPackages.add(relativePath);
        }

        @Override
        public void visitJarFileEntry(JarFile jarFile, ZipEntry fileEntry) {
            if (fileEntry.getName().startsWith(META_INF)) {
                return;
            }

            // some jars don't have correct  directory entries
            // so we look at the file paths instead of the directories
            // looking at you h2
            final int index = fileEntry.getName().lastIndexOf('/');
            if (index > 0) {
                parentFirstPackages.add(fileEntry.getName().substring(0, index));
            }
        }

        public Set<String> getParentFirstPackages() {
            return parentFirstPackages;
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

    /**
     * This class is to build up the final directory to Resource map
     * The idea here is that directories will only be contained by a single resource, so we optimistically
     * create a single item array when we encounter a directory for the first time.
     * However, when a resource is added for the same directory, we switch to using a Set to track the resources
     * of that directory and then at the end use those sets to override to create the final array of resources.
     * The reason for doing it this way to only create Sets when needed (which is only a fraction of the cases)
     */
    private static class ResourceDirectoryTracker {
        private final Map<String, ClassLoadingResource[]> result = new HashMap<>();
        private final Map<String, Set<ClassLoadingResource>> overrides = new HashMap<>();

        void addResourceDir(String dir, JarResource resource) {
            ClassLoadingResource[] existing = result.get(dir);
            if (existing == null) {
                // this is the first the dir was ever tracked
                result.put(dir, new JarResource[] { resource });
            } else {
                ClassLoadingResource existingResource = existing[0];
                if (existingResource.equals(resource)) {
                    // we don't need to do anything as the resource has already been tracked and an attempt
                    // to add it again was made
                } else {
                    Set<ClassLoadingResource> dirOverrides = overrides.get(dir);
                    if (dirOverrides == null) {
                        // we need to create the override set as this is the first time we find a resource for the dir
                        // that is not the same as the one in the array
                        dirOverrides = new LinkedHashSet<>(2);
                        dirOverrides.add(existingResource);
                        dirOverrides.add(resource);
                        overrides.put(dir, dirOverrides);

                        //replace the value in the original array with a sentinel in order to allow for quick comparisons
                        existing[0] = SENTINEL;
                    } else {
                        // in this case, overrides has already been created in a previous invocation so all we need to
                        // do is add the new resource
                        dirOverrides.add(resource);
                    }
                }
            }
        }

        Map<String, ClassLoadingResource[]> getResult() {
            overrides.forEach(this::addToResult);
            return result;
        }

        private void addToResult(String dir, Set<? extends ClassLoadingResource> jarResources) {
            result.put(dir, jarResources.toArray(EMPTY_ARRAY));
        }
    }
}
