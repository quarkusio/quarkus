package io.quarkus.amazon.lambda.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jboss.logging.Logger;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.LegacyJarRequiredBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UpxCompressedBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

/**
 * Generate deployment package zip for lambda.
 *
 */
public class FunctionZipProcessor {
    private static final Logger log = Logger.getLogger(FunctionZipProcessor.class);

    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public void requireLegacy(BuildProducer<LegacyJarRequiredBuildItem> required) {
        required.produce(new LegacyJarRequiredBuildItem());
    }

    /**
     * Function.zip is same as the runner jar plus dependencies in lib/
     * plus anything in src/main/zip.jvm
     *
     * @param target
     * @param artifactResultProducer
     * @param jar
     * @throws Exception
     */
    @BuildStep(onlyIf = IsNormal.class, onlyIfNot = NativeBuild.class)
    public void jvmZip(OutputTargetBuildItem target,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            JarBuildItem jar) throws Exception {
        Path zipPath = target.getOutputDirectory().resolve("function.zip");
        Path zipDir = findJvmZipDir(target.getOutputDirectory());
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(zipPath.toFile())) {
            try (ZipArchiveInputStream zinput = new ZipArchiveInputStream(new FileInputStream(jar.getPath().toFile()))) {
                for (;;) {
                    ZipArchiveEntry entry = zinput.getNextZipEntry();
                    if (entry == null)
                        break;
                    copyZipEntry(zip, zinput, entry);

                }
            }
            if (zipDir != null) {
                try (Stream<Path> paths = Files.walk(zipDir)) {
                    paths.filter(Files::isRegularFile)
                            .forEach(path -> {
                                try {
                                    int mode = Files.isExecutable(path) ? 0755 : 0644;
                                    addZipEntry(zip, path, zipDir.relativize(path).toString().replace('\\', '/'), mode);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                }
            }
            if (!jar.isUberJar()) {
                try (Stream<Path> paths = Files.walk(jar.getLibraryDir())) {
                    paths.filter(Files::isRegularFile)
                            .forEach(path -> {
                                try {
                                    int mode = Files.isExecutable(path) ? 0755 : 0644;
                                    addZipEntry(zip, path,
                                            "lib/" + jar.getLibraryDir().relativize(path).toString().replace('\\', '/'),
                                            mode);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                }
            }
        }
    }

    /**
     * Native function.zip adds anything in src/main/zip.native. If src/main/zip.native/bootstrap
     * exists then the native executable is renamed to "runner".
     *
     * @param target
     * @param artifactResultProducer
     * @param nativeImage
     * @throws Exception
     */
    @BuildStep(onlyIf = { IsNormal.class, NativeBuild.class })
    public void nativeZip(OutputTargetBuildItem target,
            Optional<UpxCompressedBuildItem> upxCompressed, // used to ensure that we work with the compressed native binary if compression was enabled
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            NativeImageBuildItem nativeImage,
            NativeImageRunnerBuildItem nativeImageRunner) throws Exception {
        Path zipDir = findNativeZipDir(target.getOutputDirectory());

        Path zipPath = target.getOutputDirectory().resolve("function.zip");
        Files.deleteIfExists(zipPath);
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(zipPath.toFile())) {
            String executableName = "bootstrap";
            if (zipDir != null) {

                File bootstrap = zipDir.resolve("bootstrap").toFile();
                if (bootstrap.exists()) {
                    executableName = "runner";
                }

                try (Stream<Path> paths = Files.walk(zipDir)) {
                    paths.filter(Files::isRegularFile)
                            .forEach(path -> {
                                try {
                                    if (bootstrap.equals(path.toFile())) {
                                        addZipEntry(zip, path, "bootstrap", 0755);
                                    } else {
                                        int mode = Files.isExecutable(path) ? 0755 : 0644;
                                        addZipEntry(zip, path, zipDir.relativize(path).toString().replace('\\', '/'), mode);
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                }
            }
            addZipEntry(zip, nativeImage.getPath(), executableName, 0755);

            // See https://github.com/oracle/graal/issues/4921
            try (DirectoryStream<Path> sharedLibs = Files.newDirectoryStream(nativeImage.getPath().getParent(),
                    "*.{so,dll}")) {
                sharedLibs.forEach(src -> {
                    try {
                        // In this use case, we can force all libs to be non-executable.
                        addZipEntry(zip, src, src.getFileName().toString(), 0644);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                log.errorf("Could not list files in directory %s. Continuing. Error: %s", nativeImage.getPath().getParent(), e);
            }
        }
    }

    private void copyZipEntry(ZipArchiveOutputStream zip, InputStream zinput, ZipArchiveEntry from) throws Exception {
        ZipArchiveEntry entry = new ZipArchiveEntry(from);
        zip.putArchiveEntry(entry);
        zinput.transferTo(zip);
        zip.closeArchiveEntry();
    }

    private void addZipEntry(ZipArchiveOutputStream zip, Path path, String name, int mode) throws Exception {
        ZipArchiveEntry entry = (ZipArchiveEntry) zip.createArchiveEntry(path.toFile(), name);
        entry.setUnixMode(mode);
        zip.putArchiveEntry(entry);
        try (InputStream i = Files.newInputStream(path)) {
            i.transferTo(zip);
        }
        zip.closeArchiveEntry();
    }

    private static Path findNativeZipDir(Path outputDirectory) {
        Path mainSrc = findMainSourcesRoot(outputDirectory);
        if (mainSrc == null)
            return null;
        Path zipDir = mainSrc.resolve("zip.native");
        return Files.exists(zipDir) && Files.isDirectory(zipDir) ? zipDir : null;
    }

    private static Path findJvmZipDir(Path outputDirectory) {
        Path mainSrc = findMainSourcesRoot(outputDirectory);
        if (mainSrc == null)
            return null;
        Path zipDir = mainSrc.resolve("zip.jvm");
        return Files.exists(zipDir) && Files.isDirectory(zipDir) ? zipDir : null;
    }

    private static Path findMainSourcesRoot(Path outputDirectory) {
        Path currentPath = outputDirectory;
        do {
            Path toCheck = currentPath.resolve(Paths.get("src", "main"));
            if (toCheck.toFile().exists()) {
                return toCheck;
            }
            Path parent = currentPath.getParent();
            if (parent != null && Files.exists(parent)) {
                currentPath = parent;
            } else {
                return null;
            }
        } while (true);
    }

}
