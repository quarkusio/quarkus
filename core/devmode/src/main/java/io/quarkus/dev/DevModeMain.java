package io.quarkus.dev;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.LockSupport;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;

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
            DevModeContext context = (DevModeContext) new ObjectInputStream(new DataInputStream(devModeCp)).readObject();
            try (DevModeMain devModeMain = new DevModeMain(context)) {
                devModeMain.start();

                LockSupport.park();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            QuarkusBootstrap.Builder bootstrapBuilder = QuarkusBootstrap.builder(context.getClassesRoots().get(0).toPath())
                    .setIsolateDeployment(true)
                    .setLocalProjectDiscovery(context.isLocalProjectDiscovery())
                    .addAdditionalDeploymentArchive(path)
                    .setMode(QuarkusBootstrap.Mode.DEV);
            if (context.getProjectDir() != null) {
                bootstrapBuilder.setProjectRoot(context.getProjectDir().toPath());
            } else {
                bootstrapBuilder.setProjectRoot(new File(".").toPath());
            }
            for (int i = 1; i < context.getClassesRoots().size(); ++i) {
                bootstrapBuilder.addAdditionalApplicationArchive(
                        new AdditionalDependency(context.getClassesRoots().get(i).toPath(), false, false));
            }

            for (DevModeContext.ModuleInfo i : context.getModules()) {
                if (i.getClassesPath() != null) {
                    Path classesPath = Paths.get(i.getClassesPath());
                    bootstrapBuilder.addAdditionalApplicationArchive(new AdditionalDependency(classesPath, true, false));

                }
            }
            Properties buildSystemProperties = new Properties();
            buildSystemProperties.putAll(context.getBuildSystemProperties());
            bootstrapBuilder.setBuildSystemProperties(buildSystemProperties);
            curatedApplication = bootstrapBuilder.setTest(context.isTest()).build().bootstrap();
            realCloseable = (Closeable) curatedApplication.runInAugmentClassLoader(IsolatedDevModeMain.class.getName(),
                    Collections.singletonMap(DevModeContext.class.getName(), context));
        } catch (Throwable t) {
            log.error("Quarkus dev mode failed to start in curation phase", t);
            throw new RuntimeException(t);
            //System.exit(1);
        }
    }

    @Override
    public void close() throws IOException {
        realCloseable.close();
    }
}
