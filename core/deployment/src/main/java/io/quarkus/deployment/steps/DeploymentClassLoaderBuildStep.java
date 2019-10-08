package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.util.FileUtil;

public class DeploymentClassLoaderBuildStep {

    @BuildStep
    DeploymentClassLoaderBuildItem classloader(ApplicationArchivesBuildItem archivesBuildItem) {
        return new DeploymentClassLoaderBuildItem(new DeploymentClassLoader(archivesBuildItem));
    }

    static class DeploymentClassLoader extends ClassLoader {

        private final ApplicationArchivesBuildItem archivesBuildItem;

        DeploymentClassLoader(ApplicationArchivesBuildItem archivesBuildItem) {
            super(DeploymentClassLoaderBuildStep.class.getClassLoader());
            this.archivesBuildItem = archivesBuildItem;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return loadClass(name, false);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            if (name.startsWith("java.")) {
                return super.loadClass(name, resolve);
            }
            ApplicationArchive applicationArchive = archivesBuildItem.containingArchive(name);
            if (applicationArchive != null) {

                try {
                    try (InputStream res = Files
                            .newInputStream(applicationArchive.getChildPath(name.replace(".", "/") + ".class"))) {
                        byte[] data = FileUtil.readFileContents(res);
                        return defineClass(name, data, 0, data.length);
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException("IO Exception", e);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}
