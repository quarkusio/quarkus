
package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.dev.appstate.ApplicationStateNotification;

/**
 * The main entry point for the dev mojo execution
 */
public class DevModeMain implements Closeable {

    public static final String DEV_MODE_CONTEXT = "META-INF/dev-mode-context.dat";
    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private final DevModeContext context;

    private static volatile CuratedApplication curatedApplication;
    private Closeable realCloseable;

    public DevModeMain(DevModeContext context) {
        this.context = context;
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
        for (Map.Entry<String, String> i : context.getSystemProperties().entrySet()) {
            if (!System.getProperties().containsKey(i.getKey())) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }

        try {
            URL thisArchive = getClass().getResource(DevModeMain.class.getSimpleName() + ".class");
            int endIndex = thisArchive.getPath().indexOf("!");
            Path path;
            if (endIndex != -1) {
                path = Paths.get(new URI(thisArchive.getPath().substring(0, endIndex)));
            } else {
                path = Paths.get(thisArchive.toURI());
                path = path.getParent();
                for (char i : DevModeMain.class.getName().toCharArray()) {
                    if (i == '.') {
                        path = path.getParent();
                    }
                }
            }
            final PathsCollection.Builder appRoots = PathsCollection.builder();
            Path p = Paths.get(context.getApplicationRoot().getClassesPath());
            if (Files.exists(p)) {
                appRoots.add(p);
            }
            if (context.getApplicationRoot().getResourcesOutputPath() != null
                    && !context.getApplicationRoot().getResourcesOutputPath()
                            .equals(context.getApplicationRoot().getClassesPath())) {
                p = Paths.get(context.getApplicationRoot().getResourcesOutputPath());
                if (Files.exists(p)) {
                    appRoots.add(p);
                }
            }

            QuarkusBootstrap.Builder bootstrapBuilder = QuarkusBootstrap.builder()
                    .setApplicationRoot(appRoots.build())
                    .setTargetDirectory(context.getDevModeRunnerJarFile().getParentFile().toPath())
                    .setIsolateDeployment(true)
                    .setLocalProjectDiscovery(context.isLocalProjectDiscovery())
                    .addAdditionalDeploymentArchive(path)
                    .setBaseName(context.getBaseName())
                    .setMode(context.getMode());
            if (context.getProjectDir() != null) {
                bootstrapBuilder.setProjectRoot(context.getProjectDir().toPath());
            } else {
                bootstrapBuilder.setProjectRoot(new File(".").toPath());
            }
            for (AppArtifactKey i : context.getLocalArtifacts()) {
                bootstrapBuilder.addLocalArtifact(i);
            }

            for (DevModeContext.ModuleInfo i : context.getAllModules()) {
                if (i.getClassesPath() != null) {
                    Path classesPath = Paths.get(i.getClassesPath());
                    bootstrapBuilder.addAdditionalApplicationArchive(new AdditionalDependency(classesPath, true, false));
                }
                if (i.getResourcesOutputPath() != null && !i.getResourcesOutputPath().equals(i.getClassesPath())) {
                    Path resourceOutputPath = Paths.get(i.getResourcesOutputPath());
                    bootstrapBuilder.addAdditionalApplicationArchive(new AdditionalDependency(resourceOutputPath, true, false));
                }
            }

            copyDotEnvFile();

            Properties buildSystemProperties = new Properties();
            buildSystemProperties.putAll(context.getBuildSystemProperties());
            bootstrapBuilder.setBuildSystemProperties(buildSystemProperties);
            curatedApplication = bootstrapBuilder.setTest(context.isTest()).build().bootstrap();
            realCloseable = (Closeable) curatedApplication.runInAugmentClassLoader(
                    context.getAlternateEntryPoint() == null ? IsolatedDevModeMain.class.getName()
                            : context.getAlternateEntryPoint(),
                    Collections.singletonMap(DevModeContext.class.getName(), context));
        } catch (Throwable t) {
            log.error("Quarkus dev mode failed to start", t);
            throw new RuntimeException(t);
            //System.exit(1);
        }
    }

    // copies the .env file to the directory where the process is running
    // this is done because for the .env file to take effect, it needs to be
    // in the same directory as the running process
    private void copyDotEnvFile() {
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
            try {
                Path link = currentDir.resolve(".env");
                silentDeleteFile(link);
                // create a symlink to ensure that user updates to the file have the expected effect in dev-mode
                Files.createSymbolicLink(link, dotEnvPath);
            } catch (IOException e) {
                log.warn("Unable to copy .env file", e);
            }
        }
    }

    private void silentDeleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ignored) {

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
    }
}
