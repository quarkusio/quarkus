package io.quarkus.bootstrap.runner;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class RunnerClassLoaderTest {

    @Test
    public void testConcurrentJarCloseAndReload() throws Exception {
        Map<String, ClassLoadingResource[]> resourceDirectoryMap = new HashMap<>();

        resourceDirectoryMap.put("org/simple", new ClassLoadingResource[] {
                createProjectJarResource("simple-project-1.0.jar") });

        // These jars are simply used to fill the RunnerClassLoader's jars cache
        resourceDirectoryMap.put("org/easy", new ClassLoadingResource[] {
                createProjectJarResource("empty-project-a-1.0.jar"), createProjectJarResource("empty-project-b-1.0.jar"),
                createProjectJarResource("easy-project-1.0.jar") });

        resourceDirectoryMap.put("org/trivial", new ClassLoadingResource[] {
                createProjectJarResource("trivial-project-1.0.jar") });

        RunnerClassLoader runnerClassLoader = new RunnerClassLoader(ClassLoader.getSystemClassLoader(), resourceDirectoryMap,
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptyList(), Collections.emptyMap());

        // Put the RunnerClassLoader in a postBootPhase thus enabling the jars cache
        runnerClassLoader.resetInternalCaches();

        runnerClassLoader.loadClass("org.simple.SimplePojo1");

        // Fills the RunnerClassLoader's jars cache
        runnerClassLoader.loadClass("org.easy.EasyPojo");

        // Now easy-project-1.0.jar is the least recently used jar in cache and the next to be evicted when a class
        // from a different jar will be requested

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable evictingTask = () -> {
            try {
                runnerClassLoader.loadClass("org.trivial.TrivialPojo");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
        Runnable reloadingTask = () -> {
            try {
                runnerClassLoader.loadClass("org.simple.SimplePojo2");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        };

        // Concurrently executes the task evicting the jar and the one reopening it
        try {
            executor.submit(evictingTask);
            executor.submit(reloadingTask);
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private static JarResource createProjectJarResource(String jarName) {
        ManifestInfo manifestInfo = new ManifestInfo(jarName.substring(jarName.lastIndexOf('-')), "1.0", "Apache", null, null,
                null);
        JarResource jarResource = new JarResource(manifestInfo,
                Path.of("src", "test", "resources", "jars", jarName));
        return jarResource;
    }
}
