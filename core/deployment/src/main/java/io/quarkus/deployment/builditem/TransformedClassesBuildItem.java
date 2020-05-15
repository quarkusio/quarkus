package io.quarkus.deployment.builditem;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The results of applying bytecode transformation to a class.
 */
public final class TransformedClassesBuildItem extends SimpleBuildItem {

    private final Map<Path, Set<TransformedClass>> transformedClassesByJar;
    private final Map<Path, Set<String>> transformedFilesByJar;
    private final Map<String, TransformedClass> transformedClassesByName;

    public TransformedClassesBuildItem(Map<Path, Set<TransformedClass>> transformedClassesByJar) {
        this.transformedClassesByJar = new HashMap<>(transformedClassesByJar);
        this.transformedFilesByJar = new HashMap<>();
        this.transformedClassesByName = new HashMap<>();
        for (Map.Entry<Path, Set<TransformedClass>> e : transformedClassesByJar.entrySet()) {
            transformedFilesByJar.put(e.getKey(),
                    e.getValue().stream().map(TransformedClass::getFileName).collect(Collectors.toSet()));
            for (TransformedClass t : e.getValue()) {
                final TransformedClass existing = transformedClassesByName.put(t.className, t);
                if (existing != null) {
                    throw new IllegalStateException("Non unique Classname has been transformed! '" + t.className + '\'');
                }
            }
        }
    }

    public Map<Path, Set<TransformedClass>> getTransformedClassesByJar() {
        return transformedClassesByJar;
    }

    public Map<Path, Set<String>> getTransformedFilesByJar() {
        return transformedFilesByJar;
    }

    public TransformedClass getTransformedClassByName(String name) {
        return transformedClassesByName.get(name);
    }

    public static class TransformedClass {

        private final byte[] data;
        private final String fileName;
        private final String className;

        public TransformedClass(byte[] data, String fileName, String className) {
            this.data = data;
            this.fileName = fileName;
            this.className = className;
        }

        public byte[] getData() {
            return data;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TransformedClass that = (TransformedClass) o;
            return Objects.equals(fileName, that.fileName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileName);
        }
    }
}
