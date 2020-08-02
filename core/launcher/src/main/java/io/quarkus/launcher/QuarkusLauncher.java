package io.quarkus.launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * IDE entry point.
 * <p>
 * This has a number of hacks to make it work, and is always going to be a bit fragile, as it is hard to make something
 * that will work 100% of the time as we just don't have enough information.
 * <p>
 * The launcher module has all its dependencies shaded, so it is effectively self contained. This allows deployment time
 * code to not leak into runtime code, as the launcher artifact is explicitly excluded from the production build via a
 * hard coded exclusion.
 */
public class QuarkusLauncher {

    public static void launch(String callingClass, String quarkusApplication, Consumer<Integer> exitHandler, String... args) {
        try {
            String classResource = callingClass.replace(".", "/") + ".class";
            URL resource = Thread.currentThread().getContextClassLoader().getResource(classResource);
            String path = resource.getPath();
            path = path.substring(0, path.length() - classResource.length());
            URL newResource = new URL(resource.getProtocol(), resource.getHost(), resource.getPort(), path);

            Path appClasses = Paths.get(newResource.toURI());
            if (quarkusApplication != null) {
                System.setProperty("quarkus.package.main-class", quarkusApplication);
            }

            Map<String, Object> context = new HashMap<>();
            context.put("app-classes", appClasses);
            context.put("args", args);

            IDEClassLoader loader = new IDEClassLoader(QuarkusLauncher.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);

            Class<?> launcher = loader.loadClass("io.quarkus.bootstrap.IDELauncherImpl");
            launcher.getDeclaredMethod("launch", Path.class, Map.class).invoke(null, appClasses, context);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.clearProperty(BootstrapConstants.SERIALIZED_APP_MODEL);
        }
    }

    public static class IDEClassLoader extends ClassLoader {

        static {
            registerAsParallelCapable();
        }

        public IDEClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String resourceName = name.replace(".", "/") + ".class";
            try {
                try (InputStream is = getResourceAsStream(resourceName)) {
                    if (is == null) {
                        throw new ClassNotFoundException(name);
                    }
                    definePackage(name);
                    byte[] buf = new byte[1024];
                    int r;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while ((r = is.read(buf)) > 0) {
                        out.write(buf, 0, r);
                    }
                    byte[] bytes = out.toByteArray();

                    return defineClass(name, bytes, 0, bytes.length);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        private void definePackage(String name) {
            final String pkgName = getPackageNameFromClassName(name);
            if ((pkgName != null) && getPackage(pkgName) == null) {
                synchronized (getClassLoadingLock(pkgName)) {
                    if (getPackage(pkgName) == null) {
                        // this could certainly be improved to use the actual manifest
                        definePackage(pkgName, null, null, null, null, null, null, null);
                    }
                }
            }
        }

        private String getPackageNameFromClassName(String className) {
            final int index = className.lastIndexOf('.');
            if (index == -1) {
                // we return null here since in this case no package is defined
                // this is same behavior as Package.getPackage(clazz) exhibits
                // when the class is in the default package
                return null;
            }
            return className.substring(0, index);
        }

        protected Class<?> findClass(String moduleName, String name) {
            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        protected URL findResource(String moduleName, String name) throws IOException {
            return findResource(name);
        }

        @Override
        protected URL findResource(String name) {
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            return getParent().getResource("META-INF/ide-deps" + name + ".ide-launcher-res");
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            return getParent().getResources("META-INF/ide-deps" + name + ".ide-launcher-res");
        }
    }
}
