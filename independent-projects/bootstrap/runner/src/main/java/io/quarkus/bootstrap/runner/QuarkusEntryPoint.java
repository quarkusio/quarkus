package io.quarkus.bootstrap.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class QuarkusEntryPoint {

    public static final String QUARKUS_APPLICATION_DAT = "quarkus/quarkus-application.dat";
    public static final String QUARKUS_RUNNER_CL_ADDITIONAL_INDEX_DAT = "quarkus/quarkus-rcl-additional-index.dat";
    public static final String LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT = "lib/deployment/deployment-class-path.dat";

    public static final String PRE_BOOT_SYSTEM_PROPERTY = "quarkus.application.pre-boot";

    public static void main(String... args) throws Throwable {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        Timing.staticInitStarted();
        doRun(args);
    }

    private static void doRun(Object args) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        String path = QuarkusEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        Path appRoot = new File(decodedPath).toPath().getParent().getParent().getParent();

        if (Boolean.parseBoolean(System.getenv("QUARKUS_LAUNCH_DEVMODE"))) {
            DevModeMediator.doDevMode(appRoot);
        } else if (Boolean.getBoolean("quarkus.launch.rebuild")) {
            doReaugment(appRoot);
        } else {
            SerializedApplication app = null;
            // TODO: do we want to use the same property here or have some dedicated property for capturing?
            boolean isPreBoot = Boolean.parseBoolean(System.getProperty(PRE_BOOT_SYSTEM_PROPERTY, "false"));
            try (InputStream in = Files.newInputStream(appRoot.resolve(QUARKUS_APPLICATION_DAT))) {
                RunnerClassLoader runnerClassLoader;
                if (isPreBoot) {
                    app = SerializedApplication.read(appRoot, in, null);
                    CapturingRunnerClassLoader capturingRunnerClassLoader = new CapturingRunnerClassLoader(
                            app.getRunnerClassLoader().getParent(), app.getRunnerClassLoader().getResourceDirectoryMap(),
                            app.getRunnerClassLoader().getParentFirstPackages(),
                            app.getRunnerClassLoader().getNonExistentResources());
                    // we need to use a shutdown hook because the application simply exits after "main"
                    // is called because ApplicationLifecycleManager uses a default exit handler
                    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SerializedApplication.writeAdditionalIndex(appRoot,
                                        capturingRunnerClassLoader.getCapturedFindResource(),
                                        capturingRunnerClassLoader.getCapturedFindResources());
                            } catch (Exception ignored) {

                            }
                        }
                    }));
                    runnerClassLoader = capturingRunnerClassLoader;
                } else {
                    Path additionalIndex = appRoot.resolve(QUARKUS_RUNNER_CL_ADDITIONAL_INDEX_DAT);
                    if (Files.exists(additionalIndex)) {
                        try (InputStream ais = Files.newInputStream(additionalIndex)) {
                            app = SerializedApplication.read(appRoot, in, ais);
                        }
                    } else {
                        app = SerializedApplication.read(appRoot, in, null);
                    }
                    runnerClassLoader = app.getRunnerClassLoader();
                }
                Thread.currentThread().setContextClassLoader(runnerClassLoader);
                Class<?> mainClass = runnerClassLoader.loadClass(app.getMainClass());
                mainClass.getMethod("main", String[].class).invoke(null, args);
            } finally {
                if (app != null) {
                    app.getRunnerClassLoader().close();
                }
            }
        }
    }

    private static void doReaugment(Path appRoot) throws IOException, ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(appRoot.resolve(LIB_DEPLOYMENT_DEPLOYMENT_CLASS_PATH_DAT)))) {
            List<String> paths = (List<String>) in.readObject();
            //yuck, should use runner class loader
            URLClassLoader loader = new URLClassLoader(paths.stream().map((s) -> {
                try {
                    return appRoot.resolve(s).toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new));
            try {
                loader.loadClass("io.quarkus.deployment.mutability.ReaugmentTask")
                        .getDeclaredMethod("main", Path.class).invoke(null, appRoot);
            } finally {
                loader.close();
            }
        }
    }

}
