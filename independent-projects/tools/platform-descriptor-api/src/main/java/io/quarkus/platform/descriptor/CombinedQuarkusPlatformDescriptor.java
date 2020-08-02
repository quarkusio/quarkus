package io.quarkus.platform.descriptor;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;

/**
 * Platform descriptor that is composed of multiple platform descriptors.
 * The order in which descriptors are added is significant. Platform descriptor
 * added earlier dominate over those added later.
 */
public class CombinedQuarkusPlatformDescriptor implements QuarkusPlatformDescriptor {

    public static class Builder {

        private List<QuarkusPlatformDescriptor> platforms = new ArrayList<>();

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
            return new CombinedQuarkusPlatformDescriptor(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final QuarkusPlatformDescriptor master;
    private final List<QuarkusPlatformDescriptor> platforms;
    private List<Dependency> managedDeps;
    private List<Extension> extensions;
    private List<Category> categories;

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
    public List<Dependency> getManagedDependencies() {
        if (managedDeps != null) {
            return managedDeps;
        }
        final List<Dependency> deps = new ArrayList<>();
        final Set<DepKey> depKeys = new HashSet<>();
        for (QuarkusPlatformDescriptor platform : platforms) {
            for (Dependency dep : platform.getManagedDependencies()) {
                if (depKeys.add(new DepKey(dep))) {
                    deps.add(dep);
                }
            }
        }
        return managedDeps = deps;
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
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DepKey other = (DepKey) obj;
            if (artifactId == null) {
                if (other.artifactId != null)
                    return false;
            } else if (!artifactId.equals(other.artifactId))
                return false;
            if (classifier == null) {
                if (other.classifier != null)
                    return false;
            } else if (!classifier.equals(other.classifier))
                return false;
            if (groupId == null) {
                if (other.groupId != null)
                    return false;
            } else if (!groupId.equals(other.groupId))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }
    }
}
