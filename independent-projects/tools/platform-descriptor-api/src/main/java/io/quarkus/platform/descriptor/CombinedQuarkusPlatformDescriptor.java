package io.quarkus.platform.descriptor;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.maven.model.Dependency;

/**
 * Platform descriptor that is composed of multiple platform descriptors.
 * The order in which descriptors are added is significant. Platform descriptor
 * added earlier dominate over those added later.
 */
public class CombinedQuarkusPlatformDescriptor implements QuarkusPlatformDescriptor {

    public static class Builder {

        private final List<QuarkusPlatformDescriptor> platforms = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a platform descriptor.
         * The order in which descriptors are added is significant. Platform descriptor
         * added earlier dominate over those added later.
         *
         * @param platform platform descriptor to add
         * @return this builder instance
         */
        public Builder addPlatform(QuarkusPlatformDescriptor platform) {
            platforms.add(platform);
            return this;
        }

        public QuarkusPlatformDescriptor build() {
            if (platforms.size() == 1) {
                return platforms.get(0);
            }
            return new CombinedQuarkusPlatformDescriptor(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final QuarkusPlatformDescriptor master;
    private final List<QuarkusPlatformDescriptor> platforms;
    private List<Extension> extensions;
    private List<Category> categories;
    private Map<String, Object> metadata;

    private CombinedQuarkusPlatformDescriptor(Builder builder) {
        if (builder.platforms.isEmpty()) {
            throw new IllegalArgumentException("No platforms to combine");
        }
        master = builder.platforms.get(0);
        platforms = new ArrayList<>(builder.platforms);
    }

    @Override
    public String getBomGroupId() {
        return master.getBomGroupId();
    }

    @Override
    public String getBomArtifactId() {
        return master.getBomArtifactId();
    }

    @Override
    public String getBomVersion() {
        return master.getBomVersion();
    }

    @Override
    public String getQuarkusVersion() {
        return master.getQuarkusVersion();
    }

    @Override
    public Map<String, Object> getMetadata() {
        if (this.metadata != null) {
            return this.metadata;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = platforms.size() - 1; i >= 0; i--) {
            metadata.putAll(platforms.get(i).getMetadata());
        }
        return this.metadata = metadata;
    }

    @Override
    public List<Extension> getExtensions() {
        if (extensions != null) {
            return extensions;
        }
        final List<Extension> list = new ArrayList<>();
        final Set<DepKey> depKeys = new HashSet<>();
        for (QuarkusPlatformDescriptor platform : platforms) {
            for (Extension ext : platform.getExtensions()) {
                if (depKeys.add(new DepKey(ext.getGroupId(), ext.getArtifactId()))) {
                    list.add(ext);
                }
            }
        }
        return extensions = list;
    }

    @Override
    public List<Category> getCategories() {
        if (categories != null) {
            return categories;
        }
        final List<Category> list = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        for (QuarkusPlatformDescriptor platform : platforms) {
            for (Category cat : platform.getCategories()) {
                if (ids.add(cat.getId())) {
                    list.add(cat);
                }
            }
        }
        return categories = list;
    }

    @Override
    public String getTemplate(String name) {
        for (QuarkusPlatformDescriptor platform : platforms) {
            final String template = platform.getTemplate(name);
            if (template != null) {
                return template;
            }
        }
        return null;
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        for (QuarkusPlatformDescriptor platform : platforms) {
            try {
                return platform.loadResource(name, consumer);
            } catch (IOException e) {
                // ignore
            }
        }
        throw new IOException("Failed to locate resource " + name);
    }

    @Override
    public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
        for (QuarkusPlatformDescriptor platform : platforms) {
            try {
                return platform.loadResourceAsPath(name, consumer);
            } catch (IOException e) {
                // ignore
            }
        }
        throw new IOException("Failed to locate resource " + name);
    }

    private static class DepKey {
        final String groupId;
        final String artifactId;
        final String classifier;
        final String type;

        DepKey(Dependency dep) {
            this(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType());
        }

        DepKey(String groupId, String artifactId) {
            this(groupId, artifactId, null, null);
        }

        DepKey(String groupId, String artifactId, String classifier, String type) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DepKey)) {
                return false;
            }
            DepKey depKey = (DepKey) o;
            return Objects.equals(groupId, depKey.groupId) &&
                    Objects.equals(artifactId, depKey.artifactId) &&
                    Objects.equals(classifier, depKey.classifier) &&
                    Objects.equals(type, depKey.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId, classifier, type);
        }
    }
}
