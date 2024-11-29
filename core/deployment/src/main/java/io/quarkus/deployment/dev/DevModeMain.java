
package io.quarkus.deployment.dev;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.PathList;

/**
 * The main entry point for the dev mojo execution
 */
public class DevModeMain implements Closeable {

    public static final String DEV_MODE_CONTEXT = "META-INF/dev-mode-context.dat";
    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private final DevModeContext context;
    private final ApplicationModel appModel;

    private volatile CuratedApplication curatedApplication;
    private Closeable realCloseable;

    public DevModeMain(DevModeContext context) {
        this(context, null);
    }

    public DevModeMain(DevModeContext context, ApplicationModel appModel) {
        this.context = context;
        this.appModel = appModel;
    }

    public static void main(String... args) throws Exception {
        try (InputStream devModeCp = DevModeMain.class.getClassLoader().getResourceAsStream(DEV_MODE_CONTEXT)) {
            DevModeContext context;
            try {
                context = (DevModeContext) new ObjectInputStream(new DataInputStream(devModeCp)).readObject();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to deserialize the dev mode context. Does the Quarkus plugin version match the version of Quarkus that is in use?",
                        e);
            }
            context.setArgs(args);
            DevModeMain devModeMain = new DevModeMain(context);
            devModeMain.start();
        }
    }

    public void start() throws Exception {
        //propagate system props
        propagateSystemProperties();

        try {
            QuarkusBootstrap.Builder bootstrapBuilder = QuarkusBootstrap.builder()
                    .setApplicationRoot(getApplicationBuildDirs())
                    .setExistingModel(appModel)
                    .setIsolateDeployment(true)
                    .setLocalProjectDiscovery(context.isLocalProjectDiscovery())
                    .addAdditionalDeploymentArchive(getThisClassOrigin())
                    .setBaseName(context.getBaseName())
                    .setMode(context.getMode());
            if (context.getDevModeRunnerJarFile() != null) {
                bootstrapBuilder.setTargetDirectory(context.getDevModeRunnerJarFile().getParentFile().toPath());
            } else if (context.getApplicationRoot().getTargetDir() != null) {
                bootstrapBuilder.setTargetDirectory(Path.of(context.getApplicationRoot().getTargetDir()));
            }
            bootstrapBuilder.setProjectRoot(resolveProjectRoot());
            for (ArtifactKey i : context.getLocalArtifacts()) {
                bootstrapBuilder.addLocalArtifact(i);
            }

            linkDotEnvFile();

            Properties buildSystemProperties = new Properties();
            buildSystemProperties.putAll(context.getBuildSystemProperties());
            bootstrapBuilder.setBuildSystemProperties(buildSystemProperties);

            Map<String, Object> map = new HashMap<>();
            map.put(DevModeContext.class.getName(), context);
            map.put(DevModeType.class.getName(), DevModeType.LOCAL);
            curatedApplication = bootstrapBuilder.setTest(context.isTest()).build().bootstrap();
            realCloseable = (Closeable) curatedApplication.runInAugmentClassLoader(
                    context.getAlternateEntryPoint() == null ? IsolatedDevModeMain.class.getName()
                            : context.getAlternateEntryPoint(),
                    map);
        } catch (Throwable t) {
            log.error("Quarkus dev mode failed to start", t);
            throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t));
            //System.exit(1);
        }
    }

    private PathList getApplicationBuildDirs() {
        final String classesDir = context.getApplicationRoot().getMain().getClassesPath();
        final String resourcesOutputDir = context.getApplicationRoot().getMain().getResourcesOutputPath();
        if (resourcesOutputDir == null || resourcesOutputDir.equals(classesDir)) {
            return toListOfExistingOrEmpty(Path.of(classesDir));
        }
        return toListOfExistingOrEmpty(Path.of(classesDir), Path.of(resourcesOutputDir));
    }

    private static PathList toListOfExistingOrEmpty(Path p1, Path p2) {
        return !Files.exists(p1) ? toListOfExistingOrEmpty(p2)
                : (!Files.exists(p2) ? toListOfExistingOrEmpty(p1) : PathList.of(p1, p2));
    }

    /**
     * Returns a {@link PathList} containing the path if it exists, otherwise returns an empty {@link PathList}.
     *
     * @param path path
     * @return {@link PathList} containing the path if it exists, otherwise returns an empty {@link PathList}
     */
    private static PathList toListOfExistingOrEmpty(Path path) {
        return Files.exists(path) ? PathList.of(path) : PathList.empty();
    }

    /**
     * Returns the classpath element containing this class
     *
     * @return classpath element containing this class
     * @throws URISyntaxException in case of a failure
     */
    private Path getThisClassOrigin() throws URISyntaxException {
        URL thisArchive = getClass().getResource(DevModeMain.class.getSimpleName() + ".class");
        int endIndex = thisArchive.getPath().indexOf("!");
        if (endIndex != -1) {
            return Path.of(new URI(thisArchive.getPath().substring(0, endIndex)));
        }
        Path path = Path.of(thisArchive.toURI());
        path = path.getParent();
        for (char i : DevModeMain.class.getName().toCharArray()) {
            if (i == '.') {
                path = path.getParent();
            }
        }

        return path;
    }

    private void propagateSystemProperties() {
        for (Map.Entry<String, String> i : context.getSystemProperties().entrySet()) {
            System.getProperties().putIfAbsent(i.getKey(), i.getValue());
        }
    }

    private Path resolveProjectRoot() {
        final Path projectRoot;
        if (context.getProjectDir() != null) {
            projectRoot = context.getProjectDir().toPath();
        } else if (context.getApplicationRoot().getProjectDirectory() != null) {
            projectRoot = Path.of(context.getApplicationRoot().getProjectDirectory());
        } else {
            projectRoot = new File(".").toPath();
        }
        return projectRoot;
    }

    // links the .env file to the directory where the process is running
    // this is done because for the .env file to take effect, it needs to be
    // in the same directory as the running process
    private void linkDotEnvFile() {
        File projectDir = context.getProjectDir();
        if (projectDir == null) { // this is the case for QuarkusDevModeTest
            return;
        }
        Path currentDir = Paths.get("").toAbsolutePath().normalize();
        if (projectDir.toPath().equals(currentDir)) {
            // the current directory is the same as the project directory so there is no need to copy the file as it's already in the proper location
            // see https://github.com/quarkusio/quarkus/issues/8812
            return;
        }

        Path dotEnvPath = projectDir.toPath().resolve(".env");
        if (Files.exists(dotEnvPath)) {
            Path link = currentDir.resolve(".env");
            silentDeleteFile(link);
            try {
                // create a symlink to ensure that user updates to the file have the expected effect in dev-mode
                try {
                    Files.createSymbolicLink(link, dotEnvPath);
                } catch (FileSystemException e) {
                    // on Windows fall back to mklink if symlink cannot be created via Files API (due to insufficient permissions)
                    // see https://github.com/quarkusio/quarkus/issues/8297
                    if (SystemUtils.IS_OS_WINDOWS) {
                        log.debug("Falling back to mklink on Windows after FileSystemException", e);
                        makeHardLinkWindowsFallback(link, dotEnvPath);
                    } else {
                        throw e;
                    }
                }
            } catch (IOException | InterruptedException e) {
                log.warn("Unable to link .env file", e);
            }
            link.toFile().deleteOnExit();
        }
    }

    private void silentDeleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ignored) {

        }
    }

    private void makeHardLinkWindowsFallback(Path link, Path dotEnvPath) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("cmd.exe", "/C", "mklink", "/H", link.toString(), dotEnvPath.toString())
                .redirectOutput(new File("NUL"))
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
        try {
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            ProcessUtil.streamErrorTo(new PrintStream(errStream), process);
            int exitValue = process.waitFor();
            if (exitValue > 0) {
                throw new IOException(
                        "mklink /H execution failed with exit code " + exitValue + ": " + new String(errStream.toByteArray()));
            }
        } finally {
            process.destroy();
        }
    }

    @Override
    public void close() throws IOException {
        if (realCloseable != null) {
            realCloseable.close();
        }
        if (ApplicationStateNotification.getState() == ApplicationStateNotification.State.STARTED) {
            ApplicationStateNotification.waitForApplicationStop();
        }
        curatedApplication.close();
        curatedApplication = null;
    }
}
