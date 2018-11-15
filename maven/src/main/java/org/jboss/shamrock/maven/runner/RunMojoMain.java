package org.jboss.shamrock.maven.runner;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.Timing;

/**
 * The main entry point for the run mojo execution
 */
public class RunMojoMain {

    private static final Logger log = Logger.getLogger(RunMojoMain.class);

    private static volatile boolean keepCl = false;
    private static volatile ClassLoader currentAppClassLoader;
    private static volatile URLClassLoader runtimeCl;
    private static File classesRoot;
    private static File wiringDir;
    private static File cacheDir;

    private static Closeable closeable;
    static volatile Throwable deploymentProblem;

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();
        RuntimeCompilationSetup.setup();
        //the path that contains the compiled classes
        classesRoot = new File(args[0]);
        wiringDir = new File(args[1]);
        cacheDir = new File(args[2]);
        //TODO: we can't handle an exception on startup with hot replacement, as Undertow might not have started

        doStart();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (RunMojoMain.class) {
                    if (closeable != null) {
                        try {
                            closeable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "Shamrock Shutdown Thread"));
    }

    private static synchronized void doStart() {
        try {
            if (runtimeCl == null || !keepCl) {
                runtimeCl = new URLClassLoader(new URL[]{classesRoot.toURL()}, ClassLoader.getSystemClassLoader());
            }
            currentAppClassLoader = runtimeCl;
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            //we can potentially throw away this class loader, and reload the app
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                Class<?> runnerClass = runtimeCl.loadClass("org.jboss.shamrock.runner.RuntimeRunner");
                Constructor ctor = runnerClass.getDeclaredConstructor(ClassLoader.class, Path.class, Path.class, Path.class, List.class);
                Object runner = ctor.newInstance(runtimeCl, classesRoot.toPath(), wiringDir.toPath(), cacheDir.toPath(), new ArrayList<>());
                ((Runnable) runner).run();
                closeable = ((Closeable) runner);
                deploymentProblem = null;
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Throwable t) {
            deploymentProblem = t;
            log.error("Failed to start shamrock", t);
        }
    }

    public static synchronized void restartApp(boolean keepClassloader) {
        keepCl = keepClassloader;
        if (closeable != null) {

            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(runtimeCl);
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        closeable = null;
        doStart();
    }

    public static ClassLoader getCurrentAppClassLoader() {
        return currentAppClassLoader;
    }
}
