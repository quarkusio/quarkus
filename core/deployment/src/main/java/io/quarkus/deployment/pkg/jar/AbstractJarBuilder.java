package io.quarkus.deployment.pkg.jar;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;

public abstract class AbstractJarBuilder<T extends BuildItem> implements JarBuilder<T> {

    private static final Logger LOG = Logger.getLogger(AbstractJarBuilder.class);

    protected static FileSystem createNewZip(Path runnerJar, PackageConfig config) throws IOException {
        boolean useUncompressedJar = !config.jar().compress();
        if (useUncompressedJar) {
            return ZipUtils.newZip(runnerJar, Map.of("compressionMethod", "STORED"));
        }
        return ZipUtils.newZip(runnerJar);
    }

    /**
     * Copy files from {@code archive} to {@code fs}, filtering out service providers into the given map.
     *
     * @param archive the root application archive
     * @param fs the destination filesystem
     * @param services the services map
     * @throws IOException if an error occurs
     */
    protected static void copyFiles(ApplicationArchive archive, FileSystem fs, Map<String, List<byte[]>> services,
            Predicate<String> ignoredEntriesPredicate) throws IOException {
        try {
            archive.accept(tree -> {
                tree.walk(new PathVisitor() {
                    @Override
                    public void visitPath(PathVisit visit) {
                        final Path file = visit.getRoot().relativize(visit.getPath());
                        final String relativePath = toUri(file);
                        if (relativePath.isEmpty() || ignoredEntriesPredicate.test(relativePath)) {
                            return;
                        }
                        try {
                            if (Files.isDirectory(visit.getPath())) {
                                addDirectory(fs, relativePath);
                            } else {
                                if (relativePath.startsWith("META-INF/services/") && relativePath.length() > 18
                                        && services != null) {
                                    final byte[] content;
                                    try {
                                        content = Files.readAllBytes(visit.getPath());
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                    services.computeIfAbsent(relativePath, (u) -> new ArrayList<>()).add(content);
                                } else if (!relativePath.equals("META-INF/INDEX.LIST")) {
                                    //TODO: auto generate INDEX.LIST
                                    //this may have implications for Camel though, as they change the layout
                                    //also this is only really relevant for the thin jar layout
                                    Path target = fs.getPath(relativePath);
                                    if (!Files.exists(target)) {
                                        Files.copy(visit.getPath(), target, StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            });
        } catch (RuntimeException re) {
            final Throwable cause = re.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw re;
        }
    }

    protected static void copyCommonContent(FileSystem runnerZipFs, Map<String, List<byte[]>> concatenatedEntries,
            ApplicationArchivesBuildItem appArchives, TransformedClassesBuildItem transformedClassesBuildItem,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources, Map<String, String> seen,
            Predicate<String> ignoredEntriesPredicate)
            throws IOException {

        //TODO: this is probably broken in gradle
        //        if (Files.exists(augmentOutcome.getConfigDir())) {
        //            copyFiles(augmentOutcome.getConfigDir(), runnerZipFs, services);
        //        }
        for (Set<TransformedClassesBuildItem.TransformedClass> transformed : transformedClassesBuildItem
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass i : transformed) {
                if (i.getData() != null) {
                    Path target = runnerZipFs.getPath(i.getFileName());
                    handleParent(runnerZipFs, i.getFileName(), seen);
                    try (final OutputStream out = Files.newOutputStream(target)) {
                        out.write(i.getData());
                    }
                    seen.put(i.getFileName(), "Current Application");
                }
            }
        }
        for (GeneratedClassBuildItem i : generatedClasses) {
            String fileName = fromClassNameToResourceName(i.getName());
            seen.put(fileName, "Current Application");
            Path target = runnerZipFs.getPath(fileName);
            handleParent(runnerZipFs, fileName, seen);
            if (Files.exists(target)) {
                continue;
            }
            try (final OutputStream os = Files.newOutputStream(target)) {
                os.write(i.getClassData());
            }
        }

        for (GeneratedResourceBuildItem i : generatedResources) {
            if (ignoredEntriesPredicate.test(i.getName())) {
                continue;
            }
            Path target = runnerZipFs.getPath(i.getName());
            handleParent(runnerZipFs, i.getName(), seen);
            if (Files.exists(target)) {
                continue;
            }
            if (i.getName().startsWith("META-INF/services/")) {
                concatenatedEntries.computeIfAbsent(i.getName(), (u) -> new ArrayList<>()).add(i.getData());
            } else {
                try (final OutputStream os = Files.newOutputStream(target)) {
                    os.write(i.getData());
                }
            }
        }

        copyFiles(appArchives.getRootArchive(), runnerZipFs, concatenatedEntries, ignoredEntriesPredicate);

        for (Map.Entry<String, List<byte[]>> entry : concatenatedEntries.entrySet()) {
            try (final OutputStream os = Files.newOutputStream(runnerZipFs.getPath(entry.getKey()))) {
                // TODO: Handle merging of XMLs
                for (byte[] i : entry.getValue()) {
                    os.write(i);
                    os.write('\n');
                }
            }
        }
    }

    /**
     * Manifest generation is quite simple : we just have to push some attributes in manifest.
     * However, it gets a little more complex if the manifest preexists.
     * So we first try to see if a manifest exists, and otherwise create a new one.
     *
     * <b>BEWARE</b> this method should be invoked after file copy from target/classes and so on.
     * Otherwise, this manifest manipulation will be useless.
     */
    protected static void generateManifest(FileSystem runnerZipFs, final String classPath, PackageConfig config,
            ResolvedDependency appArtifact,
            String mainClassName,
            ApplicationInfoBuildItem applicationInfo)
            throws IOException {
        final Path manifestPath = runnerZipFs.getPath("META-INF", "MANIFEST.MF");
        final Manifest manifest = new Manifest();
        if (Files.exists(manifestPath)) {
            try (InputStream is = Files.newInputStream(manifestPath)) {
                manifest.read(is);
            }
            Files.delete(manifestPath);
        } else {
            Files.createDirectories(runnerZipFs.getPath("META-INF"));
        }
        Files.createDirectories(manifestPath.getParent());
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        // JDK 24+ needs --add-opens=java.base/java.lang=ALL-UNNAMED for org.jboss.JDKSpecific.ThreadAccess.clearThreadLocals()
        attributes.put(new Attributes.Name("Add-Opens"), "java.base/java.lang");

        for (Map.Entry<String, String> attribute : config.jar().manifest().attributes().entrySet()) {
            attributes.putValue(attribute.getKey(), attribute.getValue());
        }
        if (attributes.containsKey(Attributes.Name.CLASS_PATH)) {
            LOG.warn(
                    "A CLASS_PATH entry was already defined in your MANIFEST.MF or using the property quarkus.package.jar.manifest.attributes.\"Class-Path\". Quarkus has overwritten this existing entry.");
        }
        attributes.put(Attributes.Name.CLASS_PATH, classPath);
        if (attributes.containsKey(Attributes.Name.MAIN_CLASS)) {
            String existingMainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
            if (!mainClassName.equals(existingMainClass)) {
                LOG.warn(
                        "A MAIN_CLASS entry was already defined in your MANIFEST.MF or using the property quarkus.package.jar.manifest.attributes.\"Main-Class\". Quarkus has overwritten your existing entry.");
            }
        }
        attributes.put(Attributes.Name.MAIN_CLASS, mainClassName);
        if (config.jar().manifest().addImplementationEntries()
                && !attributes.containsKey(Attributes.Name.IMPLEMENTATION_TITLE)) {
            String name = ApplicationInfoBuildItem.UNSET_VALUE.equals(applicationInfo.getName())
                    ? appArtifact.getArtifactId()
                    : applicationInfo.getName();
            attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, name);
        }
        if (config.jar().manifest().addImplementationEntries()
                && !attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String version = ApplicationInfoBuildItem.UNSET_VALUE.equals(applicationInfo.getVersion())
                    ? appArtifact.getVersion()
                    : applicationInfo.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        }
        for (String sectionName : config.jar().manifest().sections().keySet()) {
            for (Map.Entry<String, String> entry : config.jar().manifest().sections().get(sectionName).entrySet()) {
                Attributes attribs = manifest.getEntries().computeIfAbsent(sectionName, k -> new Attributes());
                attribs.putValue(entry.getKey(), entry.getValue());
            }
        }
        try (final OutputStream os = Files.newOutputStream(manifestPath)) {
            manifest.write(os);
        }
    }

    /**
     * Indicates whether the given dependency should be included or not.
     * <p>
     * A dependency should be included if it is a jar file and:
     * <p>
     * <ul>
     * <li>The dependency is not optional or</li>
     * <li>The dependency is part of the optional dependencies to include or</li>
     * <li>The optional dependencies to include are absent</li>
     * </ul>
     *
     * @param appDep the dependency to test.
     * @param optionalDependencies the optional dependencies to include into the final package.
     * @return {@code true} if the dependency should be included, {@code false} otherwise.
     */
    protected static boolean includeAppDependency(ResolvedDependency appDep, Optional<Set<ArtifactKey>> optionalDependencies,
            Set<ArtifactKey> removedArtifacts) {
        if (!appDep.isJar()) {
            return false;
        }
        if (appDep.isOptional()) {
            return optionalDependencies.map(appArtifactKeys -> appArtifactKeys.contains(appDep.getKey()))
                    .orElse(true);
        }
        if (removedArtifacts.contains(appDep.getKey())) {
            return false;
        }
        return true;
    }

    protected static String suffixToClassifier(String suffix) {
        return suffix.startsWith("-") ? suffix.substring(1) : suffix;
    }

    protected static void addDirectory(FileSystem fs, final String relativePath)
            throws IOException {
        final Path targetDir = fs.getPath(relativePath);
        try {
            Files.createDirectory(targetDir);
        } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(targetDir)) {
                throw e;
            }
        }
    }

    protected static String toUri(Path path) {
        if (path.isAbsolute()) {
            return path.toUri().getPath();
        }
        if (path.getNameCount() == 0) {
            return "";
        }
        return toUri(new StringBuilder(), path, 0).toString();
    }

    private static void handleParent(FileSystem runnerZipFs, String fileName, Map<String, String> seen) throws IOException {
        for (int i = 0; i < fileName.length(); ++i) {
            if (fileName.charAt(i) == '/') {
                String dir = fileName.substring(0, i);
                if (!seen.containsKey(dir)) {
                    seen.put(dir, "Current Application");
                    Files.createDirectories(runnerZipFs.getPath(dir));
                }
            }
        }
    }

    private static StringBuilder toUri(StringBuilder b, Path path, int seg) {
        b.append(path.getName(seg));
        if (seg < path.getNameCount() - 1) {
            b.append('/');
            toUri(b, path, seg + 1);
        }
        return b;
    }
}
