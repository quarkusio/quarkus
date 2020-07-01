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

    public TransformedClassesBuildItem(Map<Path, Set<TransformedClass>> transformedClassesByJar) {
        this.transformedClassesByJar = new HashMap<>(transformedClassesByJar);
        this.transformedFilesByJar = new HashMap<>();
        for (Map.Entry<Path, Set<TransformedClass>> e : transformedClassesByJar.entrySet()) {
            transformedFilesByJar.put(e.getKey(),
                    e.getValue().stream().map(TransformedClass::getFileName).collect(Collectors.toSet()));
        }
    }

    public Map<Path, Set<TransformedClass>> getTransformedClassesByJar() {
        return transformedClassesByJar;
    }

    public Map<Path, Set<String>> getTransformedFilesByJar() {
        return transformedFilesByJar;
    }

    public static class TransformedClass {

        private final byte[] data;
        private final String fileName;

        public TransformedClass(byte[] data, String fileName) {
            this.data = data;
            this.fileName = fileName;
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
