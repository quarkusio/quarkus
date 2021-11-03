package io.quarkus.deployment.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.builder.Version;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.io.jar.JarFiles;

/**
 * Utility for Web resource related operations
 */
public class WebJarUtil {

    private static final Logger LOG = Logger.getLogger(WebJarUtil.class);

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String CUSTOM_MEDIA_FOLDER = "META-INF/branding/";
    private static final List<String> IGNORE_LIST = Arrays.asList("logo.png", "favicon.ico", "style.css");
    private static final String CSS = ".css";
    private static final String SNAPSHOT_VERSION = "-SNAPSHOT";

    private WebJarUtil() {
    }

    public static void hotReloadBrandingChanges(CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            ResolvedDependency resourcesArtifact,
            Set<String> hotReloadChanges) throws IOException {

        hotReloadBrandingChanges(curateOutcomeBuildItem, launchMode, resourcesArtifact, hotReloadChanges, true);
    }

    public static void hotReloadBrandingChanges(CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            ResolvedDependency resourcesArtifact,
            Set<String> hotReloadChanges,
            boolean useDefaultQuarkusBranding) throws IOException {

        if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT) && hotReloadChanges != null
                && !hotReloadChanges.isEmpty()) {
            for (String changedResource : hotReloadChanges) {
                if (changedResource.startsWith(CUSTOM_MEDIA_FOLDER)) {
                    ClassLoader classLoader = WebJarUtil.class.getClassLoader();
                    final ResolvedDependency userApplication = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();
                    String fileName = changedResource.replace(CUSTOM_MEDIA_FOLDER, "");
                    // a branding file has changed !
                    String modulename = getModuleOverrideName(resourcesArtifact, fileName);
                    if (IGNORE_LIST.contains(fileName)
                            && isOverride(userApplication.getResolvedPaths(), classLoader, fileName, modulename)) {
                        Path deploymentPath = createResourcesDirectory(userApplication, resourcesArtifact);
                        Path filePath = deploymentPath.resolve(fileName);
                        try (InputStream initialOverride = getOverride(userApplication.getResolvedPaths(), classLoader,
                                fileName, modulename, useDefaultQuarkusBranding);
                                InputStream override = insertVariables(userApplication, initialOverride, fileName)) {
                            if (override != null) {
                                createFile(override, filePath);
                            }
                        }
                    }
                }
            }
        }
    }

    public static Path copyResourcesForDevOrTest(LiveReloadBuildItem liveReloadBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            ResolvedDependency resourcesArtifact,
            String rootFolderInJar)
            throws IOException {
        return copyResourcesForDevOrTest(liveReloadBuildItem, curateOutcomeBuildItem, launchMode, resourcesArtifact,
                rootFolderInJar, true);
    }

    public static Path copyResourcesForDevOrTest(LiveReloadBuildItem liveReloadBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            ResolvedDependency resourcesArtifact,
            String rootFolderInJar,
            boolean useDefaultQuarkusBranding)
            throws IOException {

        rootFolderInJar = normalizeRootFolderInJar(rootFolderInJar);
        final ResolvedDependency userApplication = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();

        Path deploymentPath = createResourcesDirectory(userApplication, resourcesArtifact);

        // Clean if not in dev mode or if the resources jar is a snapshot version
        if (!launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)
                || resourcesArtifact.getVersion().contains(SNAPSHOT_VERSION)
                || (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT) && !liveReloadBuildItem.isLiveReload())) {

            IoUtils.createOrEmptyDir(deploymentPath);
        }

        if (isEmpty(deploymentPath)) {
            ClassLoader classLoader = WebJarUtil.class.getClassLoader();
            for (Path p : resourcesArtifact.getResolvedPaths()) {
                File artifactFile = p.toFile();
                if (artifactFile.isFile()) {
                    // case of a jar file
                    try (JarFile jarFile = JarFiles.create(artifactFile)) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (entry.getName().startsWith(rootFolderInJar)) {
                                String fileName = entry.getName().replace(rootFolderInJar, "");
                                Path filePath = deploymentPath.resolve(fileName);
                                if (entry.isDirectory()) {
                                    Files.createDirectories(filePath);
                                } else {
                                    try (InputStream entryInputStream = jarFile.getInputStream(entry);
                                            InputStream inputStream = insertVariables(userApplication, entryInputStream,
                                                    fileName)) {
                                        String modulename = getModuleOverrideName(resourcesArtifact, fileName);
                                        if (IGNORE_LIST.contains(fileName)
                                                && isOverride(userApplication.getResolvedPaths(), classLoader, fileName,
                                                        modulename)) {
                                            try (InputStream override = insertVariables(userApplication,
                                                    getOverride(userApplication.getResolvedPaths(), classLoader,
                                                            fileName, modulename, useDefaultQuarkusBranding),
                                                    fileName)) {
                                                if (override == null) {
                                                    createFile(inputStream, filePath);
                                                } else {
                                                    createFile(override, filePath); // Override (either developer supplied or Quarkus)
                                                }
                                            }
                                        } else {
                                            createFile(inputStream, filePath);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // case of a directory
                    Path rootFolderToCopy = p.resolve(rootFolderInJar);
                    if (!Files.isDirectory(rootFolderToCopy)) {
                        continue;
                    }

                    Files.walkFileTree(rootFolderToCopy, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(final Path dir,
                                final BasicFileAttributes attrs) throws IOException {
                            Files.createDirectories(deploymentPath.resolve(rootFolderToCopy.relativize(dir)));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(final Path file,
                                final BasicFileAttributes attrs) throws IOException {
                            String fileName = rootFolderToCopy.relativize(file).toString();
                            Path targetFilePath = deploymentPath.resolve(rootFolderToCopy.relativize(file));

                            String modulename = getModuleOverrideName(resourcesArtifact, fileName);
                            if (IGNORE_LIST.contains(fileName)
                                    && isOverride(userApplication.getResolvedPaths(), classLoader, fileName, modulename)) {
                                try (InputStream override = insertVariables(userApplication,
                                        getOverride(userApplication.getResolvedPaths(), classLoader,
                                                fileName, modulename, useDefaultQuarkusBranding),
                                        fileName)) {
                                    if (override == null) {
                                        copyFile(userApplication, file, fileName, targetFilePath);
                                    } else {
                                        createFile(override, targetFilePath); // Override (either developer supplied or Quarkus)
                                    }
                                }
                            } else {
                                copyFile(userApplication, file, fileName, targetFilePath);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }
        return deploymentPath;
    }

    public static Map<String, byte[]> copyResourcesForProduction(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ResolvedDependency artifact,
            String rootFolderInJar) throws IOException {
        return copyResourcesForProduction(curateOutcomeBuildItem, artifact, rootFolderInJar, true);
    }

    public static Map<String, byte[]> copyResourcesForProduction(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ResolvedDependency artifact,
            String rootFolderInJar,
            boolean useDefaultQuarkusBranding) throws IOException {
        rootFolderInJar = normalizeRootFolderInJar(rootFolderInJar);
        final ResolvedDependency userApplication = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();

        Map<String, byte[]> map = new HashMap<>();
        //we are including in a production artifact
        //just stick the files in the generated output
        //we could do this for dev mode as well but then we need to extract them every time

        ClassLoader classLoader = WebJarUtil.class.getClassLoader();
        for (Path p : artifact.getResolvedPaths()) {
            File artifactFile = p.toFile();
            try (JarFile jarFile = JarFiles.create(artifactFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(rootFolderInJar) && !entry.isDirectory()) {
                        String filename = entry.getName().replace(rootFolderInJar, "");
                        try (InputStream inputStream = insertVariables(userApplication, jarFile.getInputStream(entry),
                                filename)) {
                            byte[] content = null;
                            String modulename = getModuleOverrideName(artifact, filename);
                            if (IGNORE_LIST.contains(filename)
                                    && isOverride(userApplication.getResolvedPaths(), classLoader, filename, modulename)) {
                                try (InputStream initialOverride = getOverride(userApplication.getResolvedPaths(), classLoader,
                                        filename, modulename, useDefaultQuarkusBranding);
                                        InputStream resourceAsStream = insertVariables(userApplication, initialOverride,
                                                filename)) {
                                    if (resourceAsStream != null) {
                                        content = IoUtil.readBytes(resourceAsStream); // Override (either developer supplied or Quarkus)
                                    }
                                }
                            }
                            if (content == null) {
                                content = FileUtil.readFileContents(inputStream);
                            }

                            map.put(filename, content);
                        }
                    }
                }
            }
        }
        return map;
    }

    public static void updateFile(Path original, byte[] newContent) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(newContent)) {
            createFile(bais, original);
        }
    }

    public static void updateUrl(Path original, String path, String lineStartsWith, String format) throws IOException {
        String content = new String(Files.readAllBytes(original), StandardCharsets.UTF_8);
        String result = updateUrl(content, path, lineStartsWith, format);
        if (result != null && !result.equals(content)) {
            Files.write(original, result.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String updateUrl(String original, String path, String lineStartsWith, String format) {
        try (Scanner scanner = new Scanner(original)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().startsWith(lineStartsWith)) {
                    String newLine = String.format(format, path);
                    return original.replace(line.trim(), newLine);
                }
            }
        }

        return original;
    }

    public static ResolvedDependency getAppArtifact(CurateOutcomeBuildItem curateOutcomeBuildItem, String groupId,
            String artifactId) {
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            if (dep.getArtifactId().equals(artifactId)
                    && dep.getGroupId().equals(groupId)) {
                return dep;
            }
        }
        throw new RuntimeException("Could not find artifact " + groupId + ":" + artifactId
                + " among the application dependencies");
    }

    private static void copyFile(ResolvedDependency appArtifact, Path file, String fileName, Path targetFilePath)
            throws IOException {
        InputStream providedContent = pathToStream(file).orElse(null);
        if (providedContent != null) {
            Files.copy(insertVariables(appArtifact, providedContent, fileName), targetFilePath);
        }
    }

    private static String getModuleOverrideName(ResolvedDependency artifact, String filename) {
        String type = filename.substring(filename.lastIndexOf("."));
        return artifact.getArtifactId() + type;
    }

    private static InputStream getOverride(PathCollection paths, ClassLoader classLoader, String filename, String modulename,
            boolean useDefaultQuarkusBranding)
            throws IOException {

        // First check if the developer supplied the files
        InputStream overrideStream = getCustomOverride(paths, filename, modulename);
        if (overrideStream == null && useDefaultQuarkusBranding) {
            // Else check if Quarkus has a default branding
            overrideStream = getQuarkusOverride(classLoader, filename, modulename);
        }
        return overrideStream;
    }

    private static InputStream insertVariables(ResolvedDependency appArtifact, InputStream is, String filename)
            throws IOException {
        // Allow replacement of certain values in css
        if (filename.endsWith(CSS)) {
            Config c = ConfigProvider.getConfig();
            String contents = new String(IoUtil.readBytes(is));
            contents = contents.replace("{applicationName}",
                    c.getOptionalValue("quarkus.application.name", String.class)
                            .orElse(appArtifact.getArtifactId()));

            contents = contents.replace("{applicationVersion}",
                    c.getOptionalValue("quarkus.application.version", String.class)
                            .orElse(appArtifact.getVersion()));

            contents = contents.replace("{quarkusVersion}", Version.getVersion());
            is = new ByteArrayInputStream(contents.getBytes());
        }
        return is;
    }

    private static InputStream getCustomOverride(PathCollection paths, String filename, String modulename) {
        // Check if the developer supplied the files
        Path customOverridePath = getCustomOverridePath(paths, filename, modulename);
        if (customOverridePath != null) {
            return pathToStream(customOverridePath).orElse(null);
        }
        return null;
    }

    private static Path getCustomOverridePath(PathCollection paths, String filename, String modulename) {

        // First check if the developer supplied the files
        Iterator<Path> iterator = paths.iterator();
        while (iterator.hasNext()) {
            Path root = iterator.next();
            Path customModuleOverride = root.resolve(CUSTOM_MEDIA_FOLDER + modulename);
            if (Files.exists(customModuleOverride)) {
                return customModuleOverride;
            }
            Path customOverride = root.resolve(CUSTOM_MEDIA_FOLDER + filename);
            if (Files.exists(customOverride)) {
                return customOverride;
            }
        }
        return null;
    }

    private static InputStream getQuarkusOverride(ClassLoader classLoader, String filename, String modulename) {
        // Allow quarkus per module override
        boolean quarkusModuleOverride = fileExistInClasspath(classLoader, CUSTOM_MEDIA_FOLDER + modulename);
        if (quarkusModuleOverride) {
            return classLoader.getResourceAsStream(CUSTOM_MEDIA_FOLDER + modulename);
        }
        boolean quarkusOverride = fileExistInClasspath(classLoader, CUSTOM_MEDIA_FOLDER + filename);
        if (quarkusOverride) {
            return classLoader.getResourceAsStream(CUSTOM_MEDIA_FOLDER + filename);
        }
        return null;
    }

    private static boolean isOverride(PathCollection paths, ClassLoader classLoader, String filename, String modulename) {
        // Check if quarkus override this.
        return isQuarkusOverride(classLoader, filename, modulename) || isCustomOverride(paths, filename, modulename);
    }

    private static boolean isQuarkusOverride(ClassLoader classLoader, String filename, String modulename) {
        // Check if quarkus override this.
        return fileExistInClasspath(classLoader, CUSTOM_MEDIA_FOLDER + modulename)
                || fileExistInClasspath(classLoader, CUSTOM_MEDIA_FOLDER + filename);
    }

    private static boolean isCustomOverride(PathCollection paths, String filename, String modulename) {
        Iterator<Path> iterator = paths.iterator();
        while (iterator.hasNext()) {
            Path root = iterator.next();
            Path customModuleOverride = root.resolve(CUSTOM_MEDIA_FOLDER + modulename);
            if (Files.exists(customModuleOverride)) {
                return true;
            }
            Path customOverride = root.resolve(CUSTOM_MEDIA_FOLDER + filename);
            return Files.exists(customOverride);
        }

        return false;
    }

    private static boolean fileExistInClasspath(ClassLoader classLoader, String filename) {
        URL u = classLoader.getResource(filename);
        return u != null;
    }

    private static Optional<InputStream> pathToStream(Path path) {
        if (Files.exists(path)) {
            try {
                return Optional.of(Files.newInputStream(path));
            } catch (IOException ex) {
                LOG.warn("Could not read override file [" + path + "] - " + ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private static void createFile(InputStream source, Path targetFile) throws IOException {
        FileLock lock = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFile.toString());
            FileChannel channel = fos.getChannel();
            lock = channel.tryLock();
            if (lock != null) {
                IoUtils.copy(fos, source);
            }
        } finally {
            if (lock != null) {
                lock.release();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static Path createResourcesDirectory(ResolvedDependency userApplication, ResolvedDependency resourcesArtifact) {
        try {
            Path path = Paths.get(TMP_DIR, "quarkus", userApplication.getGroupId(),
                    userApplication.getArtifactId(), resourcesArtifact.getGroupId(),
                    resourcesArtifact.getArtifactId(), resourcesArtifact.getVersion());

            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isEmpty(final Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    private static String normalizeRootFolderInJar(String rootFolderInJar) {
        if (rootFolderInJar.endsWith("/")) {
            return rootFolderInJar;
        }

        return rootFolderInJar + "/";
    }
}
