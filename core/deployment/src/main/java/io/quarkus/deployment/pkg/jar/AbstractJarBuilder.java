package io.quarkus.deployment.pkg.jar;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import io.quarkus.deployment.ResolvedJVMRequirements;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;

public abstract class AbstractJarBuilder<T extends BuildItem> implements JarBuilder<T> {

    private static final Logger LOG = Logger.getLogger(AbstractJarBuilder.class);

    protected final CurateOutcomeBuildItem curateOutcome;
    protected final OutputTargetBuildItem outputTarget;
    protected final ApplicationInfoBuildItem applicationInfo;
    protected final PackageConfig packageConfig;
    protected final MainClassBuildItem mainClass;
    protected final ApplicationArchivesBuildItem applicationArchives;
    protected final TransformedClassesBuildItem transformedClasses;
    protected final List<GeneratedClassBuildItem> generatedClasses;
    protected final List<GeneratedResourceBuildItem> generatedResources;
    protected final Set<ArtifactKey> removedArtifactKeys;
    protected final ResolvedJVMRequirements jvmRequirements;

    public AbstractJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> removedArtifactKeys,
            ResolvedJVMRequirements jvmRequirements) {
        this.curateOutcome = curateOutcome;
        this.outputTarget = outputTarget;
        this.applicationInfo = applicationInfo;
        this.packageConfig = packageConfig;
        this.mainClass = mainClass;
        this.applicationArchives = applicationArchives;
        this.transformedClasses = transformedClasses;
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.removedArtifactKeys = removedArtifactKeys;
        this.jvmRequirements = jvmRequirements;
    }

    /**
     * Copy files from {@code archive} to {@code fs}, filtering out service providers into the given map.
     *
     * @param archive the root application archive
     * @param archiveCreator the archive creator
     * @param services the services map
     * @throws IOException if an error occurs
     */
    protected static void copyFiles(ApplicationArchive archive, ArchiveCreator archiveCreator,
            Map<String, List<byte[]>> services,
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
                                archiveCreator.addDirectory(relativePath);
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
                                    archiveCreator.addFileIfNotExists(visit.getPath(), relativePath);
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

    protected void copyCommonContent(ArchiveCreator archiveCreator,
            Map<String, List<byte[]>> concatenatedEntries,
            Predicate<String> ignoredEntriesPredicate)
            throws IOException {

        //TODO: this is probably broken in gradle
        //        if (Files.exists(augmentOutcome.getConfigDir())) {
        //            copyFiles(augmentOutcome.getConfigDir(), runnerZipFs, services);
        //        }
        for (Set<TransformedClassesBuildItem.TransformedClass> transformed : transformedClasses
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass i : transformed) {
                if (i.getData() != null) {
                    archiveCreator.addFile(i.getData(), i.getFileName());
                }
            }
        }
        for (GeneratedClassBuildItem i : generatedClasses) {
            String fileName = fromClassNameToResourceName(i.internalName());
            archiveCreator.addFileIfNotExists(i.getClassData(), fileName, ArchiveCreator.CURRENT_APPLICATION);
        }

        for (GeneratedResourceBuildItem i : generatedResources) {
            if (ignoredEntriesPredicate.test(i.getName())) {
                continue;
            }
            if (i.getName().startsWith("META-INF/services/")) {
                concatenatedEntries.computeIfAbsent(i.getName(), (u) -> new ArrayList<>()).add(i.getData());
                continue;
            }
            archiveCreator.addFileIfNotExists(i.getData(), i.getName(), ArchiveCreator.CURRENT_APPLICATION);
        }

        copyFiles(applicationArchives.getRootArchive(), archiveCreator, concatenatedEntries, ignoredEntriesPredicate);

        for (Map.Entry<String, List<byte[]>> entry : concatenatedEntries.entrySet()) {
            archiveCreator.addFile(entry.getValue(), entry.getKey());
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
    protected static void generateManifest(ArchiveCreator archiveCreator, final String classPath, PackageConfig config,
            ResolvedDependency appArtifact,
            ResolvedJVMRequirements jvmRequirements,
            String mainClassName,
            ApplicationInfoBuildItem applicationInfo)
            throws IOException {
        final Manifest manifest = new Manifest();

        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        jvmRequirements.renderAddOpensElementToJarManifest(attributes);

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

        archiveCreator.addManifest(manifest);
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

    protected static String toUri(Path path) {
        if (path.isAbsolute()) {
            return path.toUri().getPath();
        }
        if (path.getNameCount() == 0) {
            return "";
        }
        return toUri(new StringBuilder(), path, 0).toString();
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
