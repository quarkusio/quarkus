package org.jboss.shamrock.legacy.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * For some reason when using class-path entries something seems broken in java service loaders, as getResources()
 * will only every return a single resource from the app class loader.
 * <p>
 * To get around this the legacy integration is broken up into a launcher and the runner, with this launcher
 * just setting up class loading.
 * <p>
 * This has the advatange than anything added to he lib dir will automatically be added to the class path.
 */
public class Main {

    public static void main(String... args) {
        String classFileName = Main.class.getName().replace('.', '/') + ".class";
        URL location = Main.class.getClassLoader().getResource(classFileName);
        File libDir;
        if (location.getProtocol().equals("jar")) {
            String loc = location.getPath().substring(5, location.getPath().lastIndexOf('!'));
            File file = new File(loc);
            libDir = new File(file.getParentFile(), "lib");
        } else if (location.getProtocol().equals("file")) {

            String loc = location.getPath().substring(0, location.getPath().length() - classFileName.length());
            File file = new File(loc);
            libDir = new File(file.getParentFile(), "lib");
        } else {
            throw new RuntimeException("Unable to determine lib dir location from URL: " + location);
        }
        if (!libDir.isDirectory()) {
            throw new RuntimeException("Could not find lib dir " + libDir);
        }
        try {
            List<URL> urls = new ArrayList<>();
            for (File i : libDir.listFiles()) {
                urls.add(i.toURL());
            }
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[urls.size()]));
            try {
                Thread.currentThread().setContextClassLoader(ucl);
                Class<?> main = ucl.loadClass("org.jboss.shamrock.legacy.Main");
                Method run = main.getDeclaredMethod("main", String[].class);
                run.invoke(null, (Object) args);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
