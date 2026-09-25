package io.quarkus.uberjar.launcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.modules.boot.JarIndex;

/**
 * Concrete implementation of the {@link JarIndex} interface loaded by the Boot shim.
 * It reads pre-indexed boot-path class coordinates from packaged metadata files.
 */
public final class JarIndexImpl implements JarIndex {

    private final List<String> bootModuleNames;
    private final Map<String, Long> bootClassOffsets = new HashMap<>();
    private final Map<String, Long> bootClassSizes = new HashMap<>();
    private final Map<String, Long> bootJarOffsets = new HashMap<>();
    private final Map<String, Long> bootJarSizes = new HashMap<>();

    /**
     * Construct a new JarIndexImpl, loading metadata resources.
     */
    public JarIndexImpl() {
        try {
            // 1. Read boot module names from /META-INF/uberjar-boot-modules.txt
            this.bootModuleNames = readLines("/META-INF/uberjar-boot-modules.txt");

            // 2. Read nested boot JAR offsets and sizes from /META-INF/uberjar-boot-jar-index.txt
            try (InputStream is = JarIndexImpl.class.getResourceAsStream("/META-INF/uberjar-boot-jar-index.txt")) {
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.strip();
                            if (line.isEmpty()) {
                                continue;
                            }
                            String[] parts = line.split(";");
                            if (parts.length != 3) {
                                throw new IllegalArgumentException("Invalid boot JAR index line format: " + line);
                            }
                            String moduleName = parts[0];
                            long offset = Long.parseLong(parts[1]);
                            long size = Long.parseLong(parts[2]);

                            bootJarOffsets.put(moduleName, offset);
                            bootJarSizes.put(moduleName, size);
                        }
                    }
                }
            }

            // 3. Read boot class file relative offsets and sizes from /META-INF/uberjar-boot-index.txt
            try (InputStream is = JarIndexImpl.class.getResourceAsStream("/META-INF/uberjar-boot-index.txt")) {
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.strip();
                            if (line.isEmpty()) {
                                continue;
                            }
                            String[] parts = line.split(";");
                            if (parts.length != 4) {
                                throw new IllegalArgumentException("Invalid boot class index line format: " + line);
                            }
                            String moduleName = parts[0];
                            String className = parts[1];
                            long offset = Long.parseLong(parts[2]);
                            long size = Long.parseLong(parts[3]);

                            String key = moduleName + "/" + className;
                            bootClassOffsets.put(key, offset);
                            bootClassSizes.put(key, size);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JarIndexImpl", e);
        }
    }

    private static List<String> readLines(String resourcePath) throws Exception {
        try (InputStream is = JarIndexImpl.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return List.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                List<String> list = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (!line.isEmpty()) {
                        list.add(line);
                    }
                }
                return List.copyOf(list);
            }
        }
    }

    @Override
    public List<String> moduleNames() {
        return bootModuleNames;
    }

    @Override
    public long classOffset(String moduleName, String className) {
        Long relativeOffset = bootClassOffsets.get(moduleName + "/" + className);
        if (relativeOffset == null) {
            return -1L;
        }
        Long jarOffset = bootJarOffsets.get(moduleName);
        if (jarOffset == null) {
            return -1L;
        }
        return jarOffset.longValue() + relativeOffset.longValue();
    }

    @Override
    public long classSize(String moduleName, String className) {
        Long val = bootClassSizes.get(moduleName + "/" + className);
        return val != null ? val.longValue() : -1L;
    }
}
