package io.quarkus.sbom;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A generic, ecosystem-agnostic descriptor for software components intended for SBOM contributions.
 * <p>
 * Component identity is represented by a {@link Purl} (Package URL), which encapsulates the
 * ecosystem type, namespace, name, and version. Each component has a {@code bomRef} that
 * uniquely identifies it within the SBOM (defaults to the PURL string).
 * <p>
 * Additional metadata includes:
 * <ul>
 * <li>Integrity hash: SRI-format hash from lock files for verification</li>
 * <li>Scope: runtime or development dependency classification</li>
 * <li>Description: human-readable component description</li>
 * </ul>
 * <p>
 * Dependencies are not stored on the component itself. Use {@link ComponentDependencies}
 * to represent the dependency graph separately, referencing components by their bom-ref.
 * <p>
 * This class follows the builder pattern. Instances are immutable and constructed via the {@link Builder}.
 */
public class ComponentDescriptor {

    public static final String SCOPE_RUNTIME = "runtime";
    public static final String SCOPE_DEVELOPMENT = "development";

    /**
     * Creates a new builder for constructing a ComponentDescriptor.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing ComponentDescriptor instances.
     */
    public static class Builder extends ComponentDescriptor {

        private Builder() {
            super();
        }

        /**
         * Creates a builder initialized with values from an existing ComponentDescriptor.
         *
         * @param component the component to copy values from
         */
        public Builder(ComponentDescriptor component) {
            super(component);
        }

        /**
         * Sets the Package URL identifying this component.
         *
         * @param purl the Package URL
         * @return this builder
         */
        public Builder setPurl(Purl purl) {
            this.purl = purl;
            if (this.bomRef == null) {
                this.bomRef = purl.toString();
            }
            return this;
        }

        /**
         * Sets an explicit bom-ref for this component.
         * If not set, defaults to {@code purl.toString()}.
         *
         * @param bomRef the bom-ref string
         * @return this builder
         */
        public Builder setBomRef(String bomRef) {
            this.bomRef = bomRef;
            return this;
        }

        /**
         * Sets the integrity hash.
         * <p>
         * This is typically an SRI hash from lock files (e.g., "sha512-abc123...").
         *
         * @param integrity the SRI hash, or null
         * @return this builder
         */
        public Builder setIntegrity(String integrity) {
            this.integrity = integrity;
            return this;
        }

        /**
         * Sets the package description.
         *
         * @param description a human-readable description, or null
         * @return this builder
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the dependency scope.
         * <p>
         * Common values are {@link #SCOPE_RUNTIME} and {@link #SCOPE_DEVELOPMENT}.
         *
         * @param scope the dependency scope, or null
         * @return this builder
         */
        public Builder setScope(String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Convenience method to set the scope to development.
         *
         * @return this builder
         */
        public Builder setDevelopmentScope() {
            return setScope(SCOPE_DEVELOPMENT);
        }

        /**
         * Sets the file system path to the component artifact.
         * Used for file hash computation in SBOM generators.
         *
         * @param path the file path, or null
         * @return this builder
         */
        public Builder setPath(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the distribution path (relative location within the application distribution).
         * Used for evidence/occurrence tracking in SBOM generators.
         *
         * @param distributionPath the distribution path, or null
         * @return this builder
         */
        public Builder setDistributionPath(String distributionPath) {
            this.distributionPath = distributionPath;
            return this;
        }

        /**
         * Sets the pedigree notes describing component modifications.
         *
         * @param pedigree the pedigree notes, or null
         * @return this builder
         */
        public Builder setPedigree(String pedigree) {
            this.pedigree = pedigree;
            return this;
        }

        /**
         * Sets whether this component is a top-level (direct) dependency of the
         * main application component.
         * <p>
         * Extensions should set this to {@code true} for components that represent
         * direct project dependencies (e.g., packages declared in {@code package.json}
         * rather than transitive dependencies).
         *
         * @param topLevel {@code true} if this is a top-level dependency
         * @return this builder
         */
        public Builder setTopLevel(boolean topLevel) {
            this.topLevel = topLevel;
            return this;
        }

        /**
         * Builds an immutable ComponentDescriptor from this builder.
         *
         * @return a new immutable ComponentDescriptor instance
         */
        public ComponentDescriptor build() {
            return ensureImmutable();
        }

        @Override
        protected ComponentDescriptor ensureImmutable() {
            return new ComponentDescriptor(this);
        }
    }

    // Fields are protected and non-final because Builder extends this class and
    // mutates them directly. The build() method copies values into a separate
    // immutable instance, so built descriptors are never mutated after construction.
    protected Purl purl;
    protected String bomRef;
    protected String integrity;
    protected String description;
    protected String scope;
    protected Path path;
    protected String distributionPath;
    protected String pedigree;
    protected boolean topLevel;

    private ComponentDescriptor() {
    }

    private ComponentDescriptor(ComponentDescriptor builder) {
        this.purl = Objects.requireNonNull(builder.purl, "purl is required");
        this.bomRef = builder.bomRef != null ? builder.bomRef : purl.toString();
        this.integrity = builder.integrity;
        this.description = builder.description;
        this.scope = builder.scope;
        this.path = builder.path;
        this.distributionPath = builder.distributionPath;
        this.pedigree = builder.pedigree;
        this.topLevel = builder.topLevel;
    }

    /**
     * Gets the Package URL identifying this component.
     *
     * @return the Package URL
     */
    public Purl getPurl() {
        return purl;
    }

    /**
     * Gets the bom-ref that uniquely identifies this component within the SBOM.
     * Defaults to the PURL string.
     *
     * @return the bom-ref
     */
    public String getBomRef() {
        return bomRef;
    }

    /**
     * Gets the package type (PURL type field).
     * <p>
     * Examples: "npm", "pypi", "maven", "cargo"
     *
     * @return the package type
     */
    public String getType() {
        return purl.getType();
    }

    /**
     * Gets the namespace (PURL namespace field).
     * <p>
     * For npm, this is the scope (e.g., "@babel").
     * For Maven, this is the groupId.
     *
     * @return the package namespace, or null
     */
    public String getNamespace() {
        return purl.getNamespace();
    }

    /**
     * Gets the package name.
     *
     * @return the package name
     */
    public String getName() {
        return purl.getName();
    }

    /**
     * Gets the resolved version.
     *
     * @return the package version, or null if not resolved
     */
    public String getVersion() {
        return purl.getVersion();
    }

    /**
     * Gets the integrity hash.
     * <p>
     * This is typically an SRI hash from lock files (e.g., "sha512-abc123...").
     *
     * @return the SRI hash, or null
     */
    public String getIntegrity() {
        return integrity;
    }

    /**
     * Gets the package description.
     *
     * @return a human-readable description, or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the dependency scope.
     * <p>
     * Common values are {@link #SCOPE_RUNTIME} and {@link #SCOPE_DEVELOPMENT}.
     *
     * @return the dependency scope, or null
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the file system path to the component artifact.
     *
     * @return the file path, or null
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the distribution path (relative location within the application distribution).
     *
     * @return the distribution path, or null
     */
    public String getDistributionPath() {
        return distributionPath;
    }

    /**
     * Gets the pedigree notes describing component modifications.
     *
     * @return the pedigree notes, or null
     */
    public String getPedigree() {
        return pedigree;
    }

    /**
     * Returns whether this component is a top-level (direct) dependency of the
     * main application component.
     * <p>
     * Top-level components appear as direct dependency edges from the main
     * application component in the generated SBOM. Components that are
     * transitive dependencies should not be marked as top-level.
     *
     * @return {@code true} if this is a top-level dependency
     */
    public boolean isTopLevel() {
        return topLevel;
    }

    protected ComponentDescriptor ensureImmutable() {
        return this;
    }
}
