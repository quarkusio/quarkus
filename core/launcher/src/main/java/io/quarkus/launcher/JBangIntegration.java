package io.quarkus.launcher;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;

public class JBangIntegration {

    public static final String CONFIG = "//Q:CONFIG";

    public static Map<String, Object> postBuild(Path appClasses, Path pomFile, List<Map.Entry<String, String>> repositories,
            List<Map.Entry<String, Path>> dependencies,
            List<String> comments, boolean nativeImage) {
        for (String comment : comments) {
            //we allow config to be provided via //Q:CONFIG name=value
            if (comment.startsWith(CONFIG)) {
                String conf = comment.substring(CONFIG.length()).trim();
                int equals = conf.indexOf("=");
                if (equals == -1) {
                    throw new RuntimeException("invalid config  " + comment);
                }
                System.setProperty(conf.substring(0, equals), conf.substring(equals + 1));
            }
        }

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            RuntimeLaunchClassLoader loader = new RuntimeLaunchClassLoader(
                    new ClassLoader(JBangIntegration.class.getClassLoader()) {
                        @Override
                        public Class<?> loadClass(String name) throws ClassNotFoundException {
                            return loadClass(name, false);
                        }

                        @Override
                        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                            if (name.startsWith("org.") && !(name.startsWith("org.xml.") || name.startsWith("org.w3c."))) {
                                //jbang has some but not all of the maven resolver classes we need on its
                                //class path. These all start with org. so we filter them out to make sure
                                //we get a complete class path
                                throw new ClassNotFoundException();
                            }
                            return super.loadClass(name, resolve);
                        }

                        @Override
                        public URL getResource(String name) {
                            if (name.startsWith("org/") && !(name.startsWith("org/xml/") || name.startsWith("org/w3c/"))) {
                                //jbang has some but not all of the maven resolver classes we need on its
                                //class path. These all start with org. so we filter them out to make sure
                                //we get a complete class path
                                return null;
                            }
                            return super.getResource(name);
                        }

                        @Override
                        public Enumeration<URL> getResources(String name) throws IOException {
                            if (name.startsWith("org/") && !(name.startsWith("org/xml/") || name.startsWith("org/w3c/"))) {
                                //jbang has some but not all of the maven resolver classes we need on its
                                //class path. These all start with org. so we filter them out to make sure
                                //we get a complete class path
                                return Collections.emptyEnumeration();
                            }
                            return super.getResources(name);
                        }
                    });
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> launcher = loader.loadClass("io.quarkus.bootstrap.JBangBuilderImpl");
            return (Map<String, Object>) launcher
                    .getDeclaredMethod("postBuild", Path.class, Path.class, List.class, List.class, boolean.class).invoke(
                            null,
                            appClasses,
                            pomFile,
                            repositories,
                            dependencies,
                            nativeImage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.clearProperty(BootstrapConstants.SERIALIZED_APP_MODEL);
            Thread.currentThread().setContextClassLoader(old);
        }
    }

}
