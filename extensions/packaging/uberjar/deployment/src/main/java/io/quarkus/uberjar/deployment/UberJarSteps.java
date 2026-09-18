package io.quarkus.uberjar.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.modular.spi.ModuleWriter;
import io.quarkus.modular.spi.items.ApplicationModuleInfoBuildItem;
import io.quarkus.modular.spi.items.BootModulePathBuildItem;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.quarkus.paths.PathTree;
import io.quarkus.uberjar.spi.UberJarBuildItem;
import io.smallrye.common.io.FileAttributes;
import io.smallrye.common.io.archive.ArchiveBuilder;
import io.smallrye.common.io.archive.ZipOption;

/**
 * The build steps for producing a modular UberJar application.
 */
public final class UberJarSteps {

    private static final Logger log = Logger.getLogger("io.quarkus.uberjar");

    private final UberJarConfig config;

    public UberJarSteps(final UberJarConfig config) {
        this.config = config;
    }

    /**
     * {@return the item for the modular UberJar launcher module}
     */
    @BuildStep
    public BootModulePathBuildItem bootPath() {
        return new BootModulePathBuildItem("io.quarkus.uberjar.launcher");
    }

    /**
     * Build the modular UberJar.
     *
     * @param curateOutcome the curate outcome item (must not be {@code null})
     * @param moduleInfoItem the modular model item (must not be {@code null})
     * @return the built UberJar build item (not {@code null})
     * @throws BuildException if there is a problem during construction
     * @throws IOException if a filesystem operation fails
     */
    @BuildStep
    public UberJarBuildItem build(
            CurateOutcomeBuildItem curateOutcome,
            ApplicationModuleInfoBuildItem moduleInfoItem) throws BuildException, IOException {

        if (!config.enabled()) {
            log.info("Modular UberJar packaging is disabled.");
            return null;
        }

        Path outputDir = config.outputDirectory();
        Files.createDirectories(outputDir);
        Path uberJarPath = outputDir.resolve(config.filename());

        log.infof("Building modular UberJar: %s", uberJarPath.toAbsolutePath());

        AppModuleModel model = moduleInfoItem.model();
        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        Set<String> bootModules = model.bootModules();

        // 1. Locate launcher and bootstrap-boot dependencies from the Curate Outcome
        ResolvedDependency launcherDep = null;
        ResolvedDependency bootShimDep = null;
        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies()) {
            if (dep.getArtifactId().equals("quarkus-uberjar-launcher")) {
                launcherDep = dep;
            } else if (dep.getArtifactId().equals("smallrye-modules-boot")) {
                bootShimDep = dep;
            }
        }

        if (launcherDep == null) {
            throw new BuildException("Could not find 'quarkus-uberjar-launcher' on the application build path");
        }
        if (bootShimDep == null) {
            throw new BuildException("Could not find 'smallrye-modules-boot' on the application build path");
        }

        // 2. Collect and sort launcher system classpath class entries
        List<ClassEntryCoord> classpathEntries = new ArrayList<>();
        collectClasspathEntries(launcherDep, classpathEntries);
        collectClasspathEntries(bootShimDep, classpathEntries);
        classpathEntries.sort(Comparator.comparing(e -> e.resourceName));

        // 3. Collect and sort Boot and App JPMS modules alphabetically by module name
        List<ModuleInfo> sortedBootModules = modulesByName.values().stream()
                .filter(m -> bootModules.contains(m.name()))
                .sorted(Comparator.comparing(ModuleInfo::name))
                .toList();

        List<ModuleInfo> sortedAppModules = modulesByName.values().stream()
                .filter(m -> !bootModules.contains(m.name()))
                .sorted(Comparator.comparing(ModuleInfo::name))
                .toList();

        List<String> bootModulesList = new ArrayList<>();
        List<String> bootJarIndexLines = new ArrayList<>();
        List<String> bootIndexLines = new ArrayList<>();
        List<String> appModulesList = new ArrayList<>();
        List<String> appIndexLines = new ArrayList<>();

        // Create the final composite archive directly in a single pass
        try (ArchiveBuilder ab = ArchiveBuilder.open(uberJarPath, Set.of(
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING), FileAttributes.posixPermissions(0_644))) {
            // [Order 1] Write META-INF/ and META-INF/MANIFEST.MF as the first two entries of the archive
            ab.addDirectory("META-INF/");

            Manifest manifest = new Manifest();
            Attributes mainAttrs = manifest.getMainAttributes();
            mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mainAttrs.put(Attributes.Name.MAIN_CLASS, "io.quarkus.uberjar.launcher.UberJarAppLauncher");
            mainAttrs.put(new Attributes.Name("Add-Exports"), "java.base/jdk.internal.module");

            try (OutputStream os = ab.addEntry("META-INF/MANIFEST.MF", FileAttributes.posixPermissions(0_644))) {
                manifest.write(os);
            }

            // [Order 2] Write System Classloader classpath launcher/bootstrap classes (sorted)
            for (ClassEntryCoord entry : classpathEntries) {
                try (InputStream is = Files.newInputStream(entry.path)) {
                    ab.addEntry(entry.resourceName, is, Set.of(ZipOption.STORED), FileAttributes.posixPermissions(0_644));
                }
            }

            // [Order 3] Manually write the launcher module as a nested JPMS named boot-path JAR (excluding System classpath classes)
            writeLauncherBootJar(ab, launcherDep, "io.quarkus.uberjar.launcher", bootIndexLines, bootJarIndexLines);
            bootModulesList.add("io.quarkus.uberjar.launcher");

            // [Order 4] Write nested boot path JPMS modules (sorted)
            for (ModuleInfo moduleInfo : sortedBootModules) {
                String moduleName = moduleInfo.name();
                if (moduleName.equals("io.quarkus.uberjar.launcher")) {
                    // don't write the launcher twice
                    continue;
                }
                bootModulesList.add(moduleName);
                Manifest modManifest = new Manifest();
                modManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

                String entryName = "boot/" + moduleName + ".jar";
                final String currentModName = moduleName;

                try (ArchiveBuilder nestedAb = ab.addArchive(entryName, Set.of(ZipOption.STORED),
                        FileAttributes.posixPermissions(0_644))) {
                    ModuleWriter.writeModule(moduleInfo, nestedAb, modManifest, true, (rp, builder) -> {
                        String className = getBinaryClassName(rp);
                        if (className != null) {
                            long relativeOffset = builder.lastEntryDataOffset();
                            long size = builder.lastEntryUncompressedSize();
                            bootIndexLines.add(currentModName + ";" + className + ";" + relativeOffset + ";" + size);
                        }
                    });
                }

                // Record the boot JAR's offset and size
                long jarOffset = ab.lastEntryDataOffset();
                long jarSize = ab.lastEntryUncompressedSize();
                bootJarIndexLines.add(moduleName + ";" + jarOffset + ";" + jarSize);
            }

            // [Order 5] Write nested app path JPMS modules (sorted)
            for (ModuleInfo moduleInfo : sortedAppModules) {
                String moduleName = moduleInfo.name();
                appModulesList.add(moduleName);
                Manifest modManifest = new Manifest();
                modManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

                String entryName = "modules/" + moduleName + ".jar";
                try (ArchiveBuilder nestedAb = ab.addArchive(entryName, Set.of(ZipOption.STORED),
                        FileAttributes.posixPermissions(0_644))) {
                    ModuleWriter.writeModule(moduleInfo, nestedAb, modManifest, false);
                }

                long jarOffset = ab.lastEntryDataOffset();
                long jarSize = ab.lastEntryUncompressedSize();
                appIndexLines.add(moduleName + ";" + jarOffset + ";" + jarSize);
            }

            // [Order 6] Write metadata resources at the end of the archive (won't shift pre-computed offsets)
            StringWriter bootModulesWriter = new StringWriter();
            try (PrintWriter pw = new PrintWriter(bootModulesWriter)) {
                for (String mod : bootModulesList) {
                    pw.println(mod);
                }
            }
            ab.addEntry("META-INF/uberjar-boot-modules.txt", bootModulesWriter.toString().getBytes(StandardCharsets.UTF_8),
                    FileAttributes.posixPermissions(0_644));

            StringWriter bootJarIndexWriter = new StringWriter();
            try (PrintWriter pw = new PrintWriter(bootJarIndexWriter)) {
                for (String line : bootJarIndexLines) {
                    pw.println(line);
                }
            }
            ab.addEntry("META-INF/uberjar-boot-jar-index.txt", bootJarIndexWriter.toString().getBytes(StandardCharsets.UTF_8),
                    FileAttributes.posixPermissions(0_644));

            StringWriter bootIndexWriter = new StringWriter();
            try (PrintWriter pw = new PrintWriter(bootIndexWriter)) {
                for (String line : bootIndexLines) {
                    pw.println(line);
                }
            }
            ab.addEntry("META-INF/uberjar-boot-index.txt", bootIndexWriter.toString().getBytes(StandardCharsets.UTF_8),
                    FileAttributes.posixPermissions(0_644));

            StringWriter appModulesWriter = new StringWriter();
            try (PrintWriter pw = new PrintWriter(appModulesWriter)) {
                for (String mod : appModulesList) {
                    pw.println(mod);
                }
            }
            ab.addEntry("META-INF/uberjar-app-modules.txt", appModulesWriter.toString().getBytes(StandardCharsets.UTF_8),
                    FileAttributes.posixPermissions(0_644));

            StringWriter appIndexWriter = new StringWriter();
            try (PrintWriter pw = new PrintWriter(appIndexWriter)) {
                for (String line : appIndexLines) {
                    pw.println(line);
                }
            }
            ab.addEntry("META-INF/uberjar-app-index.txt", appIndexWriter.toString().getBytes(StandardCharsets.UTF_8),
                    FileAttributes.posixPermissions(0_644));

            String appModuleName = model.appModuleInfo().name();
            ab.addEntry("META-INF/uberjar-boot-module.txt", appModuleName.getBytes(StandardCharsets.UTF_8),
                    FileAttributes.posixPermissions(0_644));
        }

        return new UberJarBuildItem(uberJarPath);
    }

    @BuildStep
    public ArtifactResultBuildItem produceArtifactResult(UberJarBuildItem uberJarItem) {
        if (uberJarItem == null) {
            return null;
        }
        return new ArtifactResultBuildItem(uberJarItem.jarPath().toAbsolutePath(), "uberjar", Map.of());
    }

    private void writeLauncherBootJar(ArchiveBuilder ab, ResolvedDependency dep, String moduleName, List<String> bootIndexLines,
            List<String> bootJarIndexLines) throws IOException {
        String entryName = "boot/" + moduleName + ".jar";
        try (ArchiveBuilder nestedAb = ab.addArchive(entryName, Set.of(ZipOption.STORED),
                FileAttributes.posixPermissions(0_644))) {
            PathTree contentTree = dep.getContentTree();
            contentTree.walk(visited -> {
                String rp = visited.getResourceName();
                if (Files.isDirectory(visited.getPath())) {
                    return;
                }
                if (rp.equals("META-INF/MANIFEST.MF") || rp.startsWith("META-INF/maven/")) {
                    return; // skip manifests and maven files
                }
                // Skip System Classloader classpath classes so they are NOT written to the boot JAR
                if (rp.equals("io/quarkus/uberjar/launcher/UberJarAppLauncher.class")
                        || rp.equals("io/quarkus/uberjar/launcher/JarIndexImpl.class")) {
                    return;
                }
                try {
                    try (InputStream is = Files.newInputStream(visited.getPath())) {
                        nestedAb.addEntry(rp, is, Set.of(ZipOption.STORED), FileAttributes.posixPermissions(0_644));
                    }
                    String className = getBinaryClassName(rp);
                    if (className != null) {
                        long relativeOffset = nestedAb.lastEntryDataOffset();
                        long size = nestedAb.lastEntryUncompressedSize();
                        bootIndexLines.add(moduleName + ";" + className + ";" + relativeOffset + ";" + size);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        long jarOffset = ab.lastEntryDataOffset();
        long jarSize = ab.lastEntryUncompressedSize();
        bootJarIndexLines.add(moduleName + ";" + jarOffset + ";" + jarSize);
    }

    private void collectClasspathEntries(ResolvedDependency dep, List<ClassEntryCoord> list) throws IOException {
        PathTree contentTree = dep.getContentTree();
        contentTree.walk(visited -> {
            String rp = visited.getResourceName();
            if (Files.isDirectory(visited.getPath())) {
                return;
            }
            if (rp.equals("META-INF/MANIFEST.MF") || rp.startsWith("META-INF/maven/")) {
                return; // skip manifests and maven files
            }
            if (rp.equals("module-info.class") || rp.endsWith("/module-info.class")) {
                return; // skip module-info at the root of the UberJar!
            }
            list.add(new ClassEntryCoord(rp, visited.getPath()));
        });
    }

    private static String getBinaryClassName(String rp) {
        if (rp.equals("module-info.class") || rp.endsWith("/module-info.class")) {
            return "module-info";
        }
        if (rp.endsWith(".class")) {
            return rp.substring(0, rp.length() - 6).replace('/', '.');
        }
        return null;
    }

    private static class ClassEntryCoord {
        final String resourceName;
        final Path path;

        ClassEntryCoord(String resourceName, Path path) {
            this.resourceName = resourceName;
            this.path = path;
        }
    }
}
