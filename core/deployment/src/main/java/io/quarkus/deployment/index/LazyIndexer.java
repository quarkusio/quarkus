package io.quarkus.deployment.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassSummary;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.deployment.util.IoUtil;

/**
 * A lazy creator of a Jandex {@link Index}. It reads class data from a given class loader
 * and skips classes that are present in the given existing Jandex index.
 * <p>
 * Expected usage:
 * <ol>
 * <li>Create an instance: {@code new LazyIndexer(classLoader, existingIndex)}</li>
 * <li>Add class names to be indexed lazily: {@code add(className)}, {@code addAll(classNames)}</li>
 * <li>Run the indexing process: {@code var result = indexer.complete()}</li>
 * </ol>
 * After calling {@code complete()}, the instance should be thrown away.
 * The resulting index is expected to be combined with the given existing index using
 * a {@link org.jboss.jandex.CompositeIndex CompositeIndex} or {@link org.jboss.jandex.StackedIndex StackedIndex}.
 */
public class LazyIndexer {
    /**
     * A result of lazy indexing process. Contains mainly a Jandex {@link Index},
     * but also a set of annotation types whose classes were not found. It is perfectly
     * normal for an annotation class to be missing, so this is <em>not</em> an error.
     */
    public record Result(Index index, Set<DotName> missingAnnotations) {
    }

    private final ClassLoader classLoader;
    private final IndexView existingIndex;

    private final Set<String> classNames = new HashSet<>();
    private final Map<String, byte[]> classesData = new HashMap<>();

    public LazyIndexer(ClassLoader classLoader, IndexView existingIndex) {
        this.classLoader = Objects.requireNonNull(classLoader);
        this.existingIndex = Objects.requireNonNull(existingIndex);
    }

    /**
     * Adds the given {@code className} to the set of classes to be indexed lazily.
     * <p>
     * The given class, all its superclasses, and all annotations found on the class
     * and its superclasses will be indexed.
     */
    public void add(String className) {
        classNames.add(Objects.requireNonNull(className));
    }

    /**
     * Adds all given {@code classNames} to the set of classes to be indexed lazily.
     * <p>
     * The given classes, all their superclasses, and all annotations found on the classes
     * and their superclasses will be indexed.
     */
    public void addAll(Collection<String> classNames) {
        this.classNames.addAll(Objects.requireNonNull(classNames));
    }

    /**
     * Adds the given {@code className} to the set of classes to be indexed lazily.
     * The class data will not be obtained from the class loader; instead, the given
     * {@code classData} will be used.
     * <p>
     * The given class and all annotations found on it will be indexed.
     * Unlike the other {@code add} methods, superclasses will <em>not</em> be indexed.
     * <p>
     * If the same class name has already been added with <em>different</em> class data,
     * an exception is thrown.
     */
    public void add(String className, byte[] classData) {
        Objects.requireNonNull(className);
        Objects.requireNonNull(classData);

        // this shouldn't happen very often (hopefully...), but there is code out there
        // that suggests a class name passed here may be an internal name, not a binary name
        className = className.replace('/', '.');

        byte[] existingData = classesData.get(className);
        if (existingData != null && !Arrays.equals(existingData, classData)) {
            throw new IllegalArgumentException("Class " + className + " already present with different class data");
        }

        classNames.add(className);
        classesData.put(className, classData);
    }

    /**
     * Runs the indexing process. All classes added via {@code add()} are indexed
     * and the {@link Result} is returned. If a class is not found in the class loader,
     * an exception is thrown, unless it is a class of an annotation that is present
     * on one of the previously indexed classes.
     */
    public Result complete() {
        Indexer indexer = new Indexer();
        Set<DotName> missingAnnotations = new HashSet<>();

        Set<String> alreadySeen = new HashSet<>();

        List<String> current = null;
        List<String> next = new ArrayList<>(classNames);

        while (!next.isEmpty()) {
            current = next;
            next = new ArrayList<>();

            // feed classes to the `Indexer` in deterministic order
            Collections.sort(current);

            for (String className : current) {
                if (alreadySeen.contains(className)) {
                    continue;
                }

                byte[] classData = classesData.get(className);

                DotName superclassName;
                Set<DotName> annotationNames;

                ClassInfo classInfo = existingIndex.getClassByName(className);
                if (classInfo == null) {
                    try (InputStream stream = classData != null
                            ? new ByteArrayInputStream(classData)
                            : IoUtil.readClass(classLoader, className)) {
                        if (stream == null) {
                            throw new IllegalStateException("Failed to index: " + className
                                    + ", class not present in class loader: " + classLoader);
                        }

                        ClassSummary summary = indexer.indexWithSummary(stream);
                        alreadySeen.add(summary.name().toString());
                        superclassName = summary.superclassName();
                        annotationNames = summary.annotations();
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to index: " + className, e);
                    }
                } else {
                    alreadySeen.add(className);
                    superclassName = classInfo.superName();
                    annotationNames = classInfo.annotationsMap().keySet();
                }

                for (DotName annotationDotName : annotationNames) {
                    String annotationName = annotationDotName.toString();
                    if (!alreadySeen.contains(annotationName) && existingIndex.getClassByName(annotationDotName) == null) {
                        try (InputStream annotationStream = IoUtil.readClass(classLoader, annotationName)) {
                            if (annotationStream == null) {
                                missingAnnotations.add(annotationDotName);
                            } else {
                                next.add(annotationName);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to index: " + className, e);
                        }
                    }
                }
                if (classData == null && superclassName != null && !superclassName.equals(DotName.OBJECT_NAME)) {
                    next.add(superclassName.toString());
                }
            }
        }

        classNames.clear();
        classesData.clear();

        return new Result(indexer.complete(), missingAnnotations);
    }
}
