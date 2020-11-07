package io.quarkus.minio.deployment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.minio.MinioProducer;

class MinioProcessor {

    private static final String FEATURE = "minio";
    private static final String CAPABILITY = "minio-client";

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CapabilityBuildItem> capability,
            BuildProducer<ReflectiveClassBuildItem> reflectionClasses,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        feature.produce(new FeatureBuildItem(FEATURE));
        capability.produce(new CapabilityBuildItem(CAPABILITY));

        List<Class> classes = getClasses("io.minio.messages");
        reflectionClasses.produce(new ReflectiveClassBuildItem(true, true, classes.toArray(new Class[classes.size()])));
        additionalBeans.produce(new AdditionalBeanBuildItem(MinioProducer.class));
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private List<Class> getClasses(String packageName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            assert classLoader != null;
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = null;
            resources = classLoader.getResources(path);
            List<File> dirs = new ArrayList<File>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            return dirs.stream().map(item -> findClasses(item, packageName)).flatMap(List::stream).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) {
        try {
            List<Class> classes = new ArrayList<>();
            if (!directory.exists()) {
                return classes;
            }
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
            return classes;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
