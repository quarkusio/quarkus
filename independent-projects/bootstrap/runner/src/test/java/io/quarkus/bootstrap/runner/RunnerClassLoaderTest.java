package io.quarkus.bootstrap.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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

        // These jars will be used to evict the simple-project-1.0.jar from the cache again
        resourceDirectoryMap.put("org/evict", new ClassLoadingResource[] {
                createProjectJarResource("empty-project-c-1.0.jar"), createProjectJarResource("empty-project-d-1.0.jar"),
                createProjectJarResource("empty-project-e-1.0.jar"), createProjectJarResource("evict-project-1.0.jar") });

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

        final var exceptionsInThreads = new CopyOnWriteArrayList<Throwable>();
        ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(() -> {
                try {
                    r.run();
                } catch (Throwable e) {
                    exceptionsInThreads.add(e);
                }
            });
            t.setDaemon(true);
            return t;
        });

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
        Runnable evictingAgainTask = () -> {
            try {
                runnerClassLoader.loadClass("org.evict.EvictPojo");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        };

        // Concurrently executes the task evicting the jar and the one reopening it
        try {
            executor.submit(evictingTask);
            executor.submit(reloadingTask);
            executor.submit(evictingAgainTask);
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }

        assertTrue(exceptionsInThreads.isEmpty(), "Exceptions in threads: " + exceptionsInThreads);
    }

    private static JarResource createProjectJarResource(String jarName) {
        ManifestInfo manifestInfo = new ManifestInfo(jarName.substring(jarName.lastIndexOf('-')), "1.0", "Apache", null, null,
                null);
        JarResource jarResource = new JarResource(manifestInfo,
                Path.of("src", "test", "resources", "jars", jarName));
        return jarResource;
    }
}
