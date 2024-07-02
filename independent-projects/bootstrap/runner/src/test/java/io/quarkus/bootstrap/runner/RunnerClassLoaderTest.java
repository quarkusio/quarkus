package io.quarkus.bootstrap.runner;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

public class RunnerClassLoaderTest {

    static {
        System.setProperty("jdk.tracePinnedThreads", "full");
    }

    @Test
    public void test() throws Exception {
        Map<String, ClassLoadingResource[]> resourceDirectoryMap = new HashMap<>();
        resourceDirectoryMap.put("org/mario",
                new ClassLoadingResource[] { createApacheTextJarResource("a"), createMyProjectJarResource() });

        // These jars are simply used to fill the RunnerClassLoader's jars cache
        resourceDirectoryMap.put("org/apache/commons/text", new ClassLoadingResource[] {
                createApacheExecJarResource("b"), createApacheExecJarResource("c"),
                createApacheExecJarResource("d"), createApacheTextJarResource("a") });

        RunnerClassLoader runnerClassLoader = new RunnerClassLoader(ClassLoader.getSystemClassLoader(), resourceDirectoryMap,
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptyList(), Collections.emptyMap());

        // Put the RunnerClassLoader in a postBootPhase thus enabling the jars cache
        runnerClassLoader.resetInternalCaches();

        // Fills the RunnerClassLoader's jars cache
        runnerClassLoader.loadClass("org.apache.commons.text.AlphabetConverter");

        Runnable runnable = null;

        try {
            Class<?> runnableClazz = runnerClassLoader.loadClass("org.mario.MyRunnable");
            runnable = (Runnable) runnableClazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ExecutorService virtualExecutor = createVirtualThreadExecutor();
        try {
            for (int i = 0; i < 2; i++) {
                virtualExecutor.submit(runnable);
            }
        } finally {
            closeExecutor(virtualExecutor);
        }
    }

    private static JarResource createMyProjectJarResource() {
        ManifestInfo manifestInfo = new ManifestInfo("myproject", "1.0", "mario", null, null, null);
        JarResource jarResource = new JarResource(manifestInfo,
                Path.of("src", "test", "resources", "jars", "myproject-1.0.jar"));
        return jarResource;
    }

    private static JarResource createApacheTextJarResource(String version) {
        ManifestInfo textManifest = new ManifestInfo("commons-text", "1.12.0", "Apache", null, null, null);
        JarResource textJar = new JarResource(textManifest,
                Path.of("src", "test", "resources", "jars", "commons-text-1.12." + version + ".jar"));
        return textJar;
    }

    private static JarResource createApacheExecJarResource(String version) {
        ManifestInfo textManifest = new ManifestInfo("commons-exec", "1.4.0", "Apache", null, null, null);
        JarResource textJar = new JarResource(textManifest,
                Path.of("src", "test", "resources", "jars", "commons-exec-1.4." + version + ".jar"));
        return textJar;
    }

    private ExecutorService createVirtualThreadExecutor() {
        try {
            return (ExecutorService) Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void closeExecutor(ExecutorService executor) {
        try {
            ExecutorService.class.getMethod("close").invoke(executor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
