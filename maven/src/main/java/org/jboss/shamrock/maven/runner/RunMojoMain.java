package org.jboss.shamrock.maven.runner;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * The main entry point for the run mojo execution
 */
public class RunMojoMain {

    private static CountDownLatch awaitChangeLatch = null;

    public static void main(String... args) throws Exception {
        //the path that contains the generated classes
        File classesRoot = new File(args[0]);

        do {
            //we can potentially throw away this class loader, and reload the app
            synchronized (RunMojoMain.class) {
                awaitChangeLatch = new CountDownLatch(1);
            }
            URLClassLoader runtimeCl = new URLClassLoader(new URL[]{classesRoot.toURL()}, ClassLoader.getSystemClassLoader());
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                Class<?> runnerClass = runtimeCl.loadClass("org.jboss.shamrock.runtime.RuntimeRunner");
                Constructor ctor = runnerClass.getDeclaredConstructor(Path.class, ClassLoader.class);
                Object runner = ctor.newInstance(classesRoot.toPath(), runtimeCl);
                ((Runnable) runner).run();
                awaitChangeLatch.await();
                ((Closeable)runner).close();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

        } while (true);
    }

    public static void restartApp() {
        synchronized (RunMojoMain.class) {
            if(awaitChangeLatch != null) {
                awaitChangeLatch.countDown();
            }
            awaitChangeLatch = null;
        }
    }

}
