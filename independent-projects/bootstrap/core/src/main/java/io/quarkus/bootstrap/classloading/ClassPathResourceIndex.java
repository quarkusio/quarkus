package io.quarkus.bootstrap.classloading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * We used to have a full index of the resources present in the classpath stored in ClassLoaderState of QuarkusClassLoader.
 * <p>
 * However it could lead to some major memory consumption, the index being more than 13 MB on real-life projects.
 * This was a problem especially since we also keep a list of the resources in each ClassPathElement.
 * <p>
 * Going through the whole list of ClassPathElements for each lookup is not a good option so we choose an intermediary approach
 * of having a lossy index referencing the start of the resource path to reduce the number of ClassPathElements to go through.
 * This index still returns exact references as we check the ClassPathElements themselves for the presence of the resource.
 * <p>
 * In the particular example above, we went down to less than 3 MB for the index, storing ~600 entries instead of 86000+ in the
 * mapping.
 * <p>
 * We try to be clever as to how we build the resource key to reduce the number of misses. It might need further tuning in the
 * future.
 * <p>
 * The general idea is to have this index:
 * <ul>
 * <li>store a mapping between the prefix of the resource and the {@code ClassPathElement}s that contain resources starting with
 * this prefix.</li>
 * <li>generate a prefix that is smart: we want to reduce the size of the index but we also want to reduce the impact on
 * performances (e.g. for {@code io.quarkus}, we keep one more segment, for versioned classes, we keep 3 more, we make sure
 * {@code META-INF/services/} files are fully indexed...)</li>
 * <li>store some additional information such as the transformed classes, banned resources, the classes that are considered
 * "local" to this class loader (excluding the jars). For these elements, the information is NOT lossy.</li>
 * </ul>
 * When interrogating the index, we get the candidate {@code ClassPathElement}s and we then check if the resource is actually in
 * a given {@code ClassPathElement} before actually considering it.
 * <p>
 * In most cases, the information that is in the index is that a resource with this prefix is in these {@code ClassPathElement}
 * but it might not be the precise resource we are looking for.
 */
public class ClassPathResourceIndex {

    private static final String IO_QUARKUS = "io/quarkus/";
    private static final String META_INF_MAVEN = "META-INF/maven/";
    private static final String META_INF_SERVICES = "META-INF/services/";
    private static final String META_INF_VERSIONS = "META-INF/versions/";

    private static final int MAX_SEGMENTS_DEFAULT = 3;
    private static final int MAX_SEGMENTS_IO_QUARKUS = 4;
    // let's go with default max segments + 3 for the META-INF/versions/<version> part
    private static final int MAX_SEGMENTS_META_INF_VERSIONS = MAX_SEGMENTS_DEFAULT + 3;

    private static final char SLASH = '/';
    private static final char DOT = '.';

    /**
     * This map is mapped by prefixes.
     */
    private final Map<String, ClassPathElement[]> resourceMapping;
    private final Map<String, ClassPathElement> transformedClasses;

    private final Set<String> relodableClasses;
    private final Set<String> parentFirstResources;
    private final Set<String> bannedResources;

    private ClassPathResourceIndex(Map<String, ClassPathElement[]> resourceMapping,
            Map<String, ClassPathElement> transformedClasses,
            Set<String> reloadableClasses,
            Set<String> parentFirstResources,
            Set<String> bannedResources) {
        this.resourceMapping = resourceMapping.isEmpty() ? Map.of() : Collections.unmodifiableMap(resourceMapping);
        this.transformedClasses = transformedClasses.isEmpty() ? Map.of() : transformedClasses;
        this.relodableClasses = reloadableClasses.isEmpty() ? Set.of() : Collections.unmodifiableSet(reloadableClasses);
        this.parentFirstResources = parentFirstResources.isEmpty() ? Set.of()
                : Collections.unmodifiableSet(parentFirstResources);
        this.bannedResources = bannedResources.isEmpty() ? Set.of() : Collections.unmodifiableSet(bannedResources);
    }

    public Set<String> getReloadableClasses() {
        return relodableClasses;
    }

    public boolean isParentFirst(String resource) {
        return parentFirstResources.contains(resource);
    }

    public boolean isBanned(String resource) {
        return bannedResources.contains(resource);
    }

    // it's tempting to use an Optional here but let's avoid the additional allocation
    public ClassPathElement getFirstClassPathElement(String resource) {
        ClassPathElement transformedClassClassPathElement = transformedClasses.get(resource);
        if (transformedClassClassPathElement != null) {
            return transformedClassClassPathElement;
        }

        ClassPathElement[] candidates = resourceMapping.get(getResourceKey(resource));
        if (candidates == null) {
            return null;
        }

        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].getProvidedResources().contains(resource)) {
                return candidates[i];
            }
        }

        return null;
    }

    public List<ClassPathElement> getClassPathElements(String resource) {
        ClassPathElement transformedClassClassPathElement = transformedClasses.get(resource);
        if (transformedClassClassPathElement != null) {
            return List.of(transformedClassClassPathElement);
        }

        ClassPathElement[] candidates = resourceMapping.get(getResourceKey(resource));
        if (candidates == null) {
            return List.of();
        }

        if (candidates.length == 1) {
            if (candidates[0].getProvidedResources().contains(resource)) {
                return List.of(candidates[0]);
            }

            return List.of();
        }

        List<ClassPathElement> classPathElements = new ArrayList<>(candidates.length);
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].getProvidedResources().contains(resource)) {
                classPathElements.add(candidates[i]);
            }
        }
        return classPathElements;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a key that tries to find a good compromise between reducing the size of the index and providing good
     * performances.
     * <p>
     * Probably something we will have to tweak for corner cases but let's try to keep it fast.
     */
    static String getResourceKey(String resource) {
        if (resource.isEmpty()) {
            return resource;
        }

        // we don't really care about this part, it can be slower
        if (resource.startsWith(META_INF_MAVEN)) {
            return META_INF_MAVEN;
        }
        if (resource.startsWith(META_INF_SERVICES)) {
            // for services, we want to reference the full path
            return resource;
        }

        int maxSegments;
        if (resource.startsWith(IO_QUARKUS)) {
            maxSegments = MAX_SEGMENTS_IO_QUARKUS;
        } else if (resource.startsWith(META_INF_VERSIONS)) {
            maxSegments = MAX_SEGMENTS_META_INF_VERSIONS;
        } else {
            maxSegments = MAX_SEGMENTS_DEFAULT;
        }

        int position = 0;
        for (int i = 0; i < maxSegments; i++) {
            int slashPosition = resource.indexOf(SLASH, position);
            if (slashPosition > 0) {
                position = slashPosition + 1;
            } else {
                if (i > 0 && resource.substring(position).indexOf(DOT) >= 0) {
                    break;
                } else {
                    return resource;
                }
            }
        }

        return resource.substring(0, position - 1);
    }

    public static class Builder {

        private static final String CLASS_SUFFIX = ".class";
        private static final ClassPathElement[] EMPTY_CLASSPATH_ELEMENT = new ClassPathElement[0];

        private final Map<String, ClassPathElement> transformedClassCandidates = new HashMap<>();
        private final Map<String, ClassPathElement> transformedClasses = new HashMap<>();
        private final Map<String, List<ClassPathElement>> resourceMapping = new HashMap<>();

        private final Set<String> reloadableClasses = new HashSet<>();
        private final Set<String> parentFirstResources = new HashSet<>();
        private final Set<String> bannedResources = new HashSet<>();

        public void scanClassPathElement(ClassPathElement classPathElement,
                BiConsumer<ClassPathElement, String> consumer) {
            for (String resource : classPathElement.getProvidedResources()) {
                consumer.accept(classPathElement, resource);
            }
        }

        public void addTransformedClassCandidate(ClassPathElement classPathElement, String resource) {
            transformedClassCandidates.put(resource, classPathElement);
        }

        public void addResourceMapping(ClassPathElement classPathElement, String resource) {
            if (classPathElement.containsReloadableResources() && resource.endsWith(CLASS_SUFFIX)) {
                reloadableClasses.add(resource);
            }

            ClassPathElement transformedClassClassPathElement = transformedClassCandidates.get(resource);
            if (transformedClassClassPathElement != null) {
                transformedClasses.put(resource, transformedClassClassPathElement);
                return;
            }

            String resourcePrefix = getResourceKey(resource);

            List<ClassPathElement> classPathElements = resourceMapping.get(resourcePrefix);
            if (classPathElements == null) {
                // default initial capacity of 10 is way too large
                classPathElements = new ArrayList<>(2);
                resourceMapping.put(resourcePrefix, classPathElements);
            }

            if (!classPathElements.contains(classPathElement)) {
                classPathElements.add(classPathElement);
            }
        }

        public void addParentFirstResource(String resource) {
            parentFirstResources.add(resource);
        }

        public void addBannedResource(String resource) {
            bannedResources.add(resource);
        }

        public ClassPathResourceIndex build() {
            Map<String, ClassPathElement[]> compactedResourceMapping = new HashMap<>(resourceMapping.size());
            for (Entry<String, List<ClassPathElement>> resourceMappingEntry : resourceMapping.entrySet()) {
                compactedResourceMapping.put(resourceMappingEntry.getKey(),
                        resourceMappingEntry.getValue().toArray(EMPTY_CLASSPATH_ELEMENT));
            }

            return new ClassPathResourceIndex(compactedResourceMapping, transformedClasses,
                    reloadableClasses, parentFirstResources, bannedResources);
        }
    }
}
