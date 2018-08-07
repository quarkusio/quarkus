package org.jboss.shamrock.maven.runner;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import org.jboss.shamrock.runtime.Timing;

/**
 * The main entry point for the run mojo execution
 */
public class RunMojoMain {

    private static CountDownLatch awaitChangeLatch = null;
    private static CountDownLatch awaitRestartLatch = null;
    private static volatile boolean keepCl = false;
    private static volatile ClassLoader currentAppClassLoader;

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();
        //the path that contains the generated classes
        File classesRoot = new File(args[0]);
        URLClassLoader runtimeCl = null;
        do {
            if (runtimeCl == null || !keepCl) {
                runtimeCl = new URLClassLoader(new URL[]{classesRoot.toURL()}, ClassLoader.getSystemClassLoader());
            }
            currentAppClassLoader = runtimeCl;
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            //we can potentially throw away this class loader, and reload the app
            synchronized (RunMojoMain.class) {
                awaitChangeLatch = new CountDownLatch(1);
            }
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                Class<?> runnerClass = runtimeCl.loadClass("org.jboss.shamrock.runner.RuntimeRunner");
                Constructor ctor = runnerClass.getDeclaredConstructor(Path.class, ClassLoader.class);
                Object runner = ctor.newInstance(classesRoot.toPath(), runtimeCl);
                ((Runnable) runner).run();
                synchronized (RunMojoMain.class) {
                    if (awaitRestartLatch != null) {
                        awaitRestartLatch.countDown();
                        awaitRestartLatch = null;
                    }
                }
                awaitChangeLatch.await();
                ((Closeable) runner).close();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

        } while (true);
    }

    public static void restartApp(boolean keepClassloader) {
        keepCl = keepClassloader;
        Timing.restart();
        CountDownLatch restart = null;
        synchronized (RunMojoMain.class) {
            if (awaitChangeLatch != null) {
                restart = awaitRestartLatch = new CountDownLatch(1);
                awaitChangeLatch.countDown();
            }
            awaitChangeLatch = null;
        }
        if (restart != null) {
            try {
                restart.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static ClassLoader getCurrentAppClassLoader() {
        return currentAppClassLoader;
    }
}
