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
 *
 * Note that this has also been abused somewhat to also represent removed
 * resources, as the logic is the same, and it avoids have two separate mechanisms
 * that essentially do the same thing.
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

        private final String className;
        /**
         * The class data, if it is null then the class has been removed.
         */
        private final byte[] data;
        private final String fileName;
        private final boolean eager;

        public TransformedClass(String className, byte[] data, String fileName, boolean eager) {
            this.className = className;
            this.data = data;
            this.fileName = fileName;
            this.eager = eager;
        }

        public byte[] getData() {
            return data;
        }

        public String getFileName() {
            return fileName;
        }

        public String getClassName() {
            return className;
        }

        public boolean isEager() {
            return eager;
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
