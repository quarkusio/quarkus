package io.quarkus.extest.runtime.classpath;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ClasspathEntriesRecorder {

    public static Map<String, List<String>> gather(List<String> resourceNames)
            throws IOException {
        Map<String, List<String>> classpathEntries = new HashMap<>();
        for (String resourceName : resourceNames) {
            Enumeration<URL> resourcesEnum = getClassLoader().getResources(resourceName);
            List<String> resources = new ArrayList<>();
            while (resourcesEnum.hasMoreElements()) {
                resources.add(resourcesEnum.nextElement().toString());
            }
            classpathEntries.put(resourceName, resources);
        }
        return classpathEntries;
    }

    public static void record(Path recordFilePath, RecordedClasspathEntries.Phase phase,
            Map<String, List<String>> classpathEntries) {
        for (Map.Entry<String, List<String>> entry : classpathEntries.entrySet()) {
            RecordedClasspathEntries.put(recordFilePath,
                    new RecordedClasspathEntries.Record(phase, entry.getKey(), entry.getValue()));
        }
    }

    public void gatherAndRecord(String recordFilePath, RecordedClasspathEntries.Phase phase, List<String> resourceNames)
            throws IOException {
        record(Path.of(recordFilePath), phase, gather(resourceNames));
    }

    private static ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            return ClasspathEntriesRecorder.class.getClassLoader();
        }
        return cl;
    }

    public RuntimeValue<RecordedClasspathEntries> recordedClasspathEntries(String recordFilePath) {
        return new RuntimeValue<>(new RecordedClasspathEntries(Path.of(recordFilePath)));
    }
}
