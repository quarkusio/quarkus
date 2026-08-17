package io.quarkus.test.junit.classloading;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * Works out, before a test class is loaded for real, whether the post-discovery filters of the current JUnit launcher
 * request (tag exclusions and inclusions from surefire's {@code groups}/{@code excludedGroups}, Gradle's
 * {@code includeTags}/{@code excludeTags}, IDE tag expressions, ...) are going to exclude every single test in it.
 * <p>
 * The {@link FacadeClassLoader} has to decide whether to build a Quarkus application for a class at the moment JUnit
 * loads it, which is before JUnit applies those filters. Building an application for a class none of whose tests will
 * run wastes time and, more importantly, starts Dev Services (and requires a container runtime) for nothing; see
 * https://github.com/quarkusio/quarkus/issues/54435.
 * <p>
 * JUnit applies post-discovery filters to the leaves of the discovered tree (the individual tests), and prunes
 * containers that end up empty. A leaf's tags are its own {@code @Tag}s plus those of the enclosing class(es). This
 * evaluator does the same: it feeds each test method of the class (including inherited ones and those of
 * {@code @Nested} classes) with its full tag set to the request's own filter objects. This deliberately does not
 * reimplement any filter logic; and it errs on the side of caution, so whenever the answer is unclear the class is
 * treated as one that will run and an application is built for it, exactly as before.
 */
final class PostDiscoveryFilterEvaluator {

    private static final Logger log = Logger.getLogger(PostDiscoveryFilterEvaluator.class);

    private static final String JUPITER_ENGINE_ID = "junit-jupiter";

    // These are loaded with the same classloader as the classes being inspected, which may hold a different copy of
    // JUnit than the one this class sees; hence they cannot be referenced statically
    private final Class<? extends Annotation> tagAnnotation;
    private final Method tagAnnotationValue;
    private final Class<? extends Annotation> testableAnnotation;
    private final Class<? extends Annotation> nestedAnnotation;

    private volatile List<? extends PostDiscoveryFilter> filters = List.of();

    @SuppressWarnings("unchecked")
    PostDiscoveryFilterEvaluator(ClassLoader annotationLoader) throws ClassNotFoundException, NoSuchMethodException {
        tagAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass("org.junit.jupiter.api.Tag");
        tagAnnotationValue = tagAnnotation.getMethod("value");
        testableAnnotation = (Class<? extends Annotation>) annotationLoader
                .loadClass("org.junit.platform.commons.annotation.Testable");
        nestedAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass("org.junit.jupiter.api.Nested");
    }

    void setFilters(List<? extends PostDiscoveryFilter> filters) {
        this.filters = filters == null ? List.of() : List.copyOf(filters);
    }

    /**
     * @return {@code true} only when it is certain that the post-discovery filters exclude every test JUnit could run
     *         from {@code testClass}; {@code false} when any test may run, when no filters are set, or when the
     *         evaluation cannot be completed
     */
    boolean excludesEveryTestIn(Class<?> testClass) {
        List<? extends PostDiscoveryFilter> currentFilters = filters;
        if (currentFilters.isEmpty()) {
            return false;
        }
        try {
            Filter<TestDescriptor> filter = Filter.composeFilters(currentFilters);
            return excludesEveryTestIn(testClass, enclosingTags(testClass), uniqueIdOf(testClass), filter);
        } catch (Exception | LinkageError e) {
            log.debugf(e, "Could not evaluate the JUnit post-discovery filters for %s, assuming its tests will run",
                    testClass.getName());
            return false;
        }
    }

    private boolean excludesEveryTestIn(Class<?> clazz, Set<TestTag> inheritedTags, UniqueId classId,
            Filter<TestDescriptor> filter) throws Exception {
        Set<TestTag> classTags = union(inheritedTags, tagsOf(clazz));

        // Every kind of Jupiter test method (@Test, @TestFactory, @TestTemplate, @RepeatedTest, @ParameterizedTest,
        // ...) is meta-annotated with @Testable. Considering too many methods is harmless: it can only lead to keeping
        // the application, never to dropping it.
        for (Method method : AnnotationSupport.findAnnotatedMethods(clazz, testableAnnotation,
                HierarchyTraversalMode.TOP_DOWN)) {
            Set<TestTag> tags = union(classTags, tagsOf(method));
            if (filter.apply(new LeafTestDescriptor(classId, clazz, method, tags)).included()) {
                return false;
            }
        }
        for (Class<?> nested : ReflectionSupport.findNestedClasses(clazz,
                candidate -> AnnotationSupport.isAnnotated(candidate, nestedAnnotation))) {
            if (!excludesEveryTestIn(nested, classTags, classId.append("nested-class", nested.getSimpleName()),
                    filter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A {@code @Nested} class may be loaded on its own; its tests still carry the tags of the enclosing classes.
     */
    private Set<TestTag> enclosingTags(Class<?> clazz) throws Exception {
        Set<TestTag> tags = new LinkedHashSet<>();
        Class<?> enclosing = clazz;
        while (AnnotationSupport.isAnnotated(enclosing, nestedAnnotation) && enclosing.getEnclosingClass() != null) {
            enclosing = enclosing.getEnclosingClass();
            tags.addAll(tagsOf(enclosing));
        }
        return tags;
    }

    private UniqueId uniqueIdOf(Class<?> clazz) {
        if (AnnotationSupport.isAnnotated(clazz, nestedAnnotation) && clazz.getEnclosingClass() != null) {
            return uniqueIdOf(clazz.getEnclosingClass()).append("nested-class", clazz.getSimpleName());
        }
        return UniqueId.forEngine(JUPITER_ENGINE_ID).append("class", clazz.getName());
    }

    // Same rules as JUnit: repeatable, meta-annotations and (for classes) inheritance are honoured, invalid tags ignored
    private Set<TestTag> tagsOf(AnnotatedElement element) throws Exception {
        Set<TestTag> tags = new LinkedHashSet<>();
        for (Annotation tag : AnnotationSupport.findRepeatableAnnotations(element, tagAnnotation)) {
            String value = (String) tagAnnotationValue.invoke(tag);
            if (TestTag.isValid(value)) {
                tags.add(TestTag.create(value));
            }
        }
        return tags;
    }

    private static Set<TestTag> union(Set<TestTag> a, Set<TestTag> b) {
        if (b.isEmpty()) {
            return a;
        }
        Set<TestTag> union = new LinkedHashSet<>(a);
        union.addAll(b);
        return union;
    }

    /**
     * Stands in for the test descriptor JUnit would create for a test method. Tag filters only look at the tags;
     * the unique id and source are provided on a best-effort basis for filters that look at those.
     */
    private static final class LeafTestDescriptor implements TestDescriptor {

        private final UniqueId uniqueId;
        private final Method method;
        private final MethodSource source;
        private final Set<TestTag> tags;

        LeafTestDescriptor(UniqueId classId, Class<?> clazz, Method method, Set<TestTag> tags) {
            String parameterTypes = Arrays.stream(method.getParameterTypes()).map(Class::getName)
                    .collect(Collectors.joining(", "));
            this.uniqueId = classId.append("method", method.getName() + "(" + parameterTypes + ")");
            this.method = method;
            this.source = MethodSource.from(clazz, method);
            this.tags = Collections.unmodifiableSet(tags);
        }

        @Override
        public UniqueId getUniqueId() {
            return uniqueId;
        }

        @Override
        public String getDisplayName() {
            return method.getName() + "()";
        }

        @Override
        public Set<TestTag> getTags() {
            return tags;
        }

        @Override
        public Optional<TestSource> getSource() {
            return Optional.of(source);
        }

        @Override
        public Optional<TestDescriptor> getParent() {
            return Optional.empty();
        }

        @Override
        public void setParent(TestDescriptor parent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<? extends TestDescriptor> getChildren() {
            return Collections.emptySet();
        }

        @Override
        public void addChild(TestDescriptor descriptor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeChild(TestDescriptor descriptor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeFromHierarchy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }

        @Override
        public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId) {
            return this.uniqueId.equals(uniqueId) ? Optional.of(this) : Optional.empty();
        }
    }
}
