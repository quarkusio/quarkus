package io.quarkus.deployment.configuration.definition;

/**
 *
 */
public final class GroupDefinition extends ClassDefinition {
    GroupDefinition(final Builder builder) {
        super(builder);
    }

    public static final class Builder extends ClassDefinition.Builder {
        public Builder() {
        }

        public GroupDefinition build() {
            return new GroupDefinition(this);
        }
    }
}
